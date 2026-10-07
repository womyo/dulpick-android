package com.dulpick.app.feature.notice

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.notice.NoticePage
import com.dulpick.app.domain.notice.NoticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class NoticeListViewModel @Inject constructor(
    private val noticeRepository: NoticeRepository,
) : MviViewModel<NoticeListState, NoticeListIntent, NoticeListSideEffect>(NoticeListState()) {

    override fun onIntent(intent: NoticeListIntent) {
        when (intent) {
            NoticeListIntent.OnAppear -> load()
            NoticeListIntent.ReachedEnd -> loadMore()
            is NoticeListIntent.NoticeClicked ->
                postSideEffect(NoticeListSideEffect.OpenNotice(intent.notice))
            NoticeListIntent.BackClicked -> postSideEffect(NoticeListSideEffect.Dismissed)
        }
    }

    // 상세에서 돌아올 때도 불리므로, 받아 둔 페이지를 덮지 않게 한 번만 받는다
    private fun load() {
        if (currentState.hasLoaded) return
        viewModelScope.launch {
            val page = fetch(0)
            setState {
                copy(
                    hasLoaded = true,
                    notices = page?.notices.orEmpty(),
                    hasNext = page?.hasNext ?: false,
                    page = 1,
                )
            }
        }
    }

    private fun loadMore() {
        if (!currentState.canLoadMore) return
        setState { copy(isLoadingMore = true) }
        val requested = currentState.page
        viewModelScope.launch {
            val page = fetch(requested)
            setState {
                if (page == null) {
                    copy(isLoadingMore = false, loadMoreFailed = true)
                } else {
                    copy(
                        notices = (notices + page.notices).distinctBy { it.id },
                        hasNext = page.hasNext,
                        page = this.page + 1,
                        isLoadingMore = false,
                    )
                }
            }
        }
    }

    // 실패를 구분해 쓸 데가 없다. 못 받으면 null 로 둔다 (iOS try? 와 같다)
    private suspend fun fetch(page: Int): NoticePage? = try {
        noticeRepository.notices(page, NoticeListState.PAGE_SIZE)
    } catch (error: CancellationException) {
        throw error
    } catch (ignored: Throwable) {
        null
    }
}
