package com.dulpick.app.feature.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 홈 상단 헤더. 브랜드마크 + 닉네임(연결 시 ♥ 파트너명) + 달력 버튼 (iOS HomeHeader 대응)
@Composable
fun HomeHeader(nickname: String, partnerName: String?, onCalendarClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.brandmark),
            contentDescription = null,
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = nickname, style = Typography.body2M, color = Colors.textInverse)
            if (partnerName != null) {
                Image(
                    painter = painterResource(R.drawable.heart),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Colors.textInverse),
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(20.dp),
                )
                Text(text = partnerName, style = Typography.body2M, color = Colors.textInverse)
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Colors.gray800)
                .clickable(onClick = onCalendarClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.date_calendar),
                contentDescription = "캘린더",
                colorFilter = ColorFilter.tint(Colors.textInverseTertiary),
            )
        }
    }
}
