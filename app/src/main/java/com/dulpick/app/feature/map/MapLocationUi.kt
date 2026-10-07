package com.dulpick.app.feature.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.dulpick.app.R
import com.dulpick.app.ui.component.ModalContent
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 시스템 위치 권한 요청. 거부로 돌아오면 영구 거부인지 가려 뷰모델에 알린다.
// 안드로이드는 '미결정' 과 '영구 거부' 를 구분하는 API 가 없어, 거부 뒤에도 안내를 보여줄 수
// 있는지(shouldShowRequestPermissionRationale)로 가른다
@Composable
fun rememberLocationPermissionLauncher(onIntent: (MapIntent) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.any { it }
        onIntent(
            MapIntent.LocationPermissionResult(
                granted = granted,
                permanentlyDenied = !granted && !context.canShowLocationRationale(),
            ),
        )
    }
    return {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }
}

// 한 번 거부면 true, 다시 묻지 않음이면 false
private fun Context.canShowLocationRationale(): Boolean {
    val activity = findActivity() ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    runCatching { startActivity(intent) }
}

// 위치 권한이 꺼져 있을 때의 안내 (iOS 동일 문구)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPermissionModal(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Colors.commonWhite,
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        ModalContent(
            title = "위치 권한이 꺼져 있어요",
            content = "설정에서 위치 접근을 허용하면\n현재 위치로 이동할 수 있어요",
            primaryTitle = "설정으로 가기",
            onPrimary = onOpenSettings,
            secondaryTitle = "닫기",
            onSecondary = onDismiss,
        )
    }
}

// 지도 위 내 위치 버튼. 40dp 흰 원형에 24dp 아이콘 (iOS MapFloatingButton circle 대응)
@Composable
fun CurrentLocationButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Colors.commonWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.locate),
            contentDescription = "현재 위치",
            colorFilter = ColorFilter.tint(Colors.textPrimary),
            modifier = Modifier.size(24.dp),
        )
    }
}

// 지도 좌하단 알약 버튼. 빨강 바탕에 글자만 (iOS MapFloatingButton pill 대응)
@Composable
fun MapPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Colors.primaryPink)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = Typography.body1M, color = Colors.textInverse)
    }
}
