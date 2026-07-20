package uz.jurabekov.guard.presentation.queue_management

import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.VehicleType

sealed interface QueueManagementUiEvent {
    // ===== Date control =====
    data object OpenDatePicker : QueueManagementUiEvent
    data object DismissDatePicker : QueueManagementUiEvent
    data class DateSelected(val date: String) : QueueManagementUiEvent

    // ===== List =====
    data object Refresh : QueueManagementUiEvent
    data object AppResumed : QueueManagementUiEvent
    data class TabSelected(val type: VehicleType) : QueueManagementUiEvent

    // ===== Permit dialog =====
    /** Foydalanuvchi list item'iga bosdi — permit yuklash boshlanadi. */
    data class ItemClicked(val item: QueueItem) : QueueManagementUiEvent

    /** Dialog xato bilan ochilgan — qayta urinish. */
    data object RetryPermit : QueueManagementUiEvent

    data object DismissPermitDialog : QueueManagementUiEvent

    // ===== Info-tablo =====
    /** "N YO'L" tugmasi — mashinani tabloda shu yo'lga chaqirish. */
    data class LaneCallClicked(val item: QueueItem, val lane: Int) : QueueManagementUiEvent

    /** "O'TKAZILDI" tugmasi — mashina qo'lda o'tkazildi, yo'l bo'shaydi. */
    data class ManualPassClicked(val item: QueueItem) : QueueManagementUiEvent
}
