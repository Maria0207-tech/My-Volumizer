package com.example.myvolumizer

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf

class VolumeObserver(
    context: Context,
    private val onVolumeChanged: (Int) -> Unit
) : ContentObserver(Handler()) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun startObserving() {
        appContext.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            this
        )
    }

    fun stopObserving() {
        appContext.contentResolver.unregisterContentObserver(this)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        val volume = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f /
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt()
        onVolumeChanged(volume)
    }
}
