package uz.jurabekov.guard.presentation.queue

import uz.jurabekov.guard.domain.model.VehicleType

sealed interface QueueUiEvent {
    /** TopBar "Kirish" tugmasi — login dialog ochish signali. */
    data object LoginClicked : QueueUiEvent

    /** Login dialog yopildi (back press, X, click outside, success). */
    data object DismissLoginDialog : QueueUiEvent

    data object Refresh : QueueUiEvent
    data object AppResumed : QueueUiEvent

    data class TabSelected(val type: VehicleType) : QueueUiEvent

    // Submit dialog control
    data object OpenDialog : QueueUiEvent
    data object DismissDialog : QueueUiEvent

    // Form
    data class TypeChanged(val type: VehicleType) : QueueUiEvent
    data class PlateChanged(val value: String) : QueueUiEvent
    data class NameChanged(val value: String) : QueueUiEvent
    data class PassportSeriesChanged(val value: String) : QueueUiEvent
    data class PassportNumberChanged(val value: String) : QueueUiEvent

    /** "Eslab qolish" checkbox toggle. Persist faqat Submit success'da. */
    data class RememberMeChanged(val value: Boolean) : QueueUiEvent

    data object Submit : QueueUiEvent

    data object DismissSuccess : QueueUiEvent

    // ===== "Yangilik!" e'loni =====
    /** E'lon yopildi ("Tushunarli" yoki 5s avtomatik) — qayta chiqmaydi. */
    data object DismissAnnouncement : QueueUiEvent

    // ===== Navbatni bekor qilish =====
    /** "Navbatni bekor qilish" tugmasi — saqlangan navbatlar sheet'ini ochadi. */
    data object OpenCancelSheet : QueueUiEvent
    data object DismissCancelSheet : QueueUiEvent

    /** Sheet'da radio orqali navbat tanlash. */
    data class CancelSelectionChanged(val uuid: String) : QueueUiEvent

    /** Tanlangan navbatni bekor qilish (API so'rovi). */
    data object ConfirmCancel : QueueUiEvent
}