package com.notepad.app.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import android.content.Intent
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.notepad.app.core.security.BiometricAuthManager
import com.notepad.app.core.security.BiometricResult
import com.notepad.app.core.theme.TextColorPalette
import com.notepad.app.domain.model.NoteCategory
import com.notepad.app.ui.common.MarkdownRenderer
import com.notepad.app.ui.common.TextCounter
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    biometricAuthManager: BiometricAuthManager,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val sheetState = rememberModalBottomSheetState()

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onEvent(NoteEditorUiEvent.OnToggleVoiceInput)
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is NoteEditorUiEffect.NavigateBack -> onNavigateBack()
                is NoteEditorUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is NoteEditorUiEffect.RequestBiometric -> {
                    if (activity != null) {
                        biometricAuthManager.promptBiometric(
                            activity = activity,
                            title = "Authenticate Vault",
                            subtitle = "Confirm identity to change note security",
                            onResult = { result ->
                                if (result is BiometricResult.Success) {
                                    effect.onAuthenticated()
                                }
                            }
                        )
                    } else {
                        effect.onAuthenticated()
                    }
                }
                is NoteEditorUiEffect.LaunchShareIntent -> {
                    context.startActivity(Intent.createChooser(effect.intent, "Share Note"))
                }
            }
        }
    }

    var showShareMenu by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Smart Insights & AI Task Extractor
                    IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnShowInsights) }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Smart Insights & Actions",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Scheduled Reminder
                    IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnShowReminderDialog) }) {
                        Icon(
                            imageVector = if (state.reminderAt != null) Icons.Default.AlarmOn else Icons.Default.Alarm,
                            contentDescription = "Set Reminder",
                            tint = if (state.reminderAt != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Export & Share Dropdown
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Export & Share")
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    viewModel.onEvent(NoteEditorUiEvent.OnExportPdf)
                                    showShareMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share as Markdown") },
                                onClick = {
                                    viewModel.onEvent(NoteEditorUiEvent.OnShareMarkdown)
                                    showShareMenu = false
                                }
                            )
                        }
                    }

                    // Pin toggle
                    IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnTogglePin) }) {
                        Icon(
                            imageVector = if (state.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (state.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Vault Lock toggle
                    IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnToggleVaultLock) }) {
                        Icon(
                            imageVector = if (state.isVaultLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Vault Lock",
                            tint = if (state.isVaultLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Dual-mode Markdown Preview Toggle
                    IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnToggleMarkdownPreview) }) {
                        Icon(
                            imageVector = if (state.isMarkdownPreview) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (state.isMarkdownPreview) "Edit Mode" else "Preview Mode"
                        )
                    }

                    // Version History
                    if (state.id != 0L) {
                        IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnShowVersionHistory) }) {
                            Icon(imageVector = Icons.Default.History, contentDescription = "Version History")
                        }
                    }

                    // Save
                    IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnSaveNote) }) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save Note")
                    }
                }
            )
        },
        bottomBar = {
            // Formatting toolbar (visible only in raw edit mode)
            if (!state.isMarkdownPreview) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnFormatText(MarkdownAction.BOLD)) }) {
                                Icon(imageVector = Icons.Default.FormatBold, contentDescription = "Bold")
                            }
                            IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnFormatText(MarkdownAction.ITALIC)) }) {
                                Icon(imageVector = Icons.Default.FormatItalic, contentDescription = "Italic")
                            }
                            IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnFormatText(MarkdownAction.HEADER)) }) {
                                Icon(imageVector = Icons.Default.Title, contentDescription = "Header")
                            }
                            IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnFormatText(MarkdownAction.CODE_BLOCK)) }) {
                                Icon(imageVector = Icons.Default.Code, contentDescription = "Code Block")
                            }
                            IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnFormatText(MarkdownAction.CHECKLIST)) }) {
                                Icon(imageVector = Icons.Default.CheckBox, contentDescription = "Checklist")
                            }
                            IconButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnFormatText(MarkdownAction.BULLET_LIST)) }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Bullet List")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Mic Button for Speech-to-Text
                            IconButton(
                                onClick = {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        viewModel.onEvent(NoteEditorUiEvent.OnToggleVoiceInput)
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                colors = if (state.isListeningVoice) {
                                    IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                } else {
                                    IconButtonDefaults.iconButtonColors()
                                }
                            ) {
                                Icon(
                                    imageVector = if (state.isListeningVoice) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = "Voice Input"
                                )
                            }
                        }

                        // Text Color swatches row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextColorPalette.forEach { textColorHex ->
                                val isSelected = state.colorHex == textColorHex
                                val swatchColor = if (textColorHex == 0L) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    Color(textColorHex)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.onEvent(NoteEditorUiEvent.OnTextColorChanged(textColorHex))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (textColorHex == 0L) {
                                        Text(
                                            text = "A",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.surface
                                        )
                                    } else if (isSelected) {
                                        val checkTint = if (textColorHex == 0xFFFFFFFFL || textColorHex == 0xFFFDD835L) Color.Black else Color.White
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = checkTint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Word & Char Counter
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextCounter(wordCount = state.wordCount, charCount = state.characterCount)

                            // Category Tag Label
                            Text(
                                text = "Category: ${state.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Resolve text color: 0L = theme default, otherwise custom
            val noteTextColor = if (state.colorHex != 0L) Color(state.colorHex) else MaterialTheme.colorScheme.onSurface

            // Title Input
            TextField(
                value = state.title,
                onValueChange = { viewModel.onEvent(NoteEditorUiEvent.OnTitleChanged(it)) },
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineMedium) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = noteTextColor),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Body: Switch between Raw Markdown Input and Rendered View
            if (state.isMarkdownPreview) {
                // Rendered Markdown Viewer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    MarkdownRenderer(
                        markdown = state.content.ifBlank { "*No content to preview*" },
                        textColor = noteTextColor,
                        onCheckboxToggled = { lineIndex, newChecked ->
                            viewModel.onEvent(NoteEditorUiEvent.OnCheckboxToggled(lineIndex, newChecked))
                        }
                    )
                }
            } else {
                // Raw Text Editor
                TextField(
                    value = state.content,
                    onValueChange = { viewModel.onEvent(NoteEditorUiEvent.OnContentChanged(it)) },
                    placeholder = { Text("Write markdown note here...") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = noteTextColor),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }
    }

    // Version History Bottom Sheet
    if (state.isVersionSheetVisible) {
        val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault()) }

        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(NoteEditorUiEvent.OnDismissVersionHistory) },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Version Snapshots (Last 5 Saves)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (state.versions.isEmpty()) {
                    Text(
                        text = "No saved versions recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.versions, key = { it.versionId }) { version ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = version.title.ifBlank { "Untitled Note" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = dateFormat.format(Date(version.savedAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.onEvent(NoteEditorUiEvent.OnRestoreVersion(version)) }
                                    ) {
                                        Text("Restore")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Smart Insights & AI Task Extractor Bottom Sheet
    if (state.isInsightsSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(NoteEditorUiEvent.OnDismissInsights) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart Note Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Reading Time", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${state.readingTimeMinutes} min",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Word Count", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${state.wordCount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Items Section
                Text(
                    text = "Action Items (${state.extractedTasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (state.extractedTasks.isEmpty()) {
                    Text(
                        text = "No action keywords detected. Add tasks like 'TODO: Buy milk' or 'Deadline tomorrow'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.extractedTasks.forEach { task ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckBox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = task, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.onEvent(NoteEditorUiEvent.OnAppendExtractedTasks) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Insert ${state.extractedTasks.size} Tasks as Checklist")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Key Takeaways
                if (state.keyTakeaways.isNotEmpty()) {
                    Text(
                        text = "Key Takeaways",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.keyTakeaways.forEach { takeaway ->
                            Text(
                                text = "• $takeaway",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Schedule Reminder Alert Dialog
    if (state.isReminderDialogVisible) {
        val now = System.currentTimeMillis()
        val inOneHour = now + (60 * 60 * 1000L)
        val inThreeHours = now + (3 * 60 * 60 * 1000L)
        val tomorrowMorning = now + (24 * 60 * 60 * 1000L)

        AlertDialog(
            onDismissRequest = { viewModel.onEvent(NoteEditorUiEvent.OnDismissReminderDialog) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Schedule Reminder", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose when you want to be reminded about this note:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = { viewModel.onEvent(NoteEditorUiEvent.OnSetReminder(inOneHour)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("In 1 Hour")
                    }

                    OutlinedButton(
                        onClick = { viewModel.onEvent(NoteEditorUiEvent.OnSetReminder(inThreeHours)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("In 3 Hours")
                    }

                    OutlinedButton(
                        onClick = { viewModel.onEvent(NoteEditorUiEvent.OnSetReminder(tomorrowMorning)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tomorrow (+24 Hours)")
                    }

                    if (state.reminderAt != null) {
                        TextButton(
                            onClick = { viewModel.onEvent(NoteEditorUiEvent.OnClearReminder) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear Reminder", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(NoteEditorUiEvent.OnDismissReminderDialog) }) {
                    Text("Close")
                }
            }
        )
    }
}
