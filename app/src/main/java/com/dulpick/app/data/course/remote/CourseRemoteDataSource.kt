package com.dulpick.app.data.course.remote

import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.course.remote.dto.CreateDateCourseRequestDto
import com.dulpick.app.data.course.remote.dto.CurrentDateCourseResponseDto
import com.dulpick.app.data.course.remote.dto.DateCoursePlacePoolResponseDto
import com.dulpick.app.data.course.remote.dto.DateCourseResponseDto
import com.dulpick.app.data.course.remote.dto.SaveDateCourseRequestDto
import javax.inject.Inject

class CourseRemoteDataSource @Inject constructor(
    @Authed private val courseApi: CourseApi,
) {
    suspend fun create(body: CreateDateCourseRequestDto): DateCourseResponseDto =
        safeApiCall { courseApi.create(body) }

    suspend fun placePool(): DateCoursePlacePoolResponseDto =
        safeApiCall { courseApi.placePool() }

    suspend fun detail(id: String): DateCourseResponseDto =
        safeApiCall { courseApi.detail(id) }

    suspend fun current(): CurrentDateCourseResponseDto =
        safeApiCall { courseApi.current() }

    suspend fun save(id: String, body: SaveDateCourseRequestDto): DateCourseResponseDto =
        safeApiCall { courseApi.save(id, body) }

    suspend fun notifyPartner(id: String): Unit =
        safeApiCall { courseApi.notifyPartner(id) }
}
