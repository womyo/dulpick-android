package com.dulpick.app.data.course.remote

import com.dulpick.app.data.course.remote.dto.CreateDateCourseRequestDto
import com.dulpick.app.data.course.remote.dto.CurrentDateCourseResponseDto
import com.dulpick.app.data.course.remote.dto.DateCoursePlacePoolResponseDto
import com.dulpick.app.data.course.remote.dto.DateCourseResponseDto
import com.dulpick.app.data.course.remote.dto.SaveDateCourseRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CourseApi {
    @POST("/api/v1/date-courses")
    suspend fun create(@Body body: CreateDateCourseRequestDto): DateCourseResponseDto

    // 코스에 담을 수 있는 저장 장소. 장소 번호가 늘 있다
    @GET("/api/v1/date-courses/places")
    suspend fun placePool(): DateCoursePlacePoolResponseDto

    @GET("/api/v1/date-courses/{dateCourseId}")
    suspend fun detail(@Path("dateCourseId") dateCourseId: String): DateCourseResponseDto

    @GET("/api/v1/date-courses/current")
    suspend fun current(): CurrentDateCourseResponseDto

    // 확정 저장
    @PUT("/api/v1/date-courses/{dateCourseId}")
    suspend fun save(
        @Path("dateCourseId") dateCourseId: String,
        @Body body: SaveDateCourseRequestDto,
    ): DateCourseResponseDto

    @POST("/api/v1/date-courses/{dateCourseId}/notify-partner")
    suspend fun notifyPartner(@Path("dateCourseId") dateCourseId: String)
}
