package com.dulpick.app.feature.map

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
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
) : MviViewModel<MapState, MapIntent, MapSideEffect>(MapState()) {

    override fun onIntent(intent: MapIntent) {
        when (intent) {
            MapIntent.OnAppear -> load()
        }
    }

    private fun load() {
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
}
