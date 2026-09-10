package com.dulpick.app.ui.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dulpick.app.R

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

// iOS Typography 토큰 이식. lineHeight = size×배수, letterSpacing = size 비율(.em), 굵기는 Pretendard 4종
// 자간 -0.02 는 큰 글씨(16↑ 시안 1.5 행간), -0.01 은 작은 글씨(14↓ 1.4 행간)
private fun pretendard(
    weight: FontWeight,
    size: Int,
    lineHeight: Double,
    letterSpacing: Double,
): TextStyle = TextStyle(
    fontFamily = Pretendard,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.em,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

object Typography {
    val largeTitleR = pretendard(FontWeight.Normal, 32, 48.0, -0.02)
    val largeTitleB = pretendard(FontWeight.Bold, 32, 48.0, -0.02)

    val title1R = pretendard(FontWeight.Normal, 28, 42.0, -0.02)
    val title1B = pretendard(FontWeight.Bold, 28, 42.0, -0.02)

    val title2R = pretendard(FontWeight.Normal, 22, 33.0, -0.02)
    val title2B = pretendard(FontWeight.Bold, 22, 33.0, -0.02)

    val title3R = pretendard(FontWeight.Normal, 20, 30.0, -0.02)
    val title3SB = pretendard(FontWeight.SemiBold, 20, 30.0, -0.02)

    val headline = pretendard(FontWeight.SemiBold, 18, 27.0, -0.02)

    val body1M = pretendard(FontWeight.Medium, 16, 24.0, -0.02)
    val body1SB = pretendard(FontWeight.SemiBold, 16, 24.0, -0.02)

    val body2M = pretendard(FontWeight.Medium, 14, 19.6, -0.01)
    val body2SB = pretendard(FontWeight.SemiBold, 14, 19.6, -0.01)

    val caption1R = pretendard(FontWeight.Normal, 13, 18.2, -0.01)
    val caption1M = pretendard(FontWeight.Medium, 13, 18.2, -0.01)

    val caption2R = pretendard(FontWeight.Normal, 12, 16.8, -0.01)
    val caption2M = pretendard(FontWeight.Medium, 12, 16.8, -0.01)
}
