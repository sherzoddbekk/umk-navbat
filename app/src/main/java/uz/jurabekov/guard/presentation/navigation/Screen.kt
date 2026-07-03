package uz.jurabekov.guard.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")

    /** Guest landing — Kirish tugmasi bilan login dialog ochish mumkin. */
    data object Queue : Screen("queue")

    /** Authenticated landing — token saqlangan foydalanuvchilar uchun. */
    data object Main : Screen("main")
}
