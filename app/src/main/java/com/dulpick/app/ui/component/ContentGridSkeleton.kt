package com.dulpick.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private const val THUMBNAIL_RATIO = 170f / 227f
private const val CARD_ROWS = 2

// 게시글 2×2 그리드 첫 로딩 자리. 탐색·검색이 함께 쓴다 (iOS ContentGridSkeleton 대응)
@Composable
fun ContentGridSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        repeat(CARD_ROWS) {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                ContentCardSkeleton(modifier = Modifier.weight(1f))
                ContentCardSkeleton(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ContentCard 와 같은 비율·모서리·간격으로 이미지와 2줄 제목 자리를 쉬머로 채운다
@Composable
private fun ContentCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(THUMBNAIL_RATIO)
                .clip(RoundedCornerShape(16.dp)),
        )
        // 제목 2줄 자리. 둘째 줄은 짧게 둬 실제 텍스트처럼 보이게 한다
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            ShimmerBox(
                modifier = Modifier
                    .width(60.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}
