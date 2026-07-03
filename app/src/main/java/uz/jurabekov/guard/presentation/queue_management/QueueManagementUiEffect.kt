package uz.jurabekov.guard.presentation.queue_management

sealed interface QueueManagementUiEffect {
    data class ShowToast(val message: String) : QueueManagementUiEffect
}
