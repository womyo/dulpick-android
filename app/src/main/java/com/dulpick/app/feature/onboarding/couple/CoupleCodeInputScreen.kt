package com.dulpick.app.feature.onboarding.couple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dulpick.app.feature.onboarding.couple.component.CodeInputField
import com.dulpick.app.feature.onboarding.couple.component.CoupleTopBar
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppToast
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

@Composable
fun CoupleCodeInputScreen(
    state: CoupleState,
    onIntent: (CoupleIntent) -> Unit,
    toastMessage: String?,
    onToastDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // 연결 중에는 입력칸을 잠근다. 실패로 풀리면 다시 포커스를 준다
    LaunchedEffect(state.isConnecting) {
        if (!state.isConnecting && state.connectedCouple == null) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            CoupleTopBar(title = "커플 연결", onBack = { onIntent(CoupleIntent.BackClicked) })

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "코드를 입력해주세요",
                style = Typography.title1B,
                color = Colors.gray900,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(modifier = Modifier.height(32.dp))

            CodeInputField(
                code = state.code,
                onCodeChange = { onIntent(CoupleIntent.CodeChanged(it)) },
                length = CoupleState.CODE_LENGTH,
                enabled = !state.isConnecting,
                focusRequester = focusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            CtaContainer {
                AppButton(
                    text = "연결하기",
                    onClick = { onIntent(CoupleIntent.ConnectClicked) },
                    variant = AppButtonVariant.DARK,
                    size = AppButtonSize.XL,
                    fullWidth = true,
                    enabled = state.isConnectEnabled,
                )
            }
        }

        AppToast(
            message = toastMessage,
            onDismiss = onToastDismiss,
            bottomInset = 120,
        )
    }
}
