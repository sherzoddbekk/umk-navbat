package uz.jurabekov.guard.presentation.queue_management

import uz.jurabekov.guard.domain.model.Permit
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.VehicleType
import uz.jurabekov.guard.presentation.queue.TabState

/**
 * Navbat boshqaruvi ekrani — MVI state.
 *
 * **`open` / `tent`** — `presentation.queue.TabState`'ni qayta ishlatamiz
 * (UI rendering qoidalari bir xil — `QueueListItem`, `NowEnteringBanner`).
 *
 * **`totalPermittedCount`** — ekran headerida "Jami: N ta" sifatida ko'rsatiladi.
 * `has_permit=true` mashinalar (OPEN + TENT) jami soni. Banner ichidagi
 * (currentlyEntering) hisoblanmaydi — u WAITING statusda; faqat `ENTERED`'lar.
 *
 * **`isToday`** — ViewModel WS event'larni faqat shu flag true bo'lganda
 * apply qiladi. Compose tomonidan o'qib ko'rsatish uchun ham foydali
 * (kelajakda "real-time" indicator qo'shish mumkin).
 */
data class QueueManagementUiState(
    val selectedDate: String,                            // yyyy-MM-dd

    val open: TabState = TabState(),
    val tent: TabState = TabState(),

    val selectedTab: VehicleType = VehicleType.OPEN,

    val queueDate: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val listError: String? = null,

    val showDatePicker: Boolean = false,
    val permitDialog: PermitDialogState? = null,

    // ===== Info-tablo (yo'lga chaqirish) =====
    /**
     * Joriy foydalanuvchi yo'l chaqiruvi tugmalarini ko'ra oladimi
     * (`admin` yoki `darvoza_tekshiruv`). `AuthRepository.currentUser`'dan.
     */
    val canManageInfoLane: Boolean = false,
    /** Hozir so'rov ketayotgan navbat `id`'si — tugmalar disable bo'ladi. */
    val laneActionInProgressId: Long? = null
) {
    val totalPermittedCount: Int
        get() = open.enteredOnlyCount + tent.enteredOnlyCount

    val activeTab: TabState
        get() = when (selectedTab) {
            VehicleType.OPEN -> open
            VehicleType.TENT -> tent
        }
}

/**
 * Permit dialog uchun sealed state.
 *
 * `queueId` har bir variantda saqlanadi — `Retry` ishlatish uchun kerak.
 * `item` (ixtiyoriy) — dialog header'da plate/full_name ko'rsatish uchun
 * (network'dan kelguncha placeholder).
 */
sealed interface PermitDialogState {
    val queueId: Long
    val baseItem: QueueItem?

    data class Loading(
        override val queueId: Long,
        override val baseItem: QueueItem?
    ) : PermitDialogState

    data class Loaded(
        override val queueId: Long,
        override val baseItem: QueueItem?,
        val permit: Permit
    ) : PermitDialogState

    /** API muvaffaqiyatli, lekin `data: []` qaytdi — permit hali chiqarilmagan. */
    data class Empty(
        override val queueId: Long,
        override val baseItem: QueueItem?
    ) : PermitDialogState

    data class Error(
        override val queueId: Long,
        override val baseItem: QueueItem?,
        val message: String
    ) : PermitDialogState
}
