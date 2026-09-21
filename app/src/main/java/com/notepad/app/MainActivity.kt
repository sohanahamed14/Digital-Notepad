package com.notepad.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.notepad.app.core.reminder.ReminderReceiver
import com.notepad.app.core.security.BiometricAuthManager
import com.notepad.app.core.security.BiometricResult
import com.notepad.app.core.security.SecurityPreferences
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

    @Inject
    lateinit var securityPreferences: SecurityPreferences

    private var isAppUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialNoteId = intent?.getLongExtra(ReminderReceiver.EXTRA_NOTE_ID, 0L)?.takeIf { it != 0L }

        setContent {
            val appTheme by themePreferences.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
            val isAppLockEnabled by securityPreferences.isAppLockEnabled.collectAsState(initial = false)

            NotepadTheme(appTheme = appTheme) {
                if (isAppLockEnabled && !isAppUnlocked) {
                    AppLockScreen(
                        onUnlock = {
                            promptUnlock()
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    NotepadNavHost(
                        navController = navController,
                        biometricAuthManager = biometricAuthManager,
                        initialNoteId = initialNoteId
                    )
                }
            }
        }
    }

    private fun promptUnlock() {
        biometricAuthManager.promptBiometric(
            activity = this,
            title = "Digital Notepad Locked",
            subtitle = "Authenticate to access your notes"
        ) { result ->
            if (result is BiometricResult.Success) {
                isAppUnlocked = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isAppUnlocked) {
            promptUnlock()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
private fun AppLockScreen(onUnlock: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Digital Notepad",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "App Lock is active. Touch fingerprint sensor or authenticate to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUnlock,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(0.7f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
