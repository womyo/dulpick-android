package com.dulpick.app.di

import android.content.Context
import com.dulpick.app.core.storage.EncryptedSecureStorage
import com.dulpick.app.core.storage.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun secureStorage(@ApplicationContext context: Context): SecureStorage =
        EncryptedSecureStorage(context)
}
