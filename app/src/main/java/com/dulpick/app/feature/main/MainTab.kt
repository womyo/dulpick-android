package com.dulpick.app.feature.main

import androidx.annotation.DrawableRes
import com.dulpick.app.R

// 메인 탭 구성. 홈·탐색·지도·마이 (iOS MainTabView 대응)
enum class MainTab(val route: String, val label: String, @DrawableRes val icon: Int) {
    HOME("main/home", "홈", R.drawable.home),
    EXPLORE("main/explore", "탐색", R.drawable.explore),
    MAP("main/map", "지도", R.drawable.map),
    MY("main/my", "마이", R.drawable.my),
}
