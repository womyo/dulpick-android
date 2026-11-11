package com.dulpick.app.core.map

import com.dulpick.app.domain.location.LocationRepository
import com.dulpick.app.domain.place.Coordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// 지도가 보이는 동안만 내 위치를 받는다. 권한은 요청하지 않는다
class UserLocationTracker(
    private val repository: LocationRepository,
    private val scope: CoroutineScope,
    private val onChange: (Coordinate?) -> Unit,
) {
    private var job: Job? = null

    fun start() {
        job?.cancel()
        // 다시 들어오면 새로 잡힌 좌표가 올 때까지 점을 안 보인다
        onChange(null)
        job = scope.launch {
            repository.coordinateUpdates().collect { coordinate -> onChange(coordinate) }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        onChange(null)
    }
}
