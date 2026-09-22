package com.dulpick.app.feature.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.feature.map.AliasEdit
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppTextField
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 별칭(장소 명칭) 편집 바텀시트 (iOS PlaceAliasView 대응). 최대 15자, 앞뒤 공백은 저장 직전에만 턴다
@Composable
fun PlaceAliasSheet(edit: AliasEdit, onSave: (String) -> Unit) {
    var alias by remember { mutableStateOf(sanitize(edit.initialAlias)) }
    val isSaveEnabled = alias.trim().isNotEmpty() && !edit.isSaving

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .imePadding(),
    ) {
        Text(text = "장소 명칭 수정", style = Typography.headline, color = Colors.textPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        AppTextField(
            value = alias,
            onValueChange = { alias = sanitize(it) },
            placeholder = edit.placeName,
            errorMessage = edit.errorMessage,
            // 입력이 있으면 우측에 전체 삭제(x) 버튼 (iOS accessory: .clear 대응)
            trailingContent = if (alias.isEmpty()) {
                null
            } else {
                {
                    Image(
                        painter = painterResource(R.drawable.cancel),
                        contentDescription = "지우기",
                        colorFilter = ColorFilter.tint(Colors.gray300),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { alias = "" },
                    )
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = edit.address,
            style = Typography.body2M,
            color = Colors.textTertiary,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
        )
        Spacer(modifier = Modifier.height(50.dp))

        AppButton(
            text = "저장",
            onClick = { onSave(alias) },
            variant = AppButtonVariant.PRIMARY,
            size = AppButtonSize.XL,
            fullWidth = true,
            enabled = isSaveEnabled,
        )
    }
}

// 최대 길이로 자른다. 공백은 별칭에 뜻이 있어 남긴다 (iOS sanitizedAlias 대응)
private fun sanitize(raw: String): String = raw.take(AliasEdit.MAX_ALIAS_LENGTH)
