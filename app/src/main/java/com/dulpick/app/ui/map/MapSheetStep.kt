package com.dulpick.app.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 최소 단계가 있는 지도형 시트의 단계 기억.
// 지도를 손으로 끌면 최소로 내려가고, 필터·검색을 열면 중간으로 돌아온다
class MapSheetStep {
    // 제목부터 필터 줄 아래 끝까지
    var headerHeight by mutableStateOf(0.dp)

    // 시트 아래를 가리는 것(탭바·버튼 영역)의 높이
    var obstacleHeight by mutableStateOf(0.dp)

    var isMinimized by mutableStateOf(false)

    fun minimumHeight(obstacle: Dp = obstacleHeight): Dp? = mapSheetMinimumHeight(headerHeight, obstacle)

    fun peekHeight(collapsed: Dp, minimum: Dp?): Dp = if (isMinimized) minimum ?: collapsed else collapsed
}

@Composable
fun rememberMapSheetStep(): MapSheetStep = remember { MapSheetStep() }
