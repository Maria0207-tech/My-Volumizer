package com.example.myvolumizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myvolumizer.ui.theme.MyVolumizerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle back button properly
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish() // Default behavior
            }
        })

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val currentTheme by themeViewModel.theme.collectAsState()

            // 🌗 Dynamically determine theme
            val darkTheme = when (currentTheme) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme() // "System Default"
            }

            // 🎨 Apply dynamic theme to entire app
            MyVolumizerTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val currentBackStackEntry = navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry.value?.destination?.route

                Scaffold(
                    bottomBar = {
                        // ✅ Hide bottom bar on splash screen
                        if (currentRoute != "splash") {
                            MyBottomBar(navController)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background // smooth background transition
                ) { paddingValues ->
                    // ✅ Pass ThemeViewModel into NavGraph so screens like About can access it
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues),
                        themeViewModel = themeViewModel
                    )
                }
            }
        }


    }
}

