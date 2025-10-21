package com.example.myvolumizer

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    var selectedAudioUri by mutableStateOf<Uri?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        internal set

    var mediaPlayer: MediaPlayer? = null

    fun setAudio(uri: Uri) {
        selectedAudioUri = uri
    }

    fun clearAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        selectedAudioUri = null
        isPlaying = false
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }
}
