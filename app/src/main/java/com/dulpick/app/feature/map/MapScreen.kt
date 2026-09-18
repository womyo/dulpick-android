package com.dulpick.app.feature.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dulpick.app.core.map.KakaoMapView
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory

// 지도 탭. 단계 0 은 지도 렌더링만 확인한다 (iOS MapView 대응, 저장장소·검색 등은 다음 단계)
private val SEOUL_CITY_HALL = LatLng.from(37.5666, 126.9784)
private const val DEFAULT_ZOOM_LEVEL = 15

@Composable
fun MapScreen() {
    KakaoMapView(
        modifier = Modifier.fillMaxSize(),
        onMapReady = { kakaoMap ->
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(SEOUL_CITY_HALL, DEFAULT_ZOOM_LEVEL))
        },
    )
}
