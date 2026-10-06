package com.dulpick.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.dulpick.app.domain.location.LocationAuthorization
import com.dulpick.app.domain.location.LocationError
import com.dulpick.app.domain.location.LocationRepository
import com.dulpick.app.domain.place.Coordinate
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

// 좌표 1회 조회의 시간 상한. 넘으면 Unavailable (iOS LocationClientFactory timeout 과 같다)
private const val TIMEOUT_MS = 5_000L

// 캐시 좌표를 바로 쓸 수 있는 최대 나이. 넘으면 새로 잰다 (iOS maxCacheAge 와 같다)
private const val MAX_CACHE_AGE_MS = 10_000L

private const val NANOS_PER_MILLI = 1_000_000L

// FusedLocationProviderClient 를 한 번짜리 호출로 감싼다 (iOS SystemLocationProvider 대응)
@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationRepository {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    // 안드로이드는 '미결정' 과 '영구 거부' 를 구분하는 API 가 없다. 여기서는 허용 여부만 보고,
    // 거부를 받은 뒤 어느 쪽인지는 화면이 shouldShowRequestPermissionRationale 로 가른다
    override fun authorization(): LocationAuthorization =
        if (hasPermission()) LocationAuthorization.AUTHORIZED else LocationAuthorization.NOT_DETERMINED

    @SuppressLint("MissingPermission")
    override suspend fun currentCoordinate(): Coordinate {
        if (!hasPermission()) throw LocationError.Denied
        // 지도 화면은 최근 좌표를 이미 들고 있는 때가 많다. 그때 새로 재면 얻는 것이 없다
        lastKnownFresh()?.let { return it }
        val request = CurrentLocationRequest.Builder()
            // 정밀 권한이 있으면 10m 급으로 잡는다 (iOS kCLLocationAccuracyNearestTenMeters).
            // 대략 권한만 허용한 사용자는 올려도 소용이 없어 균형 모드로 둔다
            .setPriority(
                if (granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Priority.PRIORITY_HIGH_ACCURACY
                } else {
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY
                },
            )
            .setDurationMillis(TIMEOUT_MS)
            .build()
        val location = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine<Location?> { continuation ->
                client.getCurrentLocation(request, null)
                    .addOnSuccessListener { continuation.resumeIfActive(it) }
                    .addOnFailureListener { continuation.resumeIfActive(null) }
            }
        } ?: throw LocationError.Unavailable
        return Coordinate(location.latitude, location.longitude)
    }

    // 10 초 이내에 잡힌 좌표만 쓴다
    @SuppressLint("MissingPermission")
    private suspend fun lastKnownFresh(): Coordinate? {
        val cached = suspendCancellableCoroutine<Location?> { continuation ->
            client.lastLocation
                .addOnSuccessListener { continuation.resumeIfActive(it) }
                .addOnFailureListener { continuation.resumeIfActive(null) }
        } ?: return null
        val ageMs = (SystemClock.elapsedRealtimeNanos() - cached.elapsedRealtimeNanos) / NANOS_PER_MILLI
        if (ageMs > MAX_CACHE_AGE_MS) return null
        return Coordinate(cached.latitude, cached.longitude)
    }

    // 정밀을 거부해도 대략 위치만 있으면 쓸 수 있다
    private fun hasPermission(): Boolean =
        granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            granted(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

// 콜백이 두 번 와도 한 번만 재개한다
private fun <T> CancellableContinuation<T?>.resumeIfActive(value: T?) {
    if (isActive) resume(value)
}
