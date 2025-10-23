package com.example.myvolumizer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current

    // 🔐 Permission Request Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadAudioFiles(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Audio Library",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 🎵 Fetch All Audio Button
        Button(
            onClick = {
                val permission = if (android.os.Build.VERSION.SDK_INT >= 33)
                    Manifest.permission.READ_MEDIA_AUDIO
                else
                    Manifest.permission.READ_EXTERNAL_STORAGE

                when {
                    ContextCompat.checkSelfPermission(context, permission) ==
                            PackageManager.PERMISSION_GRANTED -> {
                        viewModel.loadAudioFiles(context)
                    }
                    else -> permissionLauncher.launch(permission)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)
        ) {
            Text("Fetch Audios", fontSize = 18.sp, color = colorScheme.onPrimary)
        }

        Spacer(Modifier.height(20.dp))

        // 🎧 Show Audio Files
        if (viewModel.audioList.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.audioList) { audio ->
                    val isPlaying = viewModel.isPlaying.value &&
                            viewModel.selectedAudioUri.value == audio.uri

                    AudioCard(
                        audio = audio,
                        isPlaying = isPlaying,
                        isSelected = viewModel.boostedAudioUri.value == audio.uri, // ✅ highlight selected one
                        onPlay = { viewModel.playAudio(context, audio.uri) },
                        onStop = { viewModel.stopAudio() },
                        onSelect = { viewModel.selectBoostedAudio(audio.uri) }     // ✅ handle long press
                    )

                }
            }
        } else {
            Text(
                text = "No audio files available.",
                color = colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}

@Composable
fun AudioCard(
    audio: AudioItem,
    isPlaying: Boolean,
    isSelected: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onSelect: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(
        listOf(
            colorScheme.primary.copy(alpha = 0.85f),
            colorScheme.secondary.copy(alpha = 0.85f)
        )
    )

    // 🌈 Add a subtle border glow when selected
    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            brush = gradient,
            shape = RoundedCornerShape(18.dp)
        )
    } else Modifier

    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceColorAtElevation(2.dp)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .then(borderModifier)
            // 👇 long press selects audio as "boosted"
            .combinedClickable(
                onClick = {
                    if (isPlaying) onStop() else onPlay()
                },
                onLongClick = {
                    onSelect()
                }
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 🎵 Left: Icon + Texts
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                brush = if (isPlaying) gradient else
                                    Brush.linearGradient(
                                        listOf(
                                            colorScheme.surfaceVariant,
                                            colorScheme.surface
                                        )
                                    ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isPlaying)
                                colorScheme.onPrimary
                            else
                                colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = audio.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = when {
                                isPlaying -> "Playing..."
                                isSelected -> "Boosted"
                                else -> "Stopped"
                            },
                            fontSize = 13.sp,
                            color = when {
                                isPlaying -> colorScheme.primary
                                isSelected -> colorScheme.secondary
                                else -> colorScheme.outline
                            }
                        )
                    }
                }

                // 🎛️ Right: Play / Stop Button
                IconButton(
                    onClick = if (isPlaying) onStop else onPlay,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isPlaying)
                                colorScheme.errorContainer.copy(alpha = 0.7f)
                            else
                                colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        tint = if (isPlaying)
                            colorScheme.onErrorContainer
                        else
                            colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 🎚️ Progress / Glow Line
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth()
                    .drawBehind {
                        drawRoundRect(
                            brush = if (isPlaying || isSelected) gradient else
                                Brush.linearGradient(
                                    listOf(
                                        colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        colorScheme.outlineVariant.copy(alpha = 0.1f)
                                    )
                                ),
                            cornerRadius = CornerRadius(50f, 50f)
                        )
                    }
            )
        }
    }
}





