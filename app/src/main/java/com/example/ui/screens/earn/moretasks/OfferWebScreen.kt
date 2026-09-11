package com.example.ui.screens.earn.moretasks

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Warning
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OfferWebViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var timeRemaining by mutableStateOf(0)
    var isTimerRunning by mutableStateOf(false)
    var rewardClaimed by mutableStateOf(false)
    var isAlreadyCompleted by mutableStateOf(false)
    var statusMessage by mutableStateOf("")

    private val todayDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun startTimer(
        seconds: Int,
        reward: Int,
        offerId: String,
        title: String,
        frequency: String,
        maxCompletions: Int
    ) {
        if (isTimerRunning || rewardClaimed || isAlreadyCompleted) return

        val uid = auth.currentUser?.uid
        if (uid == null) {
            statusMessage = "Please log in to complete tasks."
            return
        }

        viewModelScope.launch {
            // 1. Verify eligibility in Firestore
            val effectiveLimit = if (maxCompletions > 0) maxCompletions else 1
            val isDaily = frequency.equals("DAILY", ignoreCase = true)

            if (offerId.isNotBlank()) {
                try {
                    val compDoc = db.collection("users").document(uid)
                        .collection("task_completions").document(offerId).get().await()

                    if (compDoc.exists()) {
                        val lastDate = compDoc.getString("lastCompletedDate") ?: ""
                        val prevDaily = compDoc.getLong("dailyCompletions")?.toInt() ?: 0
                        val prevTotal = compDoc.getLong("totalCompletions")?.toInt() ?: 0

                        if (isDaily) {
                            if (lastDate == todayDate && prevDaily >= effectiveLimit) {
                                isAlreadyCompleted = true
                                statusMessage = "You have already reached today's completion limit ($prevDaily/$effectiveLimit) for this daily task! It resets tomorrow."
                                return@launch
                            }
                        } else {
                            if (prevTotal >= effectiveLimit) {
                                isAlreadyCompleted = true
                                statusMessage = "This 1-day/one-time task has already been completed ($prevTotal/$effectiveLimit times)."
                                return@launch
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Start Countdown Timer
            timeRemaining = if (seconds > 0) seconds else 1
            isTimerRunning = true

            while (timeRemaining > 0) {
                delay(1000)
                timeRemaining--
            }
            isTimerRunning = false

            // 3. Claim Reward
            if (!rewardClaimed && !isAlreadyCompleted) {
                try {
                    // Update user coin balance
                    userRepository.updateUserBalance(
                        uid,
                        reward,
                        "Task: $title",
                        "credit",
                        "local_offer"
                    )

                    // Record completion count
                    if (offerId.isNotBlank()) {
                        val compRef = db.collection("users").document(uid)
                            .collection("task_completions").document(offerId)

                        db.runTransaction { tx ->
                            val snap = tx.get(compRef)
                            val prevTotal = snap.getLong("totalCompletions")?.toInt() ?: 0
                            val prevDaily = snap.getLong("dailyCompletions")?.toInt() ?: 0
                            val lastDate = snap.getString("lastCompletedDate") ?: ""

                            val newDaily = if (lastDate == todayDate) prevDaily + 1 else 1
                            val newTotal = prevTotal + 1

                            val data = hashMapOf(
                                "offerId" to offerId,
                                "totalCompletions" to newTotal,
                                "dailyCompletions" to newDaily,
                                "lastCompletedDate" to todayDate,
                                "lastCompletedTimestamp" to System.currentTimeMillis()
                            )
                            tx.set(compRef, data)
                        }.await()
                    }

                    // Notification
                    val notif = hashMapOf(
                        "title" to "Task Reward Received! 🎉",
                        "message" to "+$reward Coins received for completing '$title'.",
                        "userId" to uid,
                        "timestamp" to System.currentTimeMillis(),
                        "icon" to "local_offer"
                    )
                    db.collection("notifications").add(notif)

                    rewardClaimed = true
                    statusMessage = "Congratulations! +$reward coins added to your wallet."
                } catch (e: Exception) {
                    statusMessage = "Error claiming reward: ${e.message}"
                }
            }
        }
    }
}

@Composable
fun OfferWebScreen(
    offerId: String = "",
    url: String,
    timer: Int,
    reward: Int,
    title: String = "Task",
    frequency: String = "ONCE",
    maxCompletions: Int = 1,
    onBack: () -> Unit,
    viewModel: OfferWebViewModel = viewModel()
) {
    val context = LocalContext.current
    var intentLaunched by remember { mutableStateOf(false) }

    val isDaily = frequency.equals("DAILY", ignoreCase = true)

    LaunchedEffect(offerId) {
        viewModel.startTimer(
            seconds = timer,
            reward = reward,
            offerId = offerId,
            title = title,
            frequency = frequency,
            maxCompletions = maxCompletions
        )
    }

    // Automatically open external URL once if available
    LaunchedEffect(url) {
        if (!intentLaunched && url.isNotBlank() && !viewModel.isAlreadyCompleted) {
            com.example.util.FileDownloader.downloadOrOpen(context, url, title)
            intentLaunched = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B16))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161626))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    if (isDaily) "🔄 Daily Repeating Task" else "⏱️ 1-Day Only Task",
                    color = if (isDaily) Color(0xFFFFB74D) else Color(0xFF82B1FF),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFC107).copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "+$reward Coins",
                    color = Color(0xFFFFC107),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Main Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (viewModel.isAlreadyCompleted) {
                // Task Already Completed State
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(Color(0xFFFF9800).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Already Completed",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Task Already Completed",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    viewModel.statusMessage,
                    color = Color(0xFFAAAAB4),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(30.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF651FFF), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Return to Tasks List", fontWeight = FontWeight.Bold)
                }
            } else if (viewModel.rewardClaimed) {
                // Reward Claimed State
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(45.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(54.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Reward Claimed!",
                    color = Color(0xFF00E676),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "+$reward Coins has been added to your wallet!",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (isDaily) "This daily task will be available again tomorrow." else "This 1-day only task is now complete.",
                    color = Color(0xFFAAAAB4),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        val act = context as? android.app.Activity
                        if (act != null) {
                            com.example.util.AdManager.showInterstitialAd(act) {
                                onBack()
                            }
                        } else {
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF651FFF), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Done & Return", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                com.example.ui.components.BannerAdView()
            } else {
                // Active Countdown State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1B1B33), Color(0xFF121224))))
                        .border(1.dp, Color(0xFF651FFF).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = {
                                if (timer > 0) (timer - viewModel.timeRemaining).toFloat() / timer.toFloat() else 1f
                            },
                            modifier = Modifier.size(80.dp),
                            color = Color(0xFFFFC107),
                            strokeWidth = 6.dp,
                            trackColor = Color(0xFF33334D)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "${viewModel.timeRemaining}s",
                            color = Color(0xFFFFC107),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please complete the task in the opened window...",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Stay on the task until the timer finishes to claim your +$reward coins reward.",
                            color = Color(0xFFAAAAB4),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (url.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            com.example.util.FileDownloader.downloadOrOpen(context, url, title)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = Color(0xFFFFC107))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Task Link Again", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
