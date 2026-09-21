package com.dulpick.app.feature.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val BAR_HEIGHT = 48.dp
private val BAR_CORNER = 12.dp
private val BACK_SIZE = 44.dp

// 지도 위 검색바. query 가 null 이면 저장 모드(플레이스홀더+돋보기), 있으면 검색 결과 모드([뒤로][검색어 X]).
// (iOS MapSearchBar 두 상태 대응)
@Composable
fun MapSearchBar(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    query: String? = null,
    onBack: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
) {
    if (query != null) {
        SearchResultBar(
            query = query,
            onTap = onTap,
            onBack = onBack ?: {},
            onClear = onClear ?: {},
            modifier = modifier,
        )
    } else {
        SavedBar(onTap = onTap, modifier = modifier)
    }
}

// 저장 모드: 플레이스홀더 필드 + 오른쪽 핑크 돋보기 버튼
@Composable
private fun SavedBar(onTap: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(BAR_HEIGHT)
                .clip(RoundedCornerShape(BAR_CORNER))
                .background(Colors.bgDefault)
                .border(1.dp, Colors.borderDefault, RoundedCornerShape(BAR_CORNER))
                .clickable(onClick = onTap)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "원하는 장소를 검색하세요",
                style = Typography.body1M,
                color = Colors.gray400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(BAR_HEIGHT)
                .clip(RoundedCornerShape(BAR_CORNER))
                .background(Colors.brandPrimary)
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.search),
                contentDescription = "검색",
                colorFilter = ColorFilter.tint(Colors.commonWhite),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// 검색 결과 모드: [원형 뒤로] [검색어 필드 + X 지우기]
@Composable
private fun SearchResultBar(
    query: String,
    onTap: () -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(BACK_SIZE)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Colors.commonWhite)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.arrowleft),
                contentDescription = "뒤로",
                colorFilter = ColorFilter.tint(Colors.textSecondary),
                modifier = Modifier.size(24.dp),
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(BAR_HEIGHT)
                .clip(RoundedCornerShape(BAR_CORNER))
                .background(Colors.bgDefault)
                .border(1.dp, Colors.borderDefault, RoundedCornerShape(BAR_CORNER)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = query,
                style = Typography.body1M,
                color = Colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onTap)
                    .padding(start = 20.dp),
            )
            Image(
                painter = painterResource(R.drawable.x),
                contentDescription = "지우기",
                colorFilter = ColorFilter.tint(Colors.textTertiary),
                modifier = Modifier
                    .clickable(onClick = onClear)
                    .padding(horizontal = 20.dp)
                    .size(24.dp),
            )
        }
    }
}
