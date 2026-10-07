package com.dulpick.app.feature.notice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dulpick.app.domain.notice.Notice
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 공지 한 건을 읽는 화면. 목록에서 받은 값만 그리고 서버를 다시 안 부른다
// (iOS NoticeDetailView 대응)
@Composable
fun NoticeDetailScreen(notice: Notice, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize().background(Colors.bgDefault)) {
        NoticeTopBar(onBack = onBack)
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            // 제목과 날짜 사이 간격은 목록 줄과 같은 6 으로 둔다
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = notice.title, style = Typography.title2B, color = Colors.textPrimary)
                Text(
                    text = notice.createdAt.shortDateText(),
                    style = Typography.caption1R,
                    color = Colors.textTertiary,
                )
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Colors.bgSubtle))
            // 본문의 줄바꿈은 그대로 그린다. 주소가 섞여 와도 글자로만 둔다
            Text(
                text = notice.content,
                style = Typography.body1M,
                color = Colors.textSecondary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}
