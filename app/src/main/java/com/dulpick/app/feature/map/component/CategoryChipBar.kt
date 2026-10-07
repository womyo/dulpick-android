package com.dulpick.app.feature.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.ui.component.CATEGORY_ORDER
import com.dulpick.app.ui.component.displayName
import com.dulpick.app.ui.component.iconRes
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val CHIP_HEIGHT = 36.dp

// 지도 위에 떠 있는 카테고리 칩 가로 스크롤. 배경은 항상 흰색, 고르면 테두리·글자만 진해진다 (iOS CategoryChipBar 대응)
@Composable
fun CategoryChipBar(
    selected: PlaceCategory?,
    onSelect: (PlaceCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CATEGORY_ORDER.forEach { category ->
            val isSelected = category == selected
            CategoryChip(category = category, isSelected = isSelected) {
                // 같은 칩을 다시 누르면 전체로 해제한다
                onSelect(if (isSelected) null else category)
            }
        }
    }
}

@Composable
private fun CategoryChip(category: PlaceCategory, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(CHIP_HEIGHT)
            .clip(CircleShape)
            .background(Colors.bgDefault)
            .border(1.dp, if (isSelected) Colors.textPrimary else Colors.borderDefault, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(category.iconRes()),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = category.displayName(),
            style = Typography.body1M,
            color = if (isSelected) Colors.textPrimary else Colors.textTertiary,
            maxLines = 1,
        )
    }
}
