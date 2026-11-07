package com.dulpick.app.feature.course.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.dulpick.app.R
import com.dulpick.app.core.analytics.analyticsMasked
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 칩과 검색창이 같은 높이라 바뀌어도 시트 머리 높이가 그대로다
val SAVED_PLACE_SEARCH_HEIGHT = 36.dp
private val HORIZONTAL_PADDING = 16.dp
private val SEARCH_ICON_SIZE = 20.dp
private val CLEAR_ICON_SIZE = 16.dp
private const val DISABLED_ALPHA = 0.4f

// 검색어가 없으면 [🔍 검색], 있으면 선택 상태의 [🔍 검색어 ✕]
@Composable
fun SavedPlaceSearchChip(
    query: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = query.isNotEmpty()
    val contentColor = if (isSelected) Colors.textPrimary else Colors.textTertiary
    Row(
        modifier = modifier
            .height(SAVED_PLACE_SEARCH_HEIGHT)
            .alpha(if (isEnabled) 1f else DISABLED_ALPHA)
            .clip(CircleShape)
            .background(if (isSelected) Colors.bgSubtle else Colors.bgDefault)
            .border(1.dp, if (isSelected) Colors.textPrimary else Colors.borderDefault, CircleShape)
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = HORIZONTAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            colorFilter = ColorFilter.tint(contentColor),
            modifier = Modifier.size(SEARCH_ICON_SIZE),
        )
        Text(
            text = if (isSelected) query else "검색",
            style = Typography.body1M,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).analyticsMasked(isSelected),
        )
        if (isSelected) {
            Image(
                painter = painterResource(R.drawable.x),
                contentDescription = "검색어 지우기",
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier
                    .size(CLEAR_ICON_SIZE)
                    .clickable(onClick = onClear),
            )
        }
    }
}

// 칩이 줄 너비로 늘어나 된 검색창. 칩과 같은 높이·같은 알약 모양이다
@Composable
fun SavedPlaceSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .height(SAVED_PLACE_SEARCH_HEIGHT)
            .clip(CircleShape)
            .background(Colors.bgSubtle)
            .padding(horizontal = HORIZONTAL_PADDING)
            .focusRequester(focusRequester)
            .onFocusChanged { if (!it.isFocused) onFocusLost() }
            .analyticsMasked(),
        singleLine = true,
        textStyle = Typography.body1M.copy(color = Colors.gray900),
        cursorBrush = SolidColor(Colors.gray900),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onFocusLost() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "찾고싶은 장소를 검색하세요",
                            style = Typography.body1M,
                            color = Colors.gray400,
                        )
                    }
                    innerTextField()
                }
                Image(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Colors.textTertiary),
                    modifier = Modifier.size(SEARCH_ICON_SIZE),
                )
            }
        },
    )
}
