package com.dulpick.app.ui.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

// 아무것도 그리지 않는 indication. LocalIndication 에 깔아 clickable/selectable/toggleable 의
// 회색 리플(누르는 효과)을 전역으로 없앤다. Material 컴포넌트(버튼·탭바 등)는 자체 리플이라 영향 없다
object NoIndication : Indication {
    private object Instance : IndicationInstance {
        override fun ContentDrawScope.drawIndication() = drawContent()
    }

    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance = Instance
}
