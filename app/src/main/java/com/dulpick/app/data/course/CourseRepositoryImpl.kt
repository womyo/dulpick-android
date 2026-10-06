package com.dulpick.app.data.course

import com.dulpick.app.data.course.mapper.CourseDateFormat
import com.dulpick.app.data.course.mapper.CourseDtoMapper
import com.dulpick.app.data.course.mapper.CourseErrorMapper
import com.dulpick.app.data.course.remote.CourseRemoteDataSource
import com.dulpick.app.data.course.remote.dto.CreateDateCourseRequestDto
import com.dulpick.app.data.course.remote.dto.SaveDateCourseRequestDto
import com.dulpick.app.domain.course.CourseError
import com.dulpick.app.domain.course.CourseRepository
import com.dulpick.app.domain.course.DateCourse
import com.dulpick.app.domain.course.DateCourseContent
import com.dulpick.app.domain.course.DateCourseSummary
import com.dulpick.app.domain.place.SavedPlace
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class CourseRepositoryImpl @Inject constructor(
    private val remote: CourseRemoteDataSource,
) : CourseRepository {

    override suspend fun createCourse(title: String, date: LocalDate, time: LocalTime?): DateCourse {
        try {
            val body = CreateDateCourseRequestDto(
                title = title,
                date = CourseDateFormat.dateText(date),
                time = time?.let(CourseDateFormat::timeText),
            )
            return CourseDtoMapper.toDomain(remote.create(body))
        } catch (error: Throwable) {
            throw CourseErrorMapper.map(error)
        }
    }

    override suspend fun coursePlaces(): List<SavedPlace> {
        try {
            return remote.placePool().places.map(CourseDtoMapper::toSavedPlace)
        } catch (error: Throwable) {
            throw CourseErrorMapper.map(error)
        }
    }

    override suspend fun course(id: String): DateCourse {
        try {
            return CourseDtoMapper.toDomain(remote.detail(id))
        } catch (error: Throwable) {
            throw CourseErrorMapper.map(error)
        }
    }

    override suspend fun currentCourse(): DateCourseSummary? {
        try {
            return remote.current().currentDateCourse?.let(CourseDtoMapper::toSummary)
        } catch (error: Throwable) {
            throw CourseErrorMapper.map(error)
        }
    }

    override suspend fun updateCourse(id: String, content: DateCourseContent, version: Int): DateCourse {
        try {
            val body = SaveDateCourseRequestDto(
                title = content.title,
                date = CourseDateFormat.dateText(content.date),
                time = content.time?.let(CourseDateFormat::timeText),
                placeIds = content.placeIds.map { it.toLongOrNull() ?: throw CourseError.Unknown },
                version = version,
            )
            return CourseDtoMapper.toDomain(remote.save(id, body))
        } catch (error: Throwable) {
            throw CourseErrorMapper.map(error)
        }
    }

    override suspend fun notifyPartner(id: String) {
        try {
            remote.notifyPartner(id)
        } catch (error: Throwable) {
            throw CourseErrorMapper.map(error)
        }
    }
}
