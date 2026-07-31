package com.gratitudelogger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gratitudelogger.ui.auth.PinSetupScreen
import com.gratitudelogger.ui.auth.UnlockScreen
import com.gratitudelogger.ui.backup.BackupScreen
import com.gratitudelogger.ui.calendar.CalendarHomeScreen
import com.gratitudelogger.ui.dayentries.DayEntriesScreen
import com.gratitudelogger.ui.entry.AddEditEntryScreen
import com.gratitudelogger.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class DayEntriesRoute(val epochDay: Long)

@Serializable
data class AddEditEntryRoute(val entryId: Long? = null)

@Serializable
object SettingsRoute

@Serializable
object VerifyPinForChangeRoute

@Serializable
object ChangePinRoute

@Serializable
object BackupRoute

@Composable
fun GratitudeNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            CalendarHomeScreen(
                onDayClick = { date -> navController.navigate(DayEntriesRoute(date.toEpochDay())) },
                onAddEntryForToday = { navController.navigate(AddEditEntryRoute()) },
                onOpenSettings = { navController.navigate(SettingsRoute) }
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
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onChangePin = { navController.navigate(VerifyPinForChangeRoute) },
                onBackup = { navController.navigate(BackupRoute) }
            )
        }
        composable<BackupRoute> {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable<VerifyPinForChangeRoute> {
            // Changing the PIN requires re-proving identity first, even though the user
            // is already inside the authenticated area - otherwise anyone with the phone
            // unlocked could silently take over the PIN.
            UnlockScreen(
                onUnlocked = {
                    navController.navigate(ChangePinRoute) {
                        popUpTo(VerifyPinForChangeRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<ChangePinRoute> {
            PinSetupScreen(onComplete = { navController.popBackStack() })
        }
    }
}
