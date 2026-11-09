package com.dulpick.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors

// 시트 위의 잡는 칸. 위 여백 8 + 막대 4 + 아래 여백 8 = 20 이다
val SHEET_HANDLE_AREA = 20.dp
private val BAR_WIDTH = 50.dp
private val BAR_HEIGHT = 4.dp
private val BAR_VERTICAL_PADDING = 8.dp

@Composable
fun SheetHandle() {
    Box(
        modifier = Modifier.fillMaxWidth().height(SHEET_HANDLE_AREA),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = BAR_VERTICAL_PADDING)
                .size(BAR_WIDTH, BAR_HEIGHT)
                .clip(CircleShape)
                .background(Colors.gray100),
        )
    }
}
