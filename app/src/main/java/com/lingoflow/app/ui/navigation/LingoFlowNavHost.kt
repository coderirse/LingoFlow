package com.lingoflow.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lingoflow.app.ui.home.HomeRoute
import com.lingoflow.app.ui.settings.SettingsRoute

/** App-wide navigation routes. New screens register here as they are added. */
object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun LingoFlowNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeRoute(
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
