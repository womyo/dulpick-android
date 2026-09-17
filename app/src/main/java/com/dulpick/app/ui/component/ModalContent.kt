package com.dulpick.app.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 공통 모달 카드 (iOS ModalContent 대응). 좌측 정렬 제목·본문, 선택적 이미지, 하단 수평 버튼.
// Dialog 로 감싸 쓴다. 버튼은 왼쪽 보조(OUTLINED) · 오른쪽 주(DARK) 순으로 균등 배치한다
@Composable
fun ModalContent(
    title: String,
    content: String,
    primaryTitle: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes image: Int? = null,
    secondaryTitle: String? = null,
    onSecondary: (() -> Unit)? = null,
    primaryEnabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Colors.commonWhite)
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 24.dp),
    ) {
        Text(text = title, style = Typography.title2B, color = Colors.gray900)

        Text(
            text = content,
            style = Typography.body1M,
            color = Colors.textSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (image != null) {
            // 카드 좌우 패딩만 두고 폭을 꽉 채운다. 높이는 비율대로
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (secondaryTitle != null && onSecondary != null) {
                AppButton(
                    text = secondaryTitle,
                    onClick = onSecondary,
                    variant = AppButtonVariant.OUTLINED,
                    size = AppButtonSize.XL,
                    fullWidth = true,
                    modifier = Modifier.weight(1f),
                )
            }
            AppButton(
                text = primaryTitle,
                onClick = onPrimary,
                variant = AppButtonVariant.DARK,
                size = AppButtonSize.XL,
                fullWidth = true,
                enabled = primaryEnabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
