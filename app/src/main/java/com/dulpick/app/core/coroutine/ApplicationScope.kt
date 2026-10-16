package com.dulpick.app.core.coroutine

import javax.inject.Qualifier

// 화면 수명과 무관하게 앱이 살아 있는 동안 도는 작업용 스코프 (푸시 기기 등록 구독)
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
