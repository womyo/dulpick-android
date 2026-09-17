package com.dulpick.app.feature.onboarding.datetype

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.profile.ActivityLevel
import com.dulpick.app.domain.profile.DateFocus
import com.dulpick.app.domain.profile.DatePreference
import com.dulpick.app.domain.profile.DateTime
import com.dulpick.app.domain.profile.IndoorOutdoor

data class DateTypeState(
    val indoorOutdoor: IndoorOutdoor? = null,
    val activityLevel: ActivityLevel? = null,
    val dateTime: DateTime? = null,
    val dateFocus: DateFocus? = null,
    val isSubmitting: Boolean = false,
    val isTooltipPresented: Boolean = false,
    // 온보딩은 건너뛰기 노출, 마이페이지 수정은 숨김
    val showsSkip: Boolean = true,
) : UiState {

    // 4축이 전부 채워졌을 때만 만들어진다. 부분 선택은 저장 대상이 아니다
    val datePreference: DatePreference?
        get() {
            val indoor = indoorOutdoor ?: return null
            val activity = activityLevel ?: return null
            val time = dateTime ?: return null
            val focus = dateFocus ?: return null
            return DatePreference(indoor, activity, time, focus)
        }

    val isSaveEnabled: Boolean
        get() = datePreference != null && !isSubmitting
}

sealed interface DateTypeIntent : UiIntent {
    data class IndoorOutdoorSelected(val value: IndoorOutdoor) : DateTypeIntent
    data class ActivityLevelSelected(val value: ActivityLevel) : DateTypeIntent
    data class DateTimeSelected(val value: DateTime) : DateTypeIntent
    data class DateFocusSelected(val value: DateFocus) : DateTypeIntent
    data object TooltipToggled : DateTypeIntent
    data object TooltipDismissed : DateTypeIntent
    data object SaveClicked : DateTypeIntent
    data object SkipClicked : DateTypeIntent
}

sealed interface DateTypeSideEffect : UiSideEffect {
    // 저장/건너뛰기 모두 온보딩을 끝내고 메인으로 간다
    data object Finished : DateTypeSideEffect
    data object SessionExpired : DateTypeSideEffect
    // 1회성 토스트. state 에 담지 않고 side effect 로 흘린다
    data class ShowToast(val message: String) : DateTypeSideEffect
}
