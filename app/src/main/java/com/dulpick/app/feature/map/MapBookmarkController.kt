package com.dulpick.app.feature.map

import com.dulpick.app.core.analytics.Analytics
import com.dulpick.app.core.analytics.AnalyticsEvent
import com.dulpick.app.core.analytics.AnalyticsSaveSource
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceError
import com.dulpick.app.domain.place.PlaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 검색 결과 행의 북마크 토글 (iOS MapFeature.updateBookmark 갈래 대응).
// 지도 뷰모델이 커지지 않게 여기로 뺀다
@Suppress("TooGenericExceptionCaught", "LongParameterList")
class MapBookmarkController(
    private val repository: PlaceRepository,
    private val analytics: Analytics,
    private val scope: CoroutineScope,
    private val state: () -> MapState,
    private val update: (MapState.() -> MapState) -> Unit,
    private val effect: (MapSideEffect) -> Unit,
    // 저장·해제가 끝나면 저장 목록을 다시 읽는다
    private val reloadPlaces: () -> Unit,
) {
    // 먼저 표시를 뒤집고 서버 저장/해제 후 저장목록을 갱신, 실패하면 되돌린다
    fun toggle(place: Place) {
        val kakaoId = place.kakaoPlaceId ?: return
        val wasBookmarked = state().isBookmarked(place)
        // 저장으로 뒤집는 경우만 센다 (iOS placeSaveStarted 와 같다)
        if (!wasBookmarked) analytics.track(AnalyticsEvent.PlaceSaveStarted(AnalyticsSaveSource.IN_APP))
        update { copy(bookmarkOverrides = bookmarkOverrides + (kakaoId to !wasBookmarked)) }
        scope.launch {
            try {
                if (wasBookmarked) remove(kakaoId) else save(kakaoId, place.name)
                reloadPlaces()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                rollback(kakaoId, error)
            }
        }
    }

    // 해제엔 서버 placeId 를 쓴다. 저장 목록에서 못 찾으면 지울 대상이 없다
    private suspend fun remove(kakaoId: String) {
        val serverId = state().places
            .firstOrNull { it.place.kakaoPlaceId == kakaoId }?.place?.id?.toLongOrNull() ?: return
        repository.removePlace(serverId)
    }

    private suspend fun save(kakaoId: String, name: String) {
        repository.savePlace(kakaoId, name, null)
        analytics.track(AnalyticsEvent.PlaceSaveCompleted(AnalyticsSaveSource.IN_APP, analytics.userId))
    }

    private fun rollback(kakaoId: String, error: Throwable) {
        update { copy(bookmarkOverrides = bookmarkOverrides - kakaoId) }
        if (error == PlaceError.Unauthorized) {
            effect(MapSideEffect.SessionExpired)
        } else {
            effect(MapSideEffect.ShowToast("저장을 변경하지 못했어요", isError = true))
        }
    }
}
