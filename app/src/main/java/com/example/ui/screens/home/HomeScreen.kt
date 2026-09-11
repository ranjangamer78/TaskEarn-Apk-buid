package com.example.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.example.data.model.Banner
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.data.repository.AppConfig
import com.example.ui.theme.*

val AppBackground = Color(0xFF0B0B16)
val CardBackground = Color(0xFF151528)
val CardBorder = Color(0xFF2A2A4A)
val PremiumPurple = Color(0xFF7B1FA2)
val PremiumBlue = Color(0xFF1976D2)
val PremiumGold = Color(0xFFFFC107)


class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    var banners by mutableStateOf<List<Banner>>(emptyList())
        private set
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadBanners()
    }

    private fun loadBanners() {
        listenerRegistration?.remove()
        listenerRegistration = db.collection("banners")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    banners = snapshot.documents.mapNotNull { 
                        val b = it.toObject(Banner::class.java)
                        if (b != null) b.copy(id = it.id) else null
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateTo: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    user: User?,
    appConfig: AppConfig? = null,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Menu,
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onOpenDrawer() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            if (user?.photoUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF6D00)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.name?.firstOrNull()?.toString()?.uppercase() ?: "U",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Hello, ${user?.name?.takeIf { it.isNotBlank() }?.substringBefore(" ") ?: "User"} \uD83D\uDC4B",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Welcome Back!",
                    color = Color(0xFFAAAAB4),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = Color.Red,
                        contentColor = Color.White,
                        modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                    ) {
                        Text("3", fontSize = 10.sp)
                    }
                },
                modifier = Modifier
                    .clickable { onNavigateTo(com.example.ui.navigation.Routes.NOTIFICATIONS) }
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }

        // Real-time Page Notice Banner from Admin Panel
        com.example.ui.components.PageNoticeBanner(pageId = "home")

        Spacer(modifier = Modifier.height(16.dp))

        // Balance Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF2C1558), Color(0xFF140D36)))
                )
                .border(1.dp, Color(0xFF3E2075), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Your Balance", color = Color(0xFFAAAAB4), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = "Coin", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${user?.balance ?: 0}", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val rateVal = if ((appConfig?.rate ?: 100) > 0) (appConfig?.rate ?: 100).toDouble() else 100.0
                    val rupeesVal = (user?.balance ?: 0) / rateVal
                    Text("≈ ₹${String.format(java.util.Locale.US, "%.2f", rupeesVal)} (1,000 Coins = ₹10)", color = Color(0xFFAAAAB4), fontSize = 13.sp)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = Color(0xFF7C4DFF),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onNavigateTo(com.example.ui.navigation.Routes.WALLET) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Wallet  >", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Banners Slider
        if (viewModel.banners.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { viewModel.banners.size })
            
            LaunchedEffect(pagerState) {
                while (true) {
                    delay(3000)
                    if (pagerState.pageCount > 0) {
                        pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
                    }
                }
            }
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) { page ->
                val banner = viewModel.banners[page]
                Box(
                    modifier = Modifier.fillMaxSize().background(CardBorder)
                ) {
                    AsyncImage(
                        model = banner.imageUrl,
                        contentDescription = "Banner",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (!banner.link.isNullOrEmpty()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.link))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(viewModel.banners.size) { index ->
                    val color = if (pagerState.currentPage == index) PremiumGold else Color.Gray
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(color)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2C4E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Assignment, contentDescription = "Tasks", tint = Color(0xFFAAAAB4), modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Complete Tasks", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Earn Coins", color = PremiumGold, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Finish tasks and earn exciting\nrewards", color = Color(0xFFAAAAB4), fontSize = 11.sp, lineHeight = 14.sp)
                }
                
                Button(
                    onClick = { onNavigateTo(com.example.ui.navigation.Routes.MORE_TASKS) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF651FFF)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Start Now  >", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Streak
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTo(com.example.ui.navigation.Routes.DAILY_BONUS) }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text("Daily Streak \uD83D\uDD25", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Claim daily & earn more", color = Color(0xFFAAAAB4), fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = "Star", tint = PremiumGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${user?.streak ?: 0}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..5).forEach { day ->
                val isActive = day == ((user?.streak ?: 0) % 5) + 1
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) Color(0xFF2C1558) else CardBackground)
                        .border(1.dp, if (isActive) Color(0xFF651FFF) else CardBorder, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp)
                ) {
                    Text("Day $day", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(PremiumGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = "Coin", tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${day * 100}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Access
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quick Access", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("View All >", color = Color(0xFF7C4DFF), fontSize = 14.sp, modifier = Modifier.clickable {})
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(220.dp),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val items = listOf(
                Triple("Daily Bonus", Icons.Filled.CardGiftcard, listOf(Color(0xFF6A1B9A), Color(0xFF4A148C))),
                Triple("Spin Wheel", Icons.Filled.Adjust, listOf(Color(0xFFD81B60), Color(0xFFAD1457))),
                Triple("Watch & Earn", Icons.Filled.PlayArrow, listOf(Color(0xFF1976D2), Color(0xFF0D47A1))),
                Triple("Scratch Card", Icons.Filled.Style, listOf(Color(0xFFF57C00), Color(0xFFE65100))),
                Triple("Refer & Earn", Icons.Filled.GroupAdd, listOf(Color(0xFF00695C), Color(0xFF004D40))),
                Triple("More Tasks", Icons.Filled.ListAlt, listOf(Color(0xFF0277BD), Color(0xFF01579B))),
                Triple("Leaderboard", Icons.Filled.EmojiEvents, listOf(Color(0xFFF57F17), Color(0xFFF57F17))),
                Triple("Offers", Icons.Filled.LocalOffer, listOf(Color(0xFF673AB7), Color(0xFF4527A0)))
            )

            items(items) { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { 
                        val route = when(item.first) {
                            "Daily Bonus" -> com.example.ui.navigation.Routes.DAILY_BONUS
                            "Spin Wheel" -> com.example.ui.navigation.Routes.SPIN_WHEEL
                            "Watch & Earn" -> com.example.ui.navigation.Routes.WATCH_EARN
                            "Scratch Card" -> com.example.ui.navigation.Routes.SCRATCH_CARD
                            "Refer & Earn" -> com.example.ui.navigation.Routes.REFER_EARN
                            "More Tasks" -> com.example.ui.navigation.Routes.MORE_TASKS
                            "Leaderboard" -> com.example.ui.navigation.Routes.LEADERBOARD
                            "Offers" -> com.example.ui.navigation.Routes.MORE_TASKS
                            else -> com.example.ui.navigation.Routes.MORE_TASKS
                        }
                        onNavigateTo(route) 
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.verticalGradient(colors = item.third)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.second, contentDescription = item.first, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.first,
                        color = Color.White,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Invite Friends Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF2C1558), Color(0xFF140D36)))
                )
                .border(1.dp, Color(0xFF3E2075), RoundedCornerShape(20.dp))
                .padding(20.dp)
                .clickable { onNavigateTo(com.example.ui.navigation.Routes.REFER_EARN) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Invite Friends &", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Earn More Coins!", color = PremiumGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Get bonus on every referral", color = Color(0xFFAAAAB4), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onNavigateTo(com.example.ui.navigation.Routes.REFER_EARN) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF651FFF)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Invite Now  >", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // Illustration placeholder
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CardGiftcard, contentDescription = "Gift", tint = Color(0xFF7C4DFF), modifier = Modifier.size(64.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Banner Ad
        com.example.ui.components.BannerAdView()

        Spacer(modifier = Modifier.height(32.dp))
    }
}
