package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

import androidx.compose.ui.platform.LocalContext
import com.example.MyApplication

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val preferencesManager = (context.applicationContext as MyApplication).preferencesManager
    
    val pushNotif by preferencesManager.pushNotifications.collectAsState()
    var sound by remember { mutableStateOf(true) }
    var vibration by remember { mutableStateOf(true) }
    
    var config by remember { mutableStateOf(com.example.data.repository.AppConfig()) }
    LaunchedEffect(Unit) {
        config = com.example.data.repository.ConfigRepository().getConfig()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "settings")

        Spacer(modifier = Modifier.height(16.dp))
        
        Text("General", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingToggleRow(Icons.Filled.Notifications, "Push Notifications", pushNotif) { preferencesManager.setPushNotifications(it) }
        SettingToggleRow(Icons.Filled.VolumeUp, "Sound", sound) { sound = it }
        SettingToggleRow(Icons.Filled.Vibration, "Vibration", vibration) { vibration = it }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Account & Administration", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingNavRow(Icons.Filled.AdminPanelSettings, "Admin Panel", "Manage App") {
            onNavigateTo(com.example.ui.navigation.Routes.ADMIN_PANEL)
        }
        SettingNavRow(Icons.Filled.Language, "Language", "English") {}
        SettingNavRow(Icons.Filled.PrivacyTip, "Privacy Policy", "") {
            if (config.privacy_policy.isNotEmpty()) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(config.privacy_policy))
                context.startActivity(intent)
            }
        }
        SettingNavRow(Icons.Filled.Description, "Terms & Conditions", "") {
            if (config.terms.isNotEmpty()) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(config.terms))
                context.startActivity(intent)
            }
        }
        SettingNavRow(Icons.Filled.Info, "About Us", "Version ${com.example.BuildConfig.VERSION_NAME}") {}
        
        Spacer(modifier = Modifier.height(16.dp))
        com.example.ui.components.BannerAdView()
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingToggleRow(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = TextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryPurple)
        )
    }
}

@Composable
fun SettingNavRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = TextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(value, color = TextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (title != "About Us") {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Navigate", tint = TextSecondary)
        }
    }
}
