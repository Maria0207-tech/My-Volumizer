package com.example.myvolumizer

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlin.random.Random

// 🎧 Utility functions
fun isHeadphoneConnected(context: Context): Boolean {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
}

fun applyDeviceVolumeBoost(context: Context, currentVolume: Int, boostFactor: Float) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val boosted = (currentVolume / 100f * maxVolume * boostFactor)
        .toInt()
        .coerceAtMost(maxVolume)
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, boosted, 0)
}

// 🎛 Home Screen UI
@Composable
fun HomeScreen(selectedAudioUri: Uri?) {
    val context = LocalContext.current
    var volume by remember { mutableIntStateOf(getDeviceVolumePercent(context)) }
    var boostFactor by remember { mutableFloatStateOf(1.0f) }
    var boostedVolume by remember { mutableIntStateOf(volume) }
    var headphonesConnected by remember { mutableStateOf(isHeadphoneConnected(context)) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val iconColor by animateColorAsState(
        targetValue = if (headphonesConnected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    )

    // 🔄 Headphone connection observer
    LaunchedEffect(Unit) {
        while (isActive) {
            val connected = isHeadphoneConnected(context)
            if (connected != headphonesConnected) headphonesConnected = connected
            delay(1000)
        }
    }

    // 🔊 Volume observer
    DisposableEffect(Unit) {
        val observer = VolumeObserver(context) { newVolume ->
            volume = newVolume
            boostedVolume = (newVolume * boostFactor).coerceAtMost(100f).toInt()
        }
        observer.startObserving()
        onDispose {
            observer.stopObserving()
            mediaPlayer?.release()
        }
    }

    // 🎵 Media Player setup
    LaunchedEffect(selectedAudioUri) {
        mediaPlayer?.release()
        selectedAudioUri?.let { uri ->
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                isLooping = true
                prepare()
            }
        }
    }

    // 🧱 Main Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🏷 Title & Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MY VOLUMIZER",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.Headset,
                contentDescription = "Headphones",
                tint = iconColor,
                modifier = Modifier.size(if (headphonesConnected) 38.dp else 32.dp)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 🎶 Sound Pillars + Knob Layout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 🔊 Left Pillar
            SoundPillar(isPlaying = isPlaying, volume = volume, mirror = false)
            Spacer(modifier = Modifier.width(8.dp))

            // 🎛 Main Knob
            RealisticVisualizerKnob(
                volume = volume,
                isPlaying = isPlaying,
                onVolumeChange = { newVol ->
                    volume = newVol.coerceIn(0, 100)
                    boostedVolume = (newVol * boostFactor).coerceAtMost(100f).toInt()
                    setDeviceVolumePercent(context, volume)
                    applyDeviceVolumeBoost(context, volume, boostFactor)
                    mediaPlayer?.setVolume(
                        (volume / 100f * boostFactor).coerceAtMost(1f),
                        (volume / 100f * boostFactor).coerceAtMost(1f)
                    )
                }
            )

            Spacer(modifier = Modifier.width(8.dp))
            // 🔊 Right Pillar
            SoundPillar(isPlaying = isPlaying, volume = volume, mirror = true)
        }

        Spacer(modifier = Modifier.height(28.dp))
        Text("Volume: $volume%", fontSize = 18.sp)
        Text("Boosted: $boostedVolume%", fontSize = 18.sp)

        Spacer(modifier = Modifier.height(20.dp))
        Text("Boost: ${"%.1f".format(boostFactor)}x", fontSize = 18.sp)
        Slider(
            value = boostFactor,
            onValueChange = {
                boostFactor = it
                boostedVolume = (volume * boostFactor).coerceAtMost(100f).toInt()
                applyDeviceVolumeBoost(context, volume, boostFactor)
            },
            valueRange = 1f..2f,
            steps = 9,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                boostedVolume = (volume * boostFactor).coerceAtMost(100f).toInt()
                applyDeviceVolumeBoost(context, volume, boostFactor)
            },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Boost Now", color = Color.White, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 🎶 Audio Card
        selectedAudioUri?.let { uri ->
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Selected Audio:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(uri.lastPathSegment ?: "Unknown File")
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            mediaPlayer?.apply {
                                start()
                                isPlaying = true
                            }
                        }) { Text("Play") }

                        Button(onClick = {
                            mediaPlayer?.pause()
                            isPlaying = false
                        }) { Text("Pause") }
                    }
                }
            }
        } ?: Text("No audio selected")
    }
}

// 🌈 Multi-Ring Visualizer Knob
@Composable
fun RealisticVisualizerKnob(volume: Int, isPlaying: Boolean, onVolumeChange: (Int) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowPulse by infiniteTransition.animateFloat(
        0.8f, 1.2f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val rotation by infiniteTransition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing))
    )

    var angle by remember { mutableFloatStateOf((volume / 100f) * 270f - 135f) }
    val neonGradient = Brush.sweepGradient(
        listOf(Color(0xFF00E5FF), Color(0xFF651FFF), Color(0xFFFF4081), Color(0xFFFFD740), Color(0xFF00E5FF))
    )

    Box(
        modifier = Modifier
            .size(280.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    val newAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    val clamped = newAngle.coerceIn(-135f, 135f)
                    angle = clamped
                    val newVolume = (((clamped + 135f) / 270f) * 100).toInt()
                    onVolumeChange(newVolume)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2.8f

            val ringCount = 6
            repeat(ringCount) { i ->
                val fraction = i / ringCount.toFloat()
                val ringBrush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF00E5FF).copy(alpha = 1f - fraction / 1.5f),
                        Color(0xFFFF4081).copy(alpha = 1f - fraction / 1.8f),
                        Color(0xFFFFD740).copy(alpha = 1f - fraction / 2f),
                        Color(0xFF00E5FF).copy(alpha = 1f - fraction / 1.5f)
                    )
                )
                val ringRadius = baseRadius * (0.7f + fraction * 0.4f * glowPulse)
                drawCircle(brush = ringBrush, radius = ringRadius, center = center, style = Stroke(width = 5f))
            }

            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF212121), Color(0xFF616161)), center, baseRadius * 0.9f),
                radius = baseRadius * 0.9f,
                center = center
            )
            drawCircle(Color.Black.copy(alpha = 0.9f), radius = baseRadius * 0.6f, center = center)

            val rad = Math.toRadians(angle.toDouble())
            val end = Offset(center.x + cos(rad).toFloat() * baseRadius * 0.75f, center.y + sin(rad).toFloat() * baseRadius * 0.75f)
            drawLine(Color.Cyan, center, end, 6f, cap = StrokeCap.Round)

            if (isPlaying) {
                val rayCount = 40
                repeat(rayCount) { i ->
                    val rayAngle = (i * (360f / rayCount) + rotation)
                    val radian = Math.toRadians(rayAngle.toDouble())
                    val length = 30 + (volume / 100f) * 50
                    val start = Offset(center.x + baseRadius * cos(radian).toFloat(), center.y + baseRadius * sin(radian).toFloat())
                    val end = Offset(center.x + (baseRadius + length) * cos(radian).toFloat(), center.y + (baseRadius + length) * sin(radian).toFloat())
                    drawLine(brush = neonGradient, start = start, end = end, strokeWidth = 2f, alpha = 0.5f)
                }
            }
        }
        Text("$volume%", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

// 🎵 Sound Pillars (Dynamic Audio Bars)
@Composable
fun SoundPillar(isPlaying: Boolean, volume: Int, mirror: Boolean) {
    val barCount = 20
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        0.8f, 1.2f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse)
    )

    val gradient = Brush.verticalGradient(
        listOf(Color.Cyan, Color.Magenta, Color.Yellow)
    )

    Canvas(modifier = Modifier.size(width = 24.dp, height = 250.dp)) {
        val barWidth = size.width / 1.5f
        val spacing = size.height / barCount

        for (i in 0 until barCount) {
            val intensity = if (isPlaying) Random.nextFloat() * (volume / 100f) else 0.1f
            val barHeight = spacing * intensity * pulse
            val y = if (mirror) size.height - (i * spacing) else i * spacing
            drawLine(
                brush = gradient,
                start = Offset(size.width / 2, y - barHeight / 2),
                end = Offset(size.width / 2, y + barHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
                alpha = 0.9f
            )
        }
    }
}
