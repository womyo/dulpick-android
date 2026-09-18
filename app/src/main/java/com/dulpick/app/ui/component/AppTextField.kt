package com.dulpick.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// iOS AppTextField(size .large, style .filled) 포팅. 필요 시 accessory·outlined 는 나중에 확장한다
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done,
    onSubmit: () -> Unit = {},
    // 필드 우측 액세서리(예: 검색 돋보기). 없으면 안 그린다
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Colors.gray50)
                .padding(horizontal = 20.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            singleLine = true,
            textStyle = Typography.body1M.copy(color = Colors.gray900),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Colors.gray900),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onSubmit() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty()) {
                            Text(text = placeholder, style = Typography.body1M, color = Colors.gray400)
                        }
                        innerTextField()
                    }
                    if (trailingContent != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        trailingContent()
                    }
                }
            },
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = Typography.body2M,
                color = Colors.statusError,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
