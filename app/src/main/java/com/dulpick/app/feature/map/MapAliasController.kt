package com.dulpick.app.feature.map

import com.dulpick.app.domain.place.PlaceError
import com.dulpick.app.domain.place.PlaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 저장 장소 별칭 편집 흐름 (iOS PlaceAliasFeature 대응).
// 지도 뷰모델이 커지지 않게 여기로 뺀다
@Suppress("TooGenericExceptionCaught", "LongParameterList")
class MapAliasController(
    private val repository: PlaceRepository,
    private val scope: CoroutineScope,
    private val state: () -> MapState,
    private val update: (MapState.() -> MapState) -> Unit,
    private val effect: (MapSideEffect) -> Unit,
) {
    fun handle(intent: MapIntent) {
        when (intent) {
            is MapIntent.EditClicked -> openAliasEdit(intent.id)
            is MapIntent.AliasSaveClicked -> saveAlias(intent.alias)
            MapIntent.AliasEditDismissed -> update { copy(aliasEdit = null) }
            else -> Unit
        }
    }

    // 별칭 편집 시트를 연다. 초기값은 기존 별칭 없으면 장소명 (iOS PlaceAliasFeature.init 대응)
    private fun openAliasEdit(id: String) {
        val place = state().places.firstOrNull { it.id == id } ?: return
        update {
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
        val edit = state().aliasEdit ?: return
        val trimmed = alias.trim()
        if (trimmed.isEmpty() || edit.isSaving) return
        val placeId = edit.placeId.toLongOrNull() ?: run {
            update { copy(aliasEdit = aliasEdit?.copy(errorMessage = "저장한 장소가 아니에요")) }
            return
        }
        update { copy(aliasEdit = aliasEdit?.copy(isSaving = true, errorMessage = null)) }
        scope.launch {
            try {
                val saved = repository.updateAlias(placeId, trimmed)
                update {
                    copy(
                        aliasEdit = null,
                        places = places.map { if (it.id == saved.id) saved else it },
                    )
                }
                effect(MapSideEffect.ShowToast("별칭을 저장했어요", isError = false))
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
                update { copy(aliasEdit = null) }
                effect(MapSideEffect.SessionExpired)
            }
            PlaceError.NotFound ->
                update { copy(aliasEdit = aliasEdit?.copy(isSaving = false, errorMessage = "저장한 장소가 아니에요")) }
            else ->
                update { copy(aliasEdit = aliasEdit?.copy(isSaving = false, errorMessage = "잠시 뒤 다시 시도해주세요")) }
        }
    }
}
