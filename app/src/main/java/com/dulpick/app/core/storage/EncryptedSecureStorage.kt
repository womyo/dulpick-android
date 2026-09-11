package com.dulpick.app.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// Keystore 로 암호화된 SharedPreferences
@Singleton
@Suppress("TooGenericExceptionCaught")
class EncryptedSecureStorage @Inject constructor(
    @ApplicationContext context: Context,
) : SecureStorage {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        try {
            prefs.getString(key, null)
        } catch (error: Exception) {
            throw StorageException(error)
        }
    }

    override suspend fun putString(key: String, value: String): Unit = withContext(Dispatchers.IO) {
        try {
            // commit() 은 실패해도 예외 대신 false 를 낸다. 저장이 안 됐는데 성공으로 넘어가지 않게 확인한다
            if (!prefs.edit().putString(key, value).commit()) {
                throw StorageException()
            }
        } catch (error: StorageException) {
            throw error
        } catch (error: Exception) {
            throw StorageException(error)
        }
    }

    override suspend fun remove(key: String): Unit = withContext(Dispatchers.IO) {
        try {
            if (!prefs.edit().remove(key).commit()) {
                throw StorageException()
            }
        } catch (error: StorageException) {
            throw error
        } catch (error: Exception) {
            throw StorageException(error)
        }
    }

    private companion object {
        const val FILE_NAME = "dulpick_secure_prefs"
    }
}
