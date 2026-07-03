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
}