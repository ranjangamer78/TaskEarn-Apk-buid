package com.example.ui.screens.wallet

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PaymentMethod
import com.example.data.repository.AppConfig
import com.example.data.repository.ConfigRepository
import com.example.data.repository.UserRepository
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WalletViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val configRepository = ConfigRepository()
    private val userRepository = UserRepository()

    val user get() = auth.currentUser

    var balance by mutableStateOf(0)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var paymentMethods by mutableStateOf<List<PaymentMethod>>(getDefaultPaymentMethods())
        private set

    var config by mutableStateOf(AppConfig())
        private set

    private var userListener: ListenerRegistration? = null
    private var methodsListener: ListenerRegistration? = null
    private var configListener: ListenerRegistration? = null

    init {
        listenRealtimeData()
    }

    private fun listenRealtimeData() {
        // 1. Real-time User Balance Listener
        val uid = auth.currentUser?.uid
        if (uid != null) {
            userListener?.remove()
            userListener = db.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        balance = snapshot.getLong("balance")?.toInt() ?: 0
                    }
                }
        }

        // 2. Real-time Config Listener (Rate, Min Withdraw, etc.)
        configListener?.remove()
        configListener = db.collection("settings").document("config")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    config = snapshot.toObject(AppConfig::class.java) ?: AppConfig()
                }
            }

        // 3. Real-time Payment Methods & Gift Cards Listener
        methodsListener?.remove()
        methodsListener = db.collection("payment_methods")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (paymentMethods.isEmpty()) {
                        paymentMethods = getDefaultPaymentMethods()
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PaymentMethod::class.java)?.copy(id = doc.id)
                    }
                    if (list.isEmpty()) {
                        seedDefaultPaymentMethods()
                    } else {
                        paymentMethods = list
                    }
                }
            }
    }

    private fun getDefaultPaymentMethods(): List<PaymentMethod> {
        return listOf(
            // 1. eSewa Online Payment (Min ₹100, Max ₹500)
            PaymentMethod(
                id = "esewa_100",
                name = "eSewa 100 NPR",
                category = "eSewa",
                amountType = "FIXED",
                fixedCoins = 10000,
                minimumAmount = 100,
                isActive = true,
                type = "wallet",
                icon = "esewa",
                placeholder = "Enter 10-digit eSewa Mobile Number",
                description = "Direct Instant NPR transfer to eSewa Wallet (Min ₹100)",
                badge = "MIN ₹100"
            ),
            PaymentMethod(
                id = "esewa_200",
                name = "eSewa 200 NPR",
                category = "eSewa",
                amountType = "FIXED",
                fixedCoins = 20000,
                minimumAmount = 200,
                isActive = true,
                type = "wallet",
                icon = "esewa",
                placeholder = "Enter 10-digit eSewa Mobile Number",
                description = "Instant 200 NPR transfer to eSewa Wallet",
                badge = "POPULAR"
            ),
            PaymentMethod(
                id = "esewa_300",
                name = "eSewa 300 NPR",
                category = "eSewa",
                amountType = "FIXED",
                fixedCoins = 30000,
                minimumAmount = 300,
                isActive = true,
                type = "wallet",
                icon = "esewa",
                placeholder = "Enter 10-digit eSewa Mobile Number",
                description = "Instant 300 NPR transfer to eSewa Wallet",
                badge = "RECOMMENDED"
            ),
            PaymentMethod(
                id = "esewa_500",
                name = "eSewa 500 NPR",
                category = "eSewa",
                amountType = "FIXED",
                fixedCoins = 50000,
                minimumAmount = 500,
                isActive = true,
                type = "wallet",
                icon = "esewa",
                placeholder = "Enter 10-digit eSewa Mobile Number",
                description = "Instant 500 NPR transfer to eSewa Wallet (Max ₹500)",
                badge = "MAX ₹500"
            ),
            PaymentMethod(
                id = "esewa_custom",
                name = "eSewa Flexible Transfer",
                category = "eSewa",
                amountType = "RANGE",
                minCoins = 10000,
                maxCoins = 50000,
                minimumAmount = 100,
                maximumAmount = 500,
                isActive = true,
                type = "wallet",
                icon = "esewa",
                placeholder = "Enter 10-digit eSewa Mobile Number",
                description = "Custom payout within limits (Min ₹100 - Max ₹500)",
                badge = "₹100 - ₹500"
            ),

            // 2. FF Diamond Top-Up (Fixed Rupees: ₹100 = 115 Diamonds)
            PaymentMethod(
                id = "ff_115",
                name = "115 Free Fire Diamonds",
                category = "FF Diamond",
                amountType = "FIXED",
                fixedCoins = 10000,
                minimumAmount = 100,
                isActive = true,
                type = "game",
                icon = "fire",
                placeholder = "Enter Free Fire Player UID & Nickname",
                description = "Direct In-Game 115 Diamonds Top-up (Fixed ₹100)",
                badge = "₹100 = 115💎"
            ),
            PaymentMethod(
                id = "ff_240",
                name = "240 Free Fire Diamonds",
                category = "FF Diamond",
                amountType = "FIXED",
                fixedCoins = 20000,
                minimumAmount = 200,
                isActive = true,
                type = "game",
                icon = "fire",
                placeholder = "Enter Free Fire Player UID & Nickname",
                description = "Direct In-Game 240 Diamonds Top-up (Fixed ₹200)",
                badge = "₹200 = 240💎"
            ),
            PaymentMethod(
                id = "ff_355",
                name = "355 Free Fire Diamonds",
                category = "FF Diamond",
                amountType = "FIXED",
                fixedCoins = 30000,
                minimumAmount = 300,
                isActive = true,
                type = "game",
                icon = "fire",
                placeholder = "Enter Free Fire Player UID & Nickname",
                description = "Direct In-Game 355 Diamonds Top-up (Fixed ₹300)",
                badge = "₹300 = 355💎"
            ),
            PaymentMethod(
                id = "ff_610",
                name = "610 Free Fire Diamonds",
                category = "FF Diamond",
                amountType = "FIXED",
                fixedCoins = 50000,
                minimumAmount = 500,
                isActive = true,
                type = "game",
                icon = "fire",
                placeholder = "Enter Free Fire Player UID & Nickname",
                description = "Direct In-Game 610 Diamonds Top-up (Fixed ₹500)",
                badge = "₹500 = 610💎"
            ),

            // 3. Robux Top-Up
            PaymentMethod(
                id = "robux_80",
                name = "80 Robux",
                category = "Robux",
                amountType = "FIXED",
                fixedCoins = 10000,
                minimumAmount = 100,
                isActive = true,
                type = "game",
                icon = "robux",
                placeholder = "Enter Roblox Username",
                description = "Direct 80 Robux voucher code (Fixed ₹100)",
                badge = "₹100 = 80🪙"
            ),
            PaymentMethod(
                id = "robux_170",
                name = "170 Robux",
                category = "Robux",
                amountType = "FIXED",
                fixedCoins = 20000,
                minimumAmount = 200,
                isActive = true,
                type = "game",
                icon = "robux",
                placeholder = "Enter Roblox Username",
                description = "Direct 170 Robux voucher code (Fixed ₹200)",
                badge = "₹200 = 170🪙"
            ),
            PaymentMethod(
                id = "robux_400",
                name = "400 Robux",
                category = "Robux",
                amountType = "FIXED",
                fixedCoins = 45000,
                minimumAmount = 450,
                isActive = true,
                type = "game",
                icon = "robux",
                placeholder = "Enter Roblox Username",
                description = "Official 400 Robux digital voucher code (Fixed ₹450)",
                badge = "VIP"
            ),

            // 4. PUBG Top-Up
            PaymentMethod(
                id = "pubg_60",
                name = "60 PUBG Mobile UC",
                category = "PUBG",
                amountType = "FIXED",
                fixedCoins = 10000,
                minimumAmount = 100,
                isActive = true,
                type = "game",
                icon = "pubg",
                placeholder = "Enter PUBG Character ID & In-game Nickname",
                description = "Direct PUBG Mobile 60 UC Top-up to ID (Fixed ₹100)",
                badge = "₹100 = 60 UC"
            ),
            PaymentMethod(
                id = "pubg_120",
                name = "120 PUBG Mobile UC",
                category = "PUBG",
                amountType = "FIXED",
                fixedCoins = 20000,
                minimumAmount = 200,
                isActive = true,
                type = "game",
                icon = "pubg",
                placeholder = "Enter PUBG Character ID & In-game Nickname",
                description = "Instant 120 UC Top-up directly to ID (Fixed ₹200)",
                badge = "₹200 = 120 UC"
            ),
            PaymentMethod(
                id = "pubg_325",
                name = "325 PUBG Mobile UC",
                category = "PUBG",
                amountType = "FIXED",
                fixedCoins = 50000,
                minimumAmount = 500,
                isActive = true,
                type = "game",
                icon = "pubg",
                placeholder = "Enter PUBG Character ID & In-game Nickname",
                description = "Instant 325 UC Top-up directly to Character ID (Fixed ₹500)",
                badge = "₹500 = 325 UC"
            )
        )
    }

    private fun seedDefaultPaymentMethods() {
        val defaults = getDefaultPaymentMethods()
        viewModelScope.launch {
            try {
                for (item in defaults) {
                    db.collection("payment_methods").document(item.id).set(item).await()
                }
            } catch (e: Exception) {
                // If offline or permission denied, keep in-memory defaults
                paymentMethods = defaults
            }
        }
    }

    fun requestWithdrawal(
        method: PaymentMethod,
        coins: Int,
        name: String,
        number: String,
        details: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        val uid = user?.uid
        if (uid == null) {
            onError("Please log in first")
            return
        }

        if (name.trim().isEmpty()) {
            onError("Please enter your name")
            return
        }

        if (number.trim().isEmpty()) {
            onError("Please enter your account / mobile number")
            return
        }

        val rateVal = if (config.rate > 0) config.rate else 100
        val requiredCoins = method.getEffectiveCoins(rateVal)

        if (coins < requiredCoins && method.isFixed()) {
            onError("Required coins for ${method.name} is $requiredCoins coins")
            return
        }

        if (coins > balance) {
            onError("Insufficient coins! You have $balance coins, but tried to withdraw $coins.")
            return
        }

        isProcessing = true
        viewModelScope.launch {
            try {
                val rupees = (coins / rateVal.toDouble()).toInt()
                val effectiveDetails = if (details.isNotBlank()) details.trim() else "Name: ${name.trim()} | Number: ${number.trim()}"

                // Deduct balance atomically
                val res = userRepository.updateUserBalance(
                    uid = uid,
                    amount = -coins,
                    title = "Withdrawal: ${method.name} ($coins Coins)",
                    type = "debit",
                    icon = "bank"
                )

                if (res.isFailure) {
                    isProcessing = false
                    onError("Failed to update balance: ${res.exceptionOrNull()?.message}")
                    return@launch
                }

                // Add withdrawal request to Firestore (real-time for Admin Panel and any other admin panel)
                val req = hashMapOf(
                    "userId" to uid,
                    "userEmail" to (user.email ?: ""),
                    "userName" to (user.displayName ?: name.trim()),
                    "name" to name.trim(),
                    "accountName" to name.trim(),
                    "number" to number.trim(),
                    "accountNumber" to number.trim(),
                    "phone" to number.trim(),
                    "coins" to coins,
                    "coin" to coins,
                    "method" to method.name,
                    "category" to method.category,
                    "type" to method.type,
                    "rupees" to rupees,
                    "details" to effectiveDetails,
                    "status" to "pending",
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("withdraw_requests").add(req).await()

                isProcessing = false
                onSuccess()
            } catch (e: Exception) {
                isProcessing = false
                onError(e.message ?: "Withdrawal request failed")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        methodsListener?.remove()
        configListener?.remove()
    }
}

@Composable
fun WalletScreen(
    onBack: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    viewModel: WalletViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputNumber by remember { mutableStateOf("") }
    var inputCoins by remember { mutableStateOf("") }
    
    // 2 Main Withdrawal Types: "ONLINE" (eSewa) and "GAME" (Game Item Top-up)
    var withdrawalMainType by remember { mutableStateOf("ONLINE") }
    var selectedGameCategory by remember { mutableStateOf("FF Diamond") }

    val rateVal = if (viewModel.config.rate > 0) viewModel.config.rate else 100
    val rupeesBalance = viewModel.balance / rateVal.toDouble()

    val filteredMethods = remember(viewModel.paymentMethods, withdrawalMainType, selectedGameCategory) {
        if (withdrawalMainType == "ONLINE") {
            viewModel.paymentMethods.filter { it.category.equals("eSewa", ignoreCase = true) }
        } else {
            viewModel.paymentMethods.filter { it.category.equals(selectedGameCategory, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B16))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Wallet & Withdraw",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Filled.History, contentDescription = "History", tint = Color.White)
            }
        }

        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "wallet")

        Spacer(modifier = Modifier.height(12.dp))

        // Balance Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF651FFF), Color(0xFFD500F9), Color(0xFF00E5FF))
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "TOTAL BALANCE",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFC107)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "${viewModel.balance}",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "≈ ₹${String.format(java.util.Locale.US, "%.2f", rupeesBalance)}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                // Rate Banner
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "⚡ Conversion Rate: 100 Coins = ₹1 (1,000 Coins = ₹10)",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2 Primary Withdrawal Types Header
        Text(
            "Select Withdrawal Type",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        // 2 Main Types Segmented Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF161626))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Type 1: Online Payment (eSewa)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (withdrawalMainType == "ONLINE") Color(0xFF60BB46) else Color.Transparent
                    )
                    .clickable { withdrawalMainType = "ONLINE" }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "🇳🇵 1. Online Payment",
                        color = if (withdrawalMainType == "ONLINE") Color.White else Color(0xFFAAAAB4),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "eSewa (Min ₹${viewModel.config.esewa_min_rupees} - Max ₹${viewModel.config.esewa_max_rupees})",
                        color = if (withdrawalMainType == "ONLINE") Color.White.copy(alpha = 0.85f) else Color(0xFF757585),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Type 2: Game Item Top-Up
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (withdrawalMainType == "GAME") Color(0xFFFF5722) else Color.Transparent
                    )
                    .clickable { withdrawalMainType = "GAME" }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "🎮 2. Game Item Top-Up",
                        color = if (withdrawalMainType == "GAME") Color.White else Color(0xFFAAAAB4),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "FF Diamond, Robux, PUBG",
                        color = if (withdrawalMainType == "GAME") Color.White.copy(alpha = 0.85f) else Color(0xFF757585),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Info Banner for Selected Withdrawal Type
        if (withdrawalMainType == "ONLINE") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF60BB46).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF60BB46).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🇳🇵", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "eSewa Online Payment Withdrawal",
                            color = Color(0xFF69F0AE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Instant transfer to eSewa number. Admin limit: Min ₹${viewModel.config.esewa_min_rupees} to Max ₹${viewModel.config.esewa_max_rupees} per request.",
                            color = Color(0xFFE0E0E0),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            // Game Items Filter & Rate Banner
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF5722).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFF5722).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💎", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Game Item Top-Up (Fixed Rupees)",
                                color = Color(0xFFFFAB91),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Free Fire: ₹100 = ${viewModel.config.ff_diamonds_100_rupees} Diamonds | Fixed conversion rate.",
                                color = Color(0xFFE0E0E0),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Game Sub-categories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val games = listOf("FF Diamond" to "💎 FF Diamond", "Robux" to "🪙 Robux", "PUBG" to "🎯 PUBG UC")
                    games.forEach { (key, label) ->
                        val isSelected = selectedGameCategory == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFFF5722) else Color(0xFF1E1E2C))
                                .clickable { selectedGameCategory = key }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Color.White else Color(0xFFAAAAB4),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Methods List
        if (filteredMethods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (withdrawalMainType == "ONLINE") "Loading eSewa options..." else "Loading $selectedGameCategory options...",
                    color = Color(0xFFAAAAB4),
                    fontSize = 14.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredMethods.forEach { method ->
                    PaymentMethodCard(
                        method = method,
                        rate = rateVal,
                        onClick = {
                            selectedMethod = method
                            inputName = viewModel.user?.displayName ?: ""
                            inputNumber = ""
                            inputCoins = if (method.isFixed()) {
                                method.getEffectiveCoins(rateVal).toString()
                            } else {
                                val minLimit = if (method.category.equals("eSewa", ignoreCase = true)) {
                                    maxOf(method.getMinCoinsAllowed(rateVal), viewModel.config.esewa_min_rupees * rateVal)
                                } else {
                                    method.getMinCoinsAllowed(rateVal)
                                }
                                minLimit.toString()
                            }
                            showDialog = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Banner Ad
        com.example.ui.components.BannerAdView()

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Withdrawal Dialog
    if (showDialog && selectedMethod != null) {
        val method = selectedMethod!!
        val isFixed = method.isFixed()
        val isEsewa = method.category.equals("eSewa", ignoreCase = true)
        val minAllowed = if (isEsewa) {
            maxOf(method.getMinCoinsAllowed(rateVal), viewModel.config.esewa_min_rupees * rateVal)
        } else {
            method.getMinCoinsAllowed(rateVal)
        }
        val maxAllowed = if (isEsewa) {
            minOf(method.getMaxCoinsAllowed(rateVal), viewModel.config.esewa_max_rupees * rateVal)
        } else {
            method.getMaxCoinsAllowed(rateVal)
        }
        val fixedRequiredCoins = method.getEffectiveCoins(rateVal)

        val enteredCoins = if (isFixed) fixedRequiredCoins else (inputCoins.toIntOrNull() ?: minAllowed)
        val calculatedRupees = String.format(java.util.Locale.US, "%.2f", enteredCoins / rateVal.toDouble())
        val hasSufficientBalance = viewModel.balance >= enteredCoins
        val isValidAmount = isFixed || (enteredCoins in minAllowed..maxAllowed)

        AlertDialog(
            onDismissRequest = { if (!viewModel.isProcessing) showDialog = false },
            containerColor = Color(0xFF161626),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MethodIconBadge(method.icon, size = 38)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(method.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF651FFF).copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    method.category,
                                    color = Color(0xFFB388FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isFixed) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFFFF9800).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (isFixed) "FIXED AMOUNT" else "MIN-MAX RANGE",
                                    color = if (isFixed) Color(0xFF00E676) else Color(0xFFFF9800),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (method.description.isNotEmpty()) {
                        Text(
                            method.description,
                            color = Color(0xFFB0BEC5),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Balance Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Your Current Balance:", color = Color(0xFFAAAAB4), fontSize = 13.sp)
                        Text("${viewModel.balance} Coins", color = Color(0xFFFFC107), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode 1: FIXED AMOUNT SUMMARY
                    if (isFixed) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0F1A))
                                .border(1.dp, Color(0xFF00E676).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Fixed Required Coins:", color = Color(0xFFAAAAB4), fontSize = 13.sp)
                                    Text("$fixedRequiredCoins Coins", color = Color(0xFF00E676), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Reward Value:", color = Color(0xFFAAAAB4), fontSize = 12.sp)
                                    Text(method.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 1. Enter Name Input
                    Text("1. Enter Name:", color = Color(0xFFAAAAB4), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = { Text("Account Holder / Full Name") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF00E676)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color(0xFF444455)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Enter Number / Account Input
                    val hintText = method.placeholder.ifEmpty { "Enter Mobile / Account Number / ID" }
                    Text("2. Enter Number / Account:", color = Color(0xFFAAAAB4), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = inputNumber,
                        onValueChange = { inputNumber = it },
                        placeholder = { Text(hintText) },
                        leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = Color(0xFF651FFF)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF651FFF),
                            unfocusedBorderColor = Color(0xFF444455)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Enter Coin Amount Input
                    Text("3. Enter Coins to Withdraw:", color = Color(0xFFAAAAB4), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = inputCoins,
                        onValueChange = { inputCoins = it.filter { ch -> ch.isDigit() } },
                        placeholder = { Text("Coins Amount (e.g. $enteredCoins)") },
                        leadingIcon = { Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color(0xFFFFC107)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFC107),
                            unfocusedBorderColor = Color(0xFF444455)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isFixed) {
                        Spacer(modifier = Modifier.height(6.dp))
                        // Quick Amount Helper Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { inputCoins = minAllowed.toString() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Min", fontSize = 10.sp, color = Color(0xFFFF9800))
                            }
                            OutlinedButton(
                                onClick = {
                                    val cur = inputCoins.toIntOrNull() ?: minAllowed
                                    inputCoins = (cur + 500).coerceAtMost(maxAllowed).toString()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("+500", fontSize = 10.sp, color = Color.White)
                            }
                            OutlinedButton(
                                onClick = {
                                    val cur = inputCoins.toIntOrNull() ?: minAllowed
                                    inputCoins = (cur + 1000).coerceAtMost(maxAllowed).toString()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("+1000", fontSize = 10.sp, color = Color.White)
                            }
                            OutlinedButton(
                                onClick = { inputCoins = maxAllowed.toString() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Max", fontSize = 10.sp, color = Color(0xFF00E676))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "≈ ₹$calculatedRupees value",
                        color = Color(0xFF00E676),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Error warning if insufficient coins
                    if (!hasSufficientBalance) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ Insufficient coins! You need $enteredCoins coins.",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp
                        )
                    } else if (!isValidAmount) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ Amount must be between $minAllowed and $maxAllowed coins.",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.requestWithdrawal(
                            method = method,
                            coins = enteredCoins,
                            name = inputName.trim(),
                            number = inputNumber.trim(),
                            details = "Name: ${inputName.trim()} | Number: ${inputNumber.trim()}",
                            onSuccess = {
                                Toast.makeText(context, "Withdrawal Request Submitted Successfully!", Toast.LENGTH_LONG).show()
                                showDialog = false
                                inputName = ""
                                inputNumber = ""
                                inputCoins = ""
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    enabled = !viewModel.isProcessing && hasSufficientBalance && isValidAmount && inputName.trim().isNotBlank() && inputNumber.trim().length >= 3 && enteredCoins > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF651FFF), contentColor = Color.White)
                ) {
                    if (viewModel.isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Withdraw Now")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!viewModel.isProcessing) showDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFAAAAB4))
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PaymentMethodCard(method: PaymentMethod, rate: Int, onClick: () -> Unit) {
    val isFixed = method.isFixed()
    val requiredCoins = method.getEffectiveCoins(rate)
    val minCoins = method.getMinCoinsAllowed(rate)
    val maxCoins = method.getMaxCoinsAllowed(rate)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF161626))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MethodIconBadge(method.icon, size = 48)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        method.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (method.badge.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFD500F9).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                method.badge,
                                color = Color(0xFFD500F9),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF651FFF).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            method.category,
                            color = Color(0xFFB388FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isFixed) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (isFixed) "FIXED" else "MIN-MAX",
                            color = if (isFixed) Color(0xFF00E676) else Color(0xFFFF9800),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (method.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            method.description,
                            color = Color(0xFFAAAAB4),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isFixed) "Fixed: $requiredCoins Coins" else "Min: $minCoins - Max: $maxCoins Coins",
                    color = if (isFixed) Color(0xFFFFD54F) else Color(0xFFFF9800),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFAAAAB4)
            )
        }
    }
}

@Composable
fun MethodIconBadge(iconType: String, size: Int = 40) {
    val (bg, iconVector, textEmoji) = when (iconType.lowercase().trim()) {
        "esewa" -> Triple(Color(0xFF60BB46), null, "🇳🇵")
        "fire", "freefire", "diamond", "ff" -> Triple(Color(0xFFFF5722), null, "💎")
        "robux", "roblox" -> Triple(Color(0xFF00C853), null, "🪙")
        "pubg", "pubg_uc", "uc" -> Triple(Color(0xFFFF9800), null, "🎯")
        "blox", "bloxfruit", "fruit" -> Triple(Color(0xFFE91E63), null, "⚔️")
        "play", "giftcard" -> Triple(Color(0xFF00B0FF), null, "🎁")
        "upi" -> Triple(Color(0xFF651FFF), Icons.Filled.FlashOn, null)
        "paytm" -> Triple(Color(0xFF00B0FF), Icons.Filled.AccountBalanceWallet, null)
        else -> Triple(Color(0xFF7C4DFF), Icons.Filled.CreditCard, null)
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        if (textEmoji != null) {
            Text(textEmoji, fontSize = (size * 0.5).sp)
        } else if (iconVector != null) {
            Icon(
                iconVector,
                contentDescription = null,
                tint = bg,
                modifier = Modifier.size((size * 0.55).dp)
            )
        }
    }
}
