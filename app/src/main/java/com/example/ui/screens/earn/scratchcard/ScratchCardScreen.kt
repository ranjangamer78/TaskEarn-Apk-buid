package com.example.ui.screens.earn.scratchcard

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserRepository
import com.example.ui.theme.*
import com.example.util.AdManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.data.model.Transaction

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.CooldownCard

class ScratchCardViewModel : ViewModel() {
    private val configRepository = com.example.data.repository.ConfigRepository()
    var scratchLimit by mutableStateOf(10)
        private set
    var scratchCooldownMinutes by mutableStateOf(5)
        private set
    var scratchReward by mutableStateOf(10)
        private set
    var scratchMinReward by mutableStateOf(1)
        private set
    var scratchMaxReward by mutableStateOf(20)
        private set
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    
    var scratchesToday by mutableStateOf(0)
        private set
    var lastScratchAt by mutableLongStateOf(0L)
        private set

    init {
        loadUser()
        loadConfig()
    }
    private fun loadConfig() {
        viewModelScope.launch {
            configRepository.getConfigFlow().collect { config ->
                scratchLimit = if (config.scratch_limit > 0) config.scratch_limit else 10
                scratchCooldownMinutes = if (config.scratch_cooldown_minutes >= 0) config.scratch_cooldown_minutes else 5
                scratchReward = config.scratch_reward.coerceIn(1, 20)
                scratchMinReward = config.scratch_min_reward.coerceIn(1, 20)
                scratchMaxReward = config.scratch_max_reward.coerceIn(scratchMinReward, 20)
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
                calLast.timeInMillis = user.lastScratchAt
                
                val isSameDay = calCurrent.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR) &&
                                calCurrent.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR)
                
                if (!isSameDay) {
                    userRepository.resetScratchesLimit(uid)
                    scratchesToday = 0
                } else {
                    scratchesToday = user.scratchesToday
                }
                lastScratchAt = user.lastScratchAt
            }
        }
    }
    
    fun addCoinsAndRecordScratch(amount: Int) {
        val uid = auth.currentUser?.uid ?: return
        val finalAmount = amount.coerceIn(1, 20)
        lastScratchAt = System.currentTimeMillis()
        viewModelScope.launch {
            userRepository.updateUserBalance(uid, finalAmount)
            userRepository.updateScratches(uid, 1)
            userRepository.addTransaction(Transaction(userId = uid, title = "Scratch Card", amount = finalAmount, type = "credit", icon = "star"))
            scratchesToday++
        }
    }

    fun skipCooldown() {
        lastScratchAt = 0L
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.resetScratchCooldown(uid)
        }
    }
}

@Composable
fun ScratchCardScreen(onBack: () -> Unit, viewModel: ScratchCardViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = remember(context) {
        var c: android.content.Context? = context
        while (c is android.content.ContextWrapper) {
            if (c is Activity) return@remember c
            c = c.baseContext
        }
        null
    }
    var isScratched by remember { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(false) }
    var isSkipAdLoading by remember { mutableStateOf(false) }
    var scratchProgress by remember { mutableFloatStateOf(0f) }
    var remainingCooldownSeconds by remember { mutableLongStateOf(0L) }

    val minR = viewModel.scratchMinReward.coerceIn(1, 20)
    val maxR = viewModel.scratchMaxReward.coerceIn(minR, 20)
    var reward by remember(minR, maxR, viewModel.scratchesToday) {
        mutableIntStateOf(if (maxR >= minR) (minR..maxR).random().coerceIn(1, 20) else 1)
    }
    val cardsLeft = maxOf(0, viewModel.scratchLimit - viewModel.scratchesToday)

    // Cooldown countdown loop
    LaunchedEffect(viewModel.lastScratchAt, viewModel.scratchCooldownMinutes) {
        while (true) {
            val cooldownMs = viewModel.scratchCooldownMinutes * 60 * 1000L
            val elapsed = System.currentTimeMillis() - viewModel.lastScratchAt
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
                    isScratched = false
                    scratchProgress = 0f
                    reward = if (maxR >= minR) (minR..maxR).random().coerceIn(1, 20) else 1
                    android.widget.Toast.makeText(context, "Cooldown skipped! You can scratch now.", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            viewModel.skipCooldown()
            isScratched = false
            scratchProgress = 0f
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B16))
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scratch Card", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "scratch")

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Scratch the card and win 1 to 20 coins!",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Cooldown Card with countdown timer & Skip Ad button
        CooldownCard(
            remainingSeconds = remainingCooldownSeconds,
            onSkipWithAd = { skipCooldownWithAd() },
            isAdLoading = isSkipAdLoading,
            activityTitle = "Next scratch"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    when {
                        isCooldownActive -> Color(0xFF231F3A)
                        isScratched -> Color(0xFF2C1558)
                        else -> Color(0xFF651FFF)
                    }
                )
                .pointerInput(isCooldownActive, isScratched, cardsLeft, isAdLoading) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        if (isCooldownActive) {
                            // Prompt to skip cooldown
                            return@detectDragGestures
                        }
                        if (!isScratched && cardsLeft > 0 && !isAdLoading) {
                            scratchProgress += 0.05f
                            if (scratchProgress >= 1f) {
                                if (activity != null) {
                                    isAdLoading = true
                                    AdManager.showRewardedAd(
                                        activity = activity,
                                        onRewarded = {
                                            isAdLoading = false
                                            isScratched = true
                                            viewModel.addCoinsAndRecordScratch(reward)
                                        },
                                        onFailed = {
                                            isAdLoading = false
                                            isScratched = true
                                            viewModel.addCoinsAndRecordScratch(reward)
                                        }
                                    )
                                } else {
                                    isScratched = true
                                    viewModel.addCoinsAndRecordScratch(reward)
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isAdLoading) {
                CircularProgressIndicator(color = Color.White)
            } else if (isCooldownActive) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("⏳", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cooldown Active", color = Color(0xFFFFC107), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${String.format("%02d:%02d", remainingCooldownSeconds / 60, remainingCooldownSeconds % 60)} remaining",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { skipCooldownWithAd() },
                        enabled = !isSkipAdLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Skip Cooldown (Watch Ad)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else if (isScratched) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("You Won", color = Color(0xFFAAAAB4), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$reward Coins!", color = Color(0xFFFFC107), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    "Scratch\nHere\n${(scratchProgress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        if (isCooldownActive) {
            Button(
                onClick = { skipCooldownWithAd() },
                enabled = !isSkipAdLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                val cooldownText = String.format("%02d:%02d", remainingCooldownSeconds / 60, remainingCooldownSeconds % 60)
                Text("Cooldown: $cooldownText (Tap to Skip with Ad)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else if (isScratched && cardsLeft > 0) {
            Button(
                onClick = { 
                    isScratched = false
                    scratchProgress = 0f
                    reward = if (maxR >= minR) (minR..maxR).random().coerceIn(1, 20) else 1
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color.Black),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Scratch Another", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else if (cardsLeft == 0) { 
            Button(
                onClick = { },
                enabled = false,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, contentColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Come back tomorrow", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "You have $cardsLeft/${viewModel.scratchLimit} cards left today",
            color = Color(0xFFAAAAB4),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Banner Ad (Banner_Android)
        com.example.ui.components.BannerAdView()
    }
}
