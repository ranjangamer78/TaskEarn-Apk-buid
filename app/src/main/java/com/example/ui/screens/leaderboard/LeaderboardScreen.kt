package com.example.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.ui.components.BannerAdView
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LeaderboardScreen(
    onBack: (() -> Unit)? = null,
    viewModel: LeaderboardViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isMyNameHidden by viewModel.isMyNameHidden.collectAsState()
    val currentUserId = viewModel.currentUserId
    
    val format = NumberFormat.getNumberInstance(Locale.US)

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
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Leaderboard", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = if (onBack == null) 8.dp else 0.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                color = AccentYellow.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentYellow.copy(alpha = 0.4f))
            ) {
                Text(
                    "TOP 10 ONLY",
                    color = AccentYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "leaderboard")

        Spacer(modifier = Modifier.height(14.dp))

        // Hide Name Privacy Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1930)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isMyNameHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Privacy Icon",
                        tint = if (isMyNameHidden) SecondaryTeal else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isMyNameHidden) "Name Hidden (Anonymous)" else "Hide Name on Leaderboard",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMyNameHidden) "Others see you as 'Anonymous Player'" else "Keep your identity private from other users",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Switch(
                    checked = isMyNameHidden,
                    onCheckedChange = { viewModel.toggleHideName(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = SecondaryTeal,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF2A2845)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else if (users.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No users found", color = TextSecondary)
            }
        } else {
            val top10Users = users.take(10)
            // Podium
            if (top10Users.size >= 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val top3 = top10Users.take(3)
                    // Rank 2
                    if (top3.size > 1) {
                        PodiumItem(rank = 2, user = top3[1], isSelf = top3[1].uid == currentUserId, size = 64.dp, color = Color.LightGray, format = format)
                    }
                    // Rank 1
                    if (top3.isNotEmpty()) {
                        PodiumItem(rank = 1, user = top3[0], isSelf = top3[0].uid == currentUserId, size = 80.dp, color = AccentYellow, format = format)
                    }
                    // Rank 3
                    if (top3.size > 2) {
                        PodiumItem(rank = 3, user = top3[2], isSelf = top3[2].uid == currentUserId, size = 64.dp, color = Color(0xFFCD7F32), format = format) // Bronze
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            val listRanks = if (top10Users.size > 3) top10Users.drop(3).take(7) else emptyList()
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(listRanks) { index, user ->
                    val isSelf = user.uid == currentUserId
                    val displayName = when {
                        user.hideNameOnLeaderboard && isSelf -> "${user.name.ifBlank { "You" }} (Hidden)"
                        user.hideNameOnLeaderboard -> "Anonymous Player"
                        else -> user.name.ifBlank { "Guest" }
                    }
                    val isMasked = user.hideNameOnLeaderboard && !isSelf

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelf) Color(0xFF1E1B38) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text((index + 4).toString(), color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        if (!isMasked && user.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isMasked) Color(0xFF33304E) else PrimaryPurple), contentAlignment = Alignment.Center) {
                                Text(
                                    if (isMasked) "?" else (displayName.firstOrNull()?.toString()?.uppercase() ?: "U"),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, color = if (isSelf) SecondaryTeal else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            if (isSelf) {
                                Text("Your ranking", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text(format.format(user.balance), color = AccentYellow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadLeaderboard() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Refresh Leaderboard \uD83D\uDD04", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Unity Banner Ad on Leaderboard Page
        BannerAdView()
    }
}

@Composable
fun PodiumItem(rank: Int, user: User, isSelf: Boolean, size: androidx.compose.ui.unit.Dp, color: Color, format: NumberFormat) {
    val displayName = when {
        user.hideNameOnLeaderboard && isSelf -> "${user.name.ifBlank { "You" }} (Hidden)"
        user.hideNameOnLeaderboard -> "Anonymous"
        else -> user.name.ifBlank { "Guest" }
    }
    val isMasked = user.hideNameOnLeaderboard && !isSelf

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!isMasked && user.photoUrl.isNotEmpty()) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.size(size).clip(CircleShape).background(if (isMasked) Color(0xFF33304E) else PrimaryPurple), contentAlignment = Alignment.Center) {
                Text(
                    if (isMasked) "?" else (displayName.firstOrNull()?.toString()?.uppercase() ?: "U"),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(displayName.take(10), color = if (isSelf) SecondaryTeal else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(format.format(user.balance), color = AccentYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(rank.toString(), color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}
