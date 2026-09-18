package com.dulpick.app.data.explore.mapper

import com.dulpick.app.data.explore.remote.dto.ContentPageResponseDto
import com.dulpick.app.data.explore.remote.dto.ContentResponseDto
import com.dulpick.app.domain.explore.Content
import com.dulpick.app.domain.explore.ContentPage

object ContentDtoMapper {
    fun toDomain(dto: ContentPageResponseDto): ContentPage =
        ContentPage(
            items = dto.contents.map(::toContent),
            hasNext = dto.hasNext,
            popularTags = dto.popularTags ?: emptyList(),
        )

    private fun toContent(dto: ContentResponseDto): Content =
        Content(
            id = dto.contentId.toString(),
            title = dto.title,
            placeCount = dto.placeCount,
            thumbnailUrls = listOfNotNull(dto.thumbnailUrl),
        )
}
