package com.dulpick.app.core.link

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

// 앱 주소를 먼저 열고, 그 앱이 없으면 웹 주소를 연다
fun Context.openAppOrWeb(appUri: String?, webUrl: String?) {
    if (appUri != null && open(appUri)) return
    if (webUrl != null) open(webUrl)
}

private fun Context.open(uri: String): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    true
} catch (ignored: ActivityNotFoundException) {
    false
}
