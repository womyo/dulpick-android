package com.dulpick.app.feature.course.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 눌러서 시트를 여는 표시 전용 입력 필드. 키보드가 뜨면 안 되는 자리에 쓴다
// (iOS CourseInputField 대응)
@Composable
fun CourseInputField(
    value: String?,
    placeholder: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Colors.bgSubtle)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = value ?: placeholder,
                style = Typography.body1M,
                color = if (value == null) Colors.gray400 else Colors.gray900,
                modifier = Modifier.weight(1f),
            )
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Colors.textTertiary),
                modifier = Modifier.size(24.dp),
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = Typography.body2M,
                color = Colors.statusError,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
