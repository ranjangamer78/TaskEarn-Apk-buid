package com.example.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserRepository
import com.example.data.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    
    var transactions by mutableStateOf<List<Transaction>>(emptyList())
        private set
        
    init {
        loadTransactions()
    }
    
    private fun loadTransactions() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                userRepository.getTransactionsFlow(uid),
                userRepository.getWithdrawRequestsFlow(uid)
            ) { txs, reqs ->
                val mappedReqs = reqs.map { req ->
                    Transaction(
                        id = req.id,
                        userId = req.userId,
                        title = "Withdraw to " + req.method,
                        amount = req.coins,
                        type = "debit",
                        timestamp = req.timestamp,
                        icon = "card",
                        status = req.status,
                        details = req.details
                    )
                }
                (txs.filter { it.type != "debit" || !it.title.startsWith("Withdraw") } + mappedReqs).sortedByDescending { it.timestamp }
            }.collect { combined ->
                transactions = combined
            }
        }
    }
}

@Composable
fun HistoryScreen(onBack: () -> Unit = {}, viewModel: HistoryViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Earned", "Withdrawn")

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
            Spacer(modifier = Modifier.width(16.dp))
            Text("History", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "history")

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                Button(
                    onClick = { selectedTab = index },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == index) PrimaryPurple else SurfaceDark,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text(title)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        val filteredItems = when (selectedTab) {
            1 -> viewModel.transactions.filter { it.type == "credit" }
            2 -> viewModel.transactions.filter { it.type == "debit" }
            else -> viewModel.transactions
        }
        
        val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No transactions yet.", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredItems) { tx ->
                    val isCredit = tx.type == "credit"
                    val amountStr = if (isCredit) "+${tx.amount}" else "-${tx.amount}"
                    val amountColor = if (isCredit) Color.Green else Color.Red
                    val icon = when (tx.icon) {
                        "card" -> Icons.Filled.CardGiftcard
                        "play" -> Icons.Filled.PlayArrow
                        "star" -> Icons.Filled.Star
                        "people" -> Icons.Filled.People
                        else -> Icons.Filled.Star
                    }
                    val iconTint = if (isCredit) AccentYellow else Color.Gray
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = tx.title, tint = iconTint)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.title, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(dateFormat.format(Date(tx.timestamp)), color = TextSecondary, fontSize = 12.sp)
                            if (tx.status != "success" && tx.status.isNotEmpty()) {
                                val statusColor = when(tx.status) {
                                    "pending" -> AccentYellow
                                    "rejected" -> Color.Red
                                    "approved" -> Color.Green
                                    else -> TextSecondary
                                }
                                Text(tx.status.uppercase(), color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(amountStr, color = amountColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        com.example.ui.components.BannerAdView()
    }
}
