package com.dulpick.app.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography
import kotlinx.coroutines.delay

private const val TOAST_DURATION_MS = 3000L
private val TOAST_MIN_HEIGHT = 48.dp

// 하단에 잠깐 떴다 사라지는 알약 토스트 (iOS Toast 대응).
// 부모 Box 안에 겹쳐 그리고, message 가 바뀔 때마다 시간을 재서 자동으로 닫는다.
// iconRes 가 있으면(에러 토스트 등) 문구 앞에 아이콘을 붙인다.
// actionTitle 이 있으면 문구 뒤에 누를 수 있는 글자가 붙는다 (장소 삭제의 '실행취소')
@Composable
@Suppress("LongParameterList")
fun AppToast(
    message: String?,
    onDismiss: () -> Unit,
    bottomInset: Int,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    actionTitle: String? = null,
    onAction: () -> Unit = {},
) {
    message ?: return

    LaunchedEffect(message) {
        delay(TOAST_DURATION_MS)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = bottomInset.dp)
                .heightIn(min = TOAST_MIN_HEIGHT)
                .clip(CircleShape)
                .background(Colors.gray900.copy(alpha = 0.95f))
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(text = message, style = Typography.body1M, color = Colors.commonWhite)
            if (actionTitle != null) {
                Text(
                    text = actionTitle,
                    style = Typography.body2SB,
                    color = Colors.textTertiary,
                    modifier = Modifier
                        .clickable(onClick = onAction)
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}
