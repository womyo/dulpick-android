package com.dulpick.app.feature.onboarding.couple

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.feature.onboarding.couple.component.CoupleTopBar
import com.dulpick.app.feature.onboarding.couple.component.SkipConfirmDialog
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

@Composable
fun CoupleConnectScreen(
    state: CoupleState,
    onIntent: (CoupleIntent) -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault)
            .systemBarsPadding(),
    ) {
        CoupleTopBar(title = "커플 연결", onBack = { onIntent(CoupleIntent.BackClicked) })

        Spacer(modifier = Modifier.height(66.dp))

        Text(
            text = "커플 연결 시작하기",
            style = Typography.title1B,
            color = Colors.gray900,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Image(
            painter = painterResource(R.drawable.coupleconnectbefore),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(60.dp))

        CodeSection(state = state, onRetry = { onIntent(CoupleIntent.RetryInviteCodeClicked) })

        Spacer(modifier = Modifier.weight(1f))

        CtaContainer {
            if (state.showsSkip) {
                SkipButton(onClick = { onIntent(CoupleIntent.SkipClicked) })
            }
            AppButton(
                text = "코드 공유하기",
                onClick = { shareInviteCode(context, state) },
                variant = AppButtonVariant.OUTLINED,
                size = AppButtonSize.XL,
                fullWidth = true,
                enabled = state.inviteCode != null,
            )
            AppButton(
                text = "상대 코드로 연결하기",
                onClick = { onIntent(CoupleIntent.CodeInputClicked) },
                variant = AppButtonVariant.DARK,
                size = AppButtonSize.XL,
                fullWidth = true,
            )
        }
    }

    if (state.isSkipConfirmPresented) {
        SkipConfirmDialog(
            onKeepConnecting = { onIntent(CoupleIntent.SkipConfirmDismissed) },
            onSkip = { onIntent(CoupleIntent.SkipConfirmed) },
        )
    }
}

@Composable
private fun CodeSection(state: CoupleState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "내 코드",
            style = Typography.body1M,
            color = Colors.textSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Colors.gray100)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )

        when {
            state.inviteCode != null -> Text(
                text = state.inviteCode.value,
                style = Typography.largeTitleB,
                color = Colors.textPrimary,
            )
            state.inviteCodeError != null -> CodeRetry(message = state.inviteCodeError, onRetry = onRetry)
            state.isLoadingInviteCode || !state.hasAttemptedInviteCode -> CodePlaceholder()
            else -> CodeRetry(message = "코드를 불러오지 못했어요", onRetry = onRetry)
        }
    }
}

@Composable
private fun CodePlaceholder() {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Colors.gray100),
    )
}

@Composable
private fun CodeRetry(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "다시 시도",
            style = Typography.body1SB,
            color = Colors.textSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Colors.commonWhite)
                .clickable(onClick = onRetry)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        )
        Text(text = message, style = Typography.caption1M, color = Colors.gray400, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit) {
    Text(
        text = "다음에 연결할게요",
        style = Typography.body1SB,
        color = Colors.textTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clickable(onClick = onClick),
    )
}

// 코드가 있으면 shareUrl > code 순으로 공유한다
private fun shareInviteCode(context: android.content.Context, state: CoupleState) {
    val text = state.inviteCode?.shareUrl ?: state.inviteCode?.value ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
