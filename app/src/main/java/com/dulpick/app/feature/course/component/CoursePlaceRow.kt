package com.dulpick.app.feature.course.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dulpick.app.domain.place.SavedPlace
import com.dulpick.app.ui.component.iconRes
import com.dulpick.app.ui.component.RemoteImage
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 저장한 장소 목록과 같은 크기·간격. 들여쓰기는 아이콘 24 + 간격 12 + 바깥 여백 20
private val THUMBNAIL_SIZE = 88.dp
private val THUMBNAIL_LEADING_INSET = 56.dp

// 코스에 담을 장소 한 행. 고른 행은 연분홍 배경에 번호 배지가 붙는다 (iOS CoursePlacePickView.row 대응)
@Composable
fun CoursePlaceRow(
    place: SavedPlace,
    number: Int?,
    showsDivider: Boolean,
    onClick: () -> Unit,
) {
    val name = place.alias ?: place.place.name
    val address = place.place.roadAddress.ifEmpty { place.place.address }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 배경은 고른 행만 칠한다 (시안 b06)
            .background(if (number == null) Colors.commonWhite else Colors.brandSurface)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Image(
                    painter = painterResource(place.place.category.iconRes()),
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
                PlaceNumberBadge(number = number)
            }
            if (place.place.thumbnailUrls.isNotEmpty()) {
                ThumbnailStrip(urls = place.place.thumbnailUrls)
            }
        }
        if (showsDivider) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Colors.borderWeak))
        }
    }
}

// 안 고르면 빈 원, 고르면 순번이 든 채운 원. 바깥 24 안에 원 20 (iOS PlaceNumberBadge 대응)
@Composable
private fun PlaceNumberBadge(number: Int?) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        if (number == null) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.dp, Colors.borderDefault, CircleShape),
            )
        } else {
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape).background(Colors.brandPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "$number", style = Typography.caption1M, color = Colors.textInverse)
            }
        }
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
            RemoteImage(
                url = url,
                cornerRadius = 12.dp,
                modifier = Modifier.size(THUMBNAIL_SIZE),
            )
        }
    }
}
