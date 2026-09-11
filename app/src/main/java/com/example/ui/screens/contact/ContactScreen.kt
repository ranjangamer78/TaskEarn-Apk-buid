package com.example.ui.screens.contact

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

class ContactViewModel : ViewModel() {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    onBack: () -> Unit,
    viewModel: ContactViewModel = viewModel()
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()

    fun openUrl(rawUrl: String, fallbackMsg: String) {
        val url = rawUrl.trim()
        if (url.isEmpty()) {
            Toast.makeText(context, fallbackMsg, Toast.LENGTH_SHORT).show()
            return
        }
        val finalUrl = when {
            url.startsWith("http://") || url.startsWith("https://") || url.startsWith("mailto:") -> url
            url.contains("@") && !url.contains("/") -> "mailto:$url"
            url.all { it.isDigit() || it == '+' } -> "https://wa.me/${url.removePrefix("+")}"
            else -> "https://$url"
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Contact & Support",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0B16))
            )
        },
        containerColor = Color(0xFF0B0B16)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Real-time Page Notice Banner from Admin Panel
                com.example.ui.components.PageNoticeBanner(pageId = "contact")
                Spacer(modifier = Modifier.height(8.dp))
                // Hero Header Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF651FFF), Color(0xFF00E5FF))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.SupportAgent,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "We're Here to Help!",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Official Support & Community Channels",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Have questions about withdrawals, rewards, or tasks? Connect directly with us on our official platforms below!",
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // WhatsApp Card
            item {
                ContactChannelCard(
                    title = "WhatsApp",
                    subtitle = "Direct chat & instant support",
                    badge = "ACTIVE",
                    badgeColor = Color(0xFF25D366),
                    iconEmoji = "💬",
                    accentColor = Color(0xFF25D366),
                    actionLabel = "Chat on WhatsApp",
                    onClick = {
                        openUrl(
                            config.whatsapp_link,
                            "WhatsApp link is being configured in Admin Panel"
                        )
                    }
                )
            }

            // YouTube Card
            item {
                ContactChannelCard(
                    title = "YouTube",
                    subtitle = "Tutorials, promo codes & payment proofs",
                    badge = "OFFICIAL",
                    badgeColor = Color(0xFFFF0000),
                    iconEmoji = "▶️",
                    accentColor = Color(0xFFFF0000),
                    actionLabel = "Subscribe on YouTube",
                    onClick = {
                        openUrl(
                            config.youtube_link,
                            "YouTube link is being configured in Admin Panel"
                        )
                    }
                )
            }

            // TikTok Card
            item {
                ContactChannelCard(
                    title = "TikTok",
                    subtitle = "Quick tips, viral updates & giveaways",
                    badge = "COMMUNITY",
                    badgeColor = Color(0xFF00F2FE),
                    iconEmoji = "🎵",
                    accentColor = Color(0xFF4FACFE),
                    actionLabel = "Follow on TikTok",
                    onClick = {
                        openUrl(
                            config.tiktok_link,
                            "TikTok link is being configured in Admin Panel"
                        )
                    }
                )
            }

            // Telegram Card
            item {
                ContactChannelCard(
                    title = "Telegram",
                    subtitle = "Daily secret coupon drops & announcements",
                    badge = "DAILY CODES",
                    badgeColor = Color(0xFF0088CC),
                    iconEmoji = "✈️",
                    accentColor = Color(0xFF0088CC),
                    actionLabel = "Join Telegram Channel",
                    onClick = {
                        openUrl(
                            config.telegram_link,
                            "Telegram channel link is being configured in Admin Panel"
                        )
                    }
                )
            }

            // Instagram Card
            item {
                ContactChannelCard(
                    title = "Instagram",
                    subtitle = "Follow for winner announcements & events",
                    badge = "UPDATES",
                    badgeColor = Color(0xFFE1306C),
                    iconEmoji = "📸",
                    accentColor = Color(0xFFE1306C),
                    actionLabel = "Follow on Instagram",
                    onClick = {
                        openUrl(
                            config.instagram_link,
                            "Instagram link is being configured in Admin Panel"
                        )
                    }
                )
            }

            // Email Support Card
            item {
                val email = config.support_email.ifEmpty { "support@taskearn.app" }
                ContactChannelCard(
                    title = "Email Support",
                    subtitle = email,
                    badge = "24/7",
                    badgeColor = Color(0xFFFFB300),
                    iconEmoji = "✉️",
                    accentColor = Color(0xFFFFB300),
                    actionLabel = "Send Support Email",
                    onClick = {
                        openUrl(
                            "mailto:$email",
                            "Opening email client..."
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                com.example.ui.components.BannerAdView()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ContactChannelCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    iconEmoji: String,
    accentColor: Color,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161626)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.18f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iconEmoji, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                badge,
                                color = badgeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        subtitle,
                        color = Color(0xFFAAAAB4),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFAAAAB4)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor.copy(alpha = 0.22f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(actionLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
