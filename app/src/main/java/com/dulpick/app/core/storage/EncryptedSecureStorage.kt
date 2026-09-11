package com.dulpick.app.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Keystore 로 암호화된 SharedPreferences
@Suppress("TooGenericExceptionCaught")
class EncryptedSecureStorage(context: Context) : SecureStorage {

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
            prefs.edit().putString(key, value).commit()
        } catch (error: Exception) {
            throw StorageException(error)
        }
    }

    override suspend fun remove(key: String): Unit = withContext(Dispatchers.IO) {
        try {
            prefs.edit().remove(key).commit()
        } catch (error: Exception) {
            throw StorageException(error)
        }
    }

    private companion object {
        const val FILE_NAME = "dulpick_secure_prefs"
    }
}
