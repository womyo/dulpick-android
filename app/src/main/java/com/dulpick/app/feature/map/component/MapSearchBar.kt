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
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val BAR_HEIGHT = 48.dp
private val BAR_CORNER = 12.dp

// 지도 위 검색바(저장 장소 모드). 플레이스홀더를 눌러 검색 화면을 연다 (iOS MapSearchBar 대응).
// 검색어 표시·뒤로가기(검색 모드)는 다음 단계에서 붙인다
@Composable
fun MapSearchBar(onTap: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 플레이스홀더 필드
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
        // 오른쪽 핑크 돋보기 버튼
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
