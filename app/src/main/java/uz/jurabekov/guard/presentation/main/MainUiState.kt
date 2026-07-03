package uz.jurabekov.guard.presentation.main

import uz.jurabekov.guard.domain.model.User

/**
 * Main screen state.
 *
 * `user` nullable — session corrupt yoki logout race condition vaqtida
 * (logout chaqirilgan, lekin navigation hali yo'naltirilmagan).
 *
 * `availableSections` — `MainSection.availableFor(user.permissions)`.
 * `MainViewModel` ichida darivative — UI faqat o'qiydi.
 */
data class MainUiState(
    val user: User? = null,
    val availableSections: List<MainSection> = emptyList(),
    val isLoading: Boolean = true
)
