package com.dulpick.app.data.placeimport

import com.dulpick.app.data.placeimport.mapper.PlaceImportDtoMapper
import com.dulpick.app.data.placeimport.mapper.PlaceImportErrorMapper
import com.dulpick.app.data.placeimport.remote.PlaceImportRemoteDataSource
import com.dulpick.app.domain.placeimport.PlaceImport
import com.dulpick.app.domain.placeimport.PlaceImportRepository
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class PlaceImportRepositoryImpl @Inject constructor(
    private val remote: PlaceImportRemoteDataSource,
) : PlaceImportRepository {

    override suspend fun start(sourceUrl: String): PlaceImport {
        try {
            return PlaceImportDtoMapper.toDomain(remote.start(PlaceImportDtoMapper.toStartRequest(sourceUrl)))
        } catch (error: Throwable) {
            throw PlaceImportErrorMapper.map(error)
        }
    }

    override suspend fun poll(importId: Long): PlaceImport {
        try {
            return PlaceImportDtoMapper.toDomain(remote.poll(importId))
        } catch (error: Throwable) {
            throw PlaceImportErrorMapper.map(error)
        }
    }

    override suspend fun confirm(importId: Long, candidateIds: List<Long>) {
        try {
            remote.confirm(importId, PlaceImportDtoMapper.toConfirmRequest(candidateIds))
        } catch (error: Throwable) {
            throw PlaceImportErrorMapper.map(error)
        }
    }
}
