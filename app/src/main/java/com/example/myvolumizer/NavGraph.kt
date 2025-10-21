package com.example.myvolumizer

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier,    themeViewModel: ThemeViewModel ) {
    // Shared state between screens
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    val settingsViewModel: SettingsViewModel = viewModel()


    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        composable("splash") {
            SplashScreen(navController)
        }

        composable("home") {
            HomeScreen(selectedAudioUri = selectedAudioUri)
        }

        composable("settings") {
            SettingsScreen(
                navController = navController,
                onAudioSelected = { uri ->
                    selectedAudioUri = uri // ✅ Lifted state for audio
                },
                viewModel = settingsViewModel
            )
        }

        composable("about") {
            AboutScreen(themeViewModel = themeViewModel) // ✅ Fixed name + passing
        }
    }
}
