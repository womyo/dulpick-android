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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 시트 드롭다운 노출 순서. 함께 저장한 이 맨 앞 (iOS mapDisplayOrder 대응)
private val OWNERSHIP_ORDER = listOf(PlaceOwnership.TOGETHER, PlaceOwnership.MINE, PlaceOwnership.PARTNER)

// 펼침 카드와 pill 사이 간격
private val MENU_GAP = 8.dp

fun PlaceOwnership.displayName(): String = when (this) {
    PlaceOwnership.TOGETHER -> "함께 저장한"
    PlaceOwnership.MINE -> "내가 저장한"
    PlaceOwnership.PARTNER -> "상대가 저장한"
}

// 저장자(소유자) 필터 드롭다운. 커플 연동 시에만 시트 헤더에 뜬다 (iOS AppDropdown 대응).
// 기본값(함께 저장한)이면 비활성 색, 다른 값으로 거르면 활성 색으로 강조한다
@Composable
fun OwnershipDropdown(selected: PlaceOwnership, onSelect: (PlaceOwnership) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var pillHeightPx by remember { mutableIntStateOf(0) }
    val gapPx = with(LocalDensity.current) { MENU_GAP.roundToPx() }
    val isActive = selected != PlaceOwnership.TOGETHER
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
            Text(text = selected.displayName(), style = Typography.body1M, color = contentColor)
            Image(
                painter = painterResource(if (expanded) R.drawable.arrowup else R.drawable.arrowdown),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(16.dp),
            )
        }

        if (expanded) {
            OwnershipMenu(
                selected = selected,
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
private fun OwnershipMenu(
    selected: PlaceOwnership,
    offsetY: Int,
    onSelect: (PlaceOwnership) -> Unit,
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
            OWNERSHIP_ORDER.forEach { ownership ->
                val isSelected = ownership == selected
                Text(
                    text = ownership.displayName(),
                    style = Typography.body1M,
                    color = if (isSelected) Colors.textPrimary else Colors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .then(if (isSelected) Modifier.background(Colors.bgSubtle) else Modifier)
                        .clickable { onSelect(ownership) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
