package com.dulpick.app.data.place.remote

import com.dulpick.app.data.place.remote.dto.PlaceAliasRequestDto
import com.dulpick.app.data.place.remote.dto.PlaceDetailResponseDto
import com.dulpick.app.data.place.remote.dto.PlaceSaveRequestDto
import com.dulpick.app.data.place.remote.dto.PlaceSearchResponseDto
import com.dulpick.app.data.place.remote.dto.SavedPlaceResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PlaceApi {
    @GET("/api/v1/places/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PlaceSearchResponseDto

    // 커플이 저장한 장소 전체
    @GET("/api/v1/places")
    suspend fun savedPlaces(): List<SavedPlaceResponseDto>

    // 장소 저장(북마크). 저장된 장소를 돌려준다
    @POST("/api/v1/places")
    suspend fun savePlace(@Body body: PlaceSaveRequestDto): SavedPlaceResponseDto

    // 장소 상세(서버 ID). 저장 목록·게시글 장소가 쓴다
    @GET("/api/v1/places/{placeId}")
    suspend fun placeDetail(@Path("placeId") placeId: Long): PlaceDetailResponseDto

    // 장소 상세(카카오 ID). 검색 결과가 쓴다. query 는 필수(빠지면 서버 500)
    @GET("/api/v1/places/kakao/{kakaoPlaceId}")
    suspend fun kakaoPlaceDetail(
        @Path("kakaoPlaceId") kakaoPlaceId: String,
        @Query("query") query: String,
    ): PlaceDetailResponseDto

    // 저장 장소 삭제
    @DELETE("/api/v1/places/{placeId}")
    suspend fun removePlace(@Path("placeId") placeId: Long)

    // 별칭 수정. 갱신된 저장 장소를 돌려준다
    @PATCH("/api/v1/places/{placeId}/alias")
    suspend fun updateAlias(
        @Path("placeId") placeId: Long,
        @Body body: PlaceAliasRequestDto,
    ): SavedPlaceResponseDto
}
