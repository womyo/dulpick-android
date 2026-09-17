package com.dulpick.app.di

import com.dulpick.app.data.auth.AuthRepositoryImpl
import com.dulpick.app.data.couple.CoupleRepositoryImpl
import com.dulpick.app.data.explore.ExploreRepositoryImpl
import com.dulpick.app.data.home.HomeRepositoryImpl
import com.dulpick.app.data.profile.ProfileRepositoryImpl
import com.dulpick.app.data.place.PlaceRepositoryImpl
import com.dulpick.app.data.search.RecentSearchRepositoryImpl
import com.dulpick.app.domain.auth.AuthRepository
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.explore.ExploreRepository
import com.dulpick.app.domain.home.HomeRepository
import com.dulpick.app.domain.place.PlaceRepository
import com.dulpick.app.domain.profile.ProfileRepository
import com.dulpick.app.domain.search.RecentSearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindCoupleRepository(impl: CoupleRepositoryImpl): CoupleRepository

    @Binds
    @Singleton
    abstract fun bindExploreRepository(impl: ExploreRepositoryImpl): ExploreRepository

    @Binds
    @Singleton
    abstract fun bindRecentSearchRepository(impl: RecentSearchRepositoryImpl): RecentSearchRepository

    @Binds
    @Singleton
    abstract fun bindPlaceRepository(impl: PlaceRepositoryImpl): PlaceRepository

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
}
