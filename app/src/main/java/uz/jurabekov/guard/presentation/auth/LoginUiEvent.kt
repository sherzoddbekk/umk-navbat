package uz.jurabekov.guard.presentation.auth

sealed interface LoginUiEvent {
    data class UsernameChanged(val value: String) : LoginUiEvent
    data class PasswordChanged(val value: String) : LoginUiEvent
    data object TogglePasswordVisibility : LoginUiEvent
    data object Submit : LoginUiEvent
    data object TelegramLoginClicked : LoginUiEvent

    /** Dialog yopilganda formani tozalash uchun. */
    data object DialogDismissed : LoginUiEvent
}
