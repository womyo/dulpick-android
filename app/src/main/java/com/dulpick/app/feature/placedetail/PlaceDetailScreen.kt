package com.dulpick.app.feature.placedetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.link.openAppOrWeb
import com.dulpick.app.feature.placedetail.component.PlaceMapSheet
import com.dulpick.app.feature.placedetail.component.PlacePhotoViewer
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.ContentCard
import com.dulpick.app.ui.component.ShimmerBox
import com.dulpick.app.ui.component.RemoteImage
import com.dulpick.app.ui.component.SheetHandle
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val PHOTO_SIZE = 160.dp

// 장소 상세 시트 내용. 지도 위 바텀시트로 얹힌다 (iOS PlaceDetailView 대응).
// 넘겨받은 장소(target)로 즉시 그리고, 상세 API 는 부가정보만 덧입힌다. target 별로 VM 을 새로 만든다
@Composable
@Suppress("LongParameterList")
fun PlaceDetailSheet(
    target: Place,
    query: String,
    serverPlaceId: Long?,
    onClose: () -> Unit,
    onSessionExpired: () -> Unit,
    // 관련 게시물 카드 탭 → 지도가 게시글 상세를 이 위에 얹는다
    onOpenContent: (String) -> Unit,
    // 시트가 화면 바닥까지 내려와 있을 때 내용 끝에 둘 여백 (내비게이션 버튼 자리)
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
    viewModel: PlaceDetailViewModel = hiltViewModel(key = "detail-${target.id}"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            PlaceDetailSideEffect.Close -> onClose()
            PlaceDetailSideEffect.SessionExpired -> onSessionExpired()
            is PlaceDetailSideEffect.OpenMap -> context.openAppOrWeb(effect.appUri, effect.webUrl)
            is PlaceDetailSideEffect.OpenContent -> onOpenContent(effect.id)
        }
    }
    LaunchedEffect(target.id) {
        viewModel.onIntent(PlaceDetailIntent.Start(target, query, serverPlaceId))
    }

    val place = state.place ?: target
    state.photoViewerIndex?.let { index ->
        PlacePhotoViewer(
            urls = place.thumbnailUrls,
            initialIndex = index,
            // 녹화 가리기가 없는 화면이라 별칭이 아닌 실제 이름을 보인다
            title = place.name,
            onClose = { viewModel.onIntent(PlaceDetailIntent.PhotoViewerDismissed) },
        )
    }
    if (state.showsMapSheet) {
        MapAppSheet(
            title = place.name,
            onSelect = { app -> viewModel.onIntent(PlaceDetailIntent.MapAppSelected(app)) },
            onDismiss = { viewModel.onIntent(PlaceDetailIntent.MapSheetDismissed) },
        )
    }
    Column(modifier = modifier.fillMaxWidth().background(Colors.commonWhite)) {
        DetailContent(
            place = place,
            state = state,
            bottomInset = bottomInset,
            onIntent = viewModel::onIntent,
        )
    }
}

@Composable
private fun DetailContent(
    place: Place,
    state: PlaceDetailState,
    bottomInset: Dp,
    onIntent: (PlaceDetailIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomInset),
    ) {
        Header(
            place = place,
            detailCategory = state.detailCategoryText,
            isBookmarked = state.isBookmarked,
            bookmarkCount = state.bookmarkCount,
            onIntent = onIntent,
        )
        if (place.thumbnailUrls.isNotEmpty()) {
            // 지도 버튼과 사진 사이 16 (iOS PlacePhotoStrip 상단 패딩), 사진 아래 12
            PhotoStrip(
                urls = place.thumbnailUrls,
                onClick = { index -> onIntent(PlaceDetailIntent.PhotoClicked(index)) },
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            )
        }
        // 사진 줄이 없으면 주소 위에 20 만큼 띄운다 (iOS 동일)
        val addressTop = if (place.thumbnailUrls.isEmpty()) 20.dp else 0.dp
        AddressRow(
            place = place,
            isExpanded = state.isAddressExpanded,
            onToggle = { onIntent(PlaceDetailIntent.AddressToggled) },
            modifier = Modifier.padding(top = addressTop, bottom = 16.dp),
        )
        if (state.showsRelatedSection) {
            RelatedContentsSection(state = state, onIntent = onIntent)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun RelatedContentsSection(state: PlaceDetailState, onIntent: (PlaceDetailIntent) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
        Text(
            text = "장소와 관련된 게시물",
            style = Typography.headline,
            color = Colors.textPrimary,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        when {
            state.contents.isNotEmpty() -> ContentsGrid(state = state, onIntent = onIntent)
            state.contentsLoad == ContentsLoad.LOADING -> ContentsSkeleton()
            state.contentsLoad == ContentsLoad.FAILED ->
                ContentsFailure(onRetry = { onIntent(PlaceDetailIntent.RetryContentsClicked) })
            else -> Unit
        }
    }
}

@Composable
private fun ContentsGrid(state: PlaceDetailState, onIntent: (PlaceDetailIntent) -> Unit) {
    // 스크롤은 바깥 Column 이 맡는다. 여기선 2열로 직접 배치한다(LazyGrid 중첩 회피)
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        state.contents.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { content ->
                    ContentCard(
                        content = content,
                        onClick = { onIntent(PlaceDetailIntent.ContentClicked(content.id)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
        if (state.hasNextContents) {
            MoreButton(
                enabled = state.contentsLoad != ContentsLoad.LOADING,
                onClick = { onIntent(PlaceDetailIntent.MoreContentsClicked) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 20.dp),
            )
        }
    }
}

// iOS contentsSkeleton 대응. 카드 네 장 자리를 3:4 쉬머 블록으로만 채운다(제목 줄 없이)
@Composable
private fun ContentsSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ContentsSkeletonCell(modifier = Modifier.weight(1f))
                ContentsSkeletonCell(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ContentsSkeletonCell(modifier: Modifier = Modifier) {
    ShimmerBox(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun ContentsFailure(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.placeempty),
            contentDescription = null,
        )
        Text(text = "게시물을 불러오지 못했어요", style = Typography.title3SB, color = Colors.textPrimary)
        Text(text = "잠시 뒤 다시 시도해주세요", style = Typography.body1M, color = Colors.textTertiary)
        AppButton(
            text = "다시 시도",
            onClick = onRetry,
            variant = AppButtonVariant.OUTLINED,
            size = AppButtonSize.MD,
        )
    }
}

@Composable
private fun MoreButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = "더보기",
        style = Typography.caption1M,
        color = Colors.textSecondary,
        modifier = modifier
            .width(66.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Colors.gray200, RoundedCornerShape(8.dp))
            .then(Modifier.clickable(enabled = enabled, onClick = onClick))
            .wrapContentSize(),
    )
}

@Composable
private fun Header(
    place: Place,
    detailCategory: String?,
    isBookmarked: Boolean,
    bookmarkCount: Int,
    onIntent: (PlaceDetailIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 5.dp)) {
                Text(text = place.name, style = Typography.title3SB, color = Colors.textPrimary)
                Subtitle(
                    category = place.category,
                    detailCategory = detailCategory,
                    bookmarkCount = bookmarkCount,
                )
            }
            HeaderIcon(res = R.drawable.x, description = "닫기", onClick = { onIntent(PlaceDetailIntent.CloseClicked) })
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SaveButton(
                isBookmarked = isBookmarked,
                onClick = { onIntent(PlaceDetailIntent.BookmarkClicked) },
            )
            Spacer(modifier = Modifier.weight(1f))
            MapButton(onClick = { onIntent(PlaceDetailIntent.MapClicked) })
        }
    }
}

@Composable
private fun Subtitle(category: PlaceCategory, detailCategory: String?, bookmarkCount: Int) {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = Colors.textTertiary)) {
            append(category.categoryName())
            detailCategory?.let { append(" · $it") }
            append(" · 저장한 사람 ")
        }
        withStyle(SpanStyle(color = Colors.brandPrimary)) {
            append("$bookmarkCount")
        }
    }
    Text(text = text, style = Typography.body2M)
}

// 시안 「저장」. 저장 전에는 빈 아이콘을 회색으로, 저장 뒤에는 분홍 채움 아이콘을 쓴다
@Composable
private fun SaveButton(isBookmarked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Colors.commonWhite)
            .border(1.dp, Colors.gray200, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(
                if (isBookmarked) R.drawable.bookmarkfillcolor else R.drawable.bookmarkstroke,
            ),
            contentDescription = null,
            colorFilter = if (isBookmarked) null else ColorFilter.tint(Colors.textTertiary),
            modifier = Modifier.size(16.dp),
        )
        Text(text = "저장", style = Typography.caption1M, color = Colors.textSecondary)
    }
}

@Composable
private fun HeaderIcon(res: Int, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painter = painterResource(res), contentDescription = description, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun MapButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Colors.gray900)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.map),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Colors.gray50),
            modifier = Modifier.size(16.dp),
        )
        Text(text = "지도로 보기", style = Typography.caption1M, color = Colors.commonWhite)
    }
}

@Composable
private fun PhotoStrip(urls: List<String>, onClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        urls.forEachIndexed { index, url ->
            RemoteImage(
                url = url,
                cornerRadius = 12.dp,
                modifier = Modifier.size(PHOTO_SIZE).clickable { onClick(index) },
            )
        }
    }
}

@Composable
private fun AddressRow(
    place: Place,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.clickable(onClick = onToggle).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.mappin),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Colors.textSecondary),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = place.roadAddress,
                style = Typography.body2M,
                color = Colors.textSecondary,
                modifier = Modifier.weight(1f, fill = false),
            )
            Image(
                painter = painterResource(if (isExpanded) R.drawable.arrowup else R.drawable.arrowdown),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Colors.textSecondary),
                modifier = Modifier.size(16.dp),
            )
        }
        if (isExpanded) {
            Text(
                text = "[지번] ${place.address}",
                style = Typography.caption1R,
                color = Colors.textTertiary,
                modifier = Modifier.padding(start = 28.dp),
            )
        }
    }
}

// 카카오맵 앱 우선, 실패하면 웹으로 (iOS openURL 폴백 대응)
// 장소 카테고리 한글명 (iOS PlaceCategory.displayName 대응). 상세 전용으로 로컬에 둔다
private fun PlaceCategory.categoryName(): String = when (this) {
    PlaceCategory.FOOD -> "맛집"
    PlaceCategory.CAFE -> "카페"
    PlaceCategory.ACTIVITY -> "놀거리"
    PlaceCategory.SHOPPING -> "쇼핑"
    PlaceCategory.ACCOMMODATION -> "숙박"
    PlaceCategory.TOURISM -> "관광"
    PlaceCategory.CONVENIENCE -> "생활편의"
}

// 지도 앱 고르기 시트. 지도 위 상세 시트 위에 또 띄우므로 모달로 올린다
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapAppSheet(title: String, onSelect: (PlaceMapApp) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Colors.commonWhite,
        // 기본 tonalElevation 이 흰색에 톤 오버레이를 얹어 색이 뜨므로 끈다
        tonalElevation = 0.dp,
        dragHandle = { SheetHandle() },
        // 기본값은 내비게이션 바 높이만큼 시트를 띄운다. 그 틈으로 뒤 화면이 비친다.
        // 시트를 바닥까지 내리고 그 여백은 안의 내용이 직접 가진다
        windowInsets = WindowInsets(0),
    ) {
        PlaceMapSheet(
            title = title,
            // 지도 검색어는 실제 장소 이름이라 가릴 것이 없다
            isAlias = false,
            onSelect = onSelect,
            onClose = onDismiss,
        )
    }
}
