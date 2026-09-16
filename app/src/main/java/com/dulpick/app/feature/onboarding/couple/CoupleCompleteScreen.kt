package com.dulpick.app.feature.onboarding.couple

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

@Composable
fun CoupleCompleteScreen(
    state: CoupleState,
    onIntent: (CoupleIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "커플 연결이 완료되었어요",
                style = Typography.title3SB,
                color = Colors.textTertiary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "지금 바로 둘픽을 즐겨볼까요?",
                style = Typography.title1B,
                color = Colors.gray900,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.coupleconnectcomplete),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
            NicknamePill(myNickname = state.myNickname, partnerNickname = state.partnerNickname)
        }

        Spacer(modifier = Modifier.weight(1f))

        CtaContainer {
            AppButton(
                text = "확인",
                onClick = { onIntent(CoupleIntent.CompleteClicked) },
                variant = AppButtonVariant.DARK,
                size = AppButtonSize.XL,
                fullWidth = true,
            )
        }
    }
}

@Composable
private fun NicknamePill(myNickname: String, partnerNickname: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Colors.primaryPink)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = myNickname, style = Typography.body1SB, color = Colors.commonWhite)
        Image(
            painter = painterResource(R.drawable.heart),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Colors.commonWhite),
            modifier = Modifier.size(20.dp),
        )
        Text(text = partnerNickname, style = Typography.body1SB, color = Colors.commonWhite)
    }
}
