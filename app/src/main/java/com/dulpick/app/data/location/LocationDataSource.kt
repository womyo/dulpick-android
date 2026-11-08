package com.dulpick.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val NANOS_PER_MILLI = 1_000_000L

// FusedLocationProviderClient 호출을 여기에 모은다. 저장소는 이걸 주입받아 쓴다
// (iOS SystemLocationProvider 대응)
@Singleton
class LocationDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    // 정밀을 거부해도 대략 위치만 있으면 쓸 수 있다
    fun hasPermission(): Boolean =
        granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            granted(Manifest.permission.ACCESS_COARSE_LOCATION)

    // 마지막으로 잡아둔 좌표. maxAgeMillis 보다 오래됐으면 null
    @SuppressLint("MissingPermission")
    suspend fun lastKnown(maxAgeMillis: Long): Location? {
        val cached = suspendCancellableCoroutine<Location?> { continuation ->
            client.lastLocation
                .addOnSuccessListener { continuation.resumeIfActive(it) }
                .addOnFailureListener { continuation.resumeIfActive(null) }
        } ?: return null
        val ageMs = (SystemClock.elapsedRealtimeNanos() - cached.elapsedRealtimeNanos) / NANOS_PER_MILLI
        return cached.takeIf { ageMs <= maxAgeMillis }
    }

    // 지금 좌표를 한 번 잰다. 정밀 권한이 있으면 10m 급으로 잡는다
    // (iOS kCLLocationAccuracyNearestTenMeters). 대략 권한만 허용한 사용자는 올려도 소용이 없다
    @SuppressLint("MissingPermission")
    suspend fun current(durationMillis: Long): Location? {
        val request = CurrentLocationRequest.Builder()
            .setPriority(
                if (granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Priority.PRIORITY_HIGH_ACCURACY
                } else {
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY
                },
            )
            .setDurationMillis(durationMillis)
            .build()
        return suspendCancellableCoroutine<Location?> { continuation ->
            client.getCurrentLocation(request, null)
                .addOnSuccessListener { continuation.resumeIfActive(it) }
                .addOnFailureListener { continuation.resumeIfActive(null) }
        }
    }

    // 정밀 권한이 있을 때만 켠다. 대략 위치로는 점을 찍지 않는다
    fun hasPrecisePermission(): Boolean = granted(Manifest.permission.ACCESS_FINE_LOCATION)

    // 좌표가 바뀔 때마다 흘린다. 구독을 끊으면 갱신이 멈춘다
    @SuppressLint("MissingPermission")
    fun updates(intervalMillis: Long, minDistanceMeters: Float): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

// 콜백이 두 번 와도 한 번만 재개한다
private fun <T> CancellableContinuation<T?>.resumeIfActive(value: T?) {
    if (isActive) resume(value)
}
