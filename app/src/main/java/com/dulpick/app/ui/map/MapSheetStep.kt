package com.dulpick.app.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 최소 단계 높이를 내는 데 필요한 두 값을 재어 둔다
class MapSheetStep {
    // 제목부터 필터 줄 아래 끝까지
    var headerHeight by mutableStateOf(0.dp)

    // 시트 아래를 가리는 것(탭바·버튼 영역)의 높이
    var obstacleHeight by mutableStateOf(0.dp)

    fun minimumHeight(obstacle: Dp = obstacleHeight, gap: Dp): Dp? =
        mapSheetMinimumHeight(headerHeight, obstacle, gap)
}

@Composable
fun rememberMapSheetStep(): MapSheetStep = remember { MapSheetStep() }
