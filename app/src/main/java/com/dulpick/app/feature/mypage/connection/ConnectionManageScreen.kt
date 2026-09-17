package com.dulpick.app.feature.mypage.connection

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.couple.CoupleMember
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppToast
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.component.ModalContent
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val PROFILE_SIZE = 100.dp
private val RING_SIZE = 108.dp
private val HEART_SIZE = 44.dp
private val PROFILE_GAP = 20.dp
private val CARD_INSET = 20.dp

// 카드+이름을 세로 중앙에서 위로 올리는 양 (iOS centerOffsetUp)
private val CENTER_OFFSET_UP = 54.dp

// 두 프로필 사이 간격(하트 44 + 좌우 20*2). 이름을 각 프로필 정중앙 아래에 두는 데 쓴다
private val NAME_GAP = HEART_SIZE + PROFILE_GAP * 2

@Composable
fun ConnectionManageScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: ConnectionManageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            ConnectionManageSideEffect.Disconnected -> onBack()
            ConnectionManageSideEffect.SessionExpired -> onSessionExpired()
            is ConnectionManageSideEffect.ShowToast -> toastMessage = effect.message
        }
    }

    LaunchedEffect(Unit) { viewModel.onIntent(ConnectionManageIntent.OnAppear) }
    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(onBack = onBack)

            Spacer(modifier = Modifier.weight(1f))

            CoupleCardBlock(
                me = state.me,
                partner = state.partner,
                daysTogether = state.daysTogether,
            )

            Spacer(modifier = Modifier.weight(1f))

            CtaContainer(modifier = Modifier.navigationBarsPadding()) {
                AppButton(
                    text = "커플 연결 끊기",
                    onClick = { viewModel.onIntent(ConnectionManageIntent.DisconnectClicked) },
                    variant = AppButtonVariant.DARK,
                    size = AppButtonSize.XL,
                    fullWidth = true,
                )
            }
        }

        AppToast(message = toastMessage, onDismiss = { toastMessage = null }, bottomInset = 120)
    }

    if (state.isDisconnectDialogPresented) {
        DisconnectDialog(
            onConfirm = { viewModel.onIntent(ConnectionManageIntent.DisconnectConfirmed) },
            onDismiss = { viewModel.onIntent(ConnectionManageIntent.DisconnectDismissed) },
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.arrowleft),
            contentDescription = "뒤로",
            colorFilter = ColorFilter.tint(Colors.textPrimary),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 12.dp)
                .size(24.dp)
                .clickable(onClick = onBack),
        )
        Text(text = "연결 관리", style = Typography.body1SB, color = Colors.gray900)
    }
}

@Composable
private fun CoupleCardBlock(me: CoupleMember?, partner: CoupleMember?, daysTogether: Int?) {
    Column(
        modifier = Modifier.offset(y = -CENTER_OFFSET_UP),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.padding(horizontal = CARD_INSET)) {
            Image(
                painter = painterResource(R.drawable.connectionmanage),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
            DaysBadge(
                daysTogether = daysTogether,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp),
            )
            ProfilesRow(
                me = me,
                partner = partner,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        NamesRow(me = me, partner = partner)
    }
}

@Composable
private fun DaysBadge(daysTogether: Int?, modifier: Modifier = Modifier) {
    Text(
        text = "둘픽에서 함께한 지 ${daysTogether ?: 0}일째",
        style = Typography.body2SB,
        color = Colors.primaryPink,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Colors.commonWhite.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ProfilesRow(me: CoupleMember?, partner: CoupleMember?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PROFILE_GAP),
    ) {
        ProfileCircle(iconId = me?.iconId)
        Image(
            painter = painterResource(R.drawable.heartwithstroke),
            contentDescription = null,
            modifier = Modifier.size(HEART_SIZE),
        )
        ProfileCircle(iconId = partner?.iconId)
    }
}

@Composable
private fun ProfileCircle(iconId: Int?) {
    Box(contentAlignment = Alignment.Center) {
        // 프로필 뒤 흰 링 (iOS strokeWidth 4)
        Box(
            modifier = Modifier
                .size(RING_SIZE)
                .clip(CircleShape)
                .background(Colors.commonWhite),
        )
        Image(
            painter = painterResource(profileDrawable(iconId ?: 1)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(PROFILE_SIZE)
                .clip(CircleShape),
        )
    }
}

@Composable
private fun NamesRow(me: CoupleMember?, partner: CoupleMember?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NameText(name = me?.nickname.orEmpty())
        Spacer(modifier = Modifier.width(NAME_GAP))
        NameText(name = partner?.nickname.orEmpty())
    }
}

@Composable
private fun NameText(name: String) {
    Text(
        text = name,
        style = Typography.headline,
        color = Colors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(PROFILE_SIZE),
    )
}

@Composable
private fun DisconnectDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ModalContent(
            title = "잠깐!\n정말 커플 연결을 끊으시겠어요?",
            content = "지금까지 저장된 데이터가 모두 날아가요",
            image = R.drawable.disconnect,
            primaryTitle = "연결 끊기",
            onPrimary = onConfirm,
            secondaryTitle = "취소",
            onSecondary = onDismiss,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@DrawableRes
private fun profileDrawable(iconId: Int): Int = when (iconId) {
    2 -> R.drawable.profile2
    3 -> R.drawable.profile3
    4 -> R.drawable.profile4
    5 -> R.drawable.profile5
    else -> R.drawable.profile1
}
