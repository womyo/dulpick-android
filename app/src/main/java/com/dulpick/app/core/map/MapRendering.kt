package com.dulpick.app.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

// 카메라 도달 검증 재시도. 엔진 재개 직후엔 명령이 유실될 수 있어 앉을 때까지 다시 보낸다
private const val RENDER_MAX_TRIES = 10
private const val RENDER_VERIFY_MS = 300L
private const val CAMERA_EPSILON = 1e-4

// 핀과 카메라를 지도에 올린다.
// 핀과 카메라는 따로 본다 — 핀이 바뀌었다고 카메라를 다시 옮기면,
// 사용자가 지도를 움직여 둔 자리가 핀을 고를 때마다 되돌아간다
@Composable
internal fun RenderMap(
    kakaoMap: KakaoMap?,
    revision: Int,
    pins: List<MapPin>,
    camera: MapCamera?,
) {
    // 사용자가 지도를 직접 움직였는지. 움직였으면 카메라 재시도를 멈춘다
    val userMovedCamera = remember { AtomicBoolean(false) }

    LaunchedEffect(kakaoMap, revision, pins) {
        val map = kakaoMap ?: return@LaunchedEffect
        // 제스처로 끝난 카메라 이동만 사용자 조작이다(코드가 옮기면 Unknown 으로 온다)
        map.setOnCameraMoveEndListener { _, _, gestureType ->
            if (gestureType != GestureType.Unknown) userMovedCamera.set(true)
        }
        renderPins(map, pins)
    }

    LaunchedEffect(kakaoMap, revision, camera) {
        val map = kakaoMap ?: return@LaunchedEffect
        val target = camera ?: return@LaunchedEffect
        // 새 목표가 왔으니 앞서 사용자가 움직인 기록은 지운다
        userMovedCamera.set(false)
        moveUntilSettled(map, target, userMovedCamera)
    }
}

// 엔진 준비·재개 직후에는 카메라 명령이 유실될 수 있다(카메라가 기본 위치에 남아 빈 지역만 보임).
// 목표에 앉은 걸 확인할 때까지 재시도한다.
// 단 사용자가 지도를 움직인 뒤에는 멈춘다 — 안 그러면 방금 옮긴 화면을 목표로 되돌려 버린다
private suspend fun moveUntilSettled(
    map: KakaoMap,
    camera: MapCamera,
    userMovedCamera: AtomicBoolean,
) {
    repeat(RENDER_MAX_TRIES) {
        if (userMovedCamera.get()) return
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(
                LatLng.from(camera.center.latitude, camera.center.longitude),
                camera.zoomLevel,
            ),
        )
        delay(RENDER_VERIFY_MS)
        if (userMovedCamera.get() || map.arrivedAt(camera)) return
    }
}

// 카메라가 목표 좌표에 앉았는지
private fun KakaoMap.arrivedAt(camera: MapCamera): Boolean {
    val position = cameraPosition?.position ?: return false
    return abs(position.latitude - camera.center.latitude) < CAMERA_EPSILON &&
        abs(position.longitude - camera.center.longitude) < CAMERA_EPSILON
}

// 핀을 다시 올린다. 같은 styleId 는 스타일을 한 번만 만들어 재사용한다
private fun renderPins(map: KakaoMap, pins: List<MapPin>) {
    val manager = map.labelManager ?: return
    val layer = manager.layer ?: return
    layer.isClickable = true
    layer.removeAll()

    val stylesById = mutableMapOf<String, LabelStyles>()
    pins.forEach { pin ->
        val styles = stylesById[pin.styleId] ?: run {
            val style = pin.makeStyle()
            val added = manager.addLabelStyles(
                LabelStyles.from(
                    LabelStyle.from(style.bitmap).setAnchorPoint(style.anchorX, style.anchorY),
                ),
            ) ?: return@forEach
            stylesById[pin.styleId] = added
            added
        }
        val options = LabelOptions
            .from(LatLng.from(pin.coordinate.latitude, pin.coordinate.longitude))
            .setStyles(styles)
            .setRank(pin.rank)
        // 태그가 있어야 탭했을 때 어떤 핀인지 알아낸다. 없는 핀은 탭해도 무시된다
        pin.id?.let { options.setTag(it).setClickable(true) }
        layer.addLabel(options)
    }
}
