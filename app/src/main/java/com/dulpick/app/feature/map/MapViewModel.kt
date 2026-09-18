package com.dulpick.app.feature.map

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.explore.ExploreError
import com.dulpick.app.domain.place.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class MapViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val coupleRepository: CoupleRepository,
) : MviViewModel<MapState, MapIntent, MapSideEffect>(MapState()) {

    override fun onIntent(intent: MapIntent) {
        when (intent) {
            MapIntent.OnAppear -> load()
            is MapIntent.OwnershipSelected -> setState { copy(selectedOwnership = intent.ownership) }
            is MapIntent.CategorySelected -> setState { copy(selectedCategory = intent.category) }
        }
    }

    private fun load() {
        loadPlaces()
        loadCoupleConnection()
    }

    private fun loadPlaces() {
        viewModelScope.launch {
            try {
                val places = placeRepository.savedPlaces()
                setState { copy(places = places, isLoading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isLoading = false) }
                if (error == ExploreError.Unauthorized) postSideEffect(MapSideEffect.SessionExpired)
            }
        }
    }

    // 소유자 필터 노출 여부. 실패하면 미연동으로 두고 필터를 감춘다
    private fun loadCoupleConnection() {
        viewModelScope.launch {
            try {
                val connected = coupleRepository.current()?.connected ?: false
                setState { copy(isCoupleConnected = connected) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isCoupleConnected = false) }
            }
        }
    }
}
