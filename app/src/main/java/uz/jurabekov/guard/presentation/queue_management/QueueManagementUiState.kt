package uz.jurabekov.guard.presentation.queue_management

import uz.jurabekov.guard.domain.model.Permit
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.VehicleType

/**
 * Navbat boshqaruvi ekrani — MVI state.
 *
 * **Ma'lumot modeli:** ikkita xom ro'yxat ([openItems] / [tentItems]) saqlanadi
 * — bo'lim (permit/gate/given) va yo'nalish (open/tent) bo'yicha filtrlash UI
 * hisoblash paytida ([visibleItems]) amalga oshadi. Bu status-asosli partition
 * (`QueueScreen`'dagi `TabState`)dan farqli — bu yerda kesim `has_permit` /
 * `manual_passed` bo'yicha, shuning uchun xom ro'yxat toza.
 *
 * **`selectedSection`** — [availableSections] ichidan bo'lishi kafolatlanadi
 * (ViewModel rol o'zgarganda tuzatib turadi).
 */
data class QueueManagementUiState(
    val selectedDate: String,                            // yyyy-MM-dd

    val openItems: List<QueueItem> = emptyList(),
    val tentItems: List<QueueItem> = emptyList(),

    val selectedSection: QueueSection = QueueSection.PERMIT_QUEUE,
    val selectedTab: VehicleType = VehicleType.OPEN,

    val queueDate: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val listError: String? = null,

    val showDatePicker: Boolean = false,
    val permitDialog: PermitDialogState? = null,

    // ===== Info-tablo (yo'lga chaqirish) =====
    /** Joriy foydalanuvchi roli (`role_code`) — bo'lim ko'rinishini belgilaydi. */
    val userRole: String? = null,
    /** Hozir so'rov ketayotgan navbat `id`'si — chaqiruv tugmalari disable bo'ladi. */
    val laneActionInProgressId: Long? = null
) {
    /** Rolga ko'ra ko'rinadigan bo'lim tablari. */
    val availableSections: List<QueueSection>
        get() = QueueSection.availableFor(userRole)

    /** Darvoza amallarini (chaqiruv / "O'tkazildi") bajara oladimi. */
    val canManageInfoLane: Boolean
        get() = QueueSection.GATE_QUEUE.isVisibleFor(userRole)

    private val activeItems: List<QueueItem>
        get() = if (selectedTab == VehicleType.OPEN) openItems else tentItems

    /** Joriy bo'lim + yo'nalish uchun ko'rsatiladigan, tartiblangan ro'yxat. */
    val visibleItems: List<QueueItem>
        get() = activeItems
            .filter { selectedSection.matches(it) }
            .sortedBy { it.queueNumber }

    /** Date bar counter — jami berilgan ruxsatnomalar (OPEN + TENT). */
    val totalPermittedCount: Int
        get() = openItems.count { it.hasPermit } + tentItems.count { it.hasPermit }

    /** Hali hech qanday ma'lumot yuklanmagan (loading/empty state ajratish uchun). */
    val hasNoData: Boolean
        get() = openItems.isEmpty() && tentItems.isEmpty()
}

/**
 * Permit dialog uchun sealed state.
 *
 * `queueId` har bir variantda saqlanadi — `Retry` ishlatish uchun kerak.
 * `baseItem` (ixtiyoriy) — dialog header'da plate/full_name ko'rsatish uchun
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
