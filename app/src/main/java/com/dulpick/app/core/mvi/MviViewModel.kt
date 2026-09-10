package com.dulpick.app.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// MVI 3요소 마커. 화면마다 이 셋을 구현한다
// State  = 화면에 계속 남는 상태 (iOS TCA State)
// Intent = 사용자/시스템 이벤트 (iOS TCA Action)
// SideEffect = 네비게이션·토스트 같은 1회성 신호 (iOS TCA delegate)
interface UiState
interface UiIntent
interface UiSideEffect

abstract class MviViewModel<S : UiState, I : UiIntent, E : UiSideEffect>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // 1회성 이벤트는 state 와 분리한다. Channel 이라 재구독해도 다시 발화되지 않는다
    private val _sideEffect = Channel<E>(Channel.BUFFERED)
    val sideEffect: Flow<E> = _sideEffect.receiveAsFlow()

    protected val currentState: S get() = _state.value

    // 화면이 이벤트를 보내는 유일한 입구. 하위 ViewModel 이 분기 처리
    abstract fun onIntent(intent: I)

    // 상태 부분 갱신 (data class copy 패턴)
    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    // 1회성 side effect 발행
    protected fun postSideEffect(effect: E) {
        viewModelScope.launch { _sideEffect.send(effect) }
    }
}
