package com.dulpick.app.di

import com.dulpick.app.data.auth.AuthRepositoryImpl
import com.dulpick.app.data.couple.CoupleRepositoryImpl
import com.dulpick.app.data.profile.ProfileRepositoryImpl
import com.dulpick.app.domain.auth.AuthRepository
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.profile.ProfileRepository
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
}
