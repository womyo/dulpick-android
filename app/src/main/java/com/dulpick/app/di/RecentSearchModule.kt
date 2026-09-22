package com.dulpick.app.di

import android.content.Context
import com.dulpick.app.data.search.RecentSearchRepositoryImpl
import com.dulpick.app.data.search.local.RecentSearchLocalDataSource
import com.dulpick.app.domain.search.RecentSearchRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

// 탐색 검색용 최근검색 (게시물·장소 통합 검색 화면)
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExploreRecentSearch

// 지도 검색용 최근검색 (장소 전용). iOS mapRecentSearchClient 대응으로 저장소를 분리한다
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MapRecentSearch

// 같은 정책(Impl)에 저장 파일만 달리해 용도별 최근검색을 제공한다
@Module
@InstallIn(SingletonComponent::class)
object RecentSearchModule {

    private const val EXPLORE_FILE = "dulpick_recent_search"
    private const val MAP_FILE = "dulpick_map_recent_search"

    @Provides
    @Singleton
    @ExploreRecentSearch
    fun exploreRecentSearch(@ApplicationContext context: Context): RecentSearchRepository =
        RecentSearchRepositoryImpl(RecentSearchLocalDataSource(context, EXPLORE_FILE))

    @Provides
    @Singleton
    @MapRecentSearch
    fun mapRecentSearch(@ApplicationContext context: Context): RecentSearchRepository =
        RecentSearchRepositoryImpl(RecentSearchLocalDataSource(context, MAP_FILE))
}
