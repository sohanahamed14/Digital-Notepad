package com.notepad.app.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")

@Singleton
open class SecurityPreferences private constructor(
    private val context: Context?,
    @Suppress("UNUSED_PARAMETER") marker: Unit?
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, null)

    constructor() : this(null, null)

    private val appLockKey = booleanPreferencesKey("is_app_lock_enabled")

    open val isAppLockEnabled: Flow<Boolean> = context?.securityDataStore?.data
        ?.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        ?.map { prefs ->
            prefs[appLockKey] ?: false
        } ?: flowOf(false)

    open suspend fun setAppLockEnabled(enabled: Boolean) {
        context?.securityDataStore?.edit { prefs ->
            prefs[appLockKey] = enabled
        }
    }
}
