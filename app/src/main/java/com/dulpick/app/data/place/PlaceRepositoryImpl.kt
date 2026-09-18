package com.dulpick.app.data.place

import com.dulpick.app.data.explore.mapper.ExploreErrorMapper
import com.dulpick.app.data.place.mapper.PlaceDtoMapper
import com.dulpick.app.data.place.remote.PlaceRemoteDataSource
import com.dulpick.app.domain.place.PlacePage
import com.dulpick.app.domain.place.PlaceRepository
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class PlaceRepositoryImpl @Inject constructor(
    private val placeRemote: PlaceRemoteDataSource,
) : PlaceRepository {

    override suspend fun searchPlaces(query: String, page: Int, size: Int): PlacePage {
        try {
            return PlaceDtoMapper.toDomain(placeRemote.search(query, page, size))
        } catch (error: Throwable) {
            // 검색 실패 처리는 탐색과 동일하게 (네트워크/인증/그 외)
            throw ExploreErrorMapper.map(error)
        }
    }
}
