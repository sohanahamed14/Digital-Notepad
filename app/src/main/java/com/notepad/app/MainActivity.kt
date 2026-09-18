package com.notepad.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.notepad.app.core.security.BiometricAuthManager
import com.notepad.app.core.theme.AppThemeMode
import com.notepad.app.core.theme.NotepadTheme
import com.notepad.app.core.theme.ThemePreferences
import com.notepad.app.ui.navigation.NotepadNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appTheme by themePreferences.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
            NotepadTheme(appTheme = appTheme) {
                val navController = rememberNavController()
                NotepadNavHost(
                    navController = navController,
                    biometricAuthManager = biometricAuthManager
                )
            }
        }
    }
}

