package com.dulpick.app.feature.pastdates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.home.DateSchedule
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val HORIZONTAL_PADDING = 20.dp

@Composable
fun PastDateCoursesScreen(
    onBack: () -> Unit,
    viewModel: PastDateCoursesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            // TODO: 코스 만들기·코스 결과는 지도/코스 단계에서 연결한다
            PastDateCoursesSideEffect.OpenCreateCourse -> Unit
            is PastDateCoursesSideEffect.OpenCourseResult -> Unit
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault),
    ) {
        TopBar(onBack = onBack)
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // 로딩 중엔 "총 0번" 이 번쩍이지 않게 로딩만 보여준다
                !state.hasLoaded -> CenteredLoading(modifier = Modifier.fillMaxSize())
                state.courses.isEmpty() -> EmptyState(
                    showCreate = !state.hasCurrentCourse,
                    onCreate = { viewModel.onIntent(PastDateCoursesIntent.CreateCourseClicked) },
                )
                else -> CourseList(state = state, onIntent = viewModel::onIntent)
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.arrowleft),
            contentDescription = "뒤로",
            colorFilter = ColorFilter.tint(Colors.textPrimary),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 12.dp)
                .size(24.dp)
                .clickable(onClick = onBack),
        )
        Text(text = "지난 데이트 일정", style = Typography.body1SB, color = Colors.gray900)
    }
}

@Composable
private fun CourseList(state: PastDateCoursesState, onIntent: (PastDateCoursesIntent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HORIZONTAL_PADDING,
            end = HORIZONTAL_PADDING,
            top = 12.dp,
            bottom = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "countBanner") {
            CountBanner(totalCount = state.totalCount, modifier = Modifier.padding(bottom = 12.dp))
        }
        itemsIndexed(state.courses, key = { _, course -> course.id }) { index, course ->
            if (index == state.courses.lastIndex && state.hasNext && !state.isLoadingMore) {
                LaunchedEffect(index, state.courses.size) { onIntent(PastDateCoursesIntent.ReachedEnd) }
            }
            PastDateCourseRow(
                schedule = course,
                onClick = { onIntent(PastDateCoursesIntent.CourseClicked(course.id)) },
            )
        }
        if (state.isLoadingMore) {
            item(key = "loadingMore") { CenteredLoading(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) }
        }
    }
}

// 총 데이트 횟수 배너. 왼쪽 위 문구 + 오른쪽 아래 이미지
@Composable
private fun CountBanner(totalCount: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Colors.primaryPink),
    ) {
        Text(
            text = "지금까지 총 ${totalCount}번\n데이트 일정을 함께 했어요",
            style = Typography.title3SB,
            color = Colors.textInverse,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
        )
        Image(
            painter = painterResource(R.drawable.bannertogether),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .height(100.dp),
        )
    }
}

// 지난 데이트 카드. 홈의 가로 카드와 달리 폭을 꽉 채운다
@Composable
private fun PastDateCourseRow(schedule: DateSchedule, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Colors.bgSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = schedule.title, style = Typography.body1M, color = Colors.textPrimary)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Colors.commonWhite)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.mappin),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Colors.primaryPink),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "총 ${schedule.placeCount}곳의 장소",
                    style = Typography.body2M,
                    color = Colors.brandPrimary,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = schedule.date, style = Typography.caption1R, color = Colors.textTertiary)
        }
    }
}

@Composable
private fun EmptyState(showCreate: Boolean, onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HORIZONTAL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painter = painterResource(R.drawable.datescheduleempty), contentDescription = null)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "지난 데이트 일정이 없어요", style = Typography.title3SB, color = Colors.textPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "새로운 데이트 일정을 만들어보세요", style = Typography.body1M, color = Colors.textTertiary)
        if (showCreate) {
            Spacer(modifier = Modifier.height(24.dp))
            AppButton(
                text = "일정 만들러가기",
                onClick = onCreate,
                variant = AppButtonVariant.OUTLINED,
                size = AppButtonSize.LG,
            )
        }
    }
}

@Composable
private fun CenteredLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Colors.primaryPink)
    }
}
