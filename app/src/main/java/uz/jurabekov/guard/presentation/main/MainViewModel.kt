package uz.jurabekov.guard.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.jurabekov.guard.domain.repository.AuthRepository

/**
 * Main screen ViewModel.
 *
 * **Responsibilities:**
 *  - Saqlangan user'ni reactive ravishda kuzatish (`currentUser`)
 *  - Permission-filtered drawer sectionlarini hisoblash
 *  - Logout action
 *
 * **`stateIn` strategy:**
 *  - `SharingStarted.WhileSubscribed(5000)` — Configuration change (rotation)
 *    paytida 5 sek subscriber yo'q bo'lsa ham Flow saqlanadi → re-collection
 *    yo'q, DataStore qayta o'qilmaydi. Standart Compose pattern.
 *
 * **Logout flow:**
 *  - `logout()` chaqirilganda `preferences.clear()` → `isLoggedIn = false`
 *  - UI tomondan navigation `onLogoutCompleted` callback orqali handle
 *  - Race condition imkonsiz: `repository.logout()` `suspend`, UI esa
 *    `LaunchedEffect` ichida await qilib navigation chaqiradi.
 */
class MainViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val state: StateFlow<MainUiState> = authRepository.currentUser
        .map { user ->
            if (user == null) {
                MainUiState(user = null, availableSections = emptyList(), isLoading = true)
            } else {
                MainUiState(
                    user = user,
                    availableSections = MainSection.availableFor(user.permissions),
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = MainUiState(isLoading = true)
        )

    /**
     * Logout — token va user DataStore'dan tozalanadi.
     *
     * `onCompleted` — clear muvaffaqiyatli bo'lgandan keyin UI tomondan
     * navigation chaqirish uchun. Bu yondashuv Channel/Effect'siz ham
     * working — Compose `LaunchedEffect(Unit)` ichida `vm.logout { nav... }`.
     */
    fun logout(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.logout()
            onCompleted()
        }
    }
}
