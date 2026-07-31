package com.gratitudelogger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gratitudelogger.ui.calendar.CalendarHomeScreen
import com.gratitudelogger.ui.dayentries.DayEntriesScreen
import com.gratitudelogger.ui.entry.AddEditEntryScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class DayEntriesRoute(val epochDay: Long)

@Serializable
data class AddEditEntryRoute(val entryId: Long? = null)

@Composable
fun GratitudeNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            CalendarHomeScreen(
                onDayClick = { date -> navController.navigate(DayEntriesRoute(date.toEpochDay())) },
                onAddEntryForToday = { navController.navigate(AddEditEntryRoute()) }
            )
        }
        composable<DayEntriesRoute> {
            DayEntriesScreen(
                onBack = { navController.popBackStack() },
                onAddEntry = { navController.navigate(AddEditEntryRoute()) },
                onEditEntry = { id -> navController.navigate(AddEditEntryRoute(entryId = id)) }
            )
        }
        composable<AddEditEntryRoute> {
            AddEditEntryScreen(
                onSaved = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
