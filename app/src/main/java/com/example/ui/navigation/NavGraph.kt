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

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onGetStarted = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onAlreadyLoggedIn = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
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
            MainScreen(
                onNavigateTo = { route ->
                    navController.navigate(route)
                }
            )
        }
        composable(Routes.SPIN_WHEEL) {
            SpinWheelScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SCRATCH_CARD) {
            ScratchCardScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DAILY_BONUS) {
            DailyBonusScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.WATCH_EARN) {
            WatchEarnScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MORE_TASKS) {
            MoreTasksScreen(
                onBack = { navController.popBackStack() },
                onOfferClick = { url ->
                    navController.navigate("${Routes.OFFER_WEB}?url=$url")
                }
            )
        }
        composable(Routes.REFER_EARN) {
            ReferEarnScreen(onBack = { navController.popBackStack() })
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
        composable(Routes.REDEEM_COUPON) {
            RedeemCouponScreen(onBack = { navController.popBackStack() })
        }
    }
}
