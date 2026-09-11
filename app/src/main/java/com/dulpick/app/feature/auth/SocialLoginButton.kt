package com.dulpick.app.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.domain.auth.AuthProvider
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 카카오 노랑 / 구글 흰색+테두리
@Composable
fun SocialLoginButton(
    provider: AuthProvider,
    isLoading: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when (provider) {
        AuthProvider.KAKAO -> KakaoYellow
        AuthProvider.GOOGLE -> Colors.commonWhite
    }
    val iconRes = when (provider) {
        AuthProvider.KAKAO -> R.drawable.kakao
        AuthProvider.GOOGLE -> R.drawable.google
    }
    val title = when (provider) {
        AuthProvider.KAKAO -> "카카오로 로그인"
        AuthProvider.GOOGLE -> "구글로 로그인"
    }
    val shape = RoundedCornerShape(CORNER_RADIUS.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HEIGHT.dp)
            .clip(shape)
            .background(background)
            .then(
                if (provider == AuthProvider.GOOGLE) {
                    Modifier.border(1.dp, Colors.borderDefault, shape)
                } else {
                    Modifier
                },
            )
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = HORIZONTAL_PADDING.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(ICON_SIZE.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = ICON_SIZE.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ICON_SIZE.dp),
                        color = Colors.gray900,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = title, style = Typography.body1SB, color = Colors.gray900)
                }
            }
        }
    }
}

private val KakaoYellow = Color(0xFFFEE500)
private const val ICON_SIZE = 20
private const val HEIGHT = 56
private const val HORIZONTAL_PADDING = 24
private const val CORNER_RADIUS = 12
