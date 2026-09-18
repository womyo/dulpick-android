package com.dulpick.app.feature.search.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.dulpick.app.R
import com.dulpick.app.domain.place.Place
import com.dulpick.app.ui.component.ShimmerBox
import com.dulpick.app.ui.component.iconRes
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 검색 결과 장소 한 행. 카테고리 아이콘 + 이름 + 저장수 배지, 썸네일이 있으면 가로 스크롤 (iOS PlaceRow 대응)
@Composable
fun PlaceRow(place: Place, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Colors.bgSubtle)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(place.category.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = place.name,
                style = Typography.body1M,
                color = Colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            BookmarkBadge(count = place.bookmarkCount)
        }

        if (place.thumbnailUrls.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                place.thumbnailUrls.forEach { url ->
                    // 장소는 placeEmpty 를 안 쓴다. 로딩 중엔 쉬머, 실패는 회색
                    SubcomposeAsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
                        error = { Box(modifier = Modifier.fillMaxSize().background(Colors.gray100)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkBadge(count: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Colors.commonWhite)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.bookmarkfillcolor),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        Text(text = "$count", style = Typography.body2SB, color = Colors.textSecondary)
    }
}
