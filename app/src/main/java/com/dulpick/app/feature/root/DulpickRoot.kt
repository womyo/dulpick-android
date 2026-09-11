package com.dulpick.app.feature.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dulpick.app.feature.auth.AuthScreen
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

@Composable
fun DulpickRoot(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        RootState.Loading -> SplashScreen()
        is RootState.Ready -> DulpickNavHost(startRoute = current.start.route)
    }
}

@Composable
private fun DulpickNavHost(startRoute: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startRoute) {
        composable(RootRoute.AUTH.route) {
            AuthScreen(
                onLoginSucceeded = { _, isOnboardingCompleted ->
                    val target = if (isOnboardingCompleted) RootRoute.MAIN else RootRoute.ONBOARDING
                    navController.navigate(target.route) {
                        popUpTo(RootRoute.AUTH.route) { inclusive = true }
                    }
                },
            )
        }
        composable(RootRoute.ONBOARDING.route) { PlaceholderScreen(text = "온보딩 (예정)") }
        composable(RootRoute.MAIN.route) { PlaceholderScreen(text = "메인 (예정)") }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.commonWhite),
    )
}

@Composable
private fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.commonWhite),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = Typography.title3SB, color = Colors.textPrimary)
    }
}
