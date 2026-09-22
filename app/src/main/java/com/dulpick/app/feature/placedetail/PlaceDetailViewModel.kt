package com.dulpick.app.feature.placedetail

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.explore.ExploreError
import com.dulpick.app.domain.explore.ExploreRepository
import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceDetail
import com.dulpick.app.domain.place.PlaceError
import com.dulpick.app.domain.place.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CONTENTS_PAGE_SIZE = 4

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class PlaceDetailViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val exploreRepository: ExploreRepository,
) : MviViewModel<PlaceDetailState, PlaceDetailIntent, PlaceDetailSideEffect>(PlaceDetailState()) {

    // 진입 시 넘겨받은 장소·소스. Start 로 세팅한다 (iOS init(place:) 대응)
    private var initialPlace: Place? = null
    private var placeIdArg: Long = 0L
    private var query: String = ""

    private var started = false
    private var contentsPage = 0
    private var savedServerId: String? = null
    private var bookmarkToggled = false
    private var bookmarkJob: Job? = null

    override fun onIntent(intent: PlaceDetailIntent) {
        when (intent) {
            is PlaceDetailIntent.Start -> start(intent.place, intent.query, intent.serverPlaceId)
            PlaceDetailIntent.AddressToggled -> setState { copy(isAddressExpanded = !isAddressExpanded) }
            PlaceDetailIntent.MapClicked -> openKakaoMap()
            PlaceDetailIntent.CloseClicked -> postSideEffect(PlaceDetailSideEffect.Close)
            PlaceDetailIntent.MoreContentsClicked -> loadContents()
            PlaceDetailIntent.RetryContentsClicked -> loadContents()
            is PlaceDetailIntent.ContentClicked -> postSideEffect(PlaceDetailSideEffect.OpenContent(intent.id))
            PlaceDetailIntent.BookmarkClicked -> toggleBookmark()
        }
    }

    // 넘겨받은 장소로 즉시 그린 뒤 상세 API 로 부가정보를 덧입힌다
    private fun start(place: Place, query: String, serverPlaceId: Long?) {
        if (started) return
        started = true
        initialPlace = place
        this.query = query
        placeIdArg = serverPlaceId ?: 0L
        savedServerId = serverPlaceId?.takeIf { it > 0 }?.toString()
        setState {
            copy(
                place = place,
                isLoading = false,
                serverPlaceId = serverPlaceId?.takeIf { it > 0 },
                bookmarkCount = place.bookmarkCount,
            )
        }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val detail = try {
                if (placeIdArg > 0) placeRepository.placeDetail(placeIdArg)
                else placeRepository.kakaoPlaceDetail(initialPlace?.kakaoPlaceId.orEmpty(), query)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                // iOS 처럼 실패를 사용자에게 알리지 않는다. 넘겨받은 장소로 계속 보여준다.
                // 서버 ID 를 이미 알면(저장/게시글 진입) 게시물은 그래도 시도한다
                if (error == PlaceError.Unauthorized) postSideEffect(PlaceDetailSideEffect.SessionExpired)
                if (initialPlace == null) setState { copy(isLoading = false) }
                if (currentState.serverPlaceId != null) loadContents()
                return@launch
            }
            applyDetail(detail)
        }
    }

    // 상세 응답으로 장소·부가정보를 덧입힌다. 서버 ID 를 알면 게시물을 잇는다
    private fun applyDetail(detail: PlaceDetail) {
        savedServerId = detail.serverPlaceId?.toString() ?: savedServerId
        setState {
            copy(
                place = detail.place.withFallbackCoordinate(initialPlace?.coordinate),
                isLoading = false,
                serverPlaceId = detail.serverPlaceId ?: serverPlaceId,
                kakaoPlaceUrl = detail.kakaoPlaceUrl,
                bookmarkCount = detail.savedMemberCount,
                // 조회 중 북마크를 눌렀으면 응답의 savedByMe 는 낡은 값이라 덮지 않는다
                isBookmarked = if (bookmarkToggled) isBookmarked else detail.savedByMe,
            )
        }
        if (currentState.serverPlaceId != null) loadContents()
    }

    // 관련 게시물 다음 페이지. 로딩 중이면 무시한다
    private fun loadContents() {
        val id = currentState.serverPlaceId ?: return
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
                if (error == ExploreError.Unauthorized) postSideEffect(PlaceDetailSideEffect.SessionExpired)
            }
        }
    }

    // 상세 응답의 좌표가 비어(0,0) 오면 넘겨받은 장소의 좌표를 지킨다.
    // 그대로 덮으면 지도가 아무 데도 아닌 좌표로 이동한다(명세는 필수지만 nullable 로 온다)
    private fun Place.withFallbackCoordinate(fallback: Coordinate?): Place {
        val hasCoordinate = coordinate.latitude != 0.0 || coordinate.longitude != 0.0
        if (hasCoordinate || fallback == null) return this
        return copy(coordinate = fallback)
    }

    // 저장 버튼. 표시를 먼저 뒤집고 서버를 부른 뒤, 실패하면 되돌린다 (iOS toggleBookmark 대응)
    private fun toggleBookmark() {
        val place = currentState.place ?: return
        val wasBookmarked = currentState.isBookmarked
        if (!wasBookmarked && place.kakaoPlaceId == null) return

        bookmarkToggled = true
        setState {
            copy(
                isBookmarked = !wasBookmarked,
                bookmarkCount = (bookmarkCount + if (!wasBookmarked) 1 else -1).coerceAtLeast(0),
            )
        }
        // 앞 요청을 취소하지 않고 기다린다. 저장을 취소해 버리면 서버 ID 를 못 받아,
        // 곧바로 이어지는 해제가 카카오 ID 로 삭제를 부른다(엉뚱한 장소를 지울 수 있다)
        val previous = bookmarkJob
        bookmarkJob = viewModelScope.launch {
            previous?.join()
            try {
                if (wasBookmarked) removeBookmark(place) else addBookmark(place)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                rollbackBookmark(wasBookmarked)
                if (error == PlaceError.Unauthorized) postSideEffect(PlaceDetailSideEffect.SessionExpired)
            }
        }
    }

    private suspend fun removeBookmark(place: Place) {
        // 삭제엔 서버 placeId 를 쓴다. 없으면 place.id (저장 목록·홈에서 온 장소는 서버 ID 다)
        val removeId = (savedServerId ?: place.id).toLongOrNull() ?: return
        placeRepository.removePlace(removeId)
    }

    private suspend fun addBookmark(place: Place) {
        val kakaoId = place.kakaoPlaceId ?: return
        savedServerId = placeRepository.savePlace(kakaoId, place.name, null).place.id
    }

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
        val place = currentState.place ?: return
        val appUri = place.kakaoPlaceId?.let { "kakaomap://place?id=$it" }
        val webUrl = currentState.kakaoPlaceUrl
            ?: place.kakaoPlaceId?.let { "https://place.map.kakao.com/$it" }
        if (appUri == null && webUrl == null) return
        postSideEffect(PlaceDetailSideEffect.OpenKakaoMap(appUri, webUrl))
    }
}
