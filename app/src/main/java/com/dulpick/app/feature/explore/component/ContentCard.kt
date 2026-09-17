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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.domain.explore.Content
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
                .background(Colors.bgSubtle),
        ) {
            // TODO: 실제 원격 썸네일은 이미지 로더 붙이는 단계에서. 지금은 플레이스홀더
            Image(
                painter = painterResource(R.drawable.placeempty),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
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
