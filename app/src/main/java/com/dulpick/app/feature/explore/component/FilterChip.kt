package com.dulpick.app.feature.explore.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
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
