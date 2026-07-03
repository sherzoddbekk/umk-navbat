package uz.jurabekov.guard.presentation.queue

import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.QueueItemStatus
import uz.jurabekov.guard.domain.model.VehicleType

/**
 * Bitta tab uchun ekran state'i. Ikki tab — ikki [TabState] mavjud.
 */
data class TabState(
    val currentlyEntering: QueueItem? = null,
    val waitingItems: List<QueueItem> = emptyList(),
    val enteredItems: List<QueueItem> = emptyList()
) {
    val totalCount: Int
        get() = waitingItems.size + enteredItems.size +
                (if (currentlyEntering != null) 1 else 0)

    val enteredOnlyCount: Int
        get() = enteredItems.count { it.status == QueueItemStatus.ENTERED }

    val skippedCount: Int
        get() = enteredItems.count { it.status == QueueItemStatus.SKIPPED }

    val isQueueEmpty: Boolean
        get() = currentlyEntering == null && waitingItems.isEmpty()

    val isFullyEmpty: Boolean
        get() = isQueueEmpty && enteredItems.isEmpty()
}

data class QueueUiState(
    val selectedTab: VehicleType = VehicleType.OPEN,

    val open: TabState = TabState(),
    val tent: TabState = TabState(),

    val queueDate: String? = null,

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val listError: String? = null,

    // ===== Submit dialog =====
    val showDialog: Boolean = false,
    val plate: String = "",
    val fullName: String = "",
    val passportSeries: String = "",
    val passportNumber: String = "",
    val selectedType: VehicleType = VehicleType.OPEN,
    val plateError: String? = null,
    val nameError: String? = null,
    val passportError: String? = null,
    val isSubmitting: Boolean = false,

    /**
     * "Eslab qolish" checkbox holati.
     *
     * Dialog ochilganda `SavedDriverPreferences`'dan o'qiladi. Submit success
     * vaqtida `true` bo'lsa — joriy form ma'lumotlari saqlanadi; `false` bo'lsa
     * — saqlangan ma'lumotlar tozalanadi (idempotent — yo'q bo'lsa ham ok).
     */
    val rememberMe: Boolean = false,

    val successItem: QueueItem? = null,

    // ===== Login dialog =====
    /**
     * TopBar "Kirish" tugmasi bosilganda true.
     * LoginViewModel alohida — bu yerda faqat dialog visibility.
     */
    val showLoginDialog: Boolean = false
) {
    val isFormValid: Boolean
        get() {
            if (plate.isBlank() || fullName.isBlank()) return false

            val seriesEmpty = passportSeries.isEmpty()
            val numberEmpty = passportNumber.isEmpty()
            val seriesFull = passportSeries.length == 2
            val numberFull = passportNumber.length == 7

            val passportOk = (seriesEmpty && numberEmpty) || (seriesFull && numberFull)
            return passportOk
        }

    val activeTab: TabState
        get() = when (selectedTab) {
            VehicleType.OPEN -> open
            VehicleType.TENT -> tent
        }

    val isFullyEmpty: Boolean
        get() = open.isFullyEmpty && tent.isFullyEmpty
}