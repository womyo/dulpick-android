package com.dulpick.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.dulpick.app.ui.theme.Colors

// 원격 이미지 한 장. 로딩은 시머, 실패와 주소 없음은 placeholder 다 (iOS RemoteImage 대응).
// 장소 사진·게시물 썸네일이 함께 쓴다 — 화면마다 따로 만들면 로딩 모양이 갈린다
@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    contentScale: ContentScale = ContentScale.Crop,
    // 로딩 시머와 기본 placeholder 바탕색
    placeholderColor: Color = Colors.gray300,
    // 실패·주소 없음에 얹을 것. 안 주면 바탕색만 보인다
    placeholder: (@Composable () -> Unit)? = null,
) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(placeholderColor),
        loading = { ShimmerBox(modifier = Modifier.fillMaxSize(), baseColor = placeholderColor) },
        error = { Placeholder(placeholder) },
    )
}

@Composable
private fun Placeholder(content: (@Composable () -> Unit)?) {
    if (content == null) {
        Box(modifier = Modifier.fillMaxSize())
    } else {
        content()
    }
}
