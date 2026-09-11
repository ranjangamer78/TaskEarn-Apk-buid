package com.example.ui.screens.earn.dailybonus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.example.data.repository.ConfigRepository
import com.example.data.model.Transaction
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.example.util.AdManager

class DailyBonusViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val configRepository = ConfigRepository()
    
    var isClaimed by mutableStateOf(false)
        private set
        
    var currentStreak by mutableStateOf(0)
        private set
        
    var baseBonus by mutableStateOf(100)
        private set
        
    var dailyStreakCoins by mutableStateOf<List<Int>>(emptyList())
        private set

    init {
        loadUser()
        loadConfig()
    }
    
    private fun loadConfig() {
        viewModelScope.launch {
            configRepository.getConfigFlow().collect { config ->
                baseBonus = config.daily_bonus
                dailyStreakCoins = config.daily_streak_coins
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
                calLast.timeInMillis = user.lastClaimedAt
                
                val isSameDay = calCurrent.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR) &&
                                calCurrent.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR)
                                
                calLast.add(java.util.Calendar.DAY_OF_YEAR, 1)
                val isNextDay = calCurrent.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR) &&
                                calCurrent.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR)

                if (isSameDay) {
                    isClaimed = true
                    currentStreak = user.streak
                } else if (isNextDay) {
                    currentStreak = user.streak
                    isClaimed = false
                } else {
                    currentStreak = 0
                    isClaimed = false
                }
            }
        }
    }
    
    var isWatchingAd by mutableStateOf(false)
        private set

    fun watchAdAndClaim(onShowAd: () -> Unit) {
        if (isClaimed || isWatchingAd) return
        isWatchingAd = true
        onShowAd()
    }
    
    fun onAdFailed() {
        isWatchingAd = false
    }

    fun claimBonus() {
        isWatchingAd = false
        if (isClaimed) return
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val newStreak = if (currentStreak >= 7) 1 else currentStreak + 1
                val bonusAmount = if (dailyStreakCoins.isNotEmpty() && newStreak - 1 < dailyStreakCoins.size) {
                    dailyStreakCoins[newStreak - 1]
                } else {
                    if (newStreak == 7) baseBonus * 10 else newStreak * baseBonus
                }
                
                userRepository.updateUserBalance(uid, bonusAmount)
                userRepository.updateDailyStreak(uid, newStreak)
                userRepository.addTransaction(Transaction(userId = uid, title = "Daily Bonus", amount = bonusAmount, type = "credit", icon = "card"))
                
                currentStreak = newStreak
                isClaimed = true
            } catch (e: Exception) {
                // error
            }
        }
    }
}

@Composable
fun DailyBonusScreen(onBack: () -> Unit, viewModel: DailyBonusViewModel = viewModel()) {
    val currentStreak = viewModel.currentStreak
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B16))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Daily Bonus", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "bonus")

        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.CardGiftcard, contentDescription = "Gift", tint = Color(0xFFF50057), modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Claim your daily bonus every day!", color = Color(0xFFAAAAB4), fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF151528))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Today's Bonus", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = "Coin", tint = Color(0xFFFFC107), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${(currentStreak + 1) * viewModel.baseBonus}", color = Color(0xFFFFC107), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Come daily and earn more coins", color = Color(0xFFAAAAB4), fontSize = 12.sp)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        viewModel.watchAdAndClaim {
                            AdManager.showRewardedAd(
                                activity = context as Activity,
                                onRewarded = { viewModel.claimBonus() },
                                onFailed = { viewModel.onAdFailed() }
                            )
                        }
                    },
                    enabled = !viewModel.isClaimed && !viewModel.isWatchingAd,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color.Black),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        if (viewModel.isClaimed) "Claimed" 
                        else if (viewModel.isWatchingAd) "..." 
                        else "Claim Now", 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Daily Streak", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (1..4).forEach { day ->
                StreakDay(day, claimed = day <= currentStreak, current = day == currentStreak + 1, baseBonus = viewModel.baseBonus, dailyStreakCoins = viewModel.dailyStreakCoins)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (5..7).forEach { day ->
                StreakDay(day, claimed = false, current = false, isLast = day == 7, baseBonus = viewModel.baseBonus, dailyStreakCoins = viewModel.dailyStreakCoins)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33F44336))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = Color(0xFFF44336))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Don't miss your streak!", color = Color(0xFFF44336), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Streak will reset if you miss a day.", color = Color(0xFFAAAAB4), fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Banner Ad
        com.example.ui.components.BannerAdView()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StreakDay(day: Int, claimed: Boolean, current: Boolean, isLast: Boolean = false, baseBonus: Int = 100, dailyStreakCoins: List<Int> = emptyList()) {
    val amount = if (dailyStreakCoins.isNotEmpty() && day - 1 < dailyStreakCoins.size) {
        dailyStreakCoins[day - 1]
    } else {
        if (isLast) baseBonus * 10 else day * baseBonus
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (current) Color(0xFF651FFF).copy(alpha = 0.2f) else Color(0xFF151528))
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Text("Day $day", color = Color(0xFFAAAAB4), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        if (claimed) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Claimed", tint = Color.Green, modifier = Modifier.size(24.dp))
        } else if (isLast) {
             Icon(Icons.Filled.CardGiftcard, contentDescription = "Gift", tint = Color(0xFFF50057), modifier = Modifier.size(24.dp))
        } else {
            Icon(Icons.Filled.Star, contentDescription = "Coin", tint = if (current) Color(0xFFFFC107) else Color(0xFFAAAAB4), modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "$amount",
            color = if (current) Color(0xFFFFC107) else Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
