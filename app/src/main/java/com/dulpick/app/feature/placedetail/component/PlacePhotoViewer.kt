package com.dulpick.app.feature.placedetail.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dulpick.app.R
import com.dulpick.app.ui.component.RemoteImage
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography
import kotlinx.coroutines.launch
import kotlin.math.abs

private val CLOSE_BUTTON_SIZE = 40.dp
private val ARROW_BUTTON_SIZE = 36.dp
private val ARROW_ICON_SIZE = 20.dp
private val DOT_SIZE = 4.dp
private val DOT_SMALL_SIZE = 2.dp
private val DOT_CURRENT_WIDTH = 20.dp

// 이만큼 내려놓으면 닫히고, 이만큼 내리면 배경과 장식이 다 흐려진다
private const val DISMISS_DISTANCE = 120f
private const val DISMISS_FADE_DISTANCE = 300f
private const val MAX_ZOOM = 3f
private const val DOUBLE_TAP_ZOOM = 2.5f

// 전체 화면 사진 뷰어. 왼쪽 위 이름은 별칭이 아니라 실제 장소 이름을 받는다
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlacePhotoViewer(urls: List<String>, initialIndex: Int, title: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ViewerContent(urls = urls, initialIndex = initialIndex, title = title, onClose = onClose)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ViewerContent(urls: List<String>, initialIndex: Int, title: String, onClose: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { urls.size })
    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableFloatStateOf(0f) }
    // 끌어내린 만큼 배경과 장식이 흐려진다
    val visibility = (1f - abs(dragOffset) / DISMISS_FADE_DISTANCE).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.gray900.copy(alpha = visibility))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (abs(dragOffset) > DISMISS_DISTANCE) onClose() else dragOffset = 0f
                    },
                ) { _, delta -> dragOffset += delta }
            },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = dragOffset },
        ) { page ->
            ZoomablePhoto(url = urls[page])
        }

        TopBar(title = title, visibility = visibility, onClose = onClose)

        if (pagerState.currentPage > 0) {
            ArrowButton(
                res = R.drawable.arrowleft,
                description = "이전 사진",
                visibility = visibility,
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
            )
        }
        if (pagerState.currentPage < urls.lastIndex) {
            ArrowButton(
                res = R.drawable.arrowright,
                description = "다음 사진",
                visibility = visibility,
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            )
        }

        Dots(
            dots = placePhotoDots(urls.size, pagerState.currentPage),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .graphicsLayer { alpha = visibility },
        )
    }
}

@Composable
private fun TopBar(title: String, visibility: Float, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(end = 12.dp)
            .graphicsLayer { alpha = visibility },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = Typography.title3SB,
            color = Colors.commonWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 20.dp, end = 8.dp),
        )
        Image(
            painter = painterResource(R.drawable.x),
            contentDescription = "닫기",
            colorFilter = ColorFilter.tint(Colors.commonWhite),
            modifier = Modifier
                .size(CLOSE_BUTTON_SIZE)
                .clickable(onClick = onClose)
                .padding(8.dp),
        )
    }
}

@Composable
private fun ArrowButton(
    res: Int,
    description: String,
    visibility: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ARROW_BUTTON_SIZE)
            .graphicsLayer { alpha = visibility }
            .clip(CircleShape)
            .background(Colors.dimBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(res),
            contentDescription = description,
            colorFilter = ColorFilter.tint(Colors.commonWhite),
            modifier = Modifier.size(ARROW_ICON_SIZE),
        )
    }
}

@Composable
private fun Dots(dots: List<PlacePhotoDot>, modifier: Modifier = Modifier) {
    if (dots.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dots.forEach { dot ->
            val height = if (dot.isSmall) DOT_SMALL_SIZE else DOT_SIZE
            Box(
                modifier = Modifier
                    .width(if (dot.isCurrent) DOT_CURRENT_WIDTH else height)
                    .height(height)
                    .clip(CircleShape)
                    .background(if (dot.isCurrent) Colors.gray300 else Colors.gray700),
            )
        }
    }
}

// 두 손가락으로 늘리고 두 번 눌러 확대한다. 확대 중에는 끌어서 사진 안을 돌아본다
@Composable
private fun ZoomablePhoto(url: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedScale by animateFloatAsState(scale, label = "photoScale")

    // 페이지가 바뀌면 확대를 되돌린다
    LaunchedEffect(url) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    val transform = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        RemoteImage(
            url = url,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            placeholderColor = Colors.gray900,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationX = offsetX
                    translationY = offsetY
                }
                .transformable(transform)
                .pointerInput(url) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = DOUBLE_TAP_ZOOM
                            }
                        },
                    )
                },
        )
    }
}
