package com.dulpick.app.feature.onboarding.nickname

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppTextField
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NicknameScreen(
    onConfirmed: (nickname: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NicknameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // rememberModalBottomSheetState 는 rememberSaveable 기반이라, nav 전환으로 이 목적지에 진입하면
    // back stack entry 의 저장 상태가 복원되는 시점에 sheetState 가 다시 만들어져 show() 가 두 번 실행된다
    // (시트가 올라왔다 내려갔다 다시 올라오는 현상). 일반 remember 로 만들어 복원에 영향받지 않게 한다
    // skipPartiallyExpanded=true → 정착 앵커가 하나뿐이라 재구성 때 다시 정착하는 flicker 도 없다
    // confirmValueChange 로 Hidden 전이를 막아 스와이프로 닫히지 않게 한다 (닫기 불가 시트)
    val density = LocalDensity.current
    val sheetState = remember {
        SheetState(
            skipPartiallyExpanded = true,
            density = density,
            initialValue = SheetValue.Hidden,
            confirmValueChange = { it != SheetValue.Hidden },
        )
    }

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            is NicknameSideEffect.NicknameConfirmed -> onConfirmed(effect.nickname)
            // 세션 만료도 로그인으로 되돌린다
            NicknameSideEffect.NavigateBack, NicknameSideEffect.SessionExpired -> onBack()
            is NicknameSideEffect.OpenTermsUrl ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)))
        }
    }

    // 약관을 다 읽고 시트가 닫히면 바로 닉네임 입력에 포커스를 준다
    LaunchedEffect(state.isTermsSheetPresented) {
        if (!state.isTermsSheetPresented) {
            focusRequester.requestFocus()
        }
    }

    // 시트가 떠 있을 때 시스템 뒤로가기 → 온보딩을 떠나 로그인으로
    BackHandler(enabled = state.isTermsSheetPresented) {
        viewModel.onIntent(NicknameIntent.BackClicked)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Colors.bgDefault),
    ) {
        NicknameContent(
            state = state,
            focusRequester = focusRequester,
            onIntent = viewModel::onIntent,
        )

        if (state.isTermsSheetPresented) {
            ModalBottomSheet(
                // 스크림 탭·스와이프로 닫히지 않는다. 시트를 떠나려면 시스템 back 으로만
                onDismissRequest = {},
                sheetState = sheetState,
                containerColor = Colors.bgDefault,
                properties = ModalBottomSheetProperties(
                    securePolicy = SecureFlagPolicy.Inherit,
                    isFocusable = true,
                    shouldDismissOnBackPress = false,
                ),
            ) {
                TermsAgreementSheet(
                    terms = NicknameState.SHEET_TERMS,
                    agreedTerms = state.agreedTerms,
                    onCheck = { viewModel.onIntent(NicknameIntent.TermsCheckTapped(it)) },
                    onDetail = { viewModel.onIntent(NicknameIntent.TermsDetailTapped(it)) },
                    onAgree = { viewModel.onIntent(NicknameIntent.TermsAgreeButtonTapped) },
                )
            }
        }
    }
}

@Composable
private fun NicknameContent(
    state: NicknameState,
    focusRequester: FocusRequester,
    onIntent: (NicknameIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding(),
    ) {
        TopBar(onBack = { onIntent(NicknameIntent.BackClicked) })

        Spacer(modifier = Modifier.height(120.dp))

        Text(
            text = "둘픽에서 사용할 닉네임을 알려주세요",
            style = Typography.title1B,
            color = Colors.gray900,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 240.dp),
        )

        Spacer(modifier = Modifier.height(60.dp))

        AppTextField(
            value = state.nickname,
            onValueChange = { onIntent(NicknameIntent.NicknameChanged(it)) },
            placeholder = "최대 6글자",
            errorMessage = state.lengthError ?: state.inlineError,
            focusRequester = focusRequester,
            onSubmit = { onIntent(NicknameIntent.NextClicked) },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        CtaContainer {
            AppButton(
                text = "다음",
                onClick = { onIntent(NicknameIntent.NextClicked) },
                variant = AppButtonVariant.DARK,
                size = AppButtonSize.XL,
                fullWidth = true,
                enabled = state.isNextEnabled,
            )
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Image(
            painter = painterResource(R.drawable.arrowleft),
            contentDescription = "이전",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 12.dp)
                .size(24.dp)
                .clickable(onClick = onBack),
        )
        Text(
            text = "닉네임 설정",
            style = Typography.body1SB,
            color = Colors.gray900,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
