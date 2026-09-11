package com.example.ui.screens.earn.spinwheel

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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

class SpinWheelViewModel : ViewModel() {
    private val configRepository = com.example.data.repository.ConfigRepository()
    var spinLimit by mutableStateOf(10)
        private set
    var spinCooldownMinutes by mutableStateOf(5)
        private set
    var spinMinReward by mutableStateOf(1)
        private set
    var spinMaxReward by mutableStateOf(20)
        private set

    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    
    var spinsToday by mutableStateOf(0)
        private set
    var lastSpinAt by mutableLongStateOf(0L)
        private set

    init {
        loadUser()
        loadConfig()
    }
    private fun loadConfig() {
        viewModelScope.launch {
            configRepository.getConfigFlow().collect { config ->
                spinLimit = if (config.spin_limit > 0) config.spin_limit else 10
                spinCooldownMinutes = if (config.spin_cooldown_minutes >= 0) config.spin_cooldown_minutes else 5
                spinMinReward = config.spin_min_reward.coerceIn(1, 20)
                spinMaxReward = config.spin_max_reward.coerceIn(spinMinReward, 20)
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
                calLast.timeInMillis = user.lastSpinAt
                
                val isSameDay = calCurrent.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR) &&
                                calCurrent.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR)
                
                if (!isSameDay) {
                    userRepository.resetSpinsLimit(uid)
                    spinsToday = 0
                } else {
                    spinsToday = user.spinsToday
                }
                lastSpinAt = user.lastSpinAt
            }
        }
    }
    
    fun addCoinsAndRecordSpin(amount: Int) {
        val uid = auth.currentUser?.uid ?: return
        lastSpinAt = System.currentTimeMillis()
        viewModelScope.launch {
            userRepository.updateUserBalance(uid, amount)
            userRepository.updateSpins(uid, 1)
            if (amount > 0) {
                userRepository.addTransaction(Transaction(userId = uid, title = "Spin Wheel", amount = amount, type = "credit", icon = "star"))
            }
            spinsToday++
        }
    }

    fun skipCooldown() {
        lastSpinAt = 0L
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.resetSpinCooldown(uid)
        }
    }
}

@Composable
fun SpinWheelScreen(onBack: () -> Unit, viewModel: SpinWheelViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = remember(context) {
        var c: android.content.Context? = context
        while (c is android.content.ContextWrapper) {
            if (c is Activity) return@remember c
            c = c.baseContext
        }
        null
    }
    var isSpinning by remember { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(false) }
    var isSkipAdLoading by remember { mutableStateOf(false) }
    var rotationDegree by remember { mutableFloatStateOf(0f) }
    var rewardDialog by remember { mutableStateOf<Int?>(null) }
    var remainingCooldownSeconds by remember { mutableLongStateOf(0L) }
    
    val spinsLeft = maxOf(0, viewModel.spinLimit - viewModel.spinsToday)

    // Cooldown countdown loop
    LaunchedEffect(viewModel.lastSpinAt, viewModel.spinCooldownMinutes) {
        while (true) {
            val cooldownMs = viewModel.spinCooldownMinutes * 60 * 1000L
            val elapsed = System.currentTimeMillis() - viewModel.lastSpinAt
            val diff = cooldownMs - elapsed
            remainingCooldownSeconds = if (diff > 0) (diff + 999) / 1000 else 0L
            kotlinx.coroutines.delay(1000L)
        }
    }

    val isCooldownActive = remainingCooldownSeconds > 0L
    
    val minR = viewModel.spinMinReward.coerceIn(1, 20)
    val maxR = viewModel.spinMaxReward.coerceIn(minR, 20)
    val rewards = remember(minR, maxR) {
        if (minR == 1 && maxR == 20) {
            listOf(1, 2, 4, 6, 8, 10, 15, 20)
        } else {
            val step = maxOf(1, (maxR - minR) / 7)
            listOf(
                minR,
                minR + step,
                minR + step * 2,
                minR + step * 3,
                minR + step * 4,
                minR + step * 5,
                minR + step * 6,
                maxR
            ).map { it.coerceIn(1, 20) }
        }
    }
    val colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5), Color(0xFF00BCD4), Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFFFF9800), Color(0xFF795548))
    
    val animateRotation by animateFloatAsState(
        targetValue = rotationDegree,
        animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
        finishedListener = {
            isSpinning = false
            val normalizedRotation = it % 360
            val segmentAngle = 360f / rewards.size
            val pointingAngle = (360 - normalizedRotation + 270) % 360
            val index = (pointingAngle / segmentAngle).toInt() % rewards.size
            val reward = rewards[index].coerceIn(1, 20)
            rewardDialog = reward
            viewModel.addCoinsAndRecordSpin(reward)
        },
        label = "spin_wheel"
    )

    fun skipCooldownWithAd() {
        if (activity != null && !isSkipAdLoading) {
            isSkipAdLoading = true
            AdManager.showInterstitialAd(
                activity = activity,
                onComplete = {
                    isSkipAdLoading = false
                    viewModel.skipCooldown()
                    android.widget.Toast.makeText(context, "Cooldown skipped! You can spin now.", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            viewModel.skipCooldown()
        }
    }

    fun startSpin() {
        if (isCooldownActive) {
            android.widget.Toast.makeText(context, "Cooldown active! Tap Skip Cooldown to watch an ad.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (!isSpinning && !isAdLoading && spinsLeft > 0) {
            if (activity != null) {
                isAdLoading = true
                AdManager.showRewardedAd(
                    activity = activity,
                    onRewarded = {
                        isAdLoading = false
                        isSpinning = true
                        rotationDegree += (360 * 5) + (Math.random() * 360).toFloat()
                    },
                    onFailed = {
                        isAdLoading = false
                        isSpinning = true
                        rotationDegree += (360 * 5) + (Math.random() * 360).toFloat()
                    }
                )
            } else {
                isSpinning = true
                rotationDegree += (360 * 5) + (Math.random() * 360).toFloat()
            }
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
            Text("Spin Wheel", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "spin")

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Spin the wheel and win 1 to 20 coins!",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Cooldown Card with countdown timer & Skip Ad button
        CooldownCard(
            remainingSeconds = remainingCooldownSeconds,
            onSkipWithAd = { skipCooldownWithAd() },
            isAdLoading = isSkipAdLoading,
            activityTitle = "Next spin"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().rotate(animateRotation)) {
                val segmentAngle = 360f / rewards.size
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f

                for (i in rewards.indices) {
                    drawArc(
                        color = colors[i],
                        startAngle = i * segmentAngle,
                        sweepAngle = segmentAngle,
                        useCenter = true,
                        style = Fill
                    )
                }

                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = radius * 0.11f
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(180, 0, 0, 0))
                    }

                    for (i in rewards.indices) {
                        val angleDeg = i * segmentAngle + segmentAngle / 2f
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val textRadius = radius * 0.65f
                        val x = center.x + (textRadius * kotlin.math.cos(angleRad)).toFloat()
                        val y = center.y + (textRadius * kotlin.math.sin(angleRad)).toFloat() + (paint.textSize / 3)

                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.rotate(angleDeg + 90f, x, y - (paint.textSize / 3))
                        canvas.nativeCanvas.drawText("${rewards[i]}", x, y, paint)
                        canvas.nativeCanvas.restore()
                    }
                }
            }
            
            // Indicator at top (270 degrees)
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = "Pointer",
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = (-150).dp)
                    .rotate(180f)
            )
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCooldownActive -> Color(0xFFFF9800)
                            spinsLeft > 0 -> Color(0xFFFFC107)
                            else -> Color.Gray
                        }
                    )
                    .clickable(enabled = !isSpinning && !isAdLoading && (spinsLeft > 0 || isCooldownActive)) {
                        if (isCooldownActive) {
                            skipCooldownWithAd()
                        } else {
                            startSpin()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isAdLoading || isSkipAdLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        if (isCooldownActive) "SKIP" else "SPIN",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (isCooldownActive) {
                    skipCooldownWithAd()
                } else {
                    startSpin()
                }
            },
            enabled = (!isSpinning && !isAdLoading && !isSkipAdLoading && spinsLeft > 0) || isCooldownActive,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCooldownActive) Color(0xFFFF9800) else Color(0xFF651FFF),
                contentColor = Color.White,
                disabledContainerColor = Color.Gray
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            val cooldownText = String.format("%02d:%02d", remainingCooldownSeconds / 60, remainingCooldownSeconds % 60)
            Text(
                when {
                    isCooldownActive -> "Cooldown: $cooldownText (Tap to Skip)"
                    spinsLeft == 0 -> "Come back tomorrow"
                    isSpinning -> "Spinning..."
                    isAdLoading -> "Loading Ad..."
                    else -> "Spin Now (Watch Ad)"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "You have $spinsLeft/${viewModel.spinLimit} spins left today",
            color = Color(0xFFAAAAB4),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Banner Ad (Banner_Android)
        com.example.ui.components.BannerAdView()
    }
    
    if (rewardDialog != null) {
        AlertDialog(
            onDismissRequest = { rewardDialog = null },
            title = { Text(if (rewardDialog == 0) "Better luck next time!" else "Congratulations!") },
            text = { Text(if (rewardDialog == 0) "You didn't win anything this time." else "You won $rewardDialog coins!") },
            confirmButton = {
                TextButton(onClick = { rewardDialog = null }) {
                    Text("OK")
                }
            }
        )
    }
}
