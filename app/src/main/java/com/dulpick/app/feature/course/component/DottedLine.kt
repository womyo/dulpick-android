package com.dulpick.app.feature.course.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

// 동그란 점을 일정 간격으로 찍는 세로 점선의 규격 (iOS DottedVerticalLine 대응).
// 점선 중심을 다른 좌표에 맞추거나 여백을 이 주기로 나누는 쪽이 읽는다
object DottedLine {
    // 획 굵기이자 점 지름
    val WIDTH = 2.dp

    // 점 하나에서 다음 점까지
    val DOT_SPACING = 5.dp
}

// 길이 0 인 선분을 둥근 캡으로 찍어 동그란 점을 만든다. 높이는 쓰는 쪽이 정한다
@Composable
fun DottedVerticalLine(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.width(DottedLine.WIDTH)) {
        drawLine(
            color = color,
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = DottedLine.WIDTH.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, DottedLine.DOT_SPACING.toPx())),
        )
    }
}
