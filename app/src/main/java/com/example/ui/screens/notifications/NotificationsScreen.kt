package com.example.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.example.data.model.Notification
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class NotificationsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    var notifications by androidx.compose.runtime.mutableStateOf<List<Notification>>(emptyList())
        private set
            
    init {
        loadNotifications()
    }
        
    private fun loadNotifications() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        viewModelScope.launch {
            try {
                val snapshot = db.collection("notifications")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                notifications = snapshot.documents.mapNotNull { 
                    val notif = it.toObject(Notification::class.java)
                    if (notif != null) {
                        notif.copy(id = it.id)
                    } else null
                }.filter { 
                    it.userId.isNullOrEmpty() || it.userId.equals("all", ignoreCase = true) || it.userId == currentUid
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearNotifications(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("cleared_until", System.currentTimeMillis()).apply()
        notifications = emptyList()
    }
}
@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationsViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val clearedUntil = prefs.getLong("cleared_until", 0L)
    
    val visibleNotifications = viewModel.notifications.filter { it.timestamp > clearedUntil }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Notifications", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "notifications")

        Spacer(modifier = Modifier.height(16.dp))

        val dateFormat = androidx.compose.runtime.remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

        if (visibleNotifications.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No notifications yet.", color = com.example.ui.theme.TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(visibleNotifications.size) { index ->
                    val item = visibleNotifications[index]
                    val icon = when (item.icon) {
                        "card" -> Icons.Filled.CardGiftcard
                        "play" -> Icons.Filled.PlayArrow
                        "star" -> Icons.Filled.Star
                        "people" -> Icons.Filled.People
                        "bank" -> Icons.Filled.AccountBalance
                        else -> Icons.Filled.Notifications
                    }
                    val iconTint = com.example.ui.theme.AccentYellow
                    
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(iconTint.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = item.title, tint = iconTint)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(item.message, color = com.example.ui.theme.TextSecondary, fontSize = 12.sp)
                            }
                            Text(dateFormat.format(Date(item.timestamp)), color = com.example.ui.theme.TextSecondary, fontSize = 12.sp)
                        }
                        if (!item.imageUrl.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.clearNotifications(context) },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple, contentColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Clear All", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        com.example.ui.components.BannerAdView()
        Spacer(modifier = Modifier.height(8.dp))
    }
}
