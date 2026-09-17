package com.dulpick.app.feature.onboarding.datetype.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 한 축의 두 선택지를 균등 분할로 놓고 두 버튼 사이 정중앙에 VS 배지를 얹는다
@Composable
fun <T> DateTypeAxisRow(
    leading: AxisOption<T>,
    trailing: AxisOption<T>,
    selection: T?,
    onSelect: (T) -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        // 한 축의 두 선택지는 라디오 그룹. TalkBack 이 그룹으로 인식하게 한다
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OptionButton(
                option = leading,
                isSelected = selection == leading.value,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            OptionButton(
                option = trailing,
                isSelected = selection == trailing.value,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
        }
        VersusBadge()
    }
}

// AppButton 은 아이콘을 글자색으로 칠해 여기선 라벨을 직접 만든다. 패딩·radius·타이포는 XL 과 동일
@Composable
private fun <T> OptionButton(
    option: AxisOption<T>,
    isSelected: Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Colors.primaryPink else Colors.commonWhite)
            .then(
                if (isSelected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, Colors.borderDefault, RoundedCornerShape(12.dp))
                },
            )
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = { onSelect(option.value) },
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(
                painter = painterResource(option.iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (isSelected) Colors.commonWhite else Colors.gray400),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = option.title,
                style = Typography.body1SB,
                color = if (isSelected) Colors.textInverse else Colors.textSecondary,
            )
        }
    }
}

@Composable
private fun VersusBadge() {
    Text(
        text = "VS",
        style = Typography.body2SB,
        color = Colors.commonWhite,
        modifier = Modifier
            .clip(RoundedCornerShape(29.dp))
            .background(Colors.textPrimary)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}
