package com.dulpick.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography as MaterialTypography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// 라이트 단일. 다크·다이내믹 컬러 미지원 — Material 역할에 Dulpick 토큰을 고정 매핑
private val DulpickColorScheme = lightColorScheme(
    primary = Colors.brandPrimary,
    onPrimary = Colors.commonWhite,
    primaryContainer = Colors.brandSurface,
    background = Colors.bgDefault,
    onBackground = Colors.textPrimary,
    surface = Colors.bgDefault,
    onSurface = Colors.textPrimary,
    surfaceVariant = Colors.surfaceCard,
    error = Colors.statusError,
    onError = Colors.commonWhite,
    outline = Colors.borderDefault,
    outlineVariant = Colors.borderWeak,
)

// 기본 Text 가 Pretendard 를 쓰도록 Material 타이포에 매핑. 세밀한 토큰은 Typography 를 직접 쓴다
private val DulpickMaterialTypography = MaterialTypography(
    titleLarge = Typography.title3SB,
    titleMedium = Typography.headline,
    bodyLarge = Typography.body1M,
    bodyMedium = Typography.body2M,
    labelLarge = Typography.body2SB,
    labelMedium = Typography.caption1M,
    labelSmall = Typography.caption2M,
)

@Composable
fun DulpickTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DulpickColorScheme,
        typography = DulpickMaterialTypography,
        content = content,
    )
}
