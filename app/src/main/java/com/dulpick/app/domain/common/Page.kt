package com.dulpick.app.domain.common

data class Page<T>(
    val items: List<T>,
    val hasNext: Boolean,
)
