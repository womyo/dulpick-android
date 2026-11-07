package com.dulpick.app.feature.placedetail

import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.PlaceSearchResult
import com.dulpick.app.domain.place.SavedPlace
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 상세로 진입할 때 Place 를 nav 인자로 통째로 넘긴다. iOS 처럼 상세 API 가 실패해도 이 값으로 화면을 그린다.
// domain Place 에 직렬화 어노테이션을 붙이지 않으려고 화면 레이어의 별도 타입으로 감싼다
@kotlinx.serialization.Serializable
data class PlaceArg(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val bookmarkCount: Int,
    val thumbnailUrls: List<String>,
    val kakaoPlaceId: String?,
    val address: String,
    val roadAddress: String,
    val latitude: Double,
    val longitude: Double,
) {
    fun toPlace(): Place = Place(
        id = id,
        name = name,
        category = category,
        bookmarkCount = bookmarkCount,
        thumbnailUrls = thumbnailUrls,
        kakaoPlaceId = kakaoPlaceId,
        address = address,
        roadAddress = roadAddress,
        coordinate = Coordinate(latitude, longitude),
    )

    companion object {
        fun from(place: Place): PlaceArg = PlaceArg(
            id = place.id,
            name = place.name,
            category = place.category,
            bookmarkCount = place.bookmarkCount,
            thumbnailUrls = place.thumbnailUrls,
            kakaoPlaceId = place.kakaoPlaceId,
            address = place.address,
            roadAddress = place.roadAddress,
            latitude = place.coordinate.latitude,
            longitude = place.coordinate.longitude,
        )
    }
}

// 검색 결과 한 건을 인자로 실을 때 쓰는 껍데기
@kotlinx.serialization.Serializable
data class PlaceSearchResultArg(
    val place: PlaceArg,
    val ownership: PlaceOwnership?,
    val alias: String?,
) {
    fun toResult(): PlaceSearchResult = PlaceSearchResult(place.toPlace(), ownership, alias)

    companion object {
        fun from(result: PlaceSearchResult): PlaceSearchResultArg = PlaceSearchResultArg(
            place = PlaceArg.from(result.place),
            ownership = result.ownership,
            alias = result.alias,
        )

        fun from(savedPlace: SavedPlace): PlaceSearchResultArg = from(PlaceSearchResult.of(savedPlace))
    }
}

// 지도 검색 화면 → 지도로 검색 결과를 되돌려줄 때 쓰는 인자.
// 지도는 이걸로 검색 결과 모드에 들어간다. selectedIndex 가 있으면 그 장소 상세를 바로 연다
@kotlinx.serialization.Serializable
data class MapSearchReturnArg(
    // 원본 검색어. 상세 카카오 조회에 쓴다
    val searchQuery: String,
    // 검색바에 표시할 텍스트(장소 선택 시 장소명, 제출 시 검색어)
    val displayQuery: String,
    val results: List<PlaceSearchResultArg>,
    // non-null 이면 그 장소 상세를 바로 연다(장소 행 탭). null 이면 결과 리스트만(검색 제출)
    val selectedIndex: Int?,
    // 「저장한 곳만 보기」로 찾은 결과인지. 상세를 장소 번호로 열지 가른다
    val isSavedOnly: Boolean = false,
    // 반환마다 고유한 값. savedStateHandle 재전달로 같은 결과가 두 번 처리(상세 재오픈)되는 걸 막는 일회성 토큰
    val nonce: Long,
) {
    fun encode(): String = Json.encodeToString(this)

    companion object {
        // 검색 결과에서 장소 하나를 탭 → 그 장소만 리스트에 두고 상세를 연다
        fun selecting(result: PlaceSearchResult, searchQuery: String): MapSearchReturnArg = MapSearchReturnArg(
            searchQuery = searchQuery,
            displayQuery = result.place.name,
            results = listOf(PlaceSearchResultArg.from(result)),
            selectedIndex = 0,
            nonce = System.nanoTime(),
        )

        // 「저장한 곳만 보기」 결과 행. 카카오 번호가 없어 장소 번호로 상세를 연다
        fun selectingSaved(savedPlace: SavedPlace): MapSearchReturnArg = MapSearchReturnArg(
            searchQuery = savedPlace.place.name,
            displayQuery = savedPlace.alias ?: savedPlace.place.name,
            results = listOf(PlaceSearchResultArg.from(savedPlace)),
            selectedIndex = 0,
            isSavedOnly = true,
            nonce = System.nanoTime(),
        )

        // 검색 제출 → 전체 결과를 리스트로 보여준다(상세 없음)
        fun confirming(
            query: String,
            results: List<PlaceSearchResult>,
            isSavedOnly: Boolean,
        ): MapSearchReturnArg = MapSearchReturnArg(
            searchQuery = query,
            displayQuery = query,
            results = results.map(PlaceSearchResultArg::from),
            selectedIndex = null,
            isSavedOnly = isSavedOnly,
            nonce = System.nanoTime(),
        )

        fun decode(raw: String): MapSearchReturnArg? =
            runCatching { Json.decodeFromString<MapSearchReturnArg>(raw) }.getOrNull()
    }
}
