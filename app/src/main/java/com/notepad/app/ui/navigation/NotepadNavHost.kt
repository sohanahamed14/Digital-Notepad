package com.notepad.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.notepad.app.core.security.BiometricAuthManager
import com.notepad.app.ui.editor.NoteEditorScreen
import com.notepad.app.ui.editor.NoteEditorViewModel
import com.notepad.app.ui.notes.NotesListScreen
import com.notepad.app.ui.notes.NotesViewModel
import com.notepad.app.ui.trash.TrashScreen
import com.notepad.app.ui.trash.TrashViewModel
import com.notepad.app.ui.vault.VaultScreen
import com.notepad.app.ui.vault.VaultViewModel

@Composable
fun NotepadNavHost(
    navController: NavHostController,
    biometricAuthManager: BiometricAuthManager,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.NotesList.route,
        modifier = modifier
    ) {
        composable(Screen.NotesList.route) {
            val viewModel: NotesViewModel = hiltViewModel()
            NotesListScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId))
                },
                onNavigateToEditorWithTemplate = { template ->
                    navController.navigate(Screen.NoteEditor.createRoute(0L, template.name))
                },
                onNavigateToVault = {
                    navController.navigate(Screen.Vault.route)
                },
                onNavigateToTrash = {
                    navController.navigate(Screen.Trash.route)
                }
            )
        }

        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("template") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) {
            val viewModel: NoteEditorViewModel = hiltViewModel()
            NoteEditorScreen(
                viewModel = viewModel,
                biometricAuthManager = biometricAuthManager,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Vault.route) {
            val viewModel: VaultViewModel = hiltViewModel()
            VaultScreen(
                viewModel = viewModel,
                biometricAuthManager = biometricAuthManager,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditor = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId))
                }
            )
        }

        composable(Screen.Trash.route) {
            val viewModel: TrashViewModel = hiltViewModel()
            TrashScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

