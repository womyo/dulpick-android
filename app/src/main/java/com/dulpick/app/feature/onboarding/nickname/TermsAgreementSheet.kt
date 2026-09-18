package com.dulpick.app.feature.onboarding.nickname

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.core.terms.TermsType
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// NicknameScreen 이 닫기 불가 바텀시트로 올리는 내용 (iOS TermsAgreementSheet)
@Composable
fun TermsAgreementSheet(
    terms: List<TermsType>,
    agreedTerms: Set<TermsType>,
    onCheck: (TermsType) -> Unit,
    onDetail: (TermsType) -> Unit,
    onAgree: () -> Unit,
) {
    // 필수가 다 켜지면 그대로 닫는 버튼, 아니면 셋을 한 번에 켜는 버튼
    val allRequiredAgreed = terms.filter { it.isRequired }.all { it in agreedTerms }
    val agreeButtonTitle = if (allRequiredAgreed) "완료" else "모두 동의하기"

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = "잠깐만요!", style = Typography.title2B, color = Colors.gray900)
            Text(
                text = "서비스 이용을 위해 약관 동의가 필요해요",
                style = Typography.body1M,
                color = Colors.textTertiary,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            terms.forEach { item ->
                TermsRow(
                    terms = item,
                    isChecked = item in agreedTerms,
                    onCheck = { onCheck(item) },
                    onDetail = { onDetail(item) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CtaContainer {
            AppButton(
                text = agreeButtonTitle,
                onClick = onAgree,
                variant = AppButtonVariant.DARK,
                size = AppButtonSize.XL,
                fullWidth = true,
            )
        }
    }
}

// 체크 슬롯·라벨이 동의를 켜고 끈다. 오른쪽 화살표만 약관 내용을 연다
@Composable
private fun TermsRow(
    terms: TermsType,
    isChecked: Boolean,
    onCheck: () -> Unit,
    onDetail: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                // toggleable + Role.Checkbox 로 TalkBack 이 체크 상태를 안내한다. indication=null 로 ripple 은 끈다
                .toggleable(
                    value = isChecked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Checkbox,
                    onValueChange = { onCheck() },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(if (isChecked) R.drawable.checktrue else R.drawable.checkfalse),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = terms.agreementTitle,
                style = Typography.body1M,
                color = Colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDetail,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.arrowright),
                contentDescription = "약관 보기",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
