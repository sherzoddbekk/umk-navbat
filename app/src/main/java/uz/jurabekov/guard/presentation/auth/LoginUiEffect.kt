package uz.jurabekov.guard.presentation.auth

/**
 * One-shot effect'lar. State'da saqlanmaydi — qayta consume bo'lmaydi
 * (Toast ikki marta chiqishi yoki navigation ikki marta sodir bo'lishi
 * mumkin emas).
 *
 * `Channel(BUFFERED) → receiveAsFlow()` orqali UI'ga yetadi.
 */
sealed interface LoginUiEffect {
    data class ShowToast(val message: String) : LoginUiEffect

    /** Login muvaffaqiyatli — UI Main screen'ga navigate qilishi kerak. */
    data object LoginSuccess : LoginUiEffect
}
