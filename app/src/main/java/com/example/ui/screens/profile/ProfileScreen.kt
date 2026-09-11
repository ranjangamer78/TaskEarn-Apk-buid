package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    onNavigateTo: (String) -> Unit,
    user: User?
) {
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
            IconButton(onClick = { onNavigateTo("HOME") }) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Profile", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { onNavigateTo(com.example.ui.navigation.Routes.SETTINGS) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }

        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "profile")

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (user?.photoUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = user?.name?.takeIf { it.isNotBlank() } ?: "Guest User",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user?.email?.takeIf { it.isNotBlank() } ?: "guest@example.com",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Coins", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${user?.balance ?: 0}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.Star, contentDescription = "Coin", tint = AccentYellow, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B38)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Friends Invited", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("👥 ${user?.invitedCount ?: 0}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if ((user?.lastLogin ?: 0L) > 0L) {
            Spacer(modifier = Modifier.height(8.dp))
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
            Text(
                text = "Last Login: ${sdf.format(java.util.Date(user!!.lastLogin))}",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val menuItems = listOf(
            Pair(Icons.Filled.Person, "Personal Info"),
            Pair(Icons.Filled.AccountBalanceWallet, "Payment Methods"),
            Pair(Icons.Filled.History, "History"),
            Pair(Icons.Filled.GroupAdd, "Invite Friends"),
            Pair(Icons.Filled.Share, "Social Media"),
            Pair(Icons.Filled.Leaderboard, "Leaderboard"),
            Pair(Icons.Filled.AdminPanelSettings, "Admin Panel"),
            Pair(Icons.Filled.Help, "Help & Support"),
            Pair(Icons.Filled.Settings, "Settings")
        )

        menuItems.forEach { (icon, title) ->
            ProfileMenuItem(icon, title, onClick = {
                when (title) {
                    "Admin Panel" -> onNavigateTo(com.example.ui.navigation.Routes.ADMIN_PANEL)
                    "Social Media" -> onNavigateTo(com.example.ui.navigation.Routes.SOCIAL_MEDIA)
                    "Settings" -> onNavigateTo(com.example.ui.navigation.Routes.SETTINGS)
                    "Leaderboard" -> onNavigateTo(com.example.ui.navigation.Routes.LEADERBOARD)
                    "Invite Friends" -> onNavigateTo(com.example.ui.navigation.Routes.REFER_EARN)
                    "Help & Support" -> onNavigateTo(com.example.ui.navigation.Routes.CONTACT)
                    "Payment Methods" -> onNavigateTo(com.example.ui.navigation.Routes.WALLET)
                    "History" -> onNavigateTo(com.example.ui.navigation.Routes.HISTORY)
                }
            })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTo("LOGOUT") }
                .padding(vertical = 12.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = Color.Red)
            Spacer(modifier = Modifier.width(16.dp))
            Text("Logout", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        com.example.ui.components.BannerAdView()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = TextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = "Arrow", tint = TextSecondary)
    }
}
