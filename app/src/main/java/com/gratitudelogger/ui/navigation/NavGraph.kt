package com.gratitudelogger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gratitudelogger.ui.calendar.CalendarHomeScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Composable
fun GratitudeNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            CalendarHomeScreen()
        }
    }
}
