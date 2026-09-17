package com.dulpick.app.feature.mypage

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppTextField
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private const val MAX_NICKNAME_LENGTH = 6
private const val MAX_INPUT_LENGTH = 7
private const val SHEET_HEIGHT_RATIO = 0.65f
private val ICON_IDS = listOf(1, 2, 3, 4, 5)

// 프로필 수정 바텀시트 내용 (iOS ProfileEditView 대응). 닉네임·아이콘 로컬 편집 후 완료로 저장한다
@Composable
fun ProfileEditSheet(
    initialNickname: String,
    initialIconId: Int,
    isSaving: Boolean,
    onSave: (nickname: String, iconId: Int) -> Unit,
    onClose: () -> Unit,
) {
    var nickname by remember { mutableStateOf(sanitizeNickname(initialNickname)) }
    var selectedIconId by remember { mutableIntStateOf(initialIconId) }

    val lengthError = if (nickname.length > MAX_NICKNAME_LENGTH) "최대 6글자 내로 입력해주세요" else null
    val isDoneEnabled = nickname.length in 1..MAX_NICKNAME_LENGTH && !isSaving

    // iOS 처럼 시트 높이를 화면의 65% 로 고정한다
    val sheetHeight = (LocalConfiguration.current.screenHeightDp * SHEET_HEIGHT_RATIO).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetHeight)
            .padding(horizontal = 20.dp)
            .imePadding(),
    ) {
        Header(onClose = onClose)

        Image(
            painter = painterResource(profileDrawable(selectedIconId)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp)
                .size(120.dp)
                .clip(CircleShape),
        )

        IconRow(
            selectedIconId = selectedIconId,
            onSelect = { selectedIconId = it },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Colors.borderWeak),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "닉네임", style = Typography.body1M, color = Colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        AppTextField(
            value = nickname,
            onValueChange = { nickname = sanitizeNickname(it) },
            placeholder = "최대 6글자",
            errorMessage = lengthError,
        )

        Spacer(modifier = Modifier.weight(1f))

        AppButton(
            text = "완료",
            onClick = { onSave(nickname, selectedIconId) },
            variant = AppButtonVariant.DARK,
            size = AppButtonSize.XL,
            fullWidth = true,
            enabled = isDoneEnabled,
            modifier = Modifier.padding(bottom = 20.dp),
        )
    }
}

@Composable
private fun Header(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.x),
            contentDescription = "닫기",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clickable(onClick = onClose),
        )
        Text(text = "프로필 수정", style = Typography.body1SB, color = Colors.textPrimary)
    }
}

@Composable
private fun IconRow(selectedIconId: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        // 양옆 패딩만 두고 5개를 동일 간격으로 펼친다
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ICON_IDS.forEach { id ->
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(id) },
            ) {
                Image(
                    painter = painterResource(profileDrawable(id)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // 선택 링은 이미지 위에 얹는다. 이미지가 링을 덮지 않게
                if (id == selectedIconId) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(3.dp, Colors.primaryPink, CircleShape),
                    )
                }
            }
        }
    }
}

// 공백 제거 후 최대 입력 길이로 자른다 (iOS sanitizedNickname 대응)
private fun sanitizeNickname(nickname: String): String =
    nickname.filter { !it.isWhitespace() }.take(MAX_INPUT_LENGTH)

@DrawableRes
private fun profileDrawable(iconId: Int): Int = when (iconId) {
    2 -> R.drawable.profile2
    3 -> R.drawable.profile3
    4 -> R.drawable.profile4
    5 -> R.drawable.profile5
    else -> R.drawable.profile1
}
