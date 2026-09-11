package com.dulpick.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors

// 온보딩 화면 하단 공통 CTA 영역. 상단 36 그라데이션 + 버튼 컨테이너를 함께 그린다 (iOS CTAContainer)
@Composable
fun CtaContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Colors.bgDefault.copy(alpha = 0f), Colors.bgDefault),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Colors.bgDefault)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}
