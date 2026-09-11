package com.dulpick.app.di

import com.dulpick.app.core.storage.AppIntroStore
import com.dulpick.app.core.storage.DefaultAppIntroStore
import com.dulpick.app.core.storage.EncryptedSecureStorage
import com.dulpick.app.core.storage.SecureStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 계약↔구현은 @Binds. 구현체는 @Inject 생성자를 가진다
@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun secureStorage(impl: EncryptedSecureStorage): SecureStorage

    @Binds
    @Singleton
    abstract fun appIntroStore(impl: DefaultAppIntroStore): AppIntroStore
}
