package com.gratitudelogger.theme

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

@Singleton
class ThemePreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("selected_theme")
    }

    val selectedTheme: Flow<AppTheme> = context.themeDataStore.data.map { prefs ->
        prefs[Keys.THEME]?.let { name -> AppTheme.entries.find { it.name == name } } ?: AppTheme.SUNSET_GOLD
    }

    suspend fun setTheme(theme: AppTheme) {
        context.themeDataStore.edit { it[Keys.THEME] = theme.name }
    }
}
