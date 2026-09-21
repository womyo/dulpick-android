package com.dulpick.app.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

// 비동기 resume 완료를 기다리는 폴링 간격과 상한
private const val RESUME_POLL_MS = 50L
private const val RESUME_WAIT_MS = 3_000L

// 카카오 MapView 를 Compose 로 감싼다. 화면은 이걸 통해서만 지도를 쓴다 (iOS KakaoMapView 대응).
//
// 카카오 엔진은 finish 후 같은 프로세스에서 다시 start 하면 타일이 그려지지 않고,
// 뷰를 window 에서 뗐다 다시 붙여도 표면이 살아나지 않는다. 그래서 iOS 탭처럼
// 지도를 탭 컨테이너에 상시 붙여두고(다른 탭에선 가려짐+pause), 파괴하지 않는 구조를 전제로 한다.
// isActive 가 지도 탭 표시 여부다. 꺼지면 렌더링만 멈춘다
@Composable
fun KakaoMapView(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    onMapReady: (KakaoMap) -> Unit = {},
    // 렌더링이 실제로 재개된 뒤 불린다. pause 중에 쌓인 카메라·라벨 명령은 프레임을 못 잡으므로
    // 화면은 이 신호를 받아 다시 그려야 한다
    onResumed: () -> Unit = {},
    onMapError: (Throwable) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnReady by rememberUpdatedState(onMapReady)
    val currentOnResumed by rememberUpdatedState(onResumed)
    val currentOnError by rememberUpdatedState(onMapError)
    val currentActive by rememberUpdatedState(isActive)
    // 엔진 준비 여부. 준비 전에 pause 가 걸리면 초기화가 반쯤 멈춰 타일이 영영 안 그려진다
    var isReady by remember { mutableStateOf(false) }
    val mapView = remember {
        MapView(context).also {
            // 기본값은 뷰가 window 에서 떨어질 때 SDK 가 스스로 finish 한다. 정리는 dispose 한 번만 한다
            it.setFinishManually(true)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() = Unit
                    override fun onMapError(error: Exception) {
                        currentOnError(error)
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(kakaoMap: KakaoMap) {
                        isReady = true
                        currentOnReady(kakaoMap)
                    }
                },
            )
            mapView
        },
    )

    // 지도 탭 표시 여부에 따라 재생/멈춤. 엔진이 준비되기 전에 pause 를 걸면 초기화가 깨지므로
    // 준비 후에만 건다. resume 은 비동기라, 실제 재개를 확인한 뒤 onResumed 로 알린다
    LaunchedEffect(isActive, isReady) {
        if (!isReady) return@LaunchedEffect
        if (isActive) {
            mapView.post { mapView.resume() }
            withTimeoutOrNull(RESUME_WAIT_MS) {
                while (!mapView.isResumed) delay(RESUME_POLL_MS)
            }
            currentOnResumed()
        } else {
            mapView.pause()
        }
    }

    // 앱 전면/후면 전환에도 재생/멈춤을 맞춘다. 파괴(finish)는 이 컴포지션이 떠날 때 한 번만 한다
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (currentActive) mapView.post { mapView.resume() }
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }
}
