package com.notepad.app.core.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeKey = stringPreferencesKey("app_theme_mode")

    val themeMode: Flow<AppThemeMode> = context.themeDataStore.data.map { prefs ->
        val name = prefs[themeKey] ?: AppThemeMode.SYSTEM.name
        try { AppThemeMode.valueOf(name) } catch (_: Exception) { AppThemeMode.SYSTEM }
    }

    suspend fun setTheme(mode: AppThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }
}
