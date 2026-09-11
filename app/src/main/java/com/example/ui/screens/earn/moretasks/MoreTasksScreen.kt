package com.example.ui.screens.earn.moretasks

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Offer
import com.example.data.model.UserTaskCompletion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OffersViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _offers = MutableStateFlow<List<Offer>>(emptyList())
    val offers: StateFlow<List<Offer>> = _offers.asStateFlow()

    private val _completions = MutableStateFlow<Map<String, UserTaskCompletion>>(emptyMap())
    val completions: StateFlow<Map<String, UserTaskCompletion>> = _completions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val todayDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    
    private var offersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var completionsListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        listenData()
    }
    
    fun listenData() {
        _isLoading.value = true
        // 1. Real-time active offers listener
        offersListener?.remove()
        offersListener = db.collection("offers")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val loadedOffers = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Offer::class.java)?.copy(id = doc.id)
                    }
                    _offers.value = loadedOffers
                }
                _isLoading.value = false
            }

        // 2. Real-time user task completions listener
        val uid = auth.currentUser?.uid
        if (uid != null) {
            completionsListener?.remove()
            completionsListener = db.collection("users").document(uid).collection("task_completions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val compMap = mutableMapOf<String, UserTaskCompletion>()
                    snapshot.documents.forEach { doc ->
                        val item = doc.toObject(UserTaskCompletion::class.java)?.copy(offerId = doc.id)
                        if (item != null) {
                            compMap[doc.id] = item
                        }
                    }
                    _completions.value = compMap
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        offersListener?.remove()
        completionsListener?.remove()
    }

    fun isOfferCompleted(offer: Offer): Boolean {
        val comp = _completions.value[offer.id] ?: return false
        // If admin has set a reset timestamp/version on this offer, and comp was before the reset, it is active again!
        if (offer.resetVersion > 0L && comp.lastCompletedTimestamp < offer.resetVersion) {
            return false
        }
        if (offer.isRepeatable()) {
            return false
        }
        val maxAllowed = offer.getEffectiveMaxCompletions()
        return if (offer.isDaily()) {
            val dailyDone = if (comp.lastCompletedDate == todayDate) comp.dailyCompletions else 0
            dailyDone >= maxAllowed
        } else {
            comp.totalCompletions >= maxAllowed
        }
    }

    fun getDoneCount(offer: Offer): Int {
        val comp = _completions.value[offer.id] ?: return 0
        if (offer.resetVersion > 0L && comp.lastCompletedTimestamp < offer.resetVersion) {
            return 0
        }
        return if (offer.isDaily()) {
            if (comp.lastCompletedDate == todayDate) comp.dailyCompletions else 0
        } else {
            comp.totalCompletions
        }
    }
}

@Composable
fun MoreTasksScreen(
    onBack: () -> Unit, 
    onOfferClick: (Offer) -> Unit,
    viewModel: OffersViewModel = viewModel()
) {
    val offers by viewModel.offers.collectAsState()
    val completions by viewModel.completions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf("All") }
    val tabs = listOf("All", "Daily Repeat", "1-Day Only", "Completed")

    // Filter offers: Active tabs hide completed offers, Completed tab shows completed ones
    val filteredOffers = remember(offers, completions, selectedTab) {
        when (selectedTab) {
            "Daily Repeat" -> offers.filter { it.isDaily() && !viewModel.isOfferCompleted(it) }
            "1-Day Only" -> offers.filter { it.isOnce() && !viewModel.isOfferCompleted(it) }
            "Completed" -> offers.filter { viewModel.isOfferCompleted(it) }
            else -> offers.filter { !viewModel.isOfferCompleted(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B16))
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Custom Offers & Tasks", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "tasks")

        Spacer(modifier = Modifier.height(16.dp))
        
        // Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF311B92), Color(0xFF1A103D))))
                .border(1.dp, Color(0xFF651FFF).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Complete Tasks & Earn Coins",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Watch YouTube, visit sites, join channels & finish 1-day or daily repeating tasks.",
                        color = Color(0xFFD1C4E9),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    Icons.Filled.Assignment,
                    contentDescription = "Tasks",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(46.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(18.dp))
        
        // Filter Chips Bar
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs) { tab ->
                val isSelected = selectedTab == tab
                val count = when (tab) {
                    "Daily Repeat" -> offers.count { it.isDaily() }
                    "1-Day Only" -> offers.count { it.isOnce() }
                    "Completed" -> offers.count { viewModel.isOfferCompleted(it) }
                    else -> offers.size
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFF651FFF) else Color(0xFF151528))
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "$tab ($count)",
                        color = if (isSelected) Color.White else Color(0xFFAAAAB4),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF651FFF))
            }
        } else if (filteredOffers.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Assignment, contentDescription = null, tint = Color(0xFFAAAAB4), modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        if (selectedTab == "Completed") "No completed tasks yet." else "No offers available in this section.",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Check back soon or choose another tab!", color = Color(0xFFAAAAB4), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredOffers, key = { it.id }) { item ->
                    val isDone = viewModel.isOfferCompleted(item)
                    val doneCount = viewModel.getDoneCount(item)
                    val maxAllowed = item.getEffectiveMaxCompletions()
                    val isDaily = item.isDaily()

                    OfferItemCard(
                        offer = item,
                        isDone = isDone,
                        doneCount = doneCount,
                        maxAllowed = maxAllowed,
                        onClick = {
                            if (isDone) {
                                if (isDaily) {
                                    Toast.makeText(
                                        context,
                                        "You have completed this daily task today ($doneCount/$maxAllowed)! It will reset tomorrow.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "This task is for 1-day only and has already been completed!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                onOfferClick(item)
                            }
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        com.example.ui.components.BannerAdView()
    }
}

@Composable
fun OfferItemCard(
    offer: Offer,
    isDone: Boolean,
    doneCount: Int,
    maxAllowed: Int,
    onClick: () -> Unit
) {
    val isDaily = offer.isDaily()
    val isOnce = offer.isOnce()

    // Determine brand icon and theme color based on URL or title
    val (icon, iconTint, bgIcon) = remember(offer.url, offer.title) {
        val lower = (offer.url + " " + offer.title).lowercase()
        when {
            lower.contains("youtube") || lower.contains("youtu.be") || lower.contains("video") ->
                Triple(Icons.Filled.PlayArrow, Color(0xFFFF1744), Color(0xFFFF1744).copy(alpha = 0.15f))
            lower.contains("telegram") || lower.contains("t.me") ->
                Triple(Icons.Filled.Send, Color(0xFF29B6F6), Color(0xFF29B6F6).copy(alpha = 0.15f))
            lower.contains("http") || lower.contains("www") || lower.contains("web") || lower.contains("site") ->
                Triple(Icons.Filled.Language, Color(0xFF7C4DFF), Color(0xFF7C4DFF).copy(alpha = 0.15f))
            else ->
                Triple(Icons.Filled.Star, Color(0xFFFFB300), Color(0xFFFFB300).copy(alpha = 0.15f))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDone) Color(0xFF131322) else Color(0xFF1A1A2E))
            .border(
                1.dp,
                if (isDone) Color(0xFF00E676).copy(alpha = 0.25f) else Color(0xFF651FFF).copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDone) Color(0xFF00E676).copy(alpha = 0.15f) else bgIcon),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Done",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        icon,
                        contentDescription = offer.title,
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        offer.title,
                        color = if (isDone) Color(0xFFAAAAAA) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                if (offer.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        offer.description,
                        color = Color(0xFFAAAAB4),
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Frequency & Repeat Badge
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isDaily) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFF9800).copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "🔄 Daily ($doneCount/$maxAllowed/day)",
                                color = Color(0xFFFFB74D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isOnce) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2979FF).copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "⏱️ 1-Day Only ($doneCount/$maxAllowed)",
                                color = Color(0xFF82B1FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF9C27B0).copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "🔢 Limit ($doneCount/$maxAllowed)",
                                color = Color(0xFFCE93D8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (offer.timer > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF33334D))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "⏱️ ${offer.timer}s",
                                color = Color(0xFFCCCCCC),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Reward / Status Action
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "+${offer.reward}",
                    color = if (isDone) Color(0xFFAAAAAA) else Color(0xFFFFC107),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "Coins",
                    color = Color(0xFFAAAAB4),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isDone) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF00E676).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            if (isDaily) "Done Today" else "Completed",
                            color = Color(0xFF00E676),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF651FFF))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Start →",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
