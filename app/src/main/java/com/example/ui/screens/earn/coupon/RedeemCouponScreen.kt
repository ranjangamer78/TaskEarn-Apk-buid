package com.example.ui.screens.earn.coupon

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Coupon
import com.example.data.repository.ConfigRepository
import com.example.data.repository.UserRepository
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RedeemCouponViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val configRepository = ConfigRepository()
    private val auth = FirebaseAuth.getInstance()

    var couponInput by mutableStateOf("")
    var isRedeeming by mutableStateOf(false)
        private set

    var successReward by mutableStateOf<Int?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    private val _activeCoupons = MutableStateFlow<List<Coupon>>(emptyList())
    val activeCoupons = _activeCoupons.asStateFlow()

    var telegramLink by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            configRepository.getConfigFlow().collectLatest { config ->
                telegramLink = config.telegram_link
            }
        }
        viewModelScope.launch {
            userRepository.getActiveCouponsFlow().collectLatest { list ->
                _activeCoupons.value = list
            }
        }
    }

    fun onCodeChange(newCode: String) {
        couponInput = newCode.uppercase().filter { it.isLetterOrDigit() }
        errorMessage = null
    }

    fun redeem(onSuccess: (Int) -> Unit) {
        val code = couponInput.trim()
        if (code.isEmpty()) {
            errorMessage = "Please enter a coupon code"
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage = "Please sign in to redeem coupons"
            return
        }

        isRedeeming = true
        errorMessage = null

        viewModelScope.launch {
            val result = userRepository.redeemCoupon(
                uid = currentUser.uid,
                userEmail = currentUser.email ?: "",
                rawCode = code
            )
            isRedeeming = false
            result.fold(
                onSuccess = { reward ->
                    successReward = reward
                    couponInput = ""
                    onSuccess(reward)
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "Failed to redeem coupon"
                }
            )
        }
    }

    fun dismissSuccess() {
        successReward = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemCouponScreen(
    onBack: () -> Unit,
    viewModel: RedeemCouponViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activeCoupons by viewModel.activeCoupons.collectAsState()

    // Success dialog with animated scale
    viewModel.successReward?.let { reward ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccess() },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissSuccess() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentYellow,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Great! Keep Earning", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AccentYellow.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CardGiftcard,
                            contentDescription = "Reward",
                            tint = AccentYellow,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Code Redeemed!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "+$reward Coins",
                        color = AccentYellow,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "The coins have been added to your wallet successfully! You can now use them to withdraw gift cards or cash.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Redeem Coupon", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        bottomBar = {
            com.example.ui.components.BannerAdView()
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // Real-time Page Notice Banner from Admin Panel
                com.example.ui.components.PageNoticeBanner(pageId = "coupon")
            }
            // Hero Voucher Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF6A11CB),
                                    Color(0xFF2575FC)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.ConfirmationNumber,
                                contentDescription = "Coupon Ticket",
                                tint = AccentYellow,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            "Redeem Secret Coupon",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            "Enter promo codes distributed in our community or streams to claim free instant coin rewards!",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Input Form Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Enter Coupon Code",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = viewModel.couponInput,
                            onValueChange = { viewModel.onCodeChange(it) },
                            placeholder = {
                                Text("e.g. WELCOME100, BONUS500", color = TextSecondary)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.LocalOffer,
                                    contentDescription = "Promo Code",
                                    tint = AccentYellow
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (viewModel.couponInput.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onCodeChange("") }) {
                                            Icon(
                                                Icons.Filled.Clear,
                                                contentDescription = "Clear",
                                                tint = TextSecondary
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = clipboard.primaryClip
                                            if (clip != null && clip.itemCount > 0) {
                                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                                if (text.isNotBlank()) {
                                                    viewModel.onCodeChange(text)
                                                }
                                            }
                                        }
                                    ) {
                                        Text("PASTE", color = AccentYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E1E2C),
                                unfocusedContainerColor = Color(0xFF1E1E2C),
                                focusedBorderColor = AccentYellow,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Error message
                        AnimatedVisibility(visible = viewModel.errorMessage != null) {
                            viewModel.errorMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SecondaryRed.copy(alpha = 0.15f))
                                        .padding(10.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = SecondaryRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        msg,
                                        color = SecondaryRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                viewModel.redeem { reward ->
                                    val act = context as? android.app.Activity
                                    if (act != null) {
                                        com.example.util.AdManager.showInterstitialAd(act) {
                                            Toast.makeText(context, "Successfully redeemed +$reward coins!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Successfully redeemed +$reward coins!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !viewModel.isRedeeming && viewModel.couponInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentYellow,
                                contentColor = Color.Black,
                                disabledContainerColor = AccentYellow.copy(alpha = 0.4f),
                                disabledContentColor = Color.Black.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (viewModel.isRedeeming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Filled.ConfirmationNumber,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Redeem Coupon Code",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Tips & Community Channels
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Info",
                                tint = SecondaryTeal,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "How to get Coupon Codes?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "• New coupon codes are released daily on our official Telegram and YouTube channels.\n" +
                            "• Each coupon code can be redeemed once per account.\n" +
                            "• Codes may have limited redemption caps, so claim them as soon as they drop!",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        if (viewModel.telegramLink.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(viewModel.telegramLink)
                                    )
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryTeal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Send,
                                    contentDescription = "Telegram",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Join Telegram For Daily Codes", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CouponItemCard(
    coupon: Coupon,
    onUseCode: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentYellow.copy(alpha = 0.15f))
                            .border(1.dp, AccentYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            coupon.code,
                            color = AccentYellow,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        "+${coupon.reward} Coins",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (coupon.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        coupon.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }

                if (coupon.maxUses > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Claims: ${coupon.usedCount} / ${coupon.maxUses}",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(coupon.code))
                    onUseCode(coupon.code)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPurple,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text("APPLY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
