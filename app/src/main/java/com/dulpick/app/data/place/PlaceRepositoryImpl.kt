package com.dulpick.app.data.place

import com.dulpick.app.data.explore.mapper.ExploreErrorMapper
import com.dulpick.app.data.place.mapper.PlaceDtoMapper
import com.dulpick.app.data.place.mapper.PlaceErrorMapper
import com.dulpick.app.data.place.remote.PlaceRemoteDataSource
import com.dulpick.app.domain.place.PlacePage
import com.dulpick.app.domain.place.PlaceRepository
import com.dulpick.app.domain.place.SavedPlace
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

    override suspend fun savedPlaces(): List<SavedPlace> {
        try {
            return PlaceDtoMapper.toSavedPlaces(placeRemote.savedPlaces())
        } catch (error: Throwable) {
            throw PlaceErrorMapper.map(error)
        }
    }

    override suspend fun savePlace(kakaoPlaceId: String, query: String, alias: String?): SavedPlace {
        try {
            return PlaceDtoMapper.toSavedPlace(placeRemote.savePlace(kakaoPlaceId, query, alias))
        } catch (error: Throwable) {
            throw PlaceErrorMapper.map(error)
        }
    }

    override suspend fun removePlace(placeId: Long) {
        try {
            placeRemote.removePlace(placeId)
        } catch (error: Throwable) {
            // 상대가 저장한 장소 등은 서버가 404 → NotFound 로 구분한다
            throw PlaceErrorMapper.map(error)
        }
    }

    override suspend fun updateAlias(placeId: Long, alias: String?): SavedPlace {
        try {
            return PlaceDtoMapper.toSavedPlace(placeRemote.updateAlias(placeId, alias))
        } catch (error: Throwable) {
            throw PlaceErrorMapper.map(error)
        }
    }
}
