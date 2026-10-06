package com.dulpick.app.feature.course.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
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
import com.dulpick.app.feature.course.TimelineStop
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val RAIL_WIDTH = 24.dp
private val RAIL_GAP = 12.dp
private val BADGE_SIZE = 20.dp
private val WALK_ICON_SIZE = 24.dp
private val WARNING_ICON_SIZE = 13.dp

// 코스 순서를 위에서 아래로 잇는 타임라인 (iOS CourseTimeline 대응).
// 번호 배지 사이를 빨강 점선으로 잇고, 사이마다 이동 구간을 한 줄 끼운다.
// legs 는 stops 보다 하나 적다. 값이 없어도 구간 행은 그려 점선이 끊기지 않게 한다
@Composable
fun CourseTimeline(
    stops: List<TimelineStop>,
    legs: List<TimelineLeg>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        stops.forEachIndexed { index, stop ->
            val isLast = index == stops.lastIndex
            StopRow(stop = stop, number = index + 1, isFirst = index == 0, isLast = isLast)
            if (!isLast) {
                LegRow(legs.getOrNull(index) ?: TimelineLeg("", "", isLong = false))
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
private fun LegRow(leg: TimelineLeg) {
    TimelineRow(
        rail = {
            Column(modifier = Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Connector(modifier = Modifier.weight(1f), visible = true)
                // 발자국 아이콘이 점선을 끊고 그 자리에 들어간다
                Image(
                    painter = painterResource(R.drawable.walk),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Colors.textTertiary),
                    modifier = Modifier.size(WALK_ICON_SIZE),
                )
                Connector(modifier = Modifier.weight(1f), visible = true)
            }
        },
        content = {
            Row(
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 28.dp).padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = leg.duration, style = Typography.body2M, color = Colors.textSecondary, maxLines = 1)
                Text(text = leg.distance, style = Typography.caption1R, color = Colors.textTertiary, maxLines = 1)
                if (leg.isLong) LongLegBadge()
            }
        },
    )
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

// 오래 걷는 구간 안내. 경고 삼각형 + 빨강 글자에 연분홍 바탕 (iOS longLegBadge 대응)
@Composable
private fun LongLegBadge() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Colors.brandSurface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = Colors.statusError,
            modifier = Modifier.size(WARNING_ICON_SIZE),
        )
        Text(
            text = "이동이 긴 구간입니다",
            style = Typography.caption2M,
            color = Colors.statusError,
            maxLines = 1,
        )
    }
}
