package com.dulpick.app.core.analytics

import androidx.compose.ui.Modifier
import com.microsoft.clarity.modifiers.clarityMask
import com.microsoft.clarity.modifiers.clarityUnmask

// 녹화에서 가릴 곳을 표시한다. 화면은 SDK 이름을 모르고 이 확장만 쓴다
// (iOS View+AnalyticsMasking 대응)

// 이 요소와 그 안쪽을 녹화에서 가린다. 가려진 내용은 서버로 올라가지 않는다
fun Modifier.analyticsMasked(): Modifier = clarityMask()

// isMasked 가 참일 때만 가린다
fun Modifier.analyticsMasked(isMasked: Boolean): Modifier = if (isMasked) clarityMask() else this

// 가려진 묶음 안에서 이 부분만 녹화되게 한다
fun Modifier.analyticsUnmasked(): Modifier = clarityUnmask()
