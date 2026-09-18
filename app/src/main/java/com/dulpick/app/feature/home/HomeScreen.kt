package com.dulpick.app.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.home.DateSchedule
import com.dulpick.app.ui.component.ContentCard
import com.dulpick.app.ui.component.ContentCardSkeleton
import com.dulpick.app.feature.home.component.DateScheduleCard
import com.dulpick.app.feature.home.component.HomeBanner
import com.dulpick.app.feature.home.component.HomeHeader
import com.dulpick.app.feature.home.component.SavedPlaceRow
import com.dulpick.app.ui.component.ShimmerBox
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val HORIZONTAL_PADDING = 20.dp
private val RECOMMENDATION_CARD_WIDTH = 170.dp
private val SKELETON_BANNER_HEIGHT = 110.dp
private const val SHEET_CORNER_RADIUS = 24
private const val SAVED_SKELETON_ROWS = 3

@Composable
fun HomeScreen(
    onSessionExpired: () -> Unit,
    onOpenCoupleConnect: (myNickname: String) -> Unit,
    onOpenPastDates: (hasCurrentCourse: Boolean) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            HomeSideEffect.SessionExpired -> onSessionExpired()
            HomeSideEffect.OpenConnectFlow -> onOpenCoupleConnect(state.nickname)
            is HomeSideEffect.OpenPastDates -> onOpenPastDates(effect.hasCurrentCourse)
            // TODO: 코스·상세는 지도/코스 단계에서 연결한다
            HomeSideEffect.OpenCourseFlow -> Unit
            is HomeSideEffect.OpenUpcomingCourse -> Unit
            is HomeSideEffect.OpenContentDetail -> Unit
            is HomeSideEffect.OpenPastSchedule -> Unit
            is HomeSideEffect.OpenPlaceDetail -> Unit
            HomeSideEffect.OpenSavedPlacesAll -> Unit
        }
    }

    LaunchedEffect(Unit) { viewModel.onIntent(HomeIntent.OnAppear) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault)
            .verticalScroll(rememberScrollState()),
    ) {
        TopSection(state = state, onIntent = viewModel::onIntent)
        ContentSheet(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun TopSection(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 배경을 먼저 깔아 상태바 영역까지 어둡게, 그 뒤 상태바 인셋으로 내용만 내린다
            .background(Colors.gray900)
            .statusBarsPadding(),
    ) {
        HomeHeader(
            nickname = state.nickname,
            partnerName = state.partnerName,
            onCalendarClick = {
                // 로딩 중엔 달력 동작을 막는다
                if (state.didLoadSummary) onIntent(HomeIntent.CalendarClicked)
            },
        )

        Box(modifier = Modifier.padding(bottom = 20.dp)) {
            if (state.didLoadSummary) {
                HomeBanner(
                    isConnected = state.isConnected,
                    upcomingSchedule = state.upcomingSchedule,
                    onConnectClick = { onIntent(HomeIntent.ConnectFlowRequested) },
                    onCreateCourseClick = { onIntent(HomeIntent.CourseFlowRequested) },
                    onBannerClick = { onIntent(HomeIntent.BannerClicked) },
                    modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
                )
            } else {
                SkeletonBanner()
            }
        }
    }
}

@Composable
private fun ContentSheet(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // 둥근 시트 위쪽 모서리 틈으로 비치는 어두운 띠 (상단 다크와 이어 보이게)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SHEET_CORNER_RADIUS.dp)
                .background(Colors.gray900),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = SHEET_CORNER_RADIUS.dp, topEnd = SHEET_CORNER_RADIUS.dp))
                .background(Colors.bgDefault)
                .padding(top = 40.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(60.dp),
        ) {
            RecommendationSection(state = state, onIntent = onIntent)

            if (state.showsPastSchedules) {
                PastScheduleSection(schedules = state.visiblePastSchedules, onIntent = onIntent)
            }

            SavedPlaceSection(state = state, onIntent = onIntent)
        }
    }
}

// MARK: 추천

@Composable
private fun RecommendationSection(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = recommendationTitle(state.nickname),
            style = Typography.title2B,
            // 닉네임 로드 전 빈 값이 그려졌다 리플로우되는 걸 막고, 자리만 잡아둔다
            modifier = Modifier
                .padding(horizontal = HORIZONTAL_PADDING)
                .alpha(if (state.didLoadSummary) 1f else 0f),
        )

        if (!state.didLoadRecommendations) {
            RecommendationSkeletonRow()
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = HORIZONTAL_PADDING),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.recommendations.forEach { content ->
                    ContentCard(
                        content = content,
                        onClick = { onIntent(HomeIntent.RecommendationClicked(content.id)) },
                        modifier = Modifier.width(RECOMMENDATION_CARD_WIDTH),
                    )
                }
            }
        }
    }
}

private fun recommendationTitle(nickname: String) = buildAnnotatedString {
    withStyle(SpanStyle(color = Colors.textPrimary)) {
        append("${nickname}님을 위한 ")
    }
    withStyle(SpanStyle(color = Colors.primaryPink)) {
        append("장소 추천")
    }
}

// MARK: 지난 데이트

@Composable
private fun PastScheduleSection(schedules: List<DateSchedule>, onIntent: (HomeIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "우리의 지난 데이트 일정",
            style = Typography.title2B,
            color = Colors.textPrimary,
            modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = HORIZONTAL_PADDING),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            schedules.forEach { schedule ->
                DateScheduleCard(
                    schedule = schedule,
                    onClick = { onIntent(HomeIntent.PastScheduleClicked(schedule.id)) },
                )
            }
        }
    }
}

// MARK: 최근 저장 장소

@Composable
private fun SavedPlaceSection(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SavedPlaceHeader(
            showSeeAll = state.visibleSavedPlaces.isNotEmpty(),
            onSeeAll = { onIntent(HomeIntent.SavedPlacesSeeAllClicked) },
        )
        SavedPlaceContent(state = state, onIntent = onIntent)
    }
}

@Composable
private fun SavedPlaceHeader(showSeeAll: Boolean, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "최근 저장된 장소",
            style = Typography.title2B,
            color = Colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (showSeeAll) {
            Row(
                modifier = Modifier.clickable(onClick = onSeeAll),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = "전체보기", style = Typography.body1M, color = Colors.textTertiary)
                Image(
                    painter = painterResource(R.drawable.arrowright),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Colors.textTertiary),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SavedPlaceContent(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    when {
        !state.didLoadSaved -> Column(
            modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(SAVED_SKELETON_ROWS) { SavedPlaceSkeletonRow() }
        }
        state.visibleSavedPlaces.isEmpty() -> EmptySavedPlaces()
        else -> Column(
            modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.visibleSavedPlaces.forEach { place ->
                SavedPlaceRow(place = place, onClick = { onIntent(HomeIntent.SavedPlaceClicked(place.id)) })
            }
        }
    }
}

@Composable
private fun EmptySavedPlaces() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.placeempty),
            contentDescription = null,
            modifier = Modifier.size(140.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "최근 저장된 장소가 없어요!", style = Typography.headline, color = Colors.textPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "장소를 저장해주세요", style = Typography.body2M, color = Colors.textTertiary)
    }
}

// MARK: 첫 로딩 스켈레톤

@Composable
private fun SkeletonBanner() {
    ShimmerBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING)
            .height(SKELETON_BANNER_HEIGHT)
            .clip(RoundedCornerShape(16.dp)),
        baseColor = Colors.gray700,
    )
}

@Composable
private fun RecommendationSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = HORIZONTAL_PADDING),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(SAVED_SKELETON_ROWS) {
            ContentCardSkeleton(modifier = Modifier.width(RECOMMENDATION_CARD_WIDTH))
        }
    }
}

@Composable
private fun SavedPlaceSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Colors.bgSubtle)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShimmerBox(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)))
        ShimmerBox(
            modifier = Modifier
                .width(140.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
    }
}
