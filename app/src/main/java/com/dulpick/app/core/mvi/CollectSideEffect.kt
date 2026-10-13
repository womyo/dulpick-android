package com.dulpick.app.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

// 화면이 STARTED 상태일 때만 side effect 를 받아 처리한다. 백그라운드에서 네비게이션 튀는 걸 막는다
@Composable
fun <E : UiSideEffect> CollectSideEffect(
    sideEffect: Flow<E>,
    onEffect: (E) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // 수집자는 한 번만 만들어진다. 손잡이를 갱신해 두지 않으면 처음 붙잡은 콜백이
    // 그때의 상태를 들고 계속 쓰여, 뒤에 바뀐 값을 못 본다
    val currentOnEffect by rememberUpdatedState(onEffect)
    LaunchedEffect(sideEffect, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sideEffect.collect { currentOnEffect(it) }
        }
    }
}
