package com.dulpick.app.data.explore.mapper

import com.dulpick.app.data.explore.remote.dto.ContentDetailPlaceResponseDto
import com.dulpick.app.data.explore.remote.dto.ContentDetailResponseDto
import com.dulpick.app.data.explore.remote.dto.ContentPageResponseDto
import com.dulpick.app.data.explore.remote.dto.ContentResponseDto
import com.dulpick.app.data.place.mapper.PlaceCategoryMapper
import com.dulpick.app.domain.explore.Content
import com.dulpick.app.domain.explore.ContentPage
import com.dulpick.app.domain.explore.ContentPlace
import com.dulpick.app.domain.explore.PostDetailContent
import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.Place

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

    fun toDetail(dto: ContentDetailResponseDto): PostDetailContent =
        PostDetailContent(
            id = dto.contentId.toString(),
            title = dto.title,
            caption = dto.caption,
            canonicalUrl = dto.canonicalUrl,
            places = dto.places.orEmpty().map(::toContentPlace),
        )

    // 게시글 속 장소. 게시글 응답은 장소 번호를 늘 준다
    private fun toContentPlace(dto: ContentDetailPlaceResponseDto): ContentPlace =
        ContentPlace(
            place = Place(
                id = dto.placeId.toString(),
                name = dto.name,
                category = PlaceCategoryMapper.fromCodeOrName(null, dto.categoryName),
                // 게시글 응답에는 저장 수가 없다
                bookmarkCount = 0,
                // 대표 사진을 버리지 않는다. 모든 장소 매퍼가 같은 규칙이다
                thumbnailUrls = listOfNotNull(dto.thumbnailUrl) + dto.imageUrls.orEmpty(),
                kakaoPlaceId = dto.kakaoPlaceId,
                address = dto.address,
                roadAddress = dto.roadAddress.orEmpty(),
                coordinate = Coordinate(dto.latitude, dto.longitude),
            ),
            isSaved = dto.savedByMe,
        )
}
