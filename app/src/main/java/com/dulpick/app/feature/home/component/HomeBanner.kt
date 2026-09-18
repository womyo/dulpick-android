package com.dulpick.app.feature.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.domain.course.DateCourseSummary
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private const val CONNECT_TITLE = "커플 연결 후 연인과 함께\n데이트 장소를 픽해보세요!"

// 홈 배너. 연결 여부·예정 일정 유무로 3분기 (iOS HomeBanner 대응)
@Composable
fun HomeBanner(
    isConnected: Boolean,
    upcomingSchedule: DateCourseSummary?,
    onConnectClick: () -> Unit,
    onCreateCourseClick: () -> Unit,
    onBannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when {
            !isConnected -> ConnectBanner(onConnectClick = onConnectClick)
            upcomingSchedule != null -> UpcomingBanner(schedule = upcomingSchedule, onClick = onBannerClick)
            else -> CourseBanner(onClick = onCreateCourseClick)
        }
    }
}

@Composable
private fun ConnectBanner(onConnectClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Colors.primaryPink),
    ) {
        // 크기 잡기 layer: 숨긴 타이틀 자리 + 이미지. 카드 높이를 이걸로 정한다
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = CONNECT_TITLE,
                style = Typography.title3SB,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 36.dp)
                    .alpha(0f),
            )
            Image(
                painter = painterResource(R.drawable.bannercoupleconnect),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // 보이는 layer: 실제 타이틀 + 버튼. 이미지 위에 겹쳐 그린다
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = CONNECT_TITLE,
                style = Typography.title3SB,
                color = Colors.commonWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 36.dp),
            )
            Text(
                text = "커플 연결하러가기",
                style = Typography.body1SB,
                color = Colors.textSecondary,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Colors.commonWhite)
                    .border(1.dp, Colors.gray200, RoundedCornerShape(12.dp))
                    .clickable(onClick = onConnectClick)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun CourseBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Colors.primaryPink)
            .clickable(onClick = onClick)
            .padding(start = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "이번주 데이트 코스를", style = Typography.title3SB, color = Colors.commonWhite)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = "함께 정해볼까요?", style = Typography.title3SB, color = Colors.commonWhite)
                Image(
                    painter = painterResource(R.drawable.arrow2),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Colors.commonWhite),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Image(painter = painterResource(R.drawable.bannerpeek), contentDescription = null)
    }
}

@Composable
private fun UpcomingBanner(schedule: DateCourseSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Colors.primaryPink)
            .clickable(onClick = onClick)
            .padding(start = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = schedule.title, style = Typography.title3SB, color = Colors.commonWhite)
            Text(
                text = "총 ${schedule.totalPlaceCount}곳의 장소",
                style = Typography.body2M,
                color = Colors.commonWhite,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Colors.commonWhite.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Image(painter = painterResource(R.drawable.bannercalendar), contentDescription = null)
    }
}
