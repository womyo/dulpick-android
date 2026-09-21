package com.dulpick.app.feature.map

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.place.PlaceError
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
            is MapIntent.EditClicked -> openAliasEdit(intent.id)
            is MapIntent.AliasSaveClicked -> saveAlias(intent.alias)
            MapIntent.AliasEditDismissed -> setState { copy(aliasEdit = null) }
            is MapIntent.OpenDetail -> setState { copy(detail = intent.target) }
            is MapIntent.OpenSavedDetail -> openSavedDetail(intent.place)
            MapIntent.CloseDetail -> setState { copy(detail = null) }
        }
    }

    // 저장 장소는 서버 placeId 로 조회한다(검색어 불필요)
    private fun openSavedDetail(place: SavedPlace) {
        val serverPlaceId = place.place.id.toLongOrNull()
        setState { copy(detail = DetailTarget(place.place, query = "", serverPlaceId = serverPlaceId)) }
    }

    // 별칭 편집 시트를 연다. 초기값은 기존 별칭 없으면 장소명 (iOS PlaceAliasFeature.init 대응)
    private fun openAliasEdit(id: String) {
        val place = currentState.places.firstOrNull { it.id == id } ?: return
        setState {
            copy(
                aliasEdit = AliasEdit(
                    placeId = place.place.id,
                    placeName = place.place.name,
                    address = place.place.roadAddress,
                    initialAlias = place.alias ?: place.place.name,
                ),
            )
        }
    }

    // 별칭 저장. 성공하면 목록 원소를 갈아 끼우고 시트를 닫으며 토스트, 실패하면 시트에 문구를 띄운다
    private fun saveAlias(alias: String) {
        val edit = currentState.aliasEdit ?: return
        val trimmed = alias.trim()
        if (trimmed.isEmpty() || edit.isSaving) return
        val placeId = edit.placeId.toLongOrNull() ?: run {
            setState { copy(aliasEdit = aliasEdit?.copy(errorMessage = "저장한 장소가 아니에요")) }
            return
        }
        setState { copy(aliasEdit = aliasEdit?.copy(isSaving = true, errorMessage = null)) }
        viewModelScope.launch {
            try {
                val saved = placeRepository.updateAlias(placeId, trimmed)
                setState {
                    copy(
                        aliasEdit = null,
                        places = places.map { if (it.id == saved.id) saved else it },
                    )
                }
                postSideEffect(MapSideEffect.ShowToast("별칭을 저장했어요", isError = false))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                handleAliasFailure(error)
            }
        }
    }

    // iOS PlaceAliasFeature 대응: 401 은 세션 만료, 404 는 저장 대상 아님, 그 외는 재시도 안내
    private fun handleAliasFailure(error: Throwable) {
        when (error) {
            PlaceError.Unauthorized -> {
                setState { copy(aliasEdit = null) }
                postSideEffect(MapSideEffect.SessionExpired)
            }
            PlaceError.NotFound ->
                setState { copy(aliasEdit = aliasEdit?.copy(isSaving = false, errorMessage = "저장한 장소가 아니에요")) }
            else ->
                setState { copy(aliasEdit = aliasEdit?.copy(isSaving = false, errorMessage = "잠시 뒤 다시 시도해주세요")) }
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
                if (error == PlaceError.Unauthorized) {
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
                if (error == PlaceError.Unauthorized) postSideEffect(MapSideEffect.SessionExpired)
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
