package com.dulpick.app.feature.map

import com.dulpick.app.domain.location.LocationAuthorization
import com.dulpick.app.domain.location.LocationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 내 위치 버튼 흐름 (iOS MapFeature.updateMap 의 위치 갈래 대응).
// 지도 뷰모델이 커지지 않게 여기로 뺀다
@Suppress("TooGenericExceptionCaught")
class MapLocationController(
    private val repository: LocationRepository,
    private val scope: CoroutineScope,
    private val update: (MapState.() -> MapState) -> Unit,
    private val effect: (MapSideEffect) -> Unit,
) {
    fun handle(intent: MapIntent) {
        when (intent) {
            MapIntent.CurrentLocationClicked -> start()
            is MapIntent.LocationPermissionResult -> onPermissionResult(intent)
            MapIntent.LocationModalDismissed -> update { copy(showsLocationPermissionModal = false) }
            else -> Unit
        }
    }

    // 권한이 있으면 바로 조회하고, 없으면 화면에 시스템 권한 요청을 맡긴다
    private fun start() {
        if (repository.authorization() == LocationAuthorization.AUTHORIZED) {
            load()
        } else {
            effect(MapSideEffect.RequestLocationPermission)
        }
    }

    // 허용이면 조회한다. 영구 거부면 설정 안내를 띄우고, 한 번 거부는 아무것도 하지 않는다
    // (방금 거부를 누른 사람에게 곧바로 설정 이동을 조르지 않는다 — iOS 와 같다)
    private fun onPermissionResult(result: MapIntent.LocationPermissionResult) {
        when {
            result.granted -> load()
            result.permanentlyDenied -> update { copy(showsLocationPermissionModal = true) }
        }
    }

    // 좌표를 한 번 읽어 카메라 목표로 둔다. 실패는 종류를 안 가리고 같은 토스트를 띄운다
    // (조회 도중 권한이 사라지는 경우는 드물고, 그때 모달을 띄우면 누른 적 없는 창이 뜬다)
    private fun load() {
        scope.launch {
            try {
                val coordinate = repository.currentCoordinate()
                update { copy(currentLocation = CurrentLocation(coordinate, System.nanoTime())) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                effect(MapSideEffect.ShowToast("현재 위치를 찾지 못했어요", isError = true))
            }
        }
    }
}
