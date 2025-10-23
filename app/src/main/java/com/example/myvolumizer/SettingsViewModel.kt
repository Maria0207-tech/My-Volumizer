package com.example.myvolumizer

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class AudioItem(val title: String, val uri: Uri)

class SettingsViewModel : ViewModel() {

    // 🎵 All device audio files
    var audioList = mutableStateListOf<AudioItem>()
        private set

    // ▶️ Media control states
    var selectedAudioUri = mutableStateOf<Uri?>(null)   // for playback
        private set
    var isPlaying = mutableStateOf(false)
        private set
    var mediaPlayer: MediaPlayer? = null
        private set

    // 🌟 Boosted audio (selected via long press)
    var boostedAudioUri = mutableStateOf<Uri?>(null)
        private set

    // 📂 Load all device audio files
    fun loadAudioFiles(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<AudioItem>()
            val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID)
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null
            )

            cursor?.use {
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                while (it.moveToNext()) {
                    val title = it.getString(titleCol)
                    val uri = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        it.getLong(idCol).toString()
                    )
                    list.add(AudioItem(title, uri))
                }
            }

            audioList.clear()
            audioList.addAll(list)
        }
    }

    // ▶️ Play selected audio
    fun playAudio(context: Context, uri: Uri) {
        stopAudio()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri)
            prepare()
            start()
        }
        selectedAudioUri.value = uri
        isPlaying.value = true
    }

    // ⏹ Stop playback
    fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying.value = false
        selectedAudioUri.value = null
    }

    // 🌟 Long press handler → Select audio for boost
    fun selectBoostedAudio(uri: Uri) {
        boostedAudioUri.value = uri
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
