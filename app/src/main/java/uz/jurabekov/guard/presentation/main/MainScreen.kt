package uz.jurabekov.guard.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import uz.jurabekov.guard.presentation.main.components.AppDrawerContent
import uz.jurabekov.guard.presentation.main.components.MainTopBar
import uz.jurabekov.guard.presentation.main.components.NoAccessSection
import uz.jurabekov.guard.presentation.main.components.PlaceholderSection
import uz.jurabekov.guard.presentation.queue_management.QueueManagementScreen
import uz.jurabekov.guard.presentation.scale.ScaleScreen

/**
 * Authenticated foydalanuvchilar uchun asosiy oyna.
 *
 * **Arxitektura:**
 * ```
 * MainScreen
 * └── ModalNavigationDrawer
 *     ├── drawerContent: AppDrawerContent (logo + menulari + user + logout)
 *     └── content: Scaffold
 *         ├── topBar: MainTopBar (menu icon + section title)
 *         └── content: NavHost (nested)
 *             ├── Navbat       — QueueManagementScreen (sana picker + tabs +
 *             │                  permit dialog QR bilan)
 *             ├── Dashboard    — PlaceholderSection
 *             ├── Kameralar    — PlaceholderSection
 *             └── Tarozi       — ScaleScreen (to'liq implement)
 * ```
 *
 * **Permission-based start destination:**
 * Foydalanuvchining mavjud `availableSections` ro'yxatidan birinchi entry
 * default ko'rsatiladi. Tartib: Navbat → Dashboard → Kameralar → Tarozi.
 * Hech qaysiga ruxsat yo'q bo'lsa — `NoAccessSection`.
 *
 * **Drawer state:**
 * `DrawerValue.Closed` boshlanadi. `rememberDrawerState` configuration
 * change'larda saqlanadi.
 *
 * **Navigation hygiene:**
 * - `launchSingleTop = true` — bir xil section qayta tanlansa stack o'smaydi
 * - `popUpTo(startRoute) { saveState = true }` — back stack tozaligi
 * - `restoreState = true` — section'ga qaytib kelganda holat tiklanadi
 *
 * **Logout flow:**
 * `vm.logout { onLogoutCompleted() }` — `clear()` tugagandan keyin parent
 * navigation callback. Bu MainScreen'ni Queue'ga yo'naltiradi.
 */
@Composable
fun MainScreen(
    onLogoutCompleted: () -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // === Loading: user ma'lumotlari hali kelmagan (DataStore o'qib bo'lmadi) ===
    if (state.isLoading || state.user == null) {
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
        return
    }

    // === No permissions edge case ===
    if (state.availableSections.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar bo'lsa ham — menu drawer'da faqat logout
            NoAccessSection()
        }
        return
    }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val currentSection = remember(currentRoute, state.availableSections) {
        MainSection.fromRoute(currentRoute) ?: state.availableSections.first()
    }

    val startRoute = remember(state.availableSections) {
        state.availableSections.first().route
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            AppDrawerContent(
                user = state.user,
                availableSections = state.availableSections,
                currentRoute = currentRoute,
                onSectionClick = { section ->
                    scope.launch { drawerState.close() }
                    if (section.route != currentRoute) {
                        navController.navigate(section.route) {
                            popUpTo(startRoute) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onLogoutClick = {
                    scope.launch { drawerState.close() }
                    viewModel.logout {
                        onLogoutCompleted()
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                MainTopBar(
                    title = currentSection.title,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(MainSection.QUEUE.route) {
                    // Navbat boshqaruvi: sana picker + Jami counter + tabs +
                    // unified list (entered + banner + waiting, hammasi
                    // clickable → PermitDialog ochiladi). VM Koin'dan injec
                    // (factory scope: bu NavBackStackEntry uchun yangi instance,
                    // saveState bilan navigatsiya orqali state saqlanadi).
                    QueueManagementScreen()
                }
                composable(MainSection.DASHBOARD.route) {
                    PlaceholderSection(
                        title = "Dashboard",
                        icon = Icons.Outlined.Dashboard
                    )
                }
                composable(MainSection.CAMERA.route) {
                    PlaceholderSection(
                        title = "Kameralar",
                        icon = Icons.Outlined.Videocam
                    )
                }
                composable(MainSection.SCALE.route) {
                    ScaleScreen()
                }
            }
        }
    }
}
