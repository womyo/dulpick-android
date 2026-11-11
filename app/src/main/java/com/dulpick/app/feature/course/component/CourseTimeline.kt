package com.dulpick.app.feature.course.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.feature.course.TimelineLeg
import com.dulpick.app.feature.course.TimelineLegMode
import com.dulpick.app.feature.course.TimelineStop
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val RAIL_WIDTH = 24.dp
private val RAIL_GAP = 12.dp
private val BADGE_SIZE = 20.dp
private val LEG_ICON_SIZE = 24.dp
private val DIRECTIONS_ARROW_SIZE = 16.dp

// legs 는 stops 보다 하나 적다. 값이 없어도 구간 행은 그려 점선이 끊기지 않게 한다
@Composable
fun CourseTimeline(
    stops: List<TimelineStop>,
    legs: List<TimelineLeg>,
    modifier: Modifier = Modifier,
    onLegDirectionsClick: (Int) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        stops.forEachIndexed { index, stop ->
            val isLast = index == stops.lastIndex
            StopRow(stop = stop, number = index + 1, isFirst = index == 0, isLast = isLast)
            if (!isLast) {
                LegRow(
                    leg = legs.getOrNull(index) ?: TimelineLeg(TimelineLegMode.UNAVAILABLE, "", ""),
                    onDirectionsClick = { onLegDirectionsClick(index) },
                )
            }
        }
    }
}

@Composable
private fun StopRow(stop: TimelineStop, number: Int, isFirst: Boolean, isLast: Boolean) {
    TimelineRow(
        rail = {
            Column(modifier = Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                // 첫 줄 위, 마지막 줄 아래로는 점선을 잇지 않는다
                Connector(modifier = Modifier.weight(1f), visible = !isFirst)
                NumberBadge(number)
                Connector(modifier = Modifier.weight(1f), visible = !isLast)
            }
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stop.name, style = Typography.headline, color = Colors.textPrimary)
                    Text(text = stop.category, style = Typography.body2M, color = Colors.textTertiary)
                }
                Text(text = stop.address, style = Typography.caption1R, color = Colors.textTertiary)
            }
        },
    )
}

@Composable
private fun LegRow(leg: TimelineLeg, onDirectionsClick: () -> Unit) {
    TimelineRow(
        rail = {
            Column(modifier = Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Connector(modifier = Modifier.weight(1f), visible = true)
                // 수단 아이콘이 점선을 끊고 그 자리에 들어간다
                Image(
                    painter = painterResource(leg.mode.iconRes()),
                    contentDescription = null,
                    // 느낌표 아이콘은 색이 박혀 있다. 물들이면 흰 느낌표가 사라진다
                    colorFilter = if (leg.mode == TimelineLegMode.UNAVAILABLE) {
                        null
                    } else {
                        ColorFilter.tint(Colors.textTertiary)
                    },
                    modifier = Modifier.size(LEG_ICON_SIZE),
                )
                Connector(modifier = Modifier.weight(1f), visible = true)
            }
        },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 28.dp)
                    .padding(vertical = 20.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegText(leg = leg, modifier = Modifier.weight(1f))
                DirectionsLink(onClick = onDirectionsClick)
            }
        },
    )
}

// 링크 앞 폭에 한 줄로 안 들어가면 거리를 다음 줄로 내린다
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LegText(leg: TimelineLeg, modifier: Modifier = Modifier) {
    if (leg.mode == TimelineLegMode.UNAVAILABLE) {
        Text(text = leg.distance, style = Typography.caption1R, color = Colors.textTertiary, modifier = modifier)
        return
    }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(text = leg.duration, style = Typography.body2M, color = Colors.textSecondary, maxLines = 1)
        Text(text = leg.distance, style = Typography.caption1R, color = Colors.textTertiary, maxLines = 1)
    }
}

@Composable
private fun DirectionsLink(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 44.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "카카오맵으로 보기", style = Typography.body2SB, color = Colors.brandPrimary, maxLines = 1)
        Image(
            painter = painterResource(R.drawable.arrowright),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Colors.brandPrimary),
            modifier = Modifier.size(DIRECTIONS_ARROW_SIZE),
        )
    }
}

private fun TimelineLegMode.iconRes(): Int = when (this) {
    TimelineLegMode.WALK -> R.drawable.walk
    TimelineLegMode.TRANSIT -> R.drawable.bus
    TimelineLegMode.UNAVAILABLE -> R.drawable.errorgray
}

// 왼쪽은 점선 레일, 오른쪽은 내용. 구분선은 내용 쪽에만 깔린다.
// 행 높이는 내용이 정하고, 레일은 그 높이를 그대로 받아 점선이 행 끝까지 이어진다
@Composable
private fun TimelineRow(rail: @Composable () -> Unit, content: @Composable () -> Unit) {
    Layout(
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Colors.borderWeak))
            }
            Box(modifier = Modifier.width(RAIL_WIDTH)) { rail() }
        },
        measurePolicy = { measurables, constraints ->
            val railWidthPx = RAIL_WIDTH.roundToPx() + RAIL_GAP.roundToPx()
            val body = measurables[0].measure(
                constraints.copy(
                    minWidth = 0,
                    maxWidth = (constraints.maxWidth - railWidthPx).coerceAtLeast(0),
                ),
            )
            // 레일 높이를 내용 높이에 맞춘다. 그래야 점선이 행을 꽉 채운다
            val rail = measurables[1].measure(
                constraints.copy(minWidth = 0, minHeight = body.height, maxHeight = body.height),
            )
            layout(constraints.maxWidth, body.height) {
                rail.place(0, 0)
                body.place(railWidthPx, 0)
            }
        },
    )
}

// 번호 배지. 안쪽 20 원에 흰 숫자
@Composable
private fun NumberBadge(number: Int) {
    Box(
        modifier = Modifier.size(BADGE_SIZE).clip(CircleShape).background(Colors.brandPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "$number", style = Typography.caption1M, color = Colors.textInverse)
    }
}

// 세로 점선. 남는 높이를 다 채워야 배지와 배지가 끊김 없이 이어진다
@Composable
private fun Connector(modifier: Modifier = Modifier, visible: Boolean) {
    if (!visible) {
        Box(modifier = modifier.width(RAIL_WIDTH))
        return
    }
    Box(modifier = modifier.width(RAIL_WIDTH), contentAlignment = Alignment.Center) {
        DottedVerticalLine(color = Colors.brandPrimary, modifier = Modifier.fillMaxHeight())
    }
}
