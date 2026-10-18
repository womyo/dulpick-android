package com.dulpick.app.ui.map

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 지도형 시트(지도 탭·코스 결과·코스 장소 선택)가 서는 자리.
// 세 화면이 같은 값을 써야 해 계산을 여기 한 곳에 둔다
data class MapSheetMetrics(
    // 접힘(초기)에서 보이는 높이
    val collapsedHeight: Dp,
    // 접힘 시트 윗면의 y 픽셀. 지도가 목표 좌표를 이 자리의 초점에 놓는다
    val collapsedTopPx: Float,
    // 시트 내용 끝에 둘 여백. 시트가 화면 바닥까지 내려오는 경우의 내비게이션 버튼 자리다
    val contentBottomInset: Dp,
)

// 접힘 높이 비율. 기준은 시트를 담는 층의 높이다 —
// 앱이 edge-to-edge 라 그 값이 기기 화면 전체 높이와 같다.
// 기준은 iOS SheetLayout.collapsedScreenRatio 와 같고 값만 조금 더 높다
// (iOS 0.45, 실기기에서 상세 시트가 낮아 보여 올린 값)
const val COLLAPSED_SHEET_SCREEN_RATIO = 0.47f

// containerHeight 는 시트를 담는 층의 높이다.
//
// tabBarCollapsedHeight 는 탭바가 시트와 함께 올라오는 경우(지도 탭의 저장 목록)에만 넘긴다.
// 그때는 그 높이를 그대로 쓰고 내용 여백도 0 이다 — 시트 아래를 탭바가 덮는다.
// 넘기지 않으면 담는 층 높이에 비율을 곱해 띄우고, 시트가 화면 바닥까지 내려오므로
// 내비게이션 버튼 자리만큼 내용을 올린다
@Composable
fun mapSheetMetrics(containerHeight: Dp, tabBarCollapsedHeight: Dp? = null): MapSheetMetrics {
    val collapsedHeight = tabBarCollapsedHeight ?: (containerHeight * COLLAPSED_SHEET_SCREEN_RATIO)
    return MapSheetMetrics(
        collapsedHeight = collapsedHeight,
        collapsedTopPx = with(LocalDensity.current) { (containerHeight - collapsedHeight).toPx() },
        contentBottomInset = if (tabBarCollapsedHeight == null) {
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        } else {
            0.dp
        },
    )
}
