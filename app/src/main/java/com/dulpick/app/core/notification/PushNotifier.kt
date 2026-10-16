package com.dulpick.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dulpick.app.R
import java.util.concurrent.atomic.AtomicInteger

// 알림 채널 생성과 알림 노출. 백그라운드에서 온 알림은 시스템이 이 채널로 띄우고,
// 앱이 떠 있는 동안 온 알림은 여기서 직접 띄운다 (iOS willPresent 가 배너를 허용하는 것과 같다)
object PushNotifier {
    private val nextId = AtomicInteger(0)

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            context.getString(R.string.notification_channel_id),
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, title: String, body: String) {
        val manager = NotificationManagerCompat.from(context)
        // 권한이 없거나 사용자가 알림을 껐으면 띄우지 않는다
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat
            .Builder(context, context.getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(launchIntent(context))
            .build()

        manager.notify(nextId.incrementAndGet(), notification)
    }

    // 탭하면 앱을 연다. 딥링크 라우팅은 iOS 도 하지 않아 실행만 시킨다
    private fun launchIntent(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
