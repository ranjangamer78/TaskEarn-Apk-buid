package com.example.ui.screens.social

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
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
import com.example.data.repository.AppConfig
import com.example.data.repository.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SocialMediaViewModel : ViewModel() {
    private val configRepository = ConfigRepository()
    private val _config = MutableStateFlow(AppConfig())
    val config = _config.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.getConfigFlow().collectLatest { cfg ->
                _config.value = cfg
            }
        }
    }
}

data class SocialPlatform(
    val id: String,
    val name: String,
    val subtitle: String,
    val handle: String,
    val brandColor: Color,
    val secondaryColor: Color,
    val iconEmoji: String,
    val iconVector: ImageVector,
    val urlProvider: (AppConfig) -> String,
    val fallbackUrl: String,
    val badge: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMediaScreen(
    onBack: () -> Unit,
    viewModel: SocialMediaViewModel = viewModel()
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()

    fun openUrl(rawUrl: String, fallbackUrl: String) {
        val target = rawUrl.trim().ifEmpty { fallbackUrl.trim() }
        if (target.isEmpty()) {
            Toast.makeText(context, "Link will be available soon!", Toast.LENGTH_SHORT).show()
            return
        }
        val finalUrl = when {
            target.startsWith("http://") || target.startsWith("https://") -> target
            target.all { it.isDigit() || it == '+' } -> "https://wa.me/${target.removePrefix("+")}"
            else -> "https://$target"
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val platforms = listOf(
        SocialPlatform(
            id = "whatsapp",
            name = "WhatsApp Community",
            subtitle = "Official Discussion Group & Fast 1-on-1 Support",
            handle = "@TaskEarnCommunity",
            brandColor = Color(0xFF25D366),
            secondaryColor = Color(0xFF128C7E),
            iconEmoji = "💬",
            iconVector = Icons.Filled.Chat,
            urlProvider = { it.whatsapp_link },
            fallbackUrl = "https://whatsapp.com",
            badge = "24/7 SUPPORT"
        ),
        SocialPlatform(
            id = "youtube",
            name = "YouTube Channel",
            subtitle = "Watch Earning Tutorials, Proofs & Giveaway Videos",
            handle = "@TaskEarnOfficial",
            brandColor = Color(0xFFFF0000),
            secondaryColor = Color(0xFFCC0000),
            iconEmoji = "▶️",
            iconVector = Icons.Filled.PlayCircle,
            urlProvider = { it.youtube_link },
            fallbackUrl = "https://youtube.com",
            badge = "SUBSCRIBE"
        ),
        SocialPlatform(
            id = "tiktok",
            name = "TikTok",
            subtitle = "Daily Short Clips, Gaming Guides & Viral Redeem Tips",
            handle = "@taskearn_official",
            brandColor = Color(0xFF00F2FE),
            secondaryColor = Color(0xFFFE0979),
            iconEmoji = "🎵",
            iconVector = Icons.Filled.Audiotrack,
            urlProvider = { it.tiktok_link },
            fallbackUrl = "https://tiktok.com",
            badge = "FOLLOW"
        ),
        SocialPlatform(
            id = "telegram",
            name = "Telegram Channel",
            subtitle = "Instant Withdrawal Alerts, Daily Secret Promo Codes",
            handle = "@TaskEarnChannel",
            brandColor = Color(0xFF229ED9),
            secondaryColor = Color(0xFF0088CC),
            iconEmoji = "✈️",
            iconVector = Icons.Filled.Send,
            urlProvider = { it.telegram_link },
            fallbackUrl = "https://telegram.org",
            badge = "PROMO CODES"
        ),
        SocialPlatform(
            id = "instagram",
            name = "Instagram",
            subtitle = "Official Page, Winner Announcements & Daily Stories",
            handle = "@taskearn.app",
            brandColor = Color(0xFFE1306C),
            secondaryColor = Color(0xFF833AB4),
            iconEmoji = "📸",
            iconVector = Icons.Filled.CameraAlt,
            urlProvider = { it.instagram_link },
            fallbackUrl = "https://instagram.com",
            badge = "OFFICIAL"
        ),
        SocialPlatform(
            id = "facebook",
            name = "Facebook Page",
            subtitle = "Connect with 50,000+ Active Members & Post Proofs",
            handle = "TaskEarn Community Group",
            brandColor = Color(0xFF1877F2),
            secondaryColor = Color(0xFF0D65D9),
            iconEmoji = "👥",
            iconVector = Icons.Filled.Groups,
            urlProvider = { it.facebook_link },
            fallbackUrl = "https://facebook.com",
            badge = "COMMUNITY"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Social Media",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B0B16)
                )
            )
        },
        containerColor = Color(0xFF0B0B16)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Hero Header Card
            item {
                // Real-time Page Notice Banner from Admin Panel
                com.example.ui.components.PageNoticeBanner(pageId = "social")
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF651FFF), Color(0xFFD500F9), Color(0xFF00E5FF))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌐", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Join Our Social Media",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Stay updated & claim secret codes",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Follow our official channels for daily gift card giveaways, secret redeem codes, instant support, and community discussions!",
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Section Label
            item {
                Text(
                    "OFFICIAL CHANNELS",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }

            // Platform Items
            items(platforms) { platform ->
                val link = platform.urlProvider(config)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171728)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            platform.brandColor.copy(alpha = 0.35f),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { openUrl(link, platform.fallbackUrl) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo / Icon Box
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(platform.brandColor, platform.secondaryColor)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = platform.iconVector,
                                contentDescription = platform.name,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Name and Subtitle
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    platform.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(platform.brandColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        platform.badge,
                                        color = platform.brandColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                platform.subtitle,
                                color = Color(0xFFAAAAB4),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                platform.handle,
                                color = platform.brandColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Open Action Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222238)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open",
                                tint = platform.brandColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Notice
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF121222))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Security,
                            contentDescription = "Security",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Caution: Beware of scammers. TaskEarn admins will never ask for your password or secret keys.",
                            color = Color(0xFFCCCED8),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                com.example.ui.components.BannerAdView()
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
