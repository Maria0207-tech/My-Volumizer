package com.example.myvolumizer

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier,    themeViewModel: ThemeViewModel ) {
    // Shared state between screens

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
            HomeScreen(viewModel = settingsViewModel)
        }

        composable("settings") {
            SettingsScreen(
                navController = navController,
               // onAudioSelected = { uri -> selectedAudioUri = uri  },
                viewModel = settingsViewModel
            )
        }

        composable("about") {
            AboutScreen(themeViewModel = themeViewModel) // ✅ Fixed name + passing
        }
    }
}
