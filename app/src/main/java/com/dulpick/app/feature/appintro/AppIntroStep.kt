package com.dulpick.app.feature.appintro

import androidx.annotation.DrawableRes
import com.dulpick.app.R

// 앱 인트로 3단계. 단계별 문구·이미지 (iOS AppIntroStep 대응)
enum class AppIntroStep(
    val title: String,
    @DrawableRes val image: Int,
) {
    SHARE(
        title = "인스타에서 공유 버튼을\n통해 둘픽에 저장해요",
        image = R.drawable.appintroshare,
    ),
    SAVE(
        title = "둘픽에 함께 저장한\n장소를 모아볼 수 있어요",
        image = R.drawable.appintrosave,
    ),
    PLAN(
        title = "저장한 장소를 함께\n데이트 코스로 계획해요",
        image = R.drawable.appintroplan,
    ),
}
