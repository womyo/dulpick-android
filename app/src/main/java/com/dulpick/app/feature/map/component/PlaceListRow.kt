package com.dulpick.app.feature.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import coil.compose.AsyncImage
import com.dulpick.app.R
import com.dulpick.app.domain.place.SavedPlace
import com.dulpick.app.ui.component.iconRes
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 썸네일 줄이 장소명(아이콘 24 + 간격 12 + 좌패딩 20) 에 맞게 들어가는 왼쪽 인셋
private val THUMBNAIL_LEADING_INSET = 56.dp
private val THUMBNAIL_SIZE = 88.dp

// 지도 시트 목록의 장소 한 줄. 전체 폭 + 하단 구분선, 이미지가 있으면 가로 스트립 (iOS PlaceListRow 대응)
@Composable
fun PlaceListRow(place: SavedPlace, onMenuClick: () -> Unit, onClick: () -> Unit) {
    val name = place.alias ?: place.place.name
    val address = place.place.roadAddress.ifEmpty { place.place.address }
    val thumbnails = place.place.thumbnailUrls

    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RowHeader(
                name = name,
                address = address,
                category = place.place.category.iconRes(),
                onMenuClick = onMenuClick,
            )
            if (thumbnails.isNotEmpty()) {
                ThumbnailStrip(urls = thumbnails)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Colors.borderWeak),
        )
    }
}

@Composable
private fun RowHeader(name: String, address: String, category: Int, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            painter = painterResource(category),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = Typography.body1SB,
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
            painter = painterResource(R.drawable.menu),
            contentDescription = "장소 메뉴",
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onMenuClick),
        )
    }
}

@Composable
private fun ThumbnailStrip(urls: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = THUMBNAIL_LEADING_INSET, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        urls.forEach { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(THUMBNAIL_SIZE)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Colors.gray300),
            )
        }
    }
}
