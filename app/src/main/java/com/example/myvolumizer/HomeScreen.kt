package com.example.myvolumizer

import android.content.Context
import android.media.AudioManager
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
fun HomeScreen(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current

    // observe shared state from ViewModel
    val boostedAudioUri by remember { derivedStateOf { viewModel.boostedAudioUri.value } }
    val selectedAudioUri by remember { derivedStateOf { viewModel.selectedAudioUri.value } }

    val isPlaying by remember { derivedStateOf { viewModel.isPlaying.value } }
    var volume by remember { mutableIntStateOf(getDeviceVolumePercent(context)) }
    var boostFactor by remember { mutableFloatStateOf(1.0f) }
    var boostedVolume by remember { mutableIntStateOf((volume * boostFactor).coerceAtMost(100f).toInt()) }
    var headphonesConnected by remember { mutableStateOf(isHeadphoneConnected(context)) }

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
        onDispose { observer.stopObserving() }
    }

    //  Main Layout
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
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Headset,
                    contentDescription = "Headphones",
                    tint = iconColor,
                    modifier = Modifier.size(if (headphonesConnected) 38.dp else 32.dp)
                )

                if (!headphonesConnected) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val strokeWidth = size.minDimension * 0.08f
                        drawLine(
                            color = Color.Red,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
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

                    // If playback is handled in ViewModel, also update player's volume
                    viewModel.mediaPlayer?.setVolume(
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
        Text("Select Boost Level", fontSize = 18.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.height(20.dp))

        Text("Boost Level", fontSize = 18.sp, fontWeight = FontWeight.Medium)

        var boostFactor by remember { mutableFloatStateOf(1.0f) }

        Slider(
            value = boostFactor,
            onValueChange = { newBoost ->
                boostFactor = newBoost.coerceIn(1f, 2f)
                applyDeviceVolumeBoost(context, volume, boostFactor)
                viewModel.mediaPlayer?.setVolume(
                    (volume / 100f * boostFactor).coerceAtMost(1f),
                    (volume / 100f * boostFactor).coerceAtMost(1f)
                )
            },
            valueRange = 1f..2f,
            steps = 4
        )


        Text(
            text = "Boost: ${(boostFactor * 100).toInt()}%",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

// 🌟 Animated Boost Now Button with Glow + Sparks
        var isBoosting by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(1000, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                )
            )

            // 🔆 Outer glow pulse behind button
            if (isBoosting) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2.5f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.3f * glowAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * glowAlpha * 1.5f
                        )
                    )

                    // ✨ Sparks
                    repeat(15) {
                        val x = Random.nextFloat() * size.width
                        val y = Random.nextFloat() * size.height
                        val sizeSpark = Random.nextFloat() * 8f + 2f
                        drawCircle(
                            color = Color(0xFF00FFFF).copy(alpha = Random.nextFloat() * 0.7f),
                            radius = sizeSpark,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            var isBoosting by remember { mutableStateOf(false) }

            LaunchedEffect(isBoosting) {
                if (isBoosting) {
                    // Apply current boostFactor directly
                    boostedVolume = (volume * boostFactor).coerceAtMost(100f).toInt()
                    applyDeviceVolumeBoost(context, volume, boostFactor)

                    // Optional: update media player
                    viewModel.mediaPlayer?.setVolume(
                        (volume / 100f * boostFactor).coerceAtMost(1f),
                        (volume / 100f * boostFactor).coerceAtMost(1f)
                    )

                    // Glow animation or temporary button feedback
                    delay(1200)
                    isBoosting = false
                }
            }


            Button(
                onClick = { isBoosting = true },

                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp)
                    .graphicsLayer {
                        if (isBoosting) {
                            scaleX = 1.1f
                            scaleY = 1.1f
                            shadowElevation = 30f
                        }
                    },


                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBoosting)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isBoosting) "Boosting..." else "Boost Now",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }

        }

        Spacer(modifier = Modifier.height(30.dp))

        // 🎶 Boosted Audio Card: show boostedAudioUri (selected via long-press in SettingsScreen)
        boostedAudioUri?.let { uri ->
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Boosted Audio:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(uri.lastPathSegment ?: "Unknown File", maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            // Use ViewModel to play boosted audio (keeps single place for playback)
                            viewModel.playAudio(context, uri)
                        }) { Text("Play") }

                        Button(onClick = {
                            viewModel.stopAudio()
                        }) { Text("Stop") }
                    }
                }
            }
        } ?: Text("No boosted audio selected", modifier = Modifier.padding(top = 8.dp))
    }
}

// Keep RealisticVisualizerKnob and SoundPillar unchanged; included below for completeness.
// (I left their implementations identical to your previous code but adapted the imports above.)

// 🌈 Multi-Ring Visualizer Knob
@Composable
fun RealisticVisualizerKnob(volume: Int, isPlaying: Boolean, onVolumeChange: (Int) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val context = LocalContext.current
    var angled by remember { mutableFloatStateOf(0f) }  // safe default, e.g., 0
    //var volume by remember { mutableIntStateOf(getDeviceVolumePercent(context)) }
    var boostFactor by remember { mutableFloatStateOf(1.0f) }
    var boostedVolume by remember { mutableIntStateOf((volume * boostFactor).coerceAtMost(100f).toInt()) }
    // 🔄 Sync angle with boostedVolume
    LaunchedEffect(boostedVolume) {
        angled = (boostedVolume / 100f) * 270f - 135f
    }
    val glowPulse by infiniteTransition.animateFloat(
        0.8f, 1.2f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val rotation by infiniteTransition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing))
    )

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
                    angled = clamped  // ✅ now resolved
                    val newVolume = (((clamped + 135f) / 270f) * 100).toInt()
                    onVolumeChange(newVolume)
                }
            },
        contentAlignment = Alignment.Center
    )
    {
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

            // Outer metallic ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF555555), Color(0xFF222222)),
                    center = center,
                    radius = baseRadius * 0.72f
                ),
                radius = baseRadius * 0.72f,
                center = center,
                style = Stroke(width = 12f) // thicker ring
            )

            // Inner knob circle (metallic gray)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFAAAAAA), Color(0xFF444444)),
                    center = center,
                    radius = baseRadius * 0.7f
                ),
                radius = baseRadius * 0.7f,
                center = center
            )

            val rad = Math.toRadians(angled.toDouble())
            val start = Offset(
                center.x + cos(rad).toFloat() * baseRadius * 0.5f,
                center.y + sin(rad).toFloat() * baseRadius * 0.5f
            )
            val end = Offset(
                center.x + cos(rad).toFloat() * baseRadius * 0.65f,
                center.y + sin(rad).toFloat() * baseRadius * 0.65f
            )

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00FFFF),  // bright cyan tip
                        Color(0xFF007F9E)   // darker cyan base
                    ),
                    start = start,
                    end = end
                ),
                start = start,
                end = end,
                strokeWidth = 8f,
                cap = StrokeCap.Round,
                alpha = 0.95f
            )

            // Optional glow overlay (soft aura around needle)
            drawLine(
                color = Color(0x8000FFFF),
                start = start,
                end = end,
                strokeWidth = 16f,
                cap = StrokeCap.Round,
                alpha = 0.3f
            )

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

        Text(
            text = "$volume%",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp,
            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF80FFFF)
                    )
                ),
                shadow = Shadow(
                    color = Color(0x8000E5FF),
                    offset = Offset(2f, 2f),
                    blurRadius = 8f
                )
            ),
            modifier = Modifier
                .padding(top = 8.dp)
                .graphicsLayer {
                    scaleX = 1.05f
                    scaleY = 1.05f
                }
        )
    }
}

// 🎵 Sound Pillars (Dynamic Audio Bars)
@Composable
fun SoundPillar(isPlaying: Boolean, volume: Int, mirror: Boolean) {
    val barCount = 20
    val infiniteTransition = rememberInfiniteTransition(label = "soundPulse")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnim"
    )

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF00E5FF),
            Color(0xFF651FFF),
            Color(0xFFFF4081),
            Color(0xFFFFD740),
            Color(0xFF00E5FF)
        )
    )

    Canvas(
        modifier = Modifier
            .width(28.dp)
            .height(260.dp)
            .graphicsLayer {
                alpha = 0.98f
                shadowElevation = 12f
                shape = RoundedCornerShape(50)
                clip = true
            }
    ) {
        val barWidth = size.width / 1.6f
        val spacing = size.height / barCount

        for (i in 0 until barCount) {
            val intensity = if (isPlaying) Random.nextFloat() * (volume / 100f) else 0.08f
            val barHeight = spacing * intensity * pulse

            val y = if (mirror) size.height - (i * spacing) else i * spacing
            val x = size.width / 2

            // Outer glow (blurred)
            drawLine(
                color = Color(0x8000FFFF),
                start = Offset(x, y - barHeight / 2),
                end = Offset(x, y + barHeight / 2),
                strokeWidth = barWidth * 1.6f,
                cap = StrokeCap.Round,
                alpha = 0.3f
            )

            // Main bar with gradient
            drawLine(
                brush = gradient,
                start = Offset(x, y - barHeight / 2),
                end = Offset(x, y + barHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
                alpha = 0.95f
            )

            // Highlight at center of each bar
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = barWidth / 2.5f,
                center = Offset(x, y)
            )
        }
    }
}
