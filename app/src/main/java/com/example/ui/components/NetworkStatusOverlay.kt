package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.VpnKeyOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.NetworkUtil
import kotlinx.coroutines.delay

@Composable
fun NetworkStatusOverlay(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(true) }
    var isVpn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isConnected = NetworkUtil.isConnected(context)
            isVpn = NetworkUtil.isVpnConnected(context)
            delay(2000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (!isConnected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SignalWifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Internet is off", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Please turn on internet.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else if (isVpn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.VpnKeyOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Please turn off VPN", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("VPN usage is not allowed.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}
