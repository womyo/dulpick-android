package com.dulpick.app.data.home

import com.dulpick.app.data.home.mapper.HomeDtoMapper
import com.dulpick.app.data.home.mapper.HomeErrorMapper
import com.dulpick.app.data.home.remote.HomeRemoteDataSource
import com.dulpick.app.domain.home.DateSchedule
import com.dulpick.app.domain.home.HomeRepository
import com.dulpick.app.domain.home.HomeSummary
import com.dulpick.app.domain.home.PastDateCoursePage
import com.dulpick.app.domain.place.Place
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class HomeRepositoryImpl @Inject constructor(
    private val homeRemote: HomeRemoteDataSource,
) : HomeRepository {

    override suspend fun home(): HomeSummary {
        try {
            return HomeDtoMapper.toSummary(homeRemote.home())
        } catch (error: Throwable) {
            throw HomeErrorMapper.map(error)
        }
    }

    override suspend fun recentSavedPlaces(size: Int): List<Place> {
        try {
            return HomeDtoMapper.toSavedPlaces(homeRemote.recentSavedPlaces(size))
        } catch (error: Throwable) {
            throw HomeErrorMapper.map(error)
        }
    }

    override suspend fun pastDates(size: Int): List<DateSchedule> {
        try {
            return HomeDtoMapper.toPastDates(homeRemote.pastDates(size))
        } catch (error: Throwable) {
            throw HomeErrorMapper.map(error)
        }
    }

    override suspend fun pastCourses(page: Int, size: Int): PastDateCoursePage {
        try {
            return HomeDtoMapper.toPastCoursePage(homeRemote.pastCourses(page, size))
        } catch (error: Throwable) {
            throw HomeErrorMapper.map(error)
        }
    }
}
