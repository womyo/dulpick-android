package com.dulpick.app.feature.placedetail.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.core.analytics.analyticsMasked
import com.dulpick.app.feature.placedetail.PlaceMapApp
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 「지도로 보기」 시트. 고른 앱을 기억하지 않아 두 줄을 늘 보인다
@Composable
fun PlaceMapSheet(
    title: String,
    isAlias: Boolean,
    onSelect: (PlaceMapApp) -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Header(title = title, isAlias = isAlias, onClose = onClose)
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlaceMapApp.entries.forEach { app ->
                AppRow(app = app, onClick = { onSelect(app) })
            }
        }
    }
}

@Composable
private fun Header(title: String, isAlias: Boolean, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 5.dp)) {
            Text(text = "지도로 보기", style = Typography.title3SB, color = Colors.textPrimary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.mappin),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Colors.textTertiary),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = title,
                    style = Typography.body2M,
                    color = Colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.analyticsMasked(isAlias),
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.x),
            contentDescription = "닫기",
            colorFilter = ColorFilter.tint(Colors.textTertiary),
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onClose)
                .padding(8.dp),
        )
    }
}

@Composable
private fun AppRow(app: PlaceMapApp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Colors.bgSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painter = painterResource(app.logoRes()),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = app.sheetTitle(),
            style = Typography.body1M,
            color = Colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(R.drawable.arrowright),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Colors.textTertiary),
            modifier = Modifier.size(24.dp),
        )
    }
}

private fun PlaceMapApp.sheetTitle(): String = when (this) {
    PlaceMapApp.NAVER -> "네이버 지도로 보기"
    PlaceMapApp.KAKAO -> "카카오맵으로 보기"
}

private fun PlaceMapApp.logoRes(): Int = when (this) {
    PlaceMapApp.NAVER -> R.drawable.logo_navermap
    PlaceMapApp.KAKAO -> R.drawable.logo_kakaomap
}
