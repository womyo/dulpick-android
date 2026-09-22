package com.dulpick.app.feature.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dulpick.app.R
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 펼침 카드와 pill 사이 간격
private val MENU_GAP = 8.dp

// 항목 높이(iOS itemHeight 40) 와 펼침 카드 최대 높이.
// 4와 1/3 항목이 보이게 해 더 있으면 스크롤됨을 드러낸다
private val MENU_ITEM_HEIGHT = 40.dp
private val MENU_MAX_HEIGHT = MENU_ITEM_HEIGHT * 13 / 3

// 소유자·카테고리 등이 공유하는 필터 드롭다운 (iOS AppDropdown 대응).
// isActive(기본값이 아닌 값으로 거른 상태)면 강조 색으로 바뀐다. 문자열 옵션만 주고받는다
@Composable
fun FilterDropdown(
    label: String,
    isActive: Boolean,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var pillHeightPx by remember { mutableIntStateOf(0) }
    val gapPx = with(LocalDensity.current) { MENU_GAP.roundToPx() }
    val contentColor = if (isActive) Colors.textPrimary else Colors.textTertiary

    Box {
        Row(
            modifier = Modifier
                .onSizeChanged { pillHeightPx = it.height }
                .clip(CircleShape)
                .background(if (isActive) Colors.bgSubtle else Colors.bgDefault)
                .border(1.dp, if (isActive) Colors.textPrimary else Colors.borderDefault, CircleShape)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = label, style = Typography.body1M, color = contentColor)
            Image(
                painter = painterResource(if (expanded) R.drawable.arrowup else R.drawable.arrowdown),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(16.dp),
            )
        }

        if (expanded) {
            FilterDropdownMenu(
                options = options,
                selectedIndex = selectedIndex,
                offsetY = pillHeightPx + gapPx,
                onSelect = {
                    onSelect(it)
                    expanded = false
                },
                onDismiss = { expanded = false },
            )
        }
    }
}

// pill 아래로 펼쳐지는 선택 카드. Material 기본 메뉴 대신 iOS DropdownMenu 스타일로 직접 그린다
@Composable
private fun FilterDropdownMenu(
    options: List<String>,
    selectedIndex: Int,
    offsetY: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(x = 0, y = offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Colors.bgDefault)
                .border(1.dp, Colors.borderDefault, RoundedCornerShape(12.dp))
                .padding(4.dp),
        ) {
            // 항목이 많으면 4와 1/3 높이까지만 보이고 그 안에서 스크롤한다
            Column(
                modifier = Modifier
                    .heightIn(max = MENU_MAX_HEIGHT)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MENU_ITEM_HEIGHT)
                            .clip(RoundedCornerShape(8.dp))
                            .then(if (isSelected) Modifier.background(Colors.bgSubtle) else Modifier)
                            .clickable { onSelect(index) }
                            // 우측을 넉넉히 둬 카드가 글자 폭보다 넓어지게 한다 (iOS leading 16 / trailing 32)
                            .padding(start = 16.dp, end = 32.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = option,
                            style = Typography.body1M,
                            color = if (isSelected) Colors.textPrimary else Colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
