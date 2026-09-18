package com.dulpick.app.feature.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.domain.home.DateSchedule
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 지난 데이트 일정 카드. 제목 + 장소 수 캡슐 + 날짜 (iOS DateScheduleCard 대응)
@Composable
fun DateScheduleCard(schedule: DateSchedule, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(333.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Colors.bgSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = schedule.title, style = Typography.body1M, color = Colors.textPrimary)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Colors.commonWhite)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.mappin),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Colors.primaryPink),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "총 ${schedule.placeCount}곳의 장소",
                    style = Typography.body2M,
                    color = Colors.brandPrimary,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = schedule.date, style = Typography.caption1R, color = Colors.textTertiary)
        }
    }
}
