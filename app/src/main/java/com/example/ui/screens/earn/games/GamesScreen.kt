package com.example.ui.screens.earn.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.Game
import com.example.data.repository.ConfigRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.BannerAdView
import com.example.ui.components.CooldownCard
import com.example.ui.theme.*
import com.example.util.AdManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GamesViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    private val configRepository = ConfigRepository()
    private val prefs = application.getSharedPreferences("games_cooldown_prefs", android.content.Context.MODE_PRIVATE)

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games = _games.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    var gameDailyLimit by mutableIntStateOf(10)
        private set
    var gameCooldownMinutes by mutableIntStateOf(5)
        private set
    var gamesPlayedToday by mutableIntStateOf(0)
        private set
    var lastGamePlayedAt by mutableLongStateOf(0L)
        private set

    private var gamesListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        val cachedTime = prefs.getLong("last_game_played_at", 0L)
        if (cachedTime > 0L) {
            lastGamePlayedAt = cachedTime
        }
        loadGames()
        loadConfigAndUserData()
    }

    fun loadConfigAndUserData() {
        viewModelScope.launch {
            configRepository.getConfigFlow().collect { config ->
                gameDailyLimit = if (config.game_daily_limit > 0) config.game_daily_limit else 10
                gameCooldownMinutes = if (config.game_cooldown_minutes > 0) config.game_cooldown_minutes else 5
            }
        }
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            userRepository.getUser(uid).onSuccess { user ->
                val calCurrent = java.util.Calendar.getInstance()
                calCurrent.timeInMillis = System.currentTimeMillis()
                
                val calLast = java.util.Calendar.getInstance()
                calLast.timeInMillis = user.lastGamePlayedAt
                
                val isSameDay = calCurrent.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR) &&
                                calCurrent.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR)
                
                if (!isSameDay) {
                    userRepository.resetGamesLimit(uid)
                    gamesPlayedToday = 0
                } else {
                    gamesPlayedToday = user.gamesPlayedToday
                }
                
                val cachedTime = prefs.getLong("last_game_played_at", 0L)
                if (user.lastGamePlayedAt == 0L && cachedTime == 0L) {
                    lastGamePlayedAt = 0L
                } else {
                    val activeTime = maxOf(user.lastGamePlayedAt, cachedTime)
                    lastGamePlayedAt = activeTime
                    prefs.edit().putLong("last_game_played_at", activeTime).apply()
                }
            }
        }
    }

    fun recordGamePlayed() {
        val now = System.currentTimeMillis()
        lastGamePlayedAt = now
        prefs.edit().putLong("last_game_played_at", now).apply()
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.recordLastGamePlayedAt(uid, now)
        }
    }

    fun skipCooldown() {
        lastGamePlayedAt = 0L
        prefs.edit().putLong("last_game_played_at", 0L).apply()
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.resetGameCooldown(uid)
        }
    }

    fun loadGames() {
        gamesListener?.remove()
        _isLoading.value = true
        gamesListener = db.collection("games")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (_games.value.isEmpty()) {
                        _games.value = getDefaultGames()
                    }
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val firestoreGames = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Game::class.java)?.copy(id = doc.id)
                    }
                    if (firestoreGames.isNotEmpty()) {
                        _games.value = firestoreGames
                    } else {
                        _games.value = getDefaultGames()
                    }
                }
                _isLoading.value = false
            }
    }

    override fun onCleared() {
        super.onCleared()
        gamesListener?.remove()
    }

    private fun getDefaultGames(): List<Game> {
        return listOf(
            Game(
                id = "default_2048",
                title = "2048 Puzzle",
                description = "Join the numbers and get to the 2048 tile! Fun & brain sharpening.",
                link = "https://play2048.co/",
                coin = 50,
                second = 60,
                icon = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200&auto=format&fit=crop&q=60",
                category = "Puzzle",
                isActive = true
            ),
            Game(
                id = "default_tower",
                title = "Tower Building Stacker",
                description = "Stack the moving blocks to build the tallest skyscraper tower!",
                link = "https://iamkun.github.io/tower_game/",
                coin = 60,
                second = 60,
                icon = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=200&auto=format&fit=crop&q=60",
                category = "Arcade",
                isActive = true
            ),
            Game(
                id = "default_hextris",
                title = "Hextris Hexagon",
                description = "Fast paced puzzle game! Rotate the hexagon to match 3 colors.",
                link = "https://hextris.io/",
                coin = 75,
                second = 75,
                icon = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=200&auto=format&fit=crop&q=60",
                category = "Arcade",
                isActive = true
            ),
            Game(
                id = "default_flappy",
                title = "Flappy Bird Retro",
                description = "Fly through obstacles and beat your highscore in this classic arcade.",
                link = "https://flappybird.io/",
                coin = 40,
                second = 45,
                icon = "https://images.unsplash.com/photo-1534423861386-85a16f5d13fd?w=200&auto=format&fit=crop&q=60",
                category = "Casual",
                isActive = true
            ),
            Game(
                id = "default_tictactoe",
                title = "Tic Tac Toe Pro",
                description = "Play Tic-Tac-Toe against smart AI and earn fast coins!",
                link = "https://playtictactoe.org/",
                coin = 30,
                second = 30,
                icon = "https://images.unsplash.com/photo-1611996575749-79a3a250f948?w=200&auto=format&fit=crop&q=60",
                category = "Strategy",
                isActive = true
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    onBack: () -> Unit,
    onPlayGame: (Game, Boolean) -> Unit,
    viewModel: GamesViewModel = viewModel()
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
    val games by viewModel.games.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isSkipAdLoading by remember { mutableStateOf(false) }
    var selectedGameForCooldownPrompt by remember { mutableStateOf<Game?>(null) }
    var remainingCooldownSeconds by remember { mutableLongStateOf(0L) }

    // Countdown loop: calculates remaining seconds until 5-min cooldown expires
    LaunchedEffect(viewModel.lastGamePlayedAt, viewModel.gameCooldownMinutes) {
        while (true) {
            val cooldownMs = viewModel.gameCooldownMinutes * 60 * 1000L
            val elapsed = System.currentTimeMillis() - viewModel.lastGamePlayedAt
            val diff = cooldownMs - elapsed
            remainingCooldownSeconds = if (diff > 0) (diff + 999) / 1000 else 0L
            delay(1000L)
        }
    }

    val isCooldownActive = remainingCooldownSeconds > 0L

    fun skipCooldownWithAd(onCompleteCallback: (() -> Unit)? = null) {
        if (activity != null && !isSkipAdLoading) {
            isSkipAdLoading = true
            AdManager.showInterstitialAd(
                activity = activity,
                onComplete = {
                    isSkipAdLoading = false
                    viewModel.skipCooldown()
                    remainingCooldownSeconds = 0L
                    Toast.makeText(context, "⚡ Cooldown skipped! All games are now unlocked.", Toast.LENGTH_SHORT).show()
                    onCompleteCallback?.invoke()
                }
            )
        } else {
            viewModel.skipCooldown()
            remainingCooldownSeconds = 0L
            Toast.makeText(context, "⚡ Cooldown skipped! All games are now unlocked.", Toast.LENGTH_SHORT).show()
            onCompleteCallback?.invoke()
        }
    }

    fun handleGameClick(game: Game) {
        if (viewModel.gamesPlayedToday >= viewModel.gameDailyLimit) {
            Toast.makeText(context, "Daily game limit reached! Come back tomorrow.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isCooldownActive) {
            selectedGameForCooldownPrompt = game
        } else {
            viewModel.recordGamePlayed()
            onPlayGame(game, false)
        }
    }

    val categories = listOf("All", "Arcade", "Puzzle", "Casual", "Strategy")

    val filteredGames = remember(games, searchQuery, selectedCategory) {
        games.filter { game ->
            val matchesSearch = game.title.contains(searchQuery, ignoreCase = true) ||
                    game.description.contains(searchQuery, ignoreCase = true)
            val matchesCat = if (selectedCategory == "All") true else game.category.equals(selectedCategory, ignoreCase = true)
            matchesSearch && matchesCat
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🎮 Play & Earn Games", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Play for required seconds to unlock coin rewards",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Banner ad at the bottom of the games screen
            BannerAdView()
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Real-time Page Notice Banner from Admin Panel
            com.example.ui.components.PageNoticeBanner(pageId = "games")

            // Global Cooldown Banner on all games if active
            if (isCooldownActive) {
                CooldownCard(
                    remainingSeconds = remainingCooldownSeconds,
                    onSkipWithAd = { skipCooldownWithAd() },
                    isAdLoading = isSkipAdLoading,
                    activityTitle = "All games"
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search games...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = PrimaryPurple) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = SurfaceDark,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            // Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = null
                    )
                }
            }

            // Games List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            } else if (filteredGames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.SportsEsports,
                            contentDescription = "No Games",
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No games found", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Try searching with another keyword", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Card & Daily Limits
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF4A148C), Color(0xFF7B3FE4))
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.SportsEsports,
                                                contentDescription = "Game",
                                                tint = Color(0xFFFFD54F),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Play Free Games & Win!",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "Daily Limit: ${viewModel.gamesPlayedToday}/${viewModel.gameDailyLimit} Games",
                                                color = Color(0xFFFFD54F),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = {
                                            if (viewModel.gameDailyLimit > 0)
                                                viewModel.gamesPlayedToday.toFloat() / viewModel.gameDailyLimit
                                            else 0f
                                        },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFFFFC107),
                                        trackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }

                    items(filteredGames) { game ->
                        GameItemCard(
                            game = game,
                            isCooldown = isCooldownActive,
                            remainingSeconds = remainingCooldownSeconds,
                            onPlay = { handleGameClick(game) }
                        )
                    }
                }
            }
        }
    }

    // Cooldown Skip Prompt Dialog when clicking a game while on cooldown
    selectedGameForCooldownPrompt?.let { targetGame ->
        val minutes = remainingCooldownSeconds / 60
        val seconds = remainingCooldownSeconds % 60
        val formattedRemaining = String.format("%02d:%02d", minutes, seconds)
        AlertDialog(
            onDismissRequest = { selectedGameForCooldownPrompt = null },
            icon = { Text("⏳", fontSize = 36.sp) },
            title = {
                Text(
                    "Game Cooldown Active ($formattedRemaining)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "All games have a 5-minute cooldown after playing. You can wait $formattedRemaining or watch an Interstitial_Android ad to skip the wait and play '${targetGame.title}' right now!",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2E244D))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "⚡ Watch Interstitial Ad to unlock immediately",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val g = targetGame
                        selectedGameForCooldownPrompt = null
                        skipCooldownWithAd {
                            viewModel.recordGamePlayed()
                            onPlayGame(g, true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Skip", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skip Cooldown with Ad", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedGameForCooldownPrompt = null }) {
                    Text("Wait ($formattedRemaining)", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
fun GameItemCard(
    game: Game,
    isCooldown: Boolean = false,
    remainingSeconds: Long = 0L,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPlay() },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            if (isCooldown) listOf(Color(0xFFFF8F00), Color(0xFFD84315))
                            else listOf(PrimaryPurple, SecondaryTeal)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCooldown) Icons.Filled.Schedule else Icons.Filled.SportsEsports,
                    contentDescription = game.title,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        game.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2E244D))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            game.category,
                            color = Color(0xFFFFD54F),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    game.description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCooldown) {
                        val min = remainingSeconds / 60
                        val sec = remainingSeconds % 60
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFF9800).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "⏳ Cooldown ${String.format("%02d:%02d", min, sec)}",
                                color = Color(0xFFFFB74D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Coin badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFB300).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "🪙 +${game.coin} Coins",
                                color = Color(0xFFFFD54F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Timer badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00BFA5).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "⏱️ ${game.second}s",
                                color = Color(0xFF64FFDA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Play / Skip Button
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCooldown) Color(0xFFFF9800) else PrimaryPurple,
                    contentColor = if (isCooldown) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = if (isCooldown) 10.dp else 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = if (isCooldown) "Skip & Play" else "Play",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isCooldown) "Skip & Play" else "Play",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
