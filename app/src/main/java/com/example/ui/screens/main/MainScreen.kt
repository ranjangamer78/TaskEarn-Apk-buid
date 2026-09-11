package com.example.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.ui.navigation.Routes
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.wallet.WalletScreen
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.BackgroundDark
import kotlinx.coroutines.launch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Block

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import android.content.Intent
import android.net.Uri
import android.widget.Toast

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home)
    object Earn : BottomNavItem(Routes.EARN, "Earn", Icons.Filled.MonetizationOn)
    object Wallet : BottomNavItem(Routes.WALLET, "Wallet", Icons.Filled.Wallet)
    object History : BottomNavItem(Routes.HISTORY, "History", Icons.Filled.History)
}

@Composable
fun BlockedScreen(reason: String = "", onSignOut: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D18))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1428)),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0x22E53935)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Block, contentDescription = "Blocked", tint = Color(0xFFE53935), modifier = Modifier.size(44.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Account Blocked", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (reason.isNotBlank()) reason else "This account has been suspended for anti-fraud policy violations (only 1 account per device is allowed).",
                    color = Color(0xFFB0B0C0),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Out to Switch Account", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun isVersionNewer(targetVersion: String, currentVersion: String): Boolean {
    if (targetVersion.isBlank() || currentVersion.isBlank()) return false
    try {
        val targetParts = targetVersion.trim().split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val currentParts = currentVersion.trim().split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val maxLen = maxOf(targetParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val target = targetParts.getOrElse(i) { 0 }
            val current = currentParts.getOrElse(i) { 0 }
            if (target > current) return true
            if (target < current) return false
        }
        return false
    } catch (e: Exception) {
        return false
    }
}

@Composable
fun AppUpdateDialog(
    title: String,
    desc: String,
    downloadLink: String,
    isCancelable: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {
            if (isCancelable) onDismiss()
        },
        containerColor = Color(0xFF1A1830),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(PrimaryPurple, Color(0xFF00E5FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = "App Update Logo",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = title.ifBlank { "New Update Available!" },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = desc.ifBlank { "A new version of the app is available with more games, higher coin rewards, and bug fixes. Please update now to continue earning." },
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (downloadLink.isNotBlank()) {
                        com.example.util.FileDownloader.downloadOrOpen(context, downloadLink, title)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update App Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        dismissButton = {
            if (isCancelable) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remind Me Later", color = TextSecondary)
                }
            }
        }
    )
}

@Composable
fun MainScreen(
    onNavigateTo: (String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    var dismissUpdateDialog by rememberSaveable { mutableStateOf(false) }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentUid = user?.uid
    LaunchedEffect(currentUid) {
        if (!currentUid.isNullOrEmpty()) {
            viewModel.checkDeviceAndRecordLogin(context, currentUid)
        }
    }

    val currentVersionName = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.1"
        } catch (e: Exception) {
            "1.0.1"
        }
    }
    val targetVersionName = appConfig.app_update_version_name.ifBlank { "1.0" }
    val isUpdateNeeded = remember(currentVersionName, targetVersionName) {
        isVersionNewer(targetVersion = targetVersionName, currentVersion = currentVersionName)
    }

    LaunchedEffect(appConfig) {
        com.example.util.UnityAdsManager.updateConfig(
            newGameId = appConfig.unity_id,
            newPlacementId = appConfig.unity_placement_id,
            newInterstitialId = appConfig.unity_interstitial_placement,
            newBannerId = appConfig.unity_banner_placement,
            newTestMode = appConfig.unity_test_mode
        )
    }

    if (user?.isBlocked == true) {
        BlockedScreen(
            reason = user?.blockReason ?: "Only one account is permitted per device. Multiple accounts were detected on this device.",
            onSignOut = { viewModel.signOut() }
        )
        return
    }

    if (appConfig.app_update_enabled && isUpdateNeeded && !dismissUpdateDialog && appConfig.app_update_link.isNotBlank()) {
        AppUpdateDialog(
            title = appConfig.app_update_title,
            desc = appConfig.app_update_desc,
            downloadLink = appConfig.app_update_link,
            isCancelable = appConfig.app_update_cancelable,
            onDismiss = { dismissUpdateDialog = true }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle result if needed
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Earn,
        BottomNavItem.Wallet,
        BottomNavItem.History
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF151528),
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    if (user?.photoUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = user?.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF6D00)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user?.name?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = user?.name?.takeIf { it.isNotBlank() } ?: "Guest User",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user?.email?.takeIf { it.isNotBlank() } ?: "guest@example.com",
                        color = Color(0xFFAAAAB4),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DrawerItem(icon = Icons.Filled.Home, title = "Home") {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                    DrawerItem(icon = Icons.Filled.MonetizationOn, title = "Earn Coins") {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Routes.EARN) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                    DrawerItem(icon = Icons.Filled.Wallet, title = "Wallet") {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Routes.WALLET) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                    DrawerItem(icon = Icons.Filled.History, title = "History") {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Routes.HISTORY) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                    DrawerItem(icon = Icons.Filled.GroupAdd, title = "Invite Friends") {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateTo(Routes.REFER_EARN)
                    }
                    DrawerItem(icon = Icons.Filled.Share, title = "Social Media") {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateTo(Routes.SOCIAL_MEDIA)
                    }

                    DrawerItem(icon = Icons.Filled.Settings, title = "Settings") {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateTo(Routes.SETTINGS)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    DrawerItem(icon = Icons.Filled.Logout, title = "Logout", color = Color.Red) {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.signOut()
                        onNavigateTo(Routes.AUTH)
                    }
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0B0B16),
                    contentColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 11.sp) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFD500F9),
                                selectedTextColor = Color(0xFFD500F9),
                                unselectedIconColor = Color(0xFFAAAAB4),
                                unselectedTextColor = Color(0xFFAAAAB4),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding).background(Color(0xFF0B0B16))
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onNavigateTo = { route -> 
                            if (route == Routes.WALLET) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                onNavigateTo(route)
                            }
                        },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        user = user,
                        appConfig = viewModel.appConfig.collectAsStateWithLifecycle().value
                    )
                }
                composable(Routes.EARN) {
                    com.example.ui.screens.earn.EarnScreen(
                        onNavigateTo = { route -> 
                            if (route == Routes.WALLET) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                onNavigateTo(route)
                            }
                        }
                    )
                }
                composable(Routes.WALLET) {
                    WalletScreen(
                        onBack = { navController.navigate(Routes.HOME) },
                        onHistoryClick = { navController.navigate(Routes.HISTORY) }
                    )
                }
                composable(Routes.HISTORY) {
                    com.example.ui.screens.history.HistoryScreen(
                        onBack = { navController.navigate(Routes.HOME) }
                    )
                }
                composable(Routes.LEADERBOARD) {
                    com.example.ui.screens.leaderboard.LeaderboardScreen(
                        onBack = { navController.navigate(Routes.HOME) }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, title: String, color: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
