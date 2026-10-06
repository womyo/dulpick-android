package com.dulpick.app.feature.map

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceError
import com.dulpick.app.domain.place.PlaceRepository
import com.dulpick.app.domain.place.SavedPlace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class MapViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val coupleRepository: CoupleRepository,
) : MviViewModel<MapState, MapIntent, MapSideEffect>(MapState()) {

    // 별칭 편집 흐름. 상태·효과만 넘겨 받아 스스로 처리한다
    private val alias = MapAliasController(
        repository = placeRepository,
        scope = viewModelScope,
        state = { currentState },
        update = { reducer -> setState(reducer) },
        effect = { postSideEffect(it) },
    )

    // 낙관적으로 뺀 행. 서버 삭제가 실패하면 이 자리로 되돌린다
    private val pendingDeletes = mutableMapOf<String, PendingDelete>()

    override fun onIntent(intent: MapIntent) {
        when (intent) {
            MapIntent.OnAppear -> load()
            MapIntent.RetryClicked -> retryLoad()
            is MapIntent.OwnershipSelected -> setState { copy(selectedOwnership = intent.ownership) }
            is MapIntent.CategorySelected -> setState { copy(selectedCategory = intent.category) }
            is MapIntent.DeleteClicked -> delete(intent.id)
            is MapIntent.OpenSavedDetail -> openSavedDetail(intent.place)
            MapIntent.CloseDetail -> setState { closingPlaceDetail() }
            else -> onDelegatedIntent(intent)
        }
    }

    // 별칭·게시글·내 위치·검색은 각자 맡은 쪽으로 넘긴다 (onIntent 복잡도 분리)
    private fun onDelegatedIntent(intent: MapIntent) {
        when (intent) {
            is MapIntent.EditClicked, is MapIntent.AliasSaveClicked, MapIntent.AliasEditDismissed ->
                alias.handle(intent)
            is MapIntent.OpenPostDetail, is MapIntent.PostPlacesApplied,
            is MapIntent.OpenPostPlaceDetail, MapIntent.ClosePostDetail -> onPostIntent(intent)
            else -> onSearchIntent(intent)
        }
    }

    // 게시글 상세 인텐트 (onIntent 복잡도 분리). 상태 전이는 MapDetailTransitions 에 있다
    private fun onPostIntent(intent: MapIntent) {
        when (intent) {
            is MapIntent.OpenPostDetail ->
                setState { openingPostDetail(intent.contentId, intent.returnsOnClose) }
            is MapIntent.PostPlacesApplied -> setState { withPostPlaces(intent.places) }
            is MapIntent.OpenPostPlaceDetail -> setState { openingPostPlaceDetail(intent.placeId) }
            MapIntent.ClosePostDetail -> closePostDetail()
            else -> Unit
        }
    }

    // 다른 곳에서 들어온 게시글이면 닫을 때 온 곳으로 되돌리라고 알린다
    private fun closePostDetail() {
        if (currentState.postDetail?.returnsOnClose == true) {
            postSideEffect(MapSideEffect.PostDetailClosed)
        }
        setState { closingPostDetail() }
    }

    // 검색 결과·상세 진입 인텐트 (onIntent 복잡도 분리)
    private fun onSearchIntent(intent: MapIntent) {
        when (intent) {
            is MapIntent.OpenPlaceDetail -> openPlaceDetail(intent.place)
            is MapIntent.OpenContentDetail -> openContentDetail(intent.place, intent.query)
            is MapIntent.EnterSearchResult -> enterSearchResult(intent)
            is MapIntent.SearchRowClicked -> openSearchDetail(intent.place)
            is MapIntent.SearchBookmarkClicked -> toggleSearchBookmark(intent.place)
            MapIntent.ClearSearch -> setState {
                copy(searchResult = null, detail = null, bookmarkOverrides = emptyMap())
            }
            else -> Unit
        }
    }

    // 검색 화면에서 돌아온 결과로 검색 결과 모드에 들어간다. selectedIndex 있으면 그 장소 상세도 함께 연다
    private fun enterSearchResult(intent: MapIntent.EnterSearchResult) {
        val detail = intent.selectedIndex
            ?.let { intent.places.getOrNull(it) }
            ?.let { DetailTarget(it, query = intent.searchQuery, serverPlaceId = null) }
        setState {
            copy(
                searchResult = SearchResult(intent.searchQuery, intent.displayQuery, intent.places),
                detail = detail,
                bookmarkOverrides = emptyMap(),
            )
        }
    }

    // 검색 결과 리스트 행 탭 → 상세. 카카오 조회엔 원본 검색어를 쓴다
    private fun openSearchDetail(place: Place) {
        val query = currentState.searchResult?.searchQuery.orEmpty()
        setState { copy(detail = DetailTarget(place, query = query, serverPlaceId = null)) }
    }

    // 검색 결과 행 북마크. 먼저 뒤집고 서버 저장/해제 후 저장목록을 갱신, 실패하면 되돌린다
    private fun toggleSearchBookmark(place: Place) {
        val kakaoId = place.kakaoPlaceId ?: return
        val wasBookmarked = currentState.isBookmarked(place)
        setState { copy(bookmarkOverrides = bookmarkOverrides + (kakaoId to !wasBookmarked)) }
        viewModelScope.launch {
            try {
                if (wasBookmarked) {
                    val serverId = currentState.places
                        .firstOrNull { it.place.kakaoPlaceId == kakaoId }?.place?.id?.toLongOrNull()
                    if (serverId != null) placeRepository.removePlace(serverId)
                } else {
                    placeRepository.savePlace(kakaoId, place.name, null)
                }
                loadPlaces()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(bookmarkOverrides = bookmarkOverrides - kakaoId) }
                if (error == PlaceError.Unauthorized) {
                    postSideEffect(MapSideEffect.SessionExpired)
                } else {
                    postSideEffect(MapSideEffect.ShowToast("저장을 변경하지 못했어요", isError = true))
                }
            }
        }
    }

    // 저장 장소는 서버 placeId 로 조회한다(검색어 불필요)
    private fun openSavedDetail(place: SavedPlace) {
        val serverPlaceId = place.place.id.toLongOrNull()
        setState { copy(detail = DetailTarget(place.place, query = "", serverPlaceId = serverPlaceId)) }
    }

    // 탐색 검색에서 온 장소. 상세 전용(content) 모드 — 검색바 없이 그 장소 핀+상세만 (iOS mode=.content 대응)
    private fun openContentDetail(place: Place, query: String) {
        setState {
            copy(
                searchResult = null,
                detail = DetailTarget(place, query = query, serverPlaceId = null, contentMode = true),
            )
        }
    }

    // 홈 등에서 넘어온 저장 장소. 검색 결과 모드가 아니라 저장 모드에서 상세만 연다
    private fun openPlaceDetail(place: Place) {
        setState {
            copy(
                searchResult = null,
                detail = DetailTarget(place, query = "", serverPlaceId = place.id.toLongOrNull()),
            )
        }
    }

    // 목록에서 먼저 빼고 서버를 부른다. 실패하면 원래 자리로 되돌리고 토스트를 띄운다 (iOS removeSavedPlace 대응)
    private fun delete(id: String) {
        val index = currentState.places.indexOfFirst { it.id == id }
        if (index < 0) return
        val removed = currentState.places[index]
        val placeId = removed.place.id.toLongOrNull() ?: return
        pendingDeletes[id] = PendingDelete(removed, index)
        setState { copy(places = places.filterNot { it.id == id }) }
        viewModelScope.launch {
            try {
                placeRepository.removePlace(placeId)
                pendingDeletes.remove(id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                rollbackDelete(id)
                if (error == PlaceError.Unauthorized) {
                    postSideEffect(MapSideEffect.SessionExpired)
                } else {
                    postSideEffect(MapSideEffect.ShowToast("저장을 취소하지 못했어요", isError = true))
                }
            }
        }
    }

    // 뺀 자리로 되돌린다. 되돌리는 사이 목록을 다시 받아 이미 있으면 중복으로 넣지 않는다
    private fun rollbackDelete(id: String) {
        val pending = pendingDeletes.remove(id) ?: return
        if (currentState.places.any { it.id == id }) return
        setState {
            val restored = places.toMutableList().apply { add(pending.index.coerceIn(0, size), pending.place) }
            copy(places = restored)
        }
    }

    private data class PendingDelete(val place: SavedPlace, val index: Int)

    private fun load() {
        loadPlaces()
        loadCoupleConnection()
    }

    // 실패를 다시 시도한다. 빈 목록으로 두지 않고 로딩부터 다시 보여준다
    private fun retryLoad() {
        setState { copy(isLoading = true, loadFailed = false) }
        load()
    }

    private fun loadPlaces() {
        viewModelScope.launch {
            try {
                val places = placeRepository.savedPlaces()
                setState { copy(places = places, isLoading = false, loadFailed = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                // 실패를 빈 목록으로 보여주면 네트워크 장애가 '저장한 장소 없음'처럼 보인다.
                // 전역 에러(인증 만료)만 위로 올리고, 나머지는 화면에 실패로 남긴다 (iOS loadState.failed 대응)
                setState { copy(isLoading = false, loadFailed = true) }
                if (error == PlaceError.Unauthorized) {
                    postSideEffect(MapSideEffect.SessionExpired)
                } else {
                    postSideEffect(MapSideEffect.ShowToast("장소를 불러오지 못했어요", isError = true))
                }
            }
        }
    }

    // 소유자 필터 노출 여부. 실패하면 미연동으로 두고 필터를 감춘다
    private fun loadCoupleConnection() {
        viewModelScope.launch {
            try {
                val connected = coupleRepository.current()?.connected ?: false
                setState { copy(isCoupleConnected = connected) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isCoupleConnected = false) }
            }
        }
    }
}
