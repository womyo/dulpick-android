package com.dulpick.app.feature.mypage

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.core.terms.TermsType
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppToast
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 개인/보안·알림·문의 카드의 행 공통 높이. Switch 가 토글 행을 늘리지 않도록 고정한다
private val ROW_HEIGHT = 52.dp

// 마이페이지 메인 화면. 프로필·알림 설정은 API 연동, 상세 네비게이션은 이후 단계
@Composable
fun MyPageScreen(
    onLoggedOut: () -> Unit,
    viewModel: MyPageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 토스트는 1회성이라 state 가 아니라 side effect 로 받아 화면 로컬 상태로만 둔다
    var toastMessage by remember { mutableStateOf<String?>(null) }

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            // 로그아웃·세션 만료 모두 로그인으로 되돌린다
            MyPageSideEffect.LoggedOut, MyPageSideEffect.SessionExpired -> onLoggedOut()
            is MyPageSideEffect.ShowToast -> toastMessage = effect.message
        }
    }

    LaunchedEffect(Unit) { viewModel.onIntent(MyPageIntent.OnAppear) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgSubtle),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MyPageTopBar()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 16.dp),
            ) {
                ProfileSection(nickname = state.nickname, iconId = state.iconId, onEdit = {})

                Spacer(modifier = Modifier.height(20.dp))

                MyPageCard(title = "개인/보안") {
                    NavRow(title = "나의 데이트 유형", onClick = {})
                    RowDivider()
                    NavRow(title = "연결 관리", onClick = {})
                    RowDivider()
                    NavRow(title = "로그아웃") { viewModel.onIntent(MyPageIntent.LogoutClicked) }
                }

                Spacer(modifier = Modifier.height(30.dp))

                MyPageCard(title = "알림 설정") {
                    ToggleRow(title = "콘텐츠 저장 알림", checked = state.savedContentAlarm) {
                        viewModel.onIntent(MyPageIntent.ContentSavedToggled(it))
                    }
                    RowDivider()
                    ToggleRow(title = "데이트 일정 알림", checked = state.dateScheduleAlarm) {
                        viewModel.onIntent(MyPageIntent.DateScheduleToggled(it))
                    }
                    RowDivider()
                    ToggleRow(title = "마케팅 정보 알림", checked = state.marketingAlarm) {
                        viewModel.onIntent(MyPageIntent.MarketingToggled(it))
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                MyPageCard(title = "문의하기") {
                    NavRow(title = "서비스 피드백하기", onClick = {})
                }

                Spacer(modifier = Modifier.height(30.dp))

                TermsLinks(
                    onSelect = { terms ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(terms.url)))
                    },
                )

                Spacer(modifier = Modifier.height(20.dp))

                WithdrawButton(onClick = {})
            }
        }

        AppToast(
            message = toastMessage,
            onDismiss = { toastMessage = null },
            bottomInset = 120,
        )
    }
}

@Composable
private fun MyPageTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = "마이페이지", style = Typography.title2B, color = Colors.gray900)
    }
}

@Composable
private fun ProfileSection(nickname: String, iconId: Int, onEdit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painter = painterResource(profileDrawable(iconId)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
        )
        Text(text = nickname, style = Typography.headline, color = Colors.textPrimary)
        AppButton(
            text = "프로필 수정",
            onClick = onEdit,
            variant = AppButtonVariant.OUTLINED,
            size = AppButtonSize.SM,
        )
    }
}

// 회색 배경 위 흰 카드. 섹션 제목은 카드 바깥 위. 카드 세로패딩 6 + 행 세로패딩 14 로
// 첫 행 위·마지막 행 아래는 20, 행 사이는 28 이 되게 맞춘다 (iOS RowEdge 대응)
@Composable
private fun MyPageCard(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = Typography.body1SB, color = Colors.textPrimary)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Colors.commonWhite)
                .padding(horizontal = 20.dp, vertical = 6.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun NavRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = Typography.body2M,
            color = Colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.arrowright),
            contentDescription = null,
            tint = Colors.textSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = Typography.body2M,
            color = Colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Colors.commonWhite,
                checkedTrackColor = Colors.textPrimary,
                checkedBorderColor = Colors.textPrimary,
                uncheckedThumbColor = Colors.commonWhite,
                uncheckedTrackColor = Colors.gray300,
                uncheckedBorderColor = Colors.gray300,
            ),
        )
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Colors.bgSubtle),
    )
}

@Composable
private fun TermsLinks(onSelect: (TermsType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TermsType.entries.forEachIndexed { index, terms ->
            if (index > 0) {
                Text(
                    text = "|",
                    style = Typography.caption2M,
                    color = Colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            Text(
                text = terms.title,
                style = Typography.caption2M,
                color = Colors.textTertiary,
                modifier = Modifier.clickable { onSelect(terms) },
            )
        }
    }
}

@Composable
private fun WithdrawButton(onClick: () -> Unit) {
    Text(
        text = "회원탈퇴",
        style = Typography.caption2M,
        color = Colors.statusError,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

// 아이콘 ID 를 프로필 이미지로. 미매핑 값은 기본 프로필로 (iOS profileImage 대응)
@DrawableRes
private fun profileDrawable(iconId: Int): Int = when (iconId) {
    2 -> R.drawable.profile2
    3 -> R.drawable.profile3
    4 -> R.drawable.profile4
    5 -> R.drawable.profile5
    else -> R.drawable.profile1
}
