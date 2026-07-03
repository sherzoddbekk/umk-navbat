package uz.jurabekov.guard.presentation.queue

sealed interface QueueUiEffect {
    data class ShowToast(val message: String) : QueueUiEffect

    // EnsureMonitoringActive olib tashlandi - v1.1'da FCM bilan birga
    // RegisterFcmToken yoki shunga o'xshash effect qo'shilishi mumkin.
}
