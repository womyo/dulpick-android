package com.dulpick.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography
import kotlinx.coroutines.delay

private const val TOAST_DURATION_MS = 2000L

// 하단에 잠깐 떴다 사라지는 에러 토스트 (iOS ToastState 대응).
// 부모 Box 안에 겹쳐 그리고, message 가 바뀔 때마다 시간을 재서 자동으로 닫는다
@Composable
fun AppToast(
    message: String?,
    onDismiss: () -> Unit,
    bottomInset: Int,
    modifier: Modifier = Modifier,
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
        Text(
            text = message,
            style = Typography.body2M,
            color = Colors.commonWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = bottomInset.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Colors.gray900.copy(alpha = 0.9f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
