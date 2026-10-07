package com.dulpick.app.ui.map

import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberMapSheetState(): BottomSheetScaffoldState {
    return rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false,
            confirmValueChange = NOT_HIDDEN,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
// rememberStandardBottomSheetState 는 이 함수를 기억 키로 쓴다.
// 호출할 때마다 새로 만들면 그릴 때마다 시트 상태가 초기화돼 접힘으로 돌아간다
private val NOT_HIDDEN: (SheetValue) -> Boolean = { it != SheetValue.Hidden }
