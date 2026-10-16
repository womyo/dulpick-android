package com.dulpick.app.feature.map

import com.dulpick.app.core.map.MapCamera
import com.dulpick.app.domain.place.Coordinate

// 카메라 상태 전이 (iOS MapFeature 의 state.camera 대입 지점들에 대응).
//
// 카메라는 "지금 상태로부터 계산" 하지 않고 이벤트가 생길 때마다 여기서 갈아 끼운다.
// 계산식으로 두면 마지막에 일어난 일이 아니라 우선순위가 이기게 되고,
// 내 위치처럼 지워지지 않는 값이 뒤에 열리는 상세·게시글의 카메라를 계속 가로챈다

// 그 좌표를 보여 준다. 줌을 적지 않으면 보던 배율을 지킨다 —
// 상세로 들어갈 때 확대하면 보던 지도가 달라져 방향을 잃는다 (iOS rowTapped·presentPlaceDetail 과 같은 규칙)
fun MapState.focusing(coordinate: Coordinate, zoomLevel: Int = camera.zoomLevel): MapState =
    copy(camera = MapCamera(coordinate, zoomLevel))

// 저장 목록 전체를 비추는 자리로 옮긴다 (iOS overview(of:) 대응)
fun MapState.withOverviewCamera(): MapState = copy(camera = overviewCamera())

// 상세·게시글·검색 중이 아닌, 저장 목록을 그대로 보고 있는 상태인지.
// 이때만 저장 목록 로딩·필터가 카메라를 옮긴다 (iOS mode == .saved && selectedPlace == nil)
val MapState.showsSavedOverview: Boolean
    get() = detail == null && postDetail == null && searchResult == null

private fun MapState.overviewCamera(): MapCamera {
    val first = filteredPlaces.firstOrNull()
    return when {
        first != null -> MapCamera(first.place.coordinate, MapCamera.MULTI_PLACE_ZOOM)
        places.isEmpty() -> MapCamera.SEOUL_CITY_HALL
        // 필터로 빈 건 저장이 없는 게 아니라서 보던 자리를 지킨다
        else -> camera
    }
}
