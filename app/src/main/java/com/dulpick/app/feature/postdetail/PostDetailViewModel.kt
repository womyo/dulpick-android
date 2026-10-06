package com.dulpick.app.feature.postdetail

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.explore.ContentPlace
import com.dulpick.app.domain.explore.ExploreError
import com.dulpick.app.domain.explore.ExploreRepository
import com.dulpick.app.domain.place.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class PostDetailViewModel @Inject constructor(
    private val exploreRepository: ExploreRepository,
    private val placeRepository: PlaceRepository,
) : MviViewModel<PostDetailState, PostDetailIntent, PostDetailSideEffect>(PostDetailState()) {

    private var contentId: String? = null
    // 같은 장소를 연달아 누르면 앞 요청을 기다렸다 이어 부른다
    private val saveJobs = mutableMapOf<String, Job>()

    override fun onIntent(intent: PostDetailIntent) {
        when (intent) {
            is PostDetailIntent.Start -> start(intent.contentId)
            PostDetailIntent.RetryClicked -> load()
            PostDetailIntent.ExpandToggled -> setState { copy(isExpanded = !isExpanded) }
            PostDetailIntent.CloseClicked -> postSideEffect(PostDetailSideEffect.Close)
            is PostDetailIntent.PlaceClicked -> postSideEffect(PostDetailSideEffect.PlaceSelected(intent.id))
            is PostDetailIntent.PlaceBookmarkClicked -> toggleSave(intent.id)
        }
    }

    private fun start(id: String) {
        if (contentId == id) return
        contentId = id
        load()
    }

    // 값이 있거나 부르는 중이면 거른다. 다시 시도는 실패 화면에서만 눌려 늘 통과한다
    private fun load() {
        val id = contentId ?: return
        if (currentState.detail != null || currentState.isLoading) return
        setState { copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            try {
                val detail = exploreRepository.contentDetail(id)
                setState {
                    copy(
                        detail = detail,
                        savedPlaceIds = detail.places.filter { it.isSaved }.map { it.id }.toSet(),
                        isLoading = false,
                        loadFailed = false,
                    )
                }
                // 지도가 이 상세의 places 로 핀·카메라를 세우도록 올린다
                postSideEffect(PostDetailSideEffect.DetailLoaded(detail))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isLoading = false, loadFailed = true) }
                // 인증 만료만 위로 올린다. 다시 시도해도 안 풀리는 실패다
                if (error == ExploreError.Unauthorized) postSideEffect(PostDetailSideEffect.SessionExpired)
            }
        }
    }

    // 저장 버튼. 먼저 표시를 뒤집고 서버를 부른다. 실패하면 되돌린다
    private fun toggleSave(id: String) {
        val place = currentState.detail?.places?.firstOrNull { it.id == id } ?: return
        val wasSaved = id in currentState.savedPlaceIds
        // 저장하려는데 카카오 식별자가 없으면 부를 수 없다
        if (!wasSaved && place.place.kakaoPlaceId == null) return
        setState {
            copy(savedPlaceIds = if (wasSaved) savedPlaceIds - id else savedPlaceIds + id)
        }
        runSave(place, wasSaved)
    }

    private fun runSave(place: ContentPlace, wasSaved: Boolean) {
        // 게시글 속 장소는 장소 번호가 늘 있다. 없으면 부를 곳이 없어 되돌린다
        val placeId = place.place.id.toLongOrNull()
        if (wasSaved && placeId == null) {
            rollback(place.id, wasSaved)
            return
        }
        // 앞 요청을 취소하지 않고 기다린다. 저장을 취소해 버리면 서버 ID 를 못 받아,
        // 곧바로 이어지는 해제가 엉뚱한 장소를 지울 수 있다
        val previous = saveJobs[place.id]
        saveJobs[place.id] = viewModelScope.launch {
            previous?.join()
            try {
                if (wasSaved) {
                    placeRepository.removePlace(checkNotNull(placeId))
                } else {
                    placeRepository.savePlace(checkNotNull(place.place.kakaoPlaceId), place.place.name, null)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                rollback(place.id, wasSaved)
            }
        }
    }

    // 서버 실패 → 미리 뒤집었던 표시를 되돌린다
    private fun rollback(id: String, wasSaved: Boolean) {
        setState { copy(savedPlaceIds = if (wasSaved) savedPlaceIds + id else savedPlaceIds - id) }
    }
}
