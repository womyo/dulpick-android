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

    override fun onIntent(intent: PlaceDetailIntent) {
        when (intent) {
            PlaceDetailIntent.OnAppear -> load()
            PlaceDetailIntent.AddressToggled -> setState { copy(isAddressExpanded = !isAddressExpanded) }
            PlaceDetailIntent.MapClicked -> openKakaoMap()
            PlaceDetailIntent.CloseClicked -> postSideEffect(PlaceDetailSideEffect.Close)
            PlaceDetailIntent.MoreContentsClicked -> loadContents()
            PlaceDetailIntent.RetryContentsClicked -> loadContents()
            is PlaceDetailIntent.ContentClicked -> postSideEffect(PlaceDetailSideEffect.OpenContent(intent.id))
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
                setState { copy(isLoading = false, isFailed = false, detail = detail) }
                // 서버 ID 가 있으면 관련 게시물을 이어서 부른다
                serverPlaceId = detail.place.id.toLongOrNull()
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
