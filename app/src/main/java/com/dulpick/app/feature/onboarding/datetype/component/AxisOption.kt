package com.dulpick.app.feature.onboarding.datetype.component

import androidx.annotation.DrawableRes

// 한 축의 선택지 하나. 값·라벨·아이콘을 묶는다
data class AxisOption<T>(
    val value: T,
    val title: String,
    @DrawableRes val iconRes: Int,
)
