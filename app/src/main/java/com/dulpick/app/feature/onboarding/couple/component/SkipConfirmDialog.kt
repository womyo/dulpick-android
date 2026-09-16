package com.dulpick.app.feature.onboarding.couple.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dulpick.app.R
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 건너뛰기 확인 모달. dim 탭/뒤로가기로 닫으면 "계속 연결"로 본다 (iOS ModalContent 대응)
@Composable
fun SkipConfirmDialog(
    onKeepConnecting: () -> Unit,
    onSkip: () -> Unit,
) {
    Dialog(onDismissRequest = onKeepConnecting) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Colors.commonWhite)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.coupleconnectmodal),
                contentDescription = null,
                modifier = Modifier.height(120.dp),
            )
            Text(
                text = "다음과 같은 둘픽의 기능들을\n사용할 수 없어요!",
                style = Typography.title3SB,
                color = Colors.gray900,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "괜찮으신가요?",
                style = Typography.body1M,
                color = Colors.textTertiary,
                textAlign = TextAlign.Center,
            )
            AppButton(
                text = "연결할게요",
                onClick = onKeepConnecting,
                variant = AppButtonVariant.DARK,
                size = AppButtonSize.XL,
                fullWidth = true,
                modifier = Modifier.padding(top = 8.dp),
            )
            AppButton(
                text = "네",
                onClick = onSkip,
                variant = AppButtonVariant.OUTLINED,
                size = AppButtonSize.XL,
                fullWidth = true,
            )
        }
    }
}
