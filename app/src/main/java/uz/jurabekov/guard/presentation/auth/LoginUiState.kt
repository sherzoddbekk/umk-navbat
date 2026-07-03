package uz.jurabekov.guard.presentation.auth

/**
 * Login dialog state.
 *
 * `canSubmit` derived field — UI submit tugmasi shu propertyga bog'lanadi,
 * presentation layer'da tarqoq tekshiruv yo'q.
 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean
        get() = username.isNotBlank() &&
                password.isNotBlank() &&
                !isLoading
}
