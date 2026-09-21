package com.dulpick.app.feature.placedetail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.place.PlaceDetail
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.ContentCard
import com.dulpick.app.ui.component.ContentCardSkeleton
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val PHOTO_SIZE = 140.dp

// 장소 상세 화면. 검색 결과 등에서 push 되어 뒤로가면 이전 목록으로 돌아온다.
// 4c: 조회·헤더·주소·지도 버튼·사진. 관련 게시물·북마크는 다음 단계
@Composable
fun PlaceDetailScreen(
    onClose: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: PlaceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler { onClose() }
    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            PlaceDetailSideEffect.Close -> onClose()
            PlaceDetailSideEffect.SessionExpired -> onSessionExpired()
            is PlaceDetailSideEffect.OpenKakaoMap -> context.openKakaoMap(effect.appUri, effect.webUrl)
            // TODO: 게시물 상세는 게시물 상세 화면 구현 시 연결한다
            is PlaceDetailSideEffect.OpenContent -> Unit
        }
    }
    LaunchedEffect(Unit) { viewModel.onIntent(PlaceDetailIntent.OnAppear) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.commonWhite)
            .statusBarsPadding(),
    ) {
        when {
            state.isLoading -> CenteredLoading(Modifier.fillMaxSize())
            state.detail != null -> DetailContent(state = state, onIntent = viewModel::onIntent)
            else -> FailedContent(onClose = { viewModel.onIntent(PlaceDetailIntent.CloseClicked) })
        }
    }
}

@Composable
private fun DetailContent(state: PlaceDetailState, onIntent: (PlaceDetailIntent) -> Unit) {
    val detail = state.detail ?: return
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Header(
            detail = detail,
            isBookmarked = state.isBookmarked,
            bookmarkCount = state.bookmarkCount,
            onIntent = onIntent,
        )
        if (detail.place.thumbnailUrls.isNotEmpty()) {
            PhotoStrip(urls = detail.place.thumbnailUrls, modifier = Modifier.padding(bottom = 12.dp))
        }
        // 사진 줄이 없으면 주소 위에 20 만큼 띄운다 (iOS 동일)
        val addressTop = if (detail.place.thumbnailUrls.isEmpty()) 20.dp else 0.dp
        AddressRow(
            detail = detail,
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
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
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

@Composable
private fun ContentsSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                ContentCardSkeleton(modifier = Modifier.weight(1f))
                ContentCardSkeleton(modifier = Modifier.weight(1f))
            }
        }
    }
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
            modifier = Modifier.size(120.dp),
        )
        Text(text = "게시물을 불러오지 못했어요", style = Typography.headline, color = Colors.textPrimary)
        Text(text = "잠시 뒤 다시 시도해주세요", style = Typography.body2M, color = Colors.textTertiary)
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
            .width(80.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Colors.gray200, RoundedCornerShape(8.dp))
            .then(Modifier.clickable(enabled = enabled, onClick = onClick))
            .wrapContentSize(),
    )
}

@Composable
private fun Header(
    detail: PlaceDetail,
    isBookmarked: Boolean,
    bookmarkCount: Int,
    onIntent: (PlaceDetailIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = detail.place.name, style = Typography.title3SB, color = Colors.textPrimary)
                Subtitle(category = detail.place.category, bookmarkCount = bookmarkCount)
            }
            HeaderIcon(
                res = if (isBookmarked) R.drawable.bookmarkfillcolor else R.drawable.bookmarkstroke,
                description = "저장",
                onClick = { onIntent(PlaceDetailIntent.BookmarkClicked) },
            )
            HeaderIcon(res = R.drawable.x, description = "닫기", onClick = { onIntent(PlaceDetailIntent.CloseClicked) })
        }
        MapButton(
            enabled = detail.canOpenKakaoMap(),
            onClick = { onIntent(PlaceDetailIntent.MapClicked) },
            modifier = Modifier.padding(start = 20.dp),
        )
    }
}

@Composable
private fun Subtitle(category: PlaceCategory, bookmarkCount: Int) {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = Colors.textTertiary)) {
            append("${category.categoryName()} · 저장한 사람 ")
        }
        withStyle(SpanStyle(color = Colors.brandPrimary)) {
            append("$bookmarkCount")
        }
    }
    Text(text = text, style = Typography.body2M)
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
private fun MapButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val contentColor = if (enabled) Colors.textSecondary else Colors.textDisabled
    Row(
        modifier = modifier
            .width(64.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Colors.commonWhite)
            .then(
                Modifier.clickable(enabled = enabled, onClick = onClick),
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        Image(
            painter = painterResource(R.drawable.mappin),
            contentDescription = null,
            colorFilter = ColorFilter.tint(contentColor),
            modifier = Modifier.size(16.dp),
        )
        Text(text = "지도", style = Typography.caption1M, color = contentColor)
    }
}

@Composable
private fun PhotoStrip(urls: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        urls.forEach { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(PHOTO_SIZE)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Colors.gray300),
            )
        }
    }
}

@Composable
private fun AddressRow(
    detail: PlaceDetail,
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
                text = detail.place.roadAddress,
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
                text = "[지번] ${detail.place.address}",
                style = Typography.caption1R,
                color = Colors.textTertiary,
                modifier = Modifier.padding(start = 28.dp),
            )
        }
    }
}

@Composable
private fun FailedContent(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "장소를 불러오지 못했어요", style = Typography.title3SB, color = Colors.textPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "닫기",
            style = Typography.body1SB,
            color = Colors.textTertiary,
            modifier = Modifier.clickable(onClick = onClose),
        )
    }
}

@Composable
private fun CenteredLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Colors.primaryPink)
    }
}

// 카카오맵 앱 우선, 실패하면 웹으로 (iOS openURL 폴백 대응)
private fun Context.openKakaoMap(appUri: String?, webUrl: String?) {
    if (appUri != null) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(appUri)))
            return
        } catch (ignored: ActivityNotFoundException) {
            // 앱이 없으면 웹으로 떨어진다
        }
    }
    if (webUrl != null) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
        } catch (ignored: ActivityNotFoundException) {
            // 열 수 있는 앱이 없으면 무시
        }
    }
}

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
