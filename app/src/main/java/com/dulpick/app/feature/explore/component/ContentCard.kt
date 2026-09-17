package com.dulpick.app.feature.explore.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.dulpick.app.R
import com.dulpick.app.domain.explore.Content
import com.dulpick.app.ui.component.ShimmerBox
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 탐색 그리드 카드. 썸네일(비율 170:227) + 장소 수 배지 + 제목 2줄 (iOS ContentCard 대응)
@Composable
fun ContentCard(content: Content, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(THUMBNAIL_RATIO)
                .clip(RoundedCornerShape(16.dp))
                // 이미지가 없거나 로딩 중엔 이 회색 배경이 보인다 (iOS gray500)
                .background(Colors.gray500),
        ) {
            SubcomposeAsyncImage(
                model = content.thumbnailUrls.firstOrNull(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                // 로딩 중엔 쉬머, 실패·URL 없음은 iOS placeEmpty 를 중앙에 얹는다
                loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
                error = { EmptyThumbnail() },
            )
            PlaceCountBadge(
                count = content.placeCount,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 14.dp),
            )
        }

        Text(
            text = content.title,
            style = Typography.body2M,
            color = Colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// iOS RemoteImage placeholder: gray500 배경(부모) + 하단 그라디언트 + placeEmpty 중앙 fit(폭 140)
@Composable
private fun EmptyThumbnail() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))),
                ),
        )
        Image(
            painter = painterResource(R.drawable.placeempty),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .width(140.dp),
        )
    }
}

@Composable
private fun PlaceCountBadge(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.mappin),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Colors.commonWhite),
            modifier = Modifier.size(16.dp),
        )
        Text(text = "$count", style = Typography.body2SB, color = Colors.commonWhite)
    }
}

private const val THUMBNAIL_RATIO = 170f / 227f
