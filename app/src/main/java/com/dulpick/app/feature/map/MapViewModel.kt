package com.dulpick.app.feature.map

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.explore.ExploreError
import com.dulpick.app.domain.place.PlaceRepository
import com.dulpick.app.domain.place.SavedPlace
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

    // 낙관적으로 뺀 행. 서버 삭제가 실패하면 이 자리로 되돌린다
    private val pendingDeletes = mutableMapOf<String, PendingDelete>()

    override fun onIntent(intent: MapIntent) {
        when (intent) {
            MapIntent.OnAppear -> load()
            is MapIntent.OwnershipSelected -> setState { copy(selectedOwnership = intent.ownership) }
            is MapIntent.CategorySelected -> setState { copy(selectedCategory = intent.category) }
            is MapIntent.DeleteClicked -> delete(intent.id)
        }
    }

    // 목록에서 먼저 빼고 서버를 부른다. 실패하면 원래 자리로 되돌리고 토스트를 띄운다 (iOS removeSavedPlace 대응)
    private fun delete(id: String) {
        val index = currentState.places.indexOfFirst { it.id == id }
        if (index < 0) return
        val removed = currentState.places[index]
        val placeId = removed.place.id.toLongOrNull() ?: return
        pendingDeletes[id] = PendingDelete(removed, index)
        setState { copy(places = places.filterNot { it.id == id }) }
        viewModelScope.launch {
            try {
                placeRepository.removePlace(placeId)
                pendingDeletes.remove(id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                rollbackDelete(id)
                if (error == ExploreError.Unauthorized) {
                    postSideEffect(MapSideEffect.SessionExpired)
                } else {
                    postSideEffect(MapSideEffect.ShowToast("저장을 취소하지 못했어요", isError = true))
                }
            }
        }
    }

    // 뺀 자리로 되돌린다. 되돌리는 사이 목록을 다시 받아 이미 있으면 중복으로 넣지 않는다
    private fun rollbackDelete(id: String) {
        val pending = pendingDeletes.remove(id) ?: return
        if (currentState.places.any { it.id == id }) return
        setState {
            val restored = places.toMutableList().apply { add(pending.index.coerceIn(0, size), pending.place) }
            copy(places = restored)
        }
    }

    private data class PendingDelete(val place: SavedPlace, val index: Int)

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
