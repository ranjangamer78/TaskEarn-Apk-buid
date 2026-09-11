package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.earn.dailybonus.DailyBonusScreen
import com.example.ui.screens.earn.moretasks.MoreTasksScreen
import com.example.ui.screens.earn.referearn.ReferEarnScreen
import com.example.ui.screens.earn.scratchcard.ScratchCardScreen
import com.example.ui.screens.earn.spinwheel.SpinWheelScreen
import com.example.ui.screens.earn.watchearn.WatchEarnScreen
import com.example.ui.screens.earn.coupon.RedeemCouponScreen
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.main.MainScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.contact.ContactScreen
import com.example.ui.screens.social.SocialMediaScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val startDestination = if (auth.currentUser != null) Routes.MAIN else Routes.AUTH

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.AUTH) {
            AuthScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            // MainScreen manages its own bottom nav, but we can pass navController 
            // if we need to navigate out to top-level screens.
            MainScreen(
                onNavigateTo = { route ->
                    navController.navigate(route)
                }
            )
        }
        composable(Routes.DAILY_BONUS) {
            DailyBonusScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SPIN_WHEEL) {
            SpinWheelScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.WATCH_EARN) {
            WatchEarnScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SCRATCH_CARD) {
            ScratchCardScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REFER_EARN) {
            ReferEarnScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REDEEM_COUPON) {
            RedeemCouponScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.GAMES) {
            val context = androidx.compose.ui.platform.LocalContext.current
            com.example.ui.screens.earn.games.GamesScreen(
                onBack = { navController.popBackStack() },
                onPlayGame = { game, skippedCooldownWithAd ->
                    val encUrl = try { java.net.URLEncoder.encode(game.link, "UTF-8") } catch(e: Exception) { "" }
                    val encTitle = try { java.net.URLEncoder.encode(game.title, "UTF-8") } catch(e: Exception) { "Game" }
                    val launchGame = {
                        navController.navigate(
                            "${Routes.GAME_PLAY}?gameId=${game.id}&title=$encTitle&url=$encUrl&coin=${game.coin}&second=${game.second}"
                        )
                    }
                    if (skippedCooldownWithAd) {
                        launchGame()
                    } else {
                        var act: android.app.Activity? = null
                        var curCtx: android.content.Context? = context
                        while (curCtx is android.content.ContextWrapper) {
                            if (curCtx is android.app.Activity) {
                                act = curCtx
                                break
                            }
                            curCtx = curCtx.baseContext
                        }
                        if (act != null) {
                            com.example.util.AdManager.showInterstitialAd(
                                activity = act,
                                onComplete = launchGame
                            )
                        } else {
                            launchGame()
                        }
                    }
                }
            )
        }
        composable(
            route = "${Routes.GAME_PLAY}?gameId={gameId}&title={title}&url={url}&coin={coin}&second={second}",
            arguments = listOf(
                androidx.navigation.navArgument("gameId") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType; defaultValue = "Game" },
                androidx.navigation.navArgument("url") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                androidx.navigation.navArgument("coin") { type = androidx.navigation.NavType.IntType; defaultValue = 50 },
                androidx.navigation.navArgument("second") { type = androidx.navigation.NavType.IntType; defaultValue = 60 }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            val rawTitle = backStackEntry.arguments?.getString("title") ?: "Game"
            val title = try { java.net.URLDecoder.decode(rawTitle, "UTF-8") } catch(e: Exception) { rawTitle }
            val rawUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = try { java.net.URLDecoder.decode(rawUrl, "UTF-8") } catch(e: Exception) { rawUrl }
            val coin = backStackEntry.arguments?.getInt("coin") ?: 50
            val second = backStackEntry.arguments?.getInt("second") ?: 60

            com.example.ui.screens.earn.games.GamePlayScreen(
                gameId = gameId,
                title = title,
                url = url,
                coins = coin,
                seconds = second,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.MORE_TASKS) {
            MoreTasksScreen(
                onBack = { navController.popBackStack() },
                onOfferClick = { offer ->
                    val encUrl = try { java.net.URLEncoder.encode(offer.url, "UTF-8") } catch(e: Exception) { "" }
                    val encTitle = try { java.net.URLEncoder.encode(offer.title, "UTF-8") } catch(e: Exception) { "Task" }
                    navController.navigate(
                        "${Routes.OFFER_WEB}?offerId=${offer.id}&url=$encUrl&timer=${offer.timer}&reward=${offer.reward}&title=$encTitle&frequency=${offer.frequency}&maxCompletions=${offer.maxCompletions}"
                    )
                }
            )
        }
        
        composable(
            route = "${Routes.OFFER_WEB}?offerId={offerId}&url={url}&timer={timer}&reward={reward}&title={title}&frequency={frequency}&maxCompletions={maxCompletions}",
            arguments = listOf(
                androidx.navigation.navArgument("offerId") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                androidx.navigation.navArgument("url") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                androidx.navigation.navArgument("timer") { type = androidx.navigation.NavType.IntType; defaultValue = 0 },
                androidx.navigation.navArgument("reward") { type = androidx.navigation.NavType.IntType; defaultValue = 0 },
                androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType; defaultValue = "Task" },
                androidx.navigation.navArgument("frequency") { type = androidx.navigation.NavType.StringType; defaultValue = "ONCE" },
                androidx.navigation.navArgument("maxCompletions") { type = androidx.navigation.NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val offerId = backStackEntry.arguments?.getString("offerId") ?: ""
            val rawUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = try { java.net.URLDecoder.decode(rawUrl, "UTF-8") } catch(e: Exception) { rawUrl }
            val timer = backStackEntry.arguments?.getInt("timer") ?: 0
            val reward = backStackEntry.arguments?.getInt("reward") ?: 0
            val rawTitle = backStackEntry.arguments?.getString("title") ?: "Task"
            val title = try { java.net.URLDecoder.decode(rawTitle, "UTF-8") } catch(e: Exception) { rawTitle }
            val frequency = backStackEntry.arguments?.getString("frequency") ?: "ONCE"
            val maxCompletions = backStackEntry.arguments?.getInt("maxCompletions") ?: 1

            com.example.ui.screens.earn.moretasks.OfferWebScreen(
                offerId = offerId,
                url = url,
                timer = timer,
                reward = reward,
                title = title,
                frequency = frequency,
                maxCompletions = maxCompletions,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateTo = { route -> navController.navigate(route) }
            )
        }
        composable(Routes.CONTACT) {
            ContactScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SOCIAL_MEDIA) {
            SocialMediaScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_PANEL) {
            com.example.ui.screens.admin.AdminPanelScreen(onBack = { navController.popBackStack() })
        }
    }
}
