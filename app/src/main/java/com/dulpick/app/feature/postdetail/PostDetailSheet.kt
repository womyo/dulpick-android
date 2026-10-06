package com.dulpick.app.feature.postdetail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.explore.ContentPlace
import com.dulpick.app.domain.explore.PostDetailContent
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.ShimmerBox
import com.dulpick.app.ui.component.iconRes
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 접힘에서 본문이 보이는 줄 수
private const val CAPTION_LINE_LIMIT = 3
private const val SKELETON_ROW_COUNT = 3
private val ICON_SIZE = 24.dp
private val ROW_HEIGHT = 56.dp
private val ROW_CORNER_RADIUS = 12.dp

// 게시글 상세 시트 내용. 지도 위 바텀시트로 얹혀 제목·본문·저장할 수 있는 곳을 보인다
// (iOS PostDetailView 대응)
@Composable
fun PostDetailSheet(
    contentId: String,
    onClose: () -> Unit,
    onDetailLoaded: (PostDetailContent) -> Unit,
    onPlaceSelected: (String) -> Unit,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PostDetailViewModel = hiltViewModel(key = "post-$contentId"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            PostDetailSideEffect.Close -> onClose()
            is PostDetailSideEffect.DetailLoaded -> onDetailLoaded(effect.detail)
            is PostDetailSideEffect.PlaceSelected -> onPlaceSelected(effect.id)
            PostDetailSideEffect.SessionExpired -> onSessionExpired()
        }
    }
    LaunchedEffect(contentId) { viewModel.onIntent(PostDetailIntent.Start(contentId)) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Colors.commonWhite)
            .verticalScroll(rememberScrollState()),
    ) {
        Header(state = state, onIntent = viewModel::onIntent, context = context)
        when {
            state.isLoading -> Skeleton()
            state.loadFailed -> Failure(onRetry = { viewModel.onIntent(PostDetailIntent.RetryClicked) })
            else -> Loaded(state = state, onIntent = viewModel::onIntent)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// 제목 · 닫기 · 인스타 버튼 (iOS PostDetailSheetHeader 대응)
@Composable
private fun Header(state: PostDetailState, onIntent: (PostDetailIntent) -> Unit, context: Context) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 부르는 중에는 줄을 안 그린다. "제목없음" 이 스쳤다 바뀌면 깜빡이는 것처럼 보인다
            if (state.detail != null) {
                Text(
                    text = state.displayTitle,
                    style = Typography.title3SB,
                    color = Colors.textPrimary,
                    modifier = Modifier.weight(1f).padding(vertical = 5.dp),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            CloseButton(onClick = { onIntent(PostDetailIntent.CloseClicked) })
        }
        state.detail?.canonicalUrl?.let { url ->
            InstagramButton(onClick = { context.openLink(url) })
        }
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    // 아이콘은 24, 손가락 자리는 40
    Image(
        painter = painterResource(R.drawable.x),
        contentDescription = "닫기",
        colorFilter = ColorFilter.tint(Colors.textTertiary),
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
            .wrapContentSize()
            .size(ICON_SIZE),
    )
}

@Composable
private fun InstagramButton(onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Colors.borderDefault, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            // 시안 padding: 7px 16px
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.insta),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(text = "원문보기", style = Typography.caption1M, color = Colors.textSecondary)
    }
}

@Composable
private fun Loaded(state: PostDetailState, onIntent: (PostDetailIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val caption = state.detail?.caption
        if (!caption.isNullOrEmpty()) {
            ExpandableText(
                text = caption,
                isExpanded = state.isExpanded,
                onToggle = { onIntent(PostDetailIntent.ExpandToggled) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
        val places = state.detail?.places.orEmpty()
        if (places.isNotEmpty()) {
            PlaceSection(places = places, savedIds = state.savedPlaceIds, onIntent = onIntent)
        }
    }
}

@Composable
private fun PlaceSection(
    places: List<ContentPlace>,
    savedIds: Set<String>,
    onIntent: (PostDetailIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "저장할 수 있는 곳", style = Typography.headline, color = Colors.textPrimary)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            places.forEach { place ->
                PostPlaceRow(
                    place = place,
                    isSavedLocally = place.id in savedIds,
                    onTap = { onIntent(PostDetailIntent.PlaceClicked(place.id)) },
                    onBookmarkTap = { onIntent(PostDetailIntent.PlaceBookmarkClicked(place.id)) },
                )
            }
        }
    }
}

// 게시글에 딸린 장소 한 행. 시안 353×56, 배경 bgSubtle, 반지름 12 (iOS PostPlaceRow 대응)
@Composable
private fun PostPlaceRow(
    place: ContentPlace,
    isSavedLocally: Boolean,
    onTap: () -> Unit,
    onBookmarkTap: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .clip(RoundedCornerShape(ROW_CORNER_RADIUS))
            .background(Colors.bgSubtle)
            .clickable(onClick = onTap)
            .padding(horizontal = 20.dp),
    ) {
        Image(
            painter = painterResource(place.place.category.iconRes()),
            contentDescription = null,
            modifier = Modifier.size(ICON_SIZE),
        )
        Text(
            text = place.place.name,
            style = Typography.body1M,
            color = Colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(
                if (isSavedLocally) R.drawable.bookmarkfillcolor else R.drawable.bookmarkstroke,
            ),
            contentDescription = if (isSavedLocally) "저장 해제" else "저장",
            modifier = Modifier.size(ICON_SIZE).clickable(onClick = onBookmarkTap),
        )
    }
}

// 본문을 세 줄로 자르고, 넘칠 때만 더보기를 붙인다 (iOS ExpandableText 대응).
// 안드로이드는 TextLayoutResult 로 잘림을 바로 알 수 있어 재는 겹을 깔지 않는다
@Composable
private fun ExpandableText(
    text: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isTruncated by remember(text) { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = text,
            style = Typography.body2M,
            color = Colors.textSecondary,
            maxLines = if (isExpanded) Int.MAX_VALUE else CAPTION_LINE_LIMIT,
            overflow = TextOverflow.Ellipsis,
            // 펼친 뒤에는 잘림 여부가 false 로 바뀌므로, 접힌 상태의 판정만 기억한다
            onTextLayout = { if (!isExpanded) isTruncated = it.hasVisualOverflow },
            modifier = Modifier.fillMaxWidth(),
        )
        if (isTruncated) {
            Text(
                text = if (isExpanded) "접기" else "더보기",
                style = Typography.body2SB,
                color = Colors.textTertiary,
                modifier = Modifier.clickable(onClick = onToggle).padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun Skeleton() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(SKELETON_ROW_COUNT) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .clip(RoundedCornerShape(ROW_CORNER_RADIUS)),
            )
        }
    }
}

// 시트 안에서 끝낸다. 알림 띠는 지도 쪽 것이라 여기서 안 쓴다
@Composable
private fun Failure(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.placeempty),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
        )
        Text(text = "게시글을 불러오지 못했어요", style = Typography.headline, color = Colors.textPrimary)
        Text(text = "잠시 뒤 다시 시도해주세요", style = Typography.body2M, color = Colors.textTertiary)
        AppButton(
            text = "다시 시도",
            onClick = onRetry,
            variant = AppButtonVariant.OUTLINED,
            size = AppButtonSize.MD,
        )
    }
}

// 인스타 원문. 앱이 없으면 브라우저로 열린다
private fun Context.openLink(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (ignored: ActivityNotFoundException) {
        // 열 수 있는 앱이 없으면 아무 일도 하지 않는다
    }
}
