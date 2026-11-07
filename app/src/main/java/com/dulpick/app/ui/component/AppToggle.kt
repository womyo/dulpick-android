package com.dulpick.app.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors

private val TRACK_WIDTH = 40.dp
private val TRACK_HEIGHT = 24.dp
private val KNOB_SIZE = 16.dp
private val KNOB_INSET = 4.dp

@Composable
fun AppToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) TRACK_WIDTH - KNOB_SIZE - KNOB_INSET * 2 else 0.dp,
        label = "knobOffset",
    )
    Box(
        modifier = modifier
            .size(TRACK_WIDTH, TRACK_HEIGHT)
            .clip(CircleShape)
            .background(if (checked) Colors.gray900 else Colors.gray200)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(KNOB_INSET),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(KNOB_SIZE)
                .clip(CircleShape)
                .background(Colors.commonWhite),
        )
    }
}
