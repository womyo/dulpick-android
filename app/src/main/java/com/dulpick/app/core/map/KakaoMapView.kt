package com.dulpick.app.core.map

import android.util.Log
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

private const val LOG_TAG = "KakaoMapView"

// 카카오 MapView 를 Compose 로 감싼다. 화면은 이걸 통해서만 지도를 쓴다 (iOS KakaoMapView 대응).
//
// 지도는 지도 화면의 바텀시트 본문 안에 둔다. 시트(Material Surface)가 터치를 흡수하므로
// 시트 밖(아래층)에 두면 지도가 드래그·확대·핀 탭을 전혀 받지 못한다.
// 그래서 지도 화면과 함께 만들어지고, 화면이 떠날 때 finish 로 정리한다.
// isActive 를 끄면 파괴하지 않고 렌더링만 멈춘다
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
    // 앱이 전면으로 돌아올 때마다 올린다. 아래 재개 처리를 다시 타게 해, 복귀 경로에서도
    // 재개 완료를 확인한 뒤 onResumed 를 부른다
    var foregroundCount by remember { mutableStateOf(0) }
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

                    // 앱키가 틀리거나 만료되거나 할당량을 넘기면 여기로 온다. 화면이 할 수 있는 게
                    // 없으니 사용자에게 알리지 않고 로그만 남긴다 (iOS authenticationFailed 와 동일).
                    // 앱키 값은 남기지 않는다
                    override fun onMapError(error: Exception) {
                        Log.e(LOG_TAG, "카카오 지도 오류", error)
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
    LaunchedEffect(isActive, isReady, foregroundCount) {
        if (!isReady) return@LaunchedEffect
        if (!isActive) {
            mapView.pause()
            return@LaunchedEffect
        }
        mapView.post { mapView.resume() }
        withTimeoutOrNull(RESUME_WAIT_MS) {
            while (!mapView.isResumed) delay(RESUME_POLL_MS)
        }
        // 재개를 확인하지 못했으면 알리지 않는다. 멈춘 엔진에 카메라·라벨을 다시 보내도 프레임을 못 잡는다
        if (mapView.isResumed) {
            currentOnResumed()
        } else {
            Log.w(LOG_TAG, "지도 렌더링 재개를 확인하지 못했다 (${RESUME_WAIT_MS}ms)")
        }
    }

    // 앱 전면/후면 전환에도 재생/멈춤을 맞춘다. 파괴(finish)는 이 컴포지션이 떠날 때 한 번만 한다.
    // 복귀는 여기서 직접 resume 하지 않고 신호만 올려, 위의 재개 완료 확인·알림 경로를 함께 탄다
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (currentActive) foregroundCount++
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
