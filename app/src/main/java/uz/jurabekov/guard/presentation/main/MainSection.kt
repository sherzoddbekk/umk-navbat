package uz.jurabekov.guard.presentation.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import uz.jurabekov.guard.domain.model.Permissions

/**
 * Main screen ichidagi nested navigation sectionlari.
 *
 * **Declarative permission gating**:
 * Drawer'da ko'rsatilishi va navigation ruxsati `requiredPermission` ga
 * bog'liq. Yangi section qo'shganda — faqat shu enumga yangi entry qo'shing,
 * boshqa joylar avtomatik update bo'ladi (drawer rendering, NavHost
 * composables, default start destination).
 *
 * **Tartib** — foydalanuvchi xohlagani bo'yicha:
 *   1. Navbat
 *   2. Dashboard
 *   3. Kameralar
 *   4. Tarozi
 *
 * **Route naming convention**: lowercase, dash-separated. Compose Navigation
 * type-safe routes (Kotlin 2.1 / Nav 2.8+) o'rniga string routes ishlatamiz —
 * loyihada hozirgi pattern shu, konsistentlik uchun.
 */
enum class MainSection(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val requiredPermission: String
) {
    QUEUE(
        route = "main/queue",
        title = "Navbat",
        icon = Icons.AutoMirrored.Filled.List,
        requiredPermission = Permissions.QUEUE_VIEW
    ),
    DASHBOARD(
        route = "main/dashboard",
        title = "Dashboard",
        icon = Icons.Default.Dashboard,
        requiredPermission = Permissions.DASHBOARD_VIEW
    ),
    CAMERA(
        route = "main/camera",
        title = "Kameralar",
        icon = Icons.Default.Videocam,
        requiredPermission = Permissions.CAMERA_VIEW
    ),
    SCALE(
        route = "main/scale",
        title = "Tarozi",
        icon = Icons.Default.Scale,
        requiredPermission = Permissions.SCALE_VIEW
    );

    companion object {
        /**
         * Foydalanuvchi permissions'iga qarab ko'rinadigan sectionlar.
         *
         * Performance: enum.values() har chaqiruvda yangi array yaratadi
         * (Kotlin 1.9+'da `entries` cache qiladi). Kompil-time stabil
         * uchun `entries` ishlatamiz.
         */
        fun availableFor(permissions: List<String>): List<MainSection> =
            entries.filter { it.requiredPermission in permissions }

        /**
         * Route'dan section topish — top bar title rendering uchun.
         * `null` qaytsa — unknown route (recover: birinchi mavjud).
         */
        fun fromRoute(route: String?): MainSection? =
            entries.firstOrNull { it.route == route }
    }
}
