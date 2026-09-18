package com.dulpick.app.data.explore

import com.dulpick.app.data.explore.mapper.ContentDtoMapper
import com.dulpick.app.data.explore.mapper.ExploreErrorMapper
import com.dulpick.app.data.explore.remote.ExploreRemoteDataSource
import com.dulpick.app.domain.explore.ContentPage
import com.dulpick.app.domain.explore.ExploreRepository
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class ExploreRepositoryImpl @Inject constructor(
    private val exploreRemote: ExploreRemoteDataSource,
) : ExploreRepository {

    override suspend fun contents(page: Int, size: Int): ContentPage {
        try {
            return ContentDtoMapper.toDomain(exploreRemote.contents(SORT_POPULAR, page, size))
        } catch (error: Throwable) {
            throw ExploreErrorMapper.map(error)
        }
    }

    override suspend fun searchContents(query: String, page: Int, size: Int): ContentPage {
        try {
            return ContentDtoMapper.toDomain(exploreRemote.search(query, SORT_POPULAR, page, size))
        } catch (error: Throwable) {
            throw ExploreErrorMapper.map(error)
        }
    }

    private companion object {
        // 이 화면은 항상 인기순. 성향순(PREFERENCE)은 필요해질 때 추가한다
        const val SORT_POPULAR = "POPULAR"
    }
}
