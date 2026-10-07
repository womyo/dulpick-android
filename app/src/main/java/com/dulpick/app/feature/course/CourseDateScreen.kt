package com.dulpick.app.feature.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.feature.course.component.CourseInputField
import com.dulpick.app.feature.course.component.DateWheelPicker
import com.dulpick.app.feature.course.component.TimeWheelPicker
import com.dulpick.app.feature.course.component.WheelDateRange
import com.dulpick.app.feature.course.component.WheelTimeRange
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppToast
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 휠에 보이는 연도 범위 (iOS yearRange 2024...2034 대응)
private val YEAR_RANGE = 2024..2034

// 분은 5 분 간격 (iOS minuteStep)
private const val MINUTE_STEP = 5

// 데이트 날짜 선택 화면. 날짜는 필수, 시간은 선택이다 (iOS CourseDateView 대응)
@Composable
fun CourseDateScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    onPlacePick: (dateCourseId: String, version: Int) -> Unit,
    viewModel: CourseDateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            CourseDateSideEffect.Dismissed -> onBack()
            CourseDateSideEffect.SessionExpired -> onSessionExpired()
            is CourseDateSideEffect.ShowToast -> toastMessage = effect.message
            is CourseDateSideEffect.PlacePickRequested -> onPlacePick(effect.dateCourseId, effect.version)
        }
    }
    LaunchedEffect(Unit) { viewModel.onIntent(CourseDateIntent.OnAppear) }

    Box(modifier = Modifier.fillMaxSize().background(Colors.bgDefault)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(onBack = { viewModel.onIntent(CourseDateIntent.BackClicked) })
            Body(state = state, onIntent = viewModel::onIntent, modifier = Modifier.weight(1f))
            CtaContainer {
                AppButton(
                    text = "다음",
                    onClick = { viewModel.onIntent(CourseDateIntent.NextClicked) },
                    variant = AppButtonVariant.DARK,
                    size = AppButtonSize.XL,
                    enabled = !state.isCreatingCourse,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        AppToast(message = toastMessage, onDismiss = { toastMessage = null }, bottomInset = 16)
    }

    if (state.activeWheel != null) {
        WheelSheet(state = state, onIntent = viewModel::onIntent)
    }
}

// 다른 push 화면(연결 관리·탐색 검색)과 같은 상단 바. 상태바 아래 56dp
@Composable
private fun TopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp),
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
        Text(text = "데이트 날짜 선택", style = Typography.body1SB, color = Colors.gray900)
    }
}

@Composable
private fun Body(state: CourseDateState, onIntent: (CourseDateIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            // 닉네임이 문장에 섞여 있다. 떼어내면 줄바꿈을 잃어 제목 줄째로 가린다
            text = state.partnerNickname?.let { "${it}님과의 데이트\n언제 만날까요?" } ?: "언제 만날까요?",
            style = Typography.title2B,
            color = Colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "데이트 날짜가 지나면 지난 데이트 코스에 저장됩니다.",
            style = Typography.body2M,
            color = Colors.textTertiary,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CourseInputField(
                value = state.dateText,
                placeholder = "날짜를 입력하세요",
                icon = R.drawable.calendar,
                onClick = { onIntent(CourseDateIntent.DateFieldClicked) },
                errorMessage = "날짜를 필수로 입력해주세요".takeIf { state.showsDateError },
            )
            CourseInputField(
                value = state.timeText,
                placeholder = "시간을 입력하세요",
                icon = R.drawable.clock,
                onClick = { onIntent(CourseDateIntent.TimeFieldClicked) },
            )
        }
    }
}

// 휠 + 확인 버튼. 딤·흰 판·모서리는 시트 껍데기가 맡는다 (iOS WheelSheet 대응)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WheelSheet(state: CourseDateState, onIntent: (CourseDateIntent) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { onIntent(CourseDateIntent.WheelDismissed) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Colors.commonWhite,
        // 기본 tonalElevation 이 흰색에 톤 오버레이를 얹어 색이 뜨므로 끈다
        tonalElevation = 0.dp,
        dragHandle = null,
        // 기본값은 내비게이션 바 높이만큼 시트를 띄운다. 그 틈으로 아래 '다음' 버튼이 비친다.
        // 시트를 바닥까지 내리고 그 여백은 안의 버튼이 직접 가진다
        windowInsets = WindowInsets(0),
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            when (state.activeWheel) {
                WheelTarget.DATE -> DateWheelPicker(
                    selection = state.draftDate,
                    onSelect = { onIntent(CourseDateIntent.DraftDateChanged(it)) },
                    range = WheelDateRange(yearRange = YEAR_RANGE, minimum = state.tomorrow),
                )
                WheelTarget.TIME -> TimeWheelPicker(
                    selection = state.draftTime,
                    onSelect = { onIntent(CourseDateIntent.DraftTimeChanged(it)) },
                    range = WheelTimeRange(minuteStep = MINUTE_STEP),
                )
                null -> Unit
            }
            AppButton(
                text = "확인",
                onClick = { onIntent(CourseDateIntent.WheelConfirmed) },
                variant = AppButtonVariant.DARK,
                size = AppButtonSize.XL,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            )
        }
    }
}
