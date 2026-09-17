package com.dulpick.app.feature.onboarding.datetype

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.core.mvi.CollectSideEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dulpick.app.R
import com.dulpick.app.domain.profile.ActivityLevel
import com.dulpick.app.domain.profile.DateFocus
import com.dulpick.app.domain.profile.DateTime
import com.dulpick.app.domain.profile.IndoorOutdoor
import com.dulpick.app.feature.onboarding.datetype.component.AxisOption
import com.dulpick.app.feature.onboarding.datetype.component.DateTypeAxisRow
import com.dulpick.app.feature.onboarding.datetype.component.DateTypeTooltip
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppToast
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography
import androidx.compose.ui.text.style.TextAlign

private const val TOOLTIP_TEXT =
    "탐색 추천 알고리즘에 선호하시는 유형의 장소를\n우선 추천 하는데 사용되며 그렇지 않은\n데이트 장소도 추천 됩니다."

@Composable
fun DateTypeScreen(
    onFinished: () -> Unit,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier,
    // 편집 모드(마이페이지)에서만 전달. 있으면 헤더에 뒤로가기·제목이 뜬다
    onBack: (() -> Unit)? = null,
    viewModel: DateTypeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            DateTypeSideEffect.Finished -> onFinished()
            DateTypeSideEffect.SessionExpired -> onSessionExpired()
        }
    }

    if (onBack != null) {
        BackHandler { onBack() }
    }

    DateTypeContent(state = state, onIntent = viewModel::onIntent, onBack = onBack, modifier = modifier)
}

@Composable
private fun DateTypeContent(
    state: DateTypeState,
    onIntent: (DateTypeIntent) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Colors.bgDefault),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                isTooltipPresented = state.isTooltipPresented,
                onTipClick = { onIntent(DateTypeIntent.TooltipToggled) },
                onTooltipDismiss = { onIntent(DateTypeIntent.TooltipDismissed) },
                onBack = onBack,
            )

            AxisList(
                state = state,
                onIntent = onIntent,
                // iOS: 헤더 아래 41 만. 나머지 여백은 Spacer 가 가져간다
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 41.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            CtaContainer(modifier = Modifier.navigationBarsPadding()) {
                if (state.showsSkip) {
                    Text(
                        text = "다음에 할래요",
                        style = Typography.body1SB,
                        color = Colors.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !state.isSubmitting) { onIntent(DateTypeIntent.SkipClicked) },
                    )
                }
                AppButton(
                    text = "저장",
                    onClick = { onIntent(DateTypeIntent.SaveClicked) },
                    variant = AppButtonVariant.DARK,
                    size = AppButtonSize.XL,
                    fullWidth = true,
                    enabled = state.isSaveEnabled,
                )
            }
        }

        AppToast(
            message = state.toast,
            onDismiss = { onIntent(DateTypeIntent.ToastDismissed) },
            bottomInset = 120,
        )
    }
}

// 편집 모드 상단바. 핑크 헤더 위에 얹는 뒤로가기 + 제목 (iOS 네비 툴바 대응)
@Composable
private fun EditTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.arrowleft),
            contentDescription = "뒤로",
            colorFilter = ColorFilter.tint(Colors.commonWhite),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 12.dp)
                .size(24.dp)
                .clickable(onClick = onBack),
        )
        Text(text = "나의 데이트 유형", style = Typography.body1SB, color = Colors.commonWhite)
    }
}

@Composable
private fun Header(
    isTooltipPresented: Boolean,
    onTipClick: () -> Unit,
    onTooltipDismiss: () -> Unit,
    onBack: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Colors.primaryPink),
    ) {
        // 편집 모드에서만 상단바(뒤로 + "나의 데이트 유형"). 이때 상태바 인셋은 여기서 처리한다
        if (onBack != null) {
            EditTopBar(onBack = onBack)
        }
        Row(
            modifier = Modifier
                .then(if (onBack == null) Modifier.statusBarsPadding() else Modifier)
                .padding(top = 30.dp)
                .padding(horizontal = 20.dp)
                // iOS: 제목·tip 을 높이 66 영역 하단에 붙인다
                .height(66.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "선호하는 데이트 방식을\n저장하면 장소를 추천드려요",
                style = Typography.title2B,
                color = Colors.commonWhite,
                modifier = Modifier.width(233.dp),
            )
            TipButton(
                isTooltipPresented = isTooltipPresented,
                onClick = onTipClick,
                onTooltipDismiss = onTooltipDismiss,
            )
        }
        // iOS: scaledToFit + maxWidth 무한 → 폭 전체를 채우고 높이는 비율대로. FillWidth 가 동일
        Image(
            painter = painterResource(R.drawable.datetypegraphic),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TipButton(
    isTooltipPresented: Boolean,
    onClick: () -> Unit,
    onTooltipDismiss: () -> Unit,
) {
    Box {
        // 탭 영역은 40x32(iOS 와 동일)로 두고, 실제 "?" 아이콘 크기는 안쪽 Image 의 size 로 조절한다
        // (Image 를 40x32 비정사각형으로 잡으면 Fit 이 작은 쪽에 letterbox 돼 size 변경이 안 먹힌다)
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.tip),
                contentDescription = "설명 보기",
                colorFilter = ColorFilter.tint(Colors.commonWhite),
                modifier = Modifier.size(18.dp),
            )
        }
        if (isTooltipPresented) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(x = 0, y = 96),
                onDismissRequest = onTooltipDismiss,
                properties = PopupProperties(focusable = true),
            ) {
                DateTypeTooltip(text = TOOLTIP_TEXT)
            }
        }
    }
}

@Composable
private fun AxisList(
    state: DateTypeState,
    onIntent: (DateTypeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        DateTypeAxisRow(
            leading = AxisOption(IndoorOutdoor.INDOOR, "실내", R.drawable.datetype_indoor),
            trailing = AxisOption(IndoorOutdoor.OUTDOOR, "실외", R.drawable.datetype_outdoor),
            selection = state.indoorOutdoor,
            onSelect = { onIntent(DateTypeIntent.IndoorOutdoorSelected(it)) },
        )
        DateTypeAxisRow(
            leading = AxisOption(ActivityLevel.ACTIVE, "액티비티", R.drawable.datetype_active),
            trailing = AxisOption(ActivityLevel.STATIC, "정적", R.drawable.datetype_resource_static),
            selection = state.activityLevel,
            onSelect = { onIntent(DateTypeIntent.ActivityLevelSelected(it)) },
        )
        DateTypeAxisRow(
            leading = AxisOption(DateTime.DAY, "낮 데이트", R.drawable.datetype_day),
            trailing = AxisOption(DateTime.NIGHT, "밤 데이트", R.drawable.datetype_night),
            selection = state.dateTime,
            onSelect = { onIntent(DateTypeIntent.DateTimeSelected(it)) },
        )
        DateTypeAxisRow(
            leading = AxisOption(DateFocus.FOOD, "식사 중심", R.drawable.datetype_food),
            trailing = AxisOption(DateFocus.SIGHTSEEING, "볼거리 중심", R.drawable.datetype_sightseeing),
            selection = state.dateFocus,
            onSelect = { onIntent(DateTypeIntent.DateFocusSelected(it)) },
        )
    }
}
