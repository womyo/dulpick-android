package com.dulpick.app.feature.placeimport

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.domain.placeimport.ImportCandidate
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.iconRes
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 한 페이지에 담는 후보 수
private const val CANDIDATES_PER_PAGE = 4

// 후보 행 고정 높이. 페이지마다 후보 수가 달라도 높이가 흔들리지 않게 4개 기준으로 고정한다
private val CANDIDATE_ROW_HEIGHT = 80.dp

// 후보 페이저 고정 높이(4행 + 3간격). 세로 스크롤 안에서도 무한 제약 없이 측정되도록 명시한다
private val CANDIDATES_PAGE_HEIGHT = CANDIDATE_ROW_HEIGHT * CANDIDATES_PER_PAGE + 8.dp * (CANDIDATES_PER_PAGE - 1)

// 공유 링크에서 뽑은 장소 후보를 골라 저장하는 모달 (iOS PlaceImportView 대응).
// Dialog 로 띄워 회원탈퇴 모달처럼 탭바 포함 전 화면을 딤 처리하고 뒤 조작을 막는다
@Composable
fun PlaceImportScreen(
    sourceUrl: String,
    onClose: () -> Unit,
    onSessionExpired: () -> Unit,
    // 링크가 바뀌면 새 세션이 되도록 sourceUrl 로 ViewModel 을 구분한다
    viewModel: PlaceImportViewModel = hiltViewModel(key = sourceUrl),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            PlaceImportSideEffect.Dismiss -> onClose()
            PlaceImportSideEffect.SessionExpired -> onSessionExpired()
        }
    }
    LaunchedEffect(sourceUrl) { viewModel.onIntent(PlaceImportIntent.Start(sourceUrl)) }

    Dialog(
        onDismissRequest = onClose,
        // 추출한 후보가 날아가지 않게 바깥(딤) 탭으로는 닫지 않는다. 뒤로가기·닫기 버튼만 허용
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Colors.commonWhite)
                // 짧은 화면(가로·큰 글꼴)에서도 저장 버튼에 닿도록 카드 내부를 스크롤 가능하게 한다
                .verticalScroll(rememberScrollState())
                .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            when (state.phase) {
                PlaceImportState.Phase.Loading -> LoadingContent()
                PlaceImportState.Phase.Failed ->
                    FailedContent(onClose = { viewModel.onIntent(PlaceImportIntent.CloseClicked) })
                is PlaceImportState.Phase.Loaded ->
                    LoadedContent(state = state, onIntent = viewModel::onIntent)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "장소를 추출중이에요", style = Typography.title2B, color = Colors.gray900)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "잠시만 기다려주세요", style = Typography.body1M, color = Colors.textTertiary)
        CircularProgressIndicator(
            color = Colors.primaryPink,
            modifier = Modifier.padding(top = 53.dp, bottom = 33.dp),
        )
    }
}

@Composable
private fun FailedContent(onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "장소를 추출할 수 없어요!", style = Typography.title2B, color = Colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "다른 게시물 링크로 공유해보세요", style = Typography.body1M, color = Colors.textTertiary)
        Image(
            painter = painterResource(R.drawable.placeempty),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 33.dp, bottom = 40.dp),
        )
        AppButton(
            text = "닫기",
            onClick = onClose,
            variant = AppButtonVariant.DARK,
            size = AppButtonSize.XL,
            fullWidth = true,
        )
    }
}

@Composable
private fun LoadedContent(state: PlaceImportState, onIntent: (PlaceImportIntent) -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = state.title,
            style = Typography.title2B,
            color = Colors.gray900,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OriginButton(onClick = { context.openExternal(state.canonicalUrl) })

        Spacer(modifier = Modifier.height(24.dp))
        Row {
            Text(text = "저장 가능한 장소 ", style = Typography.headline, color = Colors.textPrimary)
            Text(text = "${state.candidates.size}곳", style = Typography.headline, color = Colors.primaryPink)
        }

        CandidatesPager(
            candidates = state.candidates,
            selectedIds = state.selectedIds,
            onToggle = { onIntent(PlaceImportIntent.CandidateToggled(it)) },
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))
        AppButton(
            text = state.saveButtonTitle,
            onClick = { onIntent(PlaceImportIntent.SaveClicked) },
            variant = AppButtonVariant.DARK,
            size = AppButtonSize.XL,
            fullWidth = true,
        )
    }
}

@Composable
private fun OriginButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Colors.gray200, RoundedCornerShape(8.dp))
            .noRippleClick(onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.insta),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(text = "원문보기", style = Typography.caption1M, color = Colors.textSecondary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CandidatesPager(
    candidates: List<ImportCandidate>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = remember(candidates) { candidates.chunked(CANDIDATES_PER_PAGE) }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState, modifier = Modifier.height(CANDIDATES_PAGE_HEIGHT)) { page ->
            val items = pages[page]
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { candidate ->
                    CandidateRow(
                        candidate = candidate,
                        isSelected = candidate.candidateId in selectedIds,
                        onClick = { onToggle(candidate.candidateId) },
                    )
                }
                // 후보가 4개 미만이어도 빈 자리로 채워 페이지 높이를 4개 기준으로 고정한다
                repeat(CANDIDATES_PER_PAGE - items.size) {
                    Spacer(modifier = Modifier.fillMaxWidth().height(CANDIDATE_ROW_HEIGHT))
                }
            }
        }
        if (pages.size > 1) {
            PageIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(pageCount) { page ->
            val active = page == currentPage
            Box(
                modifier = Modifier
                    .size(width = if (active) 17.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (active) Colors.textPrimary else Colors.borderDefault),
            )
        }
    }
}

@Composable
private fun CandidateRow(candidate: ImportCandidate, isSelected: Boolean, onClick: () -> Unit) {
    val name = candidate.place?.name ?: candidate.extractedName
    val address = candidate.place?.roadAddress ?: candidate.extractedAddressHint.orEmpty()
    val iconRes = (candidate.place?.category ?: PlaceCategory.FOOD).iconRes()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CANDIDATE_ROW_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(Colors.bgSubtle)
            .noRippleClick(onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = name,
                style = Typography.body1M,
                color = Colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = address,
                style = Typography.caption1R,
                color = Colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Image(
            painter = painterResource(if (isSelected) R.drawable.checktrue else R.drawable.checkfalse),
            contentDescription = if (isSelected) "선택됨" else "선택 안 됨",
            modifier = Modifier.size(24.dp),
        )
    }
}

// 리플 없이 클릭만 받는다 (스크림·카드·행 공통)
private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

// 브라우저 등 외부 앱으로 원문 링크 열기. 열 앱이 없으면 크래시 대신 무시한다
private fun Context.openExternal(url: String) {
    if (url.isEmpty()) return
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (ignored: ActivityNotFoundException) {
        // 열 수 있는 앱이 없으면 아무것도 하지 않는다
    }
}
