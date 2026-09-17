package com.dulpick.app.data.explore

import com.dulpick.app.data.explore.mapper.ContentDtoMapper
import com.dulpick.app.data.explore.mapper.ExploreErrorMapper
import com.dulpick.app.data.explore.remote.ExploreRemoteDataSource
import com.dulpick.app.domain.explore.ContentPage
import com.dulpick.app.domain.explore.ContentSort
import com.dulpick.app.domain.explore.ExploreRepository
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class ExploreRepositoryImpl @Inject constructor(
    private val exploreRemote: ExploreRemoteDataSource,
) : ExploreRepository {

    override suspend fun contents(page: Int, size: Int, sort: ContentSort): ContentPage {
        try {
            return ContentDtoMapper.toDomain(exploreRemote.contents(sort.serverValue, page, size))
        } catch (error: Throwable) {
            throw ExploreErrorMapper.map(error)
        }
    }

    override suspend fun searchContents(query: String, page: Int, size: Int): ContentPage {
        try {
            return ContentDtoMapper.toDomain(exploreRemote.search(query, ContentSort.POPULAR.serverValue, page, size))
        } catch (error: Throwable) {
            throw ExploreErrorMapper.map(error)
        }
    }
}
