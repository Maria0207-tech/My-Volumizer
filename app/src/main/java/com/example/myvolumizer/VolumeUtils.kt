package com.example.myvolumizer

import android.content.Context
import android.media.AudioManager

fun getDeviceVolumePercent(context: Context): Int {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    return (current * 100 / max.toFloat()).toInt()
}

fun setDeviceVolumePercent(context: Context, percent: Int) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val newVolume = (percent * max / 100f).toInt().coerceIn(0, max)
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
}
