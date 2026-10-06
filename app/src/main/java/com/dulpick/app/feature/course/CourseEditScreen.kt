package com.dulpick.app.feature.course

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.course.DateCourse
import com.dulpick.app.feature.course.component.CourseInputField
import com.dulpick.app.feature.course.component.DateWheelPicker
import com.dulpick.app.feature.course.component.ReorderableList
import com.dulpick.app.feature.course.component.TimeWheelPicker
import com.dulpick.app.feature.course.component.WheelDateRange
import com.dulpick.app.feature.course.component.WheelTimeRange
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppTextField
import com.dulpick.app.ui.component.AppToast
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.component.ModalContent
import com.dulpick.app.ui.component.ShimmerBox
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 휠에 보이는 연도 범위와 분 간격. 날짜 선택 화면과 같다
private val YEAR_RANGE = 2024..2034
private const val MINUTE_STEP = 5

private const val SKELETON_CARD_COUNT = 3
private val SKELETON_SECTION_TITLE_WIDTH = 100.dp
private val SKELETON_SECTION_TITLE_HEIGHT = 30.dp
private val SKELETON_FIELD_HEIGHT = 48.dp
private val SKELETON_CARD_HEIGHT = 81.dp

// 저장된 코스의 제목·날짜·시간·장소를 고치는 화면 (iOS CourseEditView 대응)
@Composable
@Suppress("LongParameterList")
fun CourseEditScreen(
    dateCourseId: String,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    onSaved: (DateCourse) -> Unit,
    onConflicted: () -> Unit,
    onAddPlace: (excluding: List<String>) -> Unit,
    viewModel: CourseEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            CourseEditSideEffect.Dismissed -> onBack()
            CourseEditSideEffect.SessionExpired -> onSessionExpired()
            CourseEditSideEffect.Conflicted -> onConflicted()
            is CourseEditSideEffect.Saved -> onSaved(effect.course)
            is CourseEditSideEffect.AddPlaceRequested -> onAddPlace(effect.excluding)
        }
    }
    LaunchedEffect(dateCourseId) {
        viewModel.onIntent(CourseEditIntent.Start(dateCourseId))
    }
    BackHandler { viewModel.onIntent(CourseEditIntent.BackClicked) }

    Box(modifier = Modifier.fillMaxSize().background(Colors.bgDefault)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(onBack = { viewModel.onIntent(CourseEditIntent.BackClicked) })
            when (state.load) {
                CourseEditLoad.LOADING -> Skeleton(modifier = Modifier.weight(1f))
                CourseEditLoad.FAILED -> Failure(
                    onRetry = { viewModel.onIntent(CourseEditIntent.RetryClicked) },
                    modifier = Modifier.weight(1f),
                )
                CourseEditLoad.LOADED -> {
                    Form(state = state, onIntent = viewModel::onIntent, modifier = Modifier.weight(1f))
                    CtaContainer {
                        AppButton(
                            text = "저장",
                            onClick = { viewModel.onIntent(CourseEditIntent.SaveClicked) },
                            variant = AppButtonVariant.DARK,
                            size = AppButtonSize.XL,
                            enabled = state.canSave,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        AppToast(
            message = state.toast?.message,
            onDismiss = { viewModel.onIntent(CourseEditIntent.ToastDismissed) },
            bottomInset = TOAST_BOTTOM_INSET,
            actionTitle = "실행취소".takeIf { state.toast?.showsUndo == true },
            onAction = { viewModel.onIntent(CourseEditIntent.UndoClicked) },
        )
    }

    if (state.activeWheel != null) {
        WheelSheet(state = state, onIntent = viewModel::onIntent)
    }
    if (state.showsBackModal) {
        BackModal(
            canSave = state.canSave,
            onSave = { viewModel.onIntent(CourseEditIntent.SaveClicked) },
            onDiscard = { viewModel.onIntent(CourseEditIntent.BackModalDiscarded) },
            onClose = { viewModel.onIntent(CourseEditIntent.BackModalClosed) },
        )
    }
}

// CTA 버튼 위로 토스트를 올린다
private const val TOAST_BOTTOM_INSET = 88

// 다른 push 화면과 같은 상단 바. 상태바 아래 56dp
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
    }
}

@Composable
private fun Form(state: CourseEditState, onIntent: (CourseEditIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        InfoSection(state = state, onIntent = onIntent)
        PlaceSection(state = state, onIntent = onIntent)
    }
}

@Composable
private fun InfoSection(state: CourseEditState, onIntent: (CourseEditIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "데이트 정보", style = Typography.title3SB, color = Colors.textPrimary)
        AppTextField(
            value = state.title,
            onValueChange = { onIntent(CourseEditIntent.TitleChanged(it)) },
            placeholder = "데이트명 입력",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CourseInputField(
                value = state.dateText,
                // 두 칸이 나란히 놓여 폭이 절반이다
                placeholder = "날짜 입력",
                icon = R.drawable.calendar,
                onClick = { onIntent(CourseEditIntent.DateFieldClicked) },
                modifier = Modifier.weight(1f),
            )
            CourseInputField(
                value = state.timeText,
                placeholder = "시간 입력",
                icon = R.drawable.clock,
                onClick = { onIntent(CourseEditIntent.TimeFieldClicked) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlaceSection(state: CourseEditState, onIntent: (CourseEditIntent) -> Unit) {
    Column {
        Text(text = "데이트 장소", style = Typography.title3SB, color = Colors.textPrimary)
        ReorderableList(
            items = state.places,
            key = { it.id },
            title = { it.name },
            subtitle = { it.address },
            category = { it.category },
            onMove = { from, to -> onIntent(CourseEditIntent.PlaceMoved(from, to)) },
            modifier = Modifier.padding(top = 12.dp),
        ) { place ->
            Image(
                painter = painterResource(R.drawable.trash),
                contentDescription = "장소 삭제",
                colorFilter = ColorFilter.tint(Colors.textTertiary),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onIntent(CourseEditIntent.PlaceDeleteClicked(place.id)) },
            )
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
            AppButton(
                text = "장소 추가",
                onClick = { onIntent(CourseEditIntent.AddPlaceClicked) },
                variant = AppButtonVariant.OUTLINED,
                size = AppButtonSize.LG,
                icon = R.drawable.plus,
            )
        }
    }
}

// 휠 + 확인 버튼. 날짜 선택 화면과 같은 껍데기다
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WheelSheet(state: CourseEditState, onIntent: (CourseEditIntent) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { onIntent(CourseEditIntent.WheelDismissed) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Colors.commonWhite,
        tonalElevation = 0.dp,
        dragHandle = null,
        windowInsets = WindowInsets(0),
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            when (state.activeWheel) {
                WheelTarget.DATE -> DateWheelPicker(
                    selection = state.draftDate,
                    onSelect = { onIntent(CourseEditIntent.DraftDateChanged(it)) },
                    range = WheelDateRange(yearRange = YEAR_RANGE, minimum = state.tomorrow),
                )
                WheelTarget.TIME -> TimeWheelPicker(
                    selection = state.draftTime,
                    onSelect = { onIntent(CourseEditIntent.DraftTimeChanged(it)) },
                    range = WheelTimeRange(minuteStep = MINUTE_STEP),
                )
                null -> Unit
            }
            AppButton(
                text = "확인",
                onClick = { onIntent(CourseEditIntent.WheelConfirmed) },
                variant = AppButtonVariant.DARK,
                size = AppButtonSize.XL,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            )
        }
    }
}

// 고친 게 있는데 뒤로 가려 할 때. 딤 탭·뒤로가기는 모달만 닫는다(그대로 머문다)
@Composable
private fun BackModal(canSave: Boolean, onSave: () -> Unit, onDiscard: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ModalContent(
            title = "변경사항을 저장할까요?",
            content = "작성중인 내용이 있어요",
            image = R.drawable.savemodal,
            primaryTitle = "네, 저장할래요",
            onPrimary = onSave,
            primaryEnabled = canSave,
            secondaryTitle = "아니요",
            onSecondary = onDiscard,
        )
    }
}

@Composable
private fun Skeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(
                modifier = Modifier
                    .size(SKELETON_SECTION_TITLE_WIDTH, SKELETON_SECTION_TITLE_HEIGHT)
                    .clip(RoundedCornerShape(4.dp)),
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SKELETON_FIELD_HEIGHT)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(SKELETON_FIELD_HEIGHT)
                        .clip(RoundedCornerShape(12.dp)),
                )
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(SKELETON_FIELD_HEIGHT)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ShimmerBox(
                modifier = Modifier
                    .size(SKELETON_SECTION_TITLE_WIDTH, SKELETON_SECTION_TITLE_HEIGHT)
                    .clip(RoundedCornerShape(4.dp)),
            )
            repeat(SKELETON_CARD_COUNT) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SKELETON_CARD_HEIGHT)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
        }
    }
}

@Composable
private fun Failure(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "코스를 불러오지 못했어요", style = Typography.body1SB, color = Colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "잠시 뒤 다시 시도해주세요", style = Typography.body2M, color = Colors.textTertiary)
        Spacer(modifier = Modifier.height(16.dp))
        AppButton(
            text = "다시 시도",
            onClick = onRetry,
            variant = AppButtonVariant.OUTLINED,
            size = AppButtonSize.MD,
        )
    }
}
