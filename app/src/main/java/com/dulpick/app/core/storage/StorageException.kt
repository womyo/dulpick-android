package com.dulpick.app.core.storage

// 보안 저장 실패 (Keystore/디스크). Data 에서 AuthError.Storage 로 매핑된다
class StorageException(cause: Throwable? = null) : Exception(cause)
