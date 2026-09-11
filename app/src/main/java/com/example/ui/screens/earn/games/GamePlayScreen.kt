package com.example.ui.screens.earn.games

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.util.FileDownloader
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.repository.UserRepository
import com.example.ui.theme.*
import com.example.util.AdManager
import com.example.ui.components.BannerAdView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreen(
    gameId: String = "",
    title: String = "Play Game",
    url: String = "https://play2048.co/",
    coins: Int = 50,
    seconds: Int = 60,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var c: android.content.Context? = context
        while (c is android.content.ContextWrapper) {
            if (c is Activity) return@remember c
            c = c.baseContext
        }
        null
    }
    val coroutineScope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var timeRemaining by remember { mutableIntStateOf(if (seconds > 0) seconds else 60) }
    var isTimerComplete by remember { mutableStateOf(false) }
    var isClaimingReward by remember { mutableStateOf(false) }
    var isRewardClaimed by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Countdown Timer Loop
    LaunchedEffect(Unit) {
        while (timeRemaining > 0 && !isRewardClaimed) {
            delay(1000L)
            timeRemaining--
        }
        if (timeRemaining <= 0) {
            isTimerComplete = true
            if (activity != null) {
                AdManager.showInterstitialAd(activity)
            }
        }
    }

    val totalSeconds = remember { if (seconds > 0) seconds else 60 }
    val progress = remember(timeRemaining) {
        1f - (timeRemaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )

                        // Timer status badge
                        if (!isRewardClaimed) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isTimerComplete) Color(0xFF00C853)
                                        else Color(0xFF7B3FE4).copy(alpha = 0.8f)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isTimerComplete) Icons.Filled.CheckCircle else Icons.Filled.Timer,
                                        contentDescription = "Timer",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isTimerComplete) "Ready! 🎉" else "${timeRemaining}s",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFFFB300))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "Claimed! 🪙",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        FileDownloader.downloadOrOpen(context, url, title)
                    }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open in browser", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Linear Progress Bar for countdown
            if (!isRewardClaimed) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = if (isTimerComplete) Color(0xFF00C853) else AccentYellow,
                    trackColor = SurfaceDark
                )
            }

            // Interactive Game WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mediaPlaybackRequiresUserGesture = false
                        }
                        setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                            FileDownloader.downloadFile(ctx, downloadUrl, userAgent, contentDisposition, mimetype, title)
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                return FileDownloader.handleWebViewUrl(ctx, reqUrl)
                            }

                            @Suppress("DEPRECATION")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url == null) return false
                                return FileDownloader.handleWebViewUrl(ctx, url)
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl(url)
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isTimerComplete || isRewardClaimed) {
                BannerAdView(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 4.dp)
                )
            }

            // Claim Reward Floating Banner / Sheet when timer reaches 0
            AnimatedVisibility(
                visible = isTimerComplete && !isRewardClaimed,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            2.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFB300), Color(0xFF7B3FE4))
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1438)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎁", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Target Time Completed!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Watch short sponsor ad to claim your reward",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (isClaimingReward || isRewardClaimed) return@Button
                                isClaimingReward = true

                                val uid = auth.currentUser?.uid
                                if (uid == null) {
                                    Toast.makeText(context, "Please log in to claim coins", Toast.LENGTH_SHORT).show()
                                    isClaimingReward = false
                                    return@Button
                                }

                                // Function to credit coins
                                fun creditCoins() {
                                    coroutineScope.launch {
                                        try {
                                            userRepository.updateUserBalance(
                                                uid = uid,
                                                amount = coins,
                                                title = "Game: $title",
                                                type = "credit",
                                                icon = "sports_esports"
                                            )
                                            userRepository.updateGamePlayed(uid = uid, increment = 1)
                                            val now = System.currentTimeMillis()
                                            userRepository.recordLastGamePlayedAt(uid = uid, time = now)
                                            context.getSharedPreferences("games_cooldown_prefs", android.content.Context.MODE_PRIVATE)
                                                .edit().putLong("last_game_played_at", now).apply()

                                            // Push Notification
                                            val notif = hashMapOf(
                                                "title" to "Game Reward Claimed! 🎮",
                                                "message" to "You earned +$coins coins playing '$title'.",
                                                "userId" to uid,
                                                "timestamp" to System.currentTimeMillis(),
                                                "icon" to "sports_esports"
                                            )
                                            db.collection("notifications").add(notif)

                                            isRewardClaimed = true
                                            isClaimingReward = false
                                            showSuccessDialog = true
                                        } catch (e: Exception) {
                                            errorMessage = e.message
                                            isClaimingReward = false
                                        }
                                    }
                                }

                                // Show Rewarded Ad as requested by user!
                                if (activity != null) {
                                    AdManager.showRewardedAd(
                                        activity = activity,
                                        onRewarded = {
                                            creditCoins()
                                        },
                                        onFailed = {
                                            // Fallback grace if ad fails to load
                                            creditCoins()
                                        }
                                    )
                                } else {
                                    creditCoins()
                                }
                            },
                            enabled = !isClaimingReward,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentYellow,
                                contentColor = Color.Black
                            )
                        ) {
                            if (isClaimingReward) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Stars,
                                        contentDescription = "Claim",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Claim +$coins Coins (Watch Ad)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Success Reward Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎉 Coins Earned!", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🪙", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "+$coins Coins",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentYellow
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Congratulations! Coins added to your wallet for playing $title.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("Keep Playing", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
