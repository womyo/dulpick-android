package com.dulpick.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// iOS AppButtonStyle 포팅. variant×enabled 로 배경·글자색이 갈리고, size 로 패딩·모서리·타이포가 정해진다
enum class AppButtonVariant { PRIMARY, DARK, OUTLINED }

enum class AppButtonSize { XL, LG, MD, SM }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.PRIMARY,
    size: AppButtonSize = AppButtonSize.XL,
    fullWidth: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(size.radius))
            .background(backgroundColor(variant, enabled))
            .then(
                if (variant == AppButtonVariant.OUTLINED) {
                    Modifier.border(1.dp, Colors.borderDefault, RoundedCornerShape(size.radius))
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .alpha(if (isPressed) 0.9f else 1f)
            .padding(size.padding),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = size.typography,
            color = foregroundColor(variant, enabled),
            textAlign = TextAlign.Center,
        )
    }
}

private fun backgroundColor(variant: AppButtonVariant, enabled: Boolean): Color = when {
    !enabled && variant == AppButtonVariant.OUTLINED -> Colors.gray50
    !enabled -> Colors.gray100
    variant == AppButtonVariant.PRIMARY -> Colors.primaryPink
    variant == AppButtonVariant.DARK -> Colors.gray900
    else -> Colors.commonWhite
}

private fun foregroundColor(variant: AppButtonVariant, enabled: Boolean): Color = when {
    !enabled && variant == AppButtonVariant.OUTLINED -> Colors.gray300
    !enabled -> Colors.gray400
    variant == AppButtonVariant.OUTLINED -> Colors.textSecondary
    else -> Colors.textInverse
}

private val AppButtonSize.radius: Dp
    get() = if (this == AppButtonSize.SM) 8.dp else 12.dp

private val AppButtonSize.padding: PaddingValues
    get() = when (this) {
        AppButtonSize.XL -> PaddingValues(horizontal = 24.dp, vertical = 16.dp)
        AppButtonSize.LG -> PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        AppButtonSize.MD -> PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        AppButtonSize.SM -> PaddingValues(horizontal = 16.dp, vertical = 7.dp)
    }

private val AppButtonSize.typography: TextStyle
    get() = when (this) {
        AppButtonSize.XL, AppButtonSize.LG -> Typography.body1SB
        AppButtonSize.MD -> Typography.body1M
        AppButtonSize.SM -> Typography.caption1M
    }
