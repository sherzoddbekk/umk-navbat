package uz.jurabekov.guard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import org.koin.android.ext.android.inject
import uz.jurabekov.guard.core.update.AppUpdateChecker
import uz.jurabekov.guard.data.remote.websocket.QueuePusherClient
import uz.jurabekov.guard.presentation.navigation.AppNavGraph
import uz.jurabekov.guard.presentation.update.FlexibleUpdateReadyBar
import uz.jurabekov.guard.ui.theme.GuardTheme

/**
 * Main entrypoint.
 *
 * v1.0 SCOPE:
 *  - Foreground service va system notification YO'Q.
 *  - Real-time UI yangilanishi - faqat app foreground'da bo'lganda
 *    QueuePusherClient orqali ViewModel'ga yetadi.
 *
 * v1.1 PLAN: FCM (Firebase Cloud Messaging) qo'shilganda:
 *   - POST_NOTIFICATIONS permission so'rash bu yerga qaytariladi.
 *   - FCM token registratsiyasi MainActivity yoki Application'da bo'ladi.
 *   - System notification - FCM'ning o'z service'i orqali (FOREGROUND_SERVICE
 *     kerak emas, FCM data message → local notification pattern).
 */
class MainActivity : ComponentActivity() {

    private val pusherClient: QueuePusherClient by inject()

    private lateinit var appUpdateChecker: AppUpdateChecker

    /** FLEXIBLE update download tugaganda → snackbar ko'rsatish flag'i. */
    private var showFlexibleUpdateReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )

        // ── Update tekshiruvi (Google In-App Updates API) ──────
        // Birinchi ishga tushadi - agar update kerak bo'lsa,
        // Google'ning full-screen UI'i app'ni egallaydi.
        appUpdateChecker = AppUpdateChecker(
            activity = this,
            onFlexibleUpdateReady = { showFlexibleUpdateReady = true },
            onImmediateUpdateCancelled = {
                // IMMEDIATE update'ni rad etgan user uchun majburiy yopish.
                // Keyingi safar ochganida yana shu flow takrorlanadi.
                finishAffinity()
            }
        )
        appUpdateChecker.checkForUpdate()

        // ── WebSocket connect (UI real-time yangilanishi uchun) ────
        // App foreground'da bo'lganda WS event'lari ViewModel'ga yetadi.
        // Background'da OS WS'ni yopadi - bu v1.0 uchun maqbul (no notification).
        pusherClient.connect()

        // ── UI ─────────────────────────────────────────────────
        setContent {
            GuardTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController)

                    // FLEXIBLE update tayyor bo'lsa ustidan snackbar
                    if (showFlexibleUpdateReady) {
                        FlexibleUpdateReadyBar(
                            onRestartClick = {
                                appUpdateChecker.completeFlexibleUpdate()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // IMMEDIATE update yarim qoldi bo'lsa - tiklaymiz.
        // FLEXIBLE update download tugagan bo'lsa - snackbar tiklanadi.
        appUpdateChecker.resumeUpdateIfInProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateChecker.cleanup()
    }
}
