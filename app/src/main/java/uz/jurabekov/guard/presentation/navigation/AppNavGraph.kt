package uz.jurabekov.guard.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.koin.compose.koinInject
import uz.jurabekov.guard.data.preferences.OnboardingPreferences
import uz.jurabekov.guard.domain.repository.AuthRepository
import uz.jurabekov.guard.presentation.main.MainScreen
import uz.jurabekov.guard.presentation.onboarding.OnboardingScreen
import uz.jurabekov.guard.presentation.queue.QueueScreen

/**
 * App'ning asosiy navigation graph'i.
 *
 * Splash router decision tree:
 *  1. onboardingCompleted = false   → Onboarding
 *  2. onboardingCompleted = true:
 *     a. isLoggedIn = true          → Main (drawer + nested sections)
 *     b. isLoggedIn = false         → Queue (guest mode)
 *
 * Back stack qoidalari:
 *  - Splash → har doim popUpTo inclusive
 *  - Onboarding → finish'dan keyin popUpTo inclusive
 *  - Queue → Login success'dan keyin popUpTo inclusive
 *  - Main → root sifatida, back press app'ni yopadi
 *  - Main → Logout: Queue'ga o'tib Main'ni stack'dan tashlaymiz
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashRouter(navController = navController)
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Queue.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Queue.route) {
            QueueScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Queue.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onLogoutCompleted = {
                    // Token va session DataStore'da tozalandi.
                    // Queue (guest) ga qaytamiz.
                    navController.navigate(Screen.Queue.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

/**
 * Splash router — DataStore'lardan ikki signal o'qib, mos route'ga
 * yo'naltiradi.
 *
 * `null` = hali yuklanmagan (DataStore birinchi emission'dan oldin).
 * Ikkalasi non-null bo'lganda yo'naltirish — shu paytgacha
 * CircularProgressIndicator.
 */
@Composable
private fun SplashRouter(navController: NavHostController) {
    val onboardingPrefs = koinInject<OnboardingPreferences>()
    val authRepository = koinInject<AuthRepository>()

    val onboardingCompleted by onboardingPrefs.onboardingCompleted
        .collectAsState(initial = null)
    val isLoggedIn by authRepository.isLoggedIn
        .collectAsState(initial = null)

    LaunchedEffect(onboardingCompleted, isLoggedIn) {
        val ob = onboardingCompleted
        val li = isLoggedIn
        if (ob == null || li == null) return@LaunchedEffect

        val destination = when {
            !ob -> Screen.Onboarding.route
            li -> Screen.Main.route
            else -> Screen.Queue.route
        }

        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
