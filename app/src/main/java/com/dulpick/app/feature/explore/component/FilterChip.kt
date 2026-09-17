package com.dulpick.app.feature.explore.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.component.ShimmerBox
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 탐색 상단 필터 칩. 선택 시 검정 배경, 미선택은 옅은 회색 (iOS FilterChip 대응)
@Composable
fun FilterChip(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = title,
        style = Typography.body1M,
        color = if (isSelected) Colors.textInverse else Colors.textSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Colors.gray900 else Colors.bgSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

// 로딩 중 칩 자리. 실제 FilterChip 과 같은 글자·패딩으로 자리를 잡고 그 위를 쉬머로 덮는다
@Composable
fun FilterChipSkeleton() {
    Box {
        ShimmerBox(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp)),
            baseColor = Colors.bgSubtle,
        )
        Text(
            text = "#태그",
            style = Typography.body1M,
            color = Color.Transparent,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}
