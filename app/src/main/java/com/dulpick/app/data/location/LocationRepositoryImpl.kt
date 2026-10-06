package com.dulpick.app.data.location

import com.dulpick.app.domain.location.LocationAuthorization
import com.dulpick.app.domain.location.LocationError
import com.dulpick.app.domain.location.LocationRepository
import com.dulpick.app.domain.place.Coordinate
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

// 좌표 1회 조회의 시간 상한. 넘으면 Unavailable (iOS LocationClientFactory timeout 과 같다)
private const val TIMEOUT_MS = 5_000L

// 캐시 좌표를 바로 쓸 수 있는 최대 나이. 넘으면 새로 잰다 (iOS maxCacheAge 와 같다)
private const val MAX_CACHE_AGE_MS = 10_000L

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationRemote: LocationDataSource,
) : LocationRepository {

    // 안드로이드는 '미결정' 과 '영구 거부' 를 구분하는 API 가 없다. 여기서는 허용 여부만 보고,
    // 거부를 받은 뒤 어느 쪽인지는 화면이 shouldShowRequestPermissionRationale 로 가른다
    override fun authorization(): LocationAuthorization =
        if (locationRemote.hasPermission()) {
            LocationAuthorization.AUTHORIZED
        } else {
            LocationAuthorization.NOT_DETERMINED
        }

    override suspend fun currentCoordinate(): Coordinate {
        if (!locationRemote.hasPermission()) throw LocationError.Denied
        // 캐시 조회와 새 측위를 한 제한 시간 안에 둔다. 캐시 조회가 늦어져도 전체가 5 초를 넘지 않는다
        val location = withTimeoutOrNull(TIMEOUT_MS) {
            // 지도 화면은 최근 좌표를 이미 들고 있는 때가 많다. 그때 새로 재면 얻는 것이 없다
            locationRemote.lastKnown(MAX_CACHE_AGE_MS) ?: locationRemote.current(TIMEOUT_MS)
        } ?: throw LocationError.Unavailable
        return Coordinate(location.latitude, location.longitude)
    }
}
