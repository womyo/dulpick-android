package com.dulpick.app.feature.placedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.place.PlaceDetail
import com.dulpick.app.domain.place.PlaceError
import com.dulpick.app.domain.place.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

// nav 인자: 검색 결과는 카카오 ID + 검색어, 저장/게시글 장소는 서버 placeId
const val ARG_DETAIL_PLACE_ID = "placeId"
const val ARG_DETAIL_KAKAO_ID = "kakaoPlaceId"
const val ARG_DETAIL_QUERY = "query"

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class PlaceDetailViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<PlaceDetailState, PlaceDetailIntent, PlaceDetailSideEffect>(PlaceDetailState()) {

    private val placeId: Long = savedStateHandle.get<Long>(ARG_DETAIL_PLACE_ID) ?: 0L
    private val kakaoPlaceId: String = savedStateHandle.get<String>(ARG_DETAIL_KAKAO_ID).orEmpty()
    private val query: String = savedStateHandle.get<String>(ARG_DETAIL_QUERY).orEmpty()
    private var started = false

    override fun onIntent(intent: PlaceDetailIntent) {
        when (intent) {
            PlaceDetailIntent.OnAppear -> load()
            PlaceDetailIntent.AddressToggled -> setState { copy(isAddressExpanded = !isAddressExpanded) }
            PlaceDetailIntent.MapClicked -> openKakaoMap()
            PlaceDetailIntent.CloseClicked -> postSideEffect(PlaceDetailSideEffect.Close)
        }
    }

    private fun load() {
        if (started) return
        started = true
        viewModelScope.launch {
            try {
                val detail = if (placeId > 0) {
                    placeRepository.placeDetail(placeId)
                } else {
                    placeRepository.kakaoPlaceDetail(kakaoPlaceId, query)
                }
                setState { copy(isLoading = false, isFailed = false, detail = detail) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isLoading = false, isFailed = true) }
                if (error == PlaceError.Unauthorized) postSideEffect(PlaceDetailSideEffect.SessionExpired)
            }
        }
    }

    // 카카오맵 앱(kakaomap://) 우선, 없으면 웹. 웹은 서버가 준 kakaoPlaceUrl 이 먼저 (iOS 로직 대응)
    private fun openKakaoMap() {
        val detail = currentState.detail ?: return
        val appUri = detail.place.kakaoPlaceId?.let { "kakaomap://place?id=$it" }
        val webUrl = detail.kakaoPlaceUrl
            ?: detail.place.kakaoPlaceId?.let { "https://place.map.kakao.com/$it" }
        if (appUri == null && webUrl == null) return
        postSideEffect(PlaceDetailSideEffect.OpenKakaoMap(appUri, webUrl))
    }
}

// 앱/웹 어느 쪽이든 열 곳이 있는지 (지도 버튼 활성 판단)
fun PlaceDetail.canOpenKakaoMap(): Boolean = place.kakaoPlaceId != null || kakaoPlaceUrl != null
