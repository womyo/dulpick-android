package com.dulpick.app.data.explore.remote

import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.explore.remote.dto.ContentPageResponseDto
import javax.inject.Inject

class ExploreRemoteDataSource @Inject constructor(
    @Authed private val exploreApi: ExploreApi,
) {
    suspend fun contents(sort: String, page: Int, size: Int): ContentPageResponseDto =
        safeApiCall { exploreApi.contents(sort, page, size) }

    suspend fun search(query: String, sort: String, page: Int, size: Int): ContentPageResponseDto =
        safeApiCall { exploreApi.search(query, sort, page, size) }
}
