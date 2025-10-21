package com.example.myvolumizer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel : ViewModel() {
    private val _theme = MutableStateFlow("System Default")
    val theme: StateFlow<String> = _theme

    fun setTheme(theme: String) {
        _theme.value = theme
    }
}

