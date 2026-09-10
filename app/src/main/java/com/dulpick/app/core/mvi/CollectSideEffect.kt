package com.dulpick.app.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    LaunchedEffect(sideEffect, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sideEffect.collect(onEffect)
        }
    }
}
