package com.dulpick.app.feature.onboarding.datetype.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// tip 버튼 아래 말풍선. 위쪽 화살표가 버튼을 가리키도록 오른쪽에 붙는다 (iOS DateTypeTooltip 대응)
@Composable
fun DateTypeTooltip(text: String) {
    Column(
        modifier = Modifier.width(262.dp),
        horizontalAlignment = Alignment.End,
    ) {
        UpArrow(modifier = Modifier.padding(end = 24.dp))
        Text(
            text = text,
            style = Typography.caption2M,
            color = Colors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Colors.commonWhite)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun UpArrow(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 18.dp, height = 10.dp)) {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path = path, color = Colors.commonWhite)
    }
}
