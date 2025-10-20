package com.example.myvolumizer

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myvolumizer.VolumeKnob

// Helper to check headphones
fun isHeadphoneConnected(context: Context): Boolean {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
    } else {
        audioManager.isWiredHeadsetOn
    }
}

// Optional: BroadcastReceiver to observe headset plug/unplug
class HeadsetBroadcastReceiver(private val onChange: (Boolean) -> Unit) : androidx.lifecycle.LifecycleObserver {
    fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra("state", 0)
        onChange(state == 1)
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // Volume state (0-100)
    var volume by remember { mutableIntStateOf(getDeviceVolumePercent(context)) }

    // Observe device volume changes
    DisposableEffect(Unit) {
        val observer = VolumeObserver(context) { newVolume ->
            volume = newVolume
        }
        observer.startObserving()
        onDispose { observer.stopObserving() }
    }

    // Headphone state
    var headphonesConnected by remember { mutableStateOf(isHeadphoneConnected(context)) }

    // Animated icon color
    val iconColor by animateColorAsState(
        targetValue = if (headphonesConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Title + Headphone Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "MY VOLUMIZER",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Default.Headset,
                contentDescription = "Headphone Connected",
                tint = iconColor,
                modifier = Modifier.size(if (headphonesConnected) 40.dp else 32.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Current Volume Display
        Text(
            text = "Current Volume: $volume%",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Volume knob
        VolumeKnob(
            volume = volume,
            onVolumeChange = { newVolume ->
                volume = newVolume
                setDeviceVolumePercent(context, newVolume)
            },
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Back / Digital / Front row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            TextButton(onClick = {
                if (volume > 0) {
                    val newVolume = volume - 1
                    volume = newVolume
                    setDeviceVolumePercent(context, newVolume)
                }
            }) {
                Text("<", fontSize = 32.sp)
            }

            // Digital volume
            Text(
                text = "$volume",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // Front button
            TextButton(onClick = {
                if (volume < 100) {
                    val newVolume = volume + 1
                    volume = newVolume
                    setDeviceVolumePercent(context, newVolume)
                }
            }) {
                Text(">", fontSize = 32.sp)
            }
        }
    }
}
