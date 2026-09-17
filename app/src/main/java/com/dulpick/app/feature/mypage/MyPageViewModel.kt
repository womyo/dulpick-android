package com.dulpick.app.feature.mypage

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.auth.AuthRepository
import com.dulpick.app.domain.couple.CoupleError
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.profile.NotificationSettings
import com.dulpick.app.domain.profile.ProfileError
import com.dulpick.app.domain.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class MyPageViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val coupleRepository: CoupleRepository,
) : MviViewModel<MyPageState, MyPageIntent, MyPageSideEffect>(MyPageState()) {

    // 알림 설정 PUT. 연타 시 직전 요청을 취소해 최신 상태만 반영한다
    private var updateJob: Job? = null

    override fun onIntent(intent: MyPageIntent) {
        when (intent) {
            MyPageIntent.OnAppear -> load()
            is MyPageIntent.ContentSavedToggled ->
                toggle { copy(savedContentAlarm = intent.enabled) }
            is MyPageIntent.DateScheduleToggled ->
                toggle { copy(dateScheduleAlarm = intent.enabled) }
            is MyPageIntent.MarketingToggled ->
                toggle { copy(marketingAlarm = intent.enabled) }
            MyPageIntent.LogoutClicked -> logout()
            else -> handleAccountIntent(intent)
        }
    }

    // 회원탈퇴·프로필 수정 관련 인텐트. onIntent 복잡도를 낮추려 분리한다
    private fun handleAccountIntent(intent: MyPageIntent) {
        when (intent) {
            MyPageIntent.WithdrawClicked -> setState { copy(isWithdrawDialogPresented = true) }
            MyPageIntent.WithdrawDismissed -> setState { copy(isWithdrawDialogPresented = false) }
            MyPageIntent.WithdrawConfirmed -> withdraw()
            MyPageIntent.ProfileEditClicked -> setState { copy(isProfileEditPresented = true) }
            MyPageIntent.ProfileEditDismissed ->
                if (!currentState.isSavingProfile) setState { copy(isProfileEditPresented = false) }
            is MyPageIntent.ProfileSaveClicked -> saveProfile(intent.nickname, intent.iconId)
            MyPageIntent.ConnectionClicked -> checkConnection()
            else -> Unit
        }
    }

    // 연결 여부를 먼저 확인해 연결 관리 화면 / 커플 연결 플로우로 가른다 (iOS handleConnection 대응)
    private fun checkConnection() {
        viewModelScope.launch {
            runCatching { coupleRepository.current() }
                .onSuccess { status ->
                    if (status?.connected == true) {
                        postSideEffect(MyPageSideEffect.OpenConnection)
                    } else {
                        postSideEffect(MyPageSideEffect.OpenCoupleConnect(currentState.nickname))
                    }
                }
                .onFailure { error ->
                    // 조회 실패를 미연결로 오해하지 않도록 이동 없이 알린다
                    if (error == CoupleError.Unauthorized) {
                        postSideEffect(MyPageSideEffect.SessionExpired)
                    } else {
                        postSideEffect(MyPageSideEffect.ShowToast("연결 상태를 확인하지 못했어요. 잠시 후 다시 시도해 주세요."))
                    }
                }
        }
    }

    private fun load() {
        viewModelScope.launch { loadProfile() }
        viewModelScope.launch { loadNotifications() }
    }

    private suspend fun loadProfile() {
        runCatching { profileRepository.profile() }
            .onSuccess { profile ->
                setState { copy(nickname = profile.nickname, iconId = profile.iconId) }
            }
            .onFailure { error ->
                if (error == ProfileError.Unauthorized) {
                    postSideEffect(MyPageSideEffect.SessionExpired)
                }
            }
    }

    private suspend fun loadNotifications() {
        runCatching { profileRepository.notificationSettings() }
            .onSuccess { settings -> applyLoaded(settings) }
            .onFailure { error ->
                // 실패해도 로딩은 걷어 무한 로딩을 막는다
                setState { copy(isLoading = false) }
                if (error == ProfileError.Unauthorized) postSideEffect(MyPageSideEffect.SessionExpired)
            }
    }

    private fun applyLoaded(settings: NotificationSettings) {
        setState {
            copy(
                isLoading = false,
                savedContentAlarm = settings.contentSavedEnabled,
                dateScheduleAlarm = settings.dateScheduleEnabled,
                marketingAlarm = settings.marketingEnabled,
                marketingConsentVersion = settings.marketingConsentVersion,
                availableMarketingConsentVersion = settings.availableMarketingConsentVersion,
            )
        }
    }

    // 토글을 즉시 반영(낙관적)하고 현재 상태 전체를 PUT 한다
    private fun toggle(reducer: MyPageState.() -> MyPageState) {
        setState { reducer() }
        pushNotifications()
    }

    private fun pushNotifications() {
        val state = currentState
        // 마케팅을 처음 켜면 저장된 동의 버전이 없어 현재 가능한 버전을 실어 보낸다 (iOS 동일)
        val outgoing = NotificationSettings(
            contentSavedEnabled = state.savedContentAlarm,
            dateScheduleEnabled = state.dateScheduleAlarm,
            marketingEnabled = state.marketingAlarm,
            marketingConsentVersion = state.marketingConsentVersion ?: state.availableMarketingConsentVersion,
            availableMarketingConsentVersion = state.availableMarketingConsentVersion,
        )
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            try {
                applyLoaded(profileRepository.updateNotificationSettings(outgoing))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (error == ProfileError.Unauthorized) {
                    postSideEffect(MyPageSideEffect.SessionExpired)
                    return@launch
                }
                // 서버 반영에 실패했으니 서버 값으로 되돌려 화면과 서버를 맞춘다
                postSideEffect(MyPageSideEffect.ShowToast("설정 저장에 실패했어요. 다시 시도해 주세요."))
                loadNotifications()
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            // 로그아웃은 실패해도 로컬 세션 정리 후 로그인으로 보낸다
            runCatching { authRepository.logout() }
            postSideEffect(MyPageSideEffect.LoggedOut)
        }
    }

    // 프로필 수정 저장(PATCH). 성공 시 표시값 갱신하고 시트 닫음, 실패 시 토스트(시트는 유지)
    private fun saveProfile(nickname: String, iconId: Int) {
        if (currentState.isSavingProfile) return
        setState { copy(isSavingProfile = true) }
        viewModelScope.launch {
            runCatching { profileRepository.updateProfile(nickname, iconId) }
                .onSuccess { profile ->
                    setState {
                        copy(
                            isSavingProfile = false,
                            isProfileEditPresented = false,
                            nickname = profile.nickname,
                            iconId = profile.iconId,
                        )
                    }
                }
                .onFailure { error ->
                    setState { copy(isSavingProfile = false) }
                    if (error == ProfileError.Unauthorized) {
                        postSideEffect(MyPageSideEffect.SessionExpired)
                    } else {
                        postSideEffect(MyPageSideEffect.ShowToast("저장에 실패했어요. 잠시 후 다시 시도해 주세요."))
                    }
                }
        }
    }

    // 탈퇴 성공 시 로그아웃(로컬 정리)까지 하고 로그인으로. 실패하면 모달만 닫고 알린다
    private fun withdraw() {
        if (currentState.isWithdrawing) return
        setState { copy(isWithdrawing = true) }
        viewModelScope.launch {
            runCatching { profileRepository.withdraw() }
                .onSuccess {
                    runCatching { authRepository.logout() }
                    setState { copy(isWithdrawing = false, isWithdrawDialogPresented = false) }
                    postSideEffect(MyPageSideEffect.LoggedOut)
                }
                .onFailure { error ->
                    setState { copy(isWithdrawing = false, isWithdrawDialogPresented = false) }
                    if (error == ProfileError.Unauthorized) {
                        postSideEffect(MyPageSideEffect.SessionExpired)
                    } else {
                        postSideEffect(MyPageSideEffect.ShowToast("탈퇴에 실패했어요. 잠시 후 다시 시도해 주세요."))
                    }
                }
        }
    }
}
