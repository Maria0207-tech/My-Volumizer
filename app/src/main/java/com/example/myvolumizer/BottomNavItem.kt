package com.example.myvolumizer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController


data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)