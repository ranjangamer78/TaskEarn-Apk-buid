package com.example.ui.screens.earn.watchearn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserRepository
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.example.util.AdManager
import kotlinx.coroutines.launch

import com.example.data.repository.ConfigRepository
import com.example.data.repository.AppConfig
import com.example.data.model.Transaction

import com.example.ui.components.CooldownCard

class WatchEarnViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val configRepository = ConfigRepository()
    
    var videosWatched by mutableStateOf(0)
        private set
        
    var isWatchingAd by mutableStateOf(false)
        private set
        
    var rewardDialog by mutableStateOf(false)
        private set
        
    var rewardAmount by mutableStateOf(20)
        private set
    
    var videoLimit by mutableStateOf(10)
        private set

    var videoCooldownMinutes by mutableStateOf(5)
        private set

    var lastVideoWatchedAt by mutableLongStateOf(0L)
        private set
        
    init {
        loadUser()
        loadConfig()
    }
    
    private fun loadConfig() {
        viewModelScope.launch {
            configRepository.getConfigFlow().collect { config ->
                rewardAmount = if (config.video > 0) config.video else 20
                videoLimit = if (config.video_limit > 0) config.video_limit else 10
                videoCooldownMinutes = if (config.video_cooldown_minutes >= 0) config.video_cooldown_minutes else 5
            }
        }
    }
    
    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUser(uid).onSuccess { user ->
                val calCurrent = java.util.Calendar.getInstance()
                calCurrent.timeInMillis = System.currentTimeMillis()
                
                val calLast = java.util.Calendar.getInstance()
                calLast.timeInMillis = user.lastVideoWatchedAt
                
                val isSameDay = calCurrent.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR) &&
                                calCurrent.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR)
                
                if (!isSameDay) {
                    userRepository.resetVideoLimit(uid)
                    videosWatched = 0
                } else {
                    videosWatched = user.videosWatchedToday
                }
                lastVideoWatchedAt = user.lastVideoWatchedAt
            }
        }
    }
        
    fun watchAd(onShowAd: () -> Unit) {
        if (videosWatched >= videoLimit || isWatchingAd) return
        isWatchingAd = true
        onShowAd()
    }
    
    fun onAdWatched() {
        lastVideoWatchedAt = System.currentTimeMillis()
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                userRepository.updateUserBalance(uid, rewardAmount)
                userRepository.updateVideoWatched(uid, 1)
                userRepository.addTransaction(Transaction(userId = uid, title = "Watch Video Reward", amount = rewardAmount, type = "credit", icon = "play"))
                videosWatched++
                rewardDialog = true
            }
            isWatchingAd = false
        }
    }
    
    fun onAdFailed() {
        isWatchingAd = false
    }

    fun skipCooldown() {
        lastVideoWatchedAt = 0L
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.resetVideoCooldown(uid)
        }
    }
    
    fun dismissDialog() {
        rewardDialog = false
    }
}

@Composable
fun WatchEarnScreen(onBack: () -> Unit, viewModel: WatchEarnViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = remember(context) {
        var c: android.content.Context? = context
        while (c is android.content.ContextWrapper) {
            if (c is Activity) return@remember c
            c = c.baseContext
        }
        null
    }
    var isSkipAdLoading by remember { mutableStateOf(false) }
    var remainingCooldownSeconds by remember { mutableLongStateOf(0L) }

    // Cooldown countdown loop
    LaunchedEffect(viewModel.lastVideoWatchedAt, viewModel.videoCooldownMinutes) {
        while (true) {
            val cooldownMs = viewModel.videoCooldownMinutes * 60 * 1000L
            val elapsed = System.currentTimeMillis() - viewModel.lastVideoWatchedAt
            val diff = cooldownMs - elapsed
            remainingCooldownSeconds = if (diff > 0) (diff + 999) / 1000 else 0L
            kotlinx.coroutines.delay(1000L)
        }
    }

    val isCooldownActive = remainingCooldownSeconds > 0L

    fun skipCooldownWithAd() {
        if (activity != null && !isSkipAdLoading) {
            isSkipAdLoading = true
            AdManager.showInterstitialAd(
                activity = activity,
                onComplete = {
                    isSkipAdLoading = false
                    viewModel.skipCooldown()
                    android.widget.Toast.makeText(context, "Cooldown skipped! You can watch videos now.", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            viewModel.skipCooldown()
        }
    }

    fun startWatchVideo() {
        if (isCooldownActive) {
            android.widget.Toast.makeText(context, "Cooldown active! Tap Skip Cooldown to watch an ad.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (activity != null) {
            viewModel.watchAd {
                AdManager.showRewardedAd(
                    activity = activity,
                    onRewarded = { viewModel.onAdWatched() },
                    onFailed = { viewModel.onAdFailed() }
                )
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B16))
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
            Text("Watch & Earn", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "watch")

        Spacer(modifier = Modifier.height(12.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF651FFF), Color(0xFF2C1558))))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Watch Ads & Earn Coins", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Earn ${viewModel.rewardAmount} coins for every ad watched", color = Color(0xFFFFD54F), fontSize = 13.sp)
                }
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color(0xFFFFC107), modifier = Modifier.size(44.dp))
            }
        }

        // Cooldown Card
        CooldownCard(
            remainingSeconds = remainingCooldownSeconds,
            onSkipWithAd = { skipCooldownWithAd() },
            isAdLoading = isSkipAdLoading,
            activityTitle = "Next video"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                val remainingVideos = maxOf(0, viewModel.videoLimit - viewModel.videosWatched)
                items(remainingVideos) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF151528))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCooldownActive) Color(0xFFFF9800) else Color(0xFFF50057)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Watch Video #${viewModel.videosWatched + index + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Earn ${viewModel.rewardAmount} Coins", color = Color(0xFFAAAAB4), fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (isCooldownActive) {
                                    skipCooldownWithAd()
                                } else {
                                    startWatchVideo()
                                }
                            },
                            enabled = !viewModel.isWatchingAd && !isSkipAdLoading && viewModel.videosWatched < viewModel.videoLimit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCooldownActive) Color(0xFFFF9800) else Color(0xFFFFC107),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = when {
                                    viewModel.isWatchingAd -> "..."
                                    isCooldownActive -> "Skip Wait"
                                    else -> "Watch"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Daily Limit", color = Color(0xFFAAAAB4), fontSize = 12.sp)
                Text("${viewModel.videosWatched}/${viewModel.videoLimit} Videos", color = Color(0xFFAAAAB4), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (viewModel.videoLimit > 0) viewModel.videosWatched.toFloat() / viewModel.videoLimit else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFFC107),
                trackColor = Color(0xFF151528)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Banner Ad (Banner_Android)
        com.example.ui.components.BannerAdView()

        Spacer(modifier = Modifier.height(16.dp))
    }
    
    if (viewModel.rewardDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text("Reward Received!") },
            text = { Text("You successfully watched the ad and earned ${viewModel.rewardAmount} coins.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("Awesome")
                }
            }
        )
    }
}
