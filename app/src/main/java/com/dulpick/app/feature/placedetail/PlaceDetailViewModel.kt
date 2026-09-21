package com.dulpick.app.feature.placedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.explore.ExploreError
import com.dulpick.app.domain.explore.ExploreRepository
import com.dulpick.app.domain.place.PlaceDetail
import com.dulpick.app.domain.place.PlaceError
import com.dulpick.app.domain.place.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CONTENTS_PAGE_SIZE = 4

// nav 인자: 검색 결과는 카카오 ID + 검색어, 저장/게시글 장소는 서버 placeId
const val ARG_DETAIL_PLACE_ID = "placeId"
const val ARG_DETAIL_KAKAO_ID = "kakaoPlaceId"
const val ARG_DETAIL_QUERY = "query"

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class PlaceDetailViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val exploreRepository: ExploreRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<PlaceDetailState, PlaceDetailIntent, PlaceDetailSideEffect>(PlaceDetailState()) {

    private val placeId: Long = savedStateHandle.get<Long>(ARG_DETAIL_PLACE_ID) ?: 0L
    private val kakaoPlaceId: String = savedStateHandle.get<String>(ARG_DETAIL_KAKAO_ID).orEmpty()
    private val query: String = savedStateHandle.get<String>(ARG_DETAIL_QUERY).orEmpty()
    private var started = false
    // 게시물 조회에 쓰는 서버 장소 ID(있을 때만 게시물이 보인다), 다음 페이지 번호
    private var serverPlaceId: Long? = null
    private var contentsPage = 0
    // 저장 응답이 준 서버 placeId. 검색 장소는 place.id 가 kakaoId 라 삭제엔 이걸 쓴다
    private var savedServerId: String? = null
    private var bookmarkJob: Job? = null

    override fun onIntent(intent: PlaceDetailIntent) {
        when (intent) {
            PlaceDetailIntent.OnAppear -> load()
            PlaceDetailIntent.AddressToggled -> setState { copy(isAddressExpanded = !isAddressExpanded) }
            PlaceDetailIntent.MapClicked -> openKakaoMap()
            PlaceDetailIntent.CloseClicked -> postSideEffect(PlaceDetailSideEffect.Close)
            PlaceDetailIntent.MoreContentsClicked -> loadContents()
            PlaceDetailIntent.RetryContentsClicked -> loadContents()
            is PlaceDetailIntent.ContentClicked -> postSideEffect(PlaceDetailSideEffect.OpenContent(intent.id))
            PlaceDetailIntent.BookmarkClicked -> toggleBookmark()
        }
    }

    private fun load() {
        if (started) return
        started = true
        viewModelScope.launch {
            try {
                val detail = if (placeId > 0) {
                    placeRepository.placeDetail(placeId)
                } else {
                    placeRepository.kakaoPlaceDetail(kakaoPlaceId, query)
                }
                setState {
                    copy(
                        isLoading = false,
                        isFailed = false,
                        detail = detail,
                        isBookmarked = detail.savedByMe,
                        bookmarkCount = detail.savedMemberCount,
                    )
                }
                // 서버 ID 가 있으면 저장 목록의 삭제 경로에 쓰고, 관련 게시물도 이어서 부른다
                serverPlaceId = detail.place.id.toLongOrNull()
                savedServerId = serverPlaceId?.toString()
                if (serverPlaceId != null) loadContents()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isLoading = false, isFailed = true) }
                if (error == PlaceError.Unauthorized) postSideEffect(PlaceDetailSideEffect.SessionExpired)
            }
        }
    }

    // 관련 게시물 다음 페이지. 로딩 중이면 무시한다
    private fun loadContents() {
        val id = serverPlaceId ?: return
        if (currentState.contentsLoad == ContentsLoad.LOADING && currentState.contents.isNotEmpty()) return
        setState { copy(contentsLoad = ContentsLoad.LOADING) }
        val page = contentsPage
        viewModelScope.launch {
            try {
                val result = exploreRepository.placeContents(id, page, CONTENTS_PAGE_SIZE)
                contentsPage = page + 1
                setState {
                    copy(
                        contents = (contents + result.items).distinctBy { it.id },
                        hasNextContents = result.hasNext,
                        contentsLoad = ContentsLoad.LOADED,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(contentsLoad = ContentsLoad.FAILED) }
                // 게시물 조회는 Explore 계층이라 ExploreError 로 온다
                if (error == ExploreError.Unauthorized) postSideEffect(PlaceDetailSideEffect.SessionExpired)
            }
        }
    }

    // 저장 버튼. 표시를 먼저 뒤집고 서버를 부른 뒤, 실패하면 되돌린다 (iOS toggleBookmark 대응)
    private fun toggleBookmark() {
        val detail = currentState.detail ?: return
        val wasBookmarked = currentState.isBookmarked
        // 저장하려는데 카카오 식별자가 없으면 부를 수 없다
        if (!wasBookmarked && detail.place.kakaoPlaceId == null) return

        // 낙관적으로 표시·카운트를 뒤집는다(0 에서 끄면 음수가 안 되게 막는다)
        setState {
            copy(
                isBookmarked = !wasBookmarked,
                bookmarkCount = (bookmarkCount + if (!wasBookmarked) 1 else -1).coerceAtLeast(0),
            )
        }
        bookmarkJob?.cancel()
        bookmarkJob = viewModelScope.launch {
            try {
                if (wasBookmarked) removeBookmark(detail) else addBookmark(detail)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                rollbackBookmark(wasBookmarked)
                if (error == PlaceError.Unauthorized) postSideEffect(PlaceDetailSideEffect.SessionExpired)
            }
        }
    }

    private suspend fun removeBookmark(detail: PlaceDetail) {
        // 삭제엔 서버 placeId 를 쓴다. 없으면 place.id (검색 장소는 kakaoId 일 수 있음)
        val removeId = (savedServerId ?: detail.place.id).toLongOrNull() ?: return
        placeRepository.removePlace(removeId)
    }

    private suspend fun addBookmark(detail: PlaceDetail) {
        val kakaoId = detail.place.kakaoPlaceId ?: return
        savedServerId = placeRepository.savePlace(kakaoId, detail.place.name, null).place.id
    }

    // 서버 실패 시 표시·카운트를 원래대로 되돌린다
    private fun rollbackBookmark(wasBookmarked: Boolean) {
        setState {
            copy(
                isBookmarked = wasBookmarked,
                bookmarkCount = (bookmarkCount + if (wasBookmarked) 1 else -1).coerceAtLeast(0),
            )
        }
    }

    // 카카오맵 앱(kakaomap://) 우선, 없으면 웹. 웹은 서버가 준 kakaoPlaceUrl 이 먼저 (iOS 로직 대응)
    private fun openKakaoMap() {
        val detail = currentState.detail ?: return
        val appUri = detail.place.kakaoPlaceId?.let { "kakaomap://place?id=$it" }
        val webUrl = detail.kakaoPlaceUrl
            ?: detail.place.kakaoPlaceId?.let { "https://place.map.kakao.com/$it" }
        if (appUri == null && webUrl == null) return
        postSideEffect(PlaceDetailSideEffect.OpenKakaoMap(appUri, webUrl))
    }
}

// 앱/웹 어느 쪽이든 열 곳이 있는지 (지도 버튼 활성 판단)
fun PlaceDetail.canOpenKakaoMap(): Boolean = place.kakaoPlaceId != null || kakaoPlaceUrl != null
