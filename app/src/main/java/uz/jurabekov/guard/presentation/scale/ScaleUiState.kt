package uz.jurabekov.guard.presentation.scale

import uz.jurabekov.guard.domain.model.ScaleRecord

/**
 * Scale ekran state'i (MVI).
 *
 * **`records` vs `displayRecords`:**
 *  - `records` — backend'dan kelgan to'liq ro'yxat (filter'ga bog'liq emas)
 *  - `displayRecords` — UI'da `remember(records, statusFilter)` bilan
 *    hisoblanadi (state'da yo'q). Sabab: filter o'zgarganda har emit
 *    yangi state allocate qilmaslik + presentation/state separation.
 *
 * **`statusCounts` — pre-computed:**
 *  ViewModel records yangilanganda bir marta hisoblaydi. Filter chip'larda
 *  count har recompose'da chaqirilishini optimallashtiradi.
 *
 * Loading strategiyalari:
 *  - `isLoading`: full-screen loading (birinchi load yoki sana o'zgardi)
 *  - `isRefreshing`: explicit refresh (mavjud ma'lumot ustida progress)
 */
data class ScaleUiState(
    val selectedDate: String,                                // yyyy-MM-dd
    val records: List<ScaleRecord> = emptyList(),
    val statusFilter: ScaleStatusFilter = ScaleStatusFilter.ALL,
    val statusCounts: StatusCounts = StatusCounts(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showDatePicker: Boolean = false
) {
    /**
     * Ekranda hech qanday yozuv ko'rsatib bo'lmaydi (loading/error/empty
     * branch tanlash uchun).
     *
     * `records` (filter applied emas) ni tekshiramiz — filter ostida
     * empty bo'lsa `EmptyState` ko'rinmaydi (records bor, filter chip
     * orqali boshqasiga o'tish mumkin).
     */
    val isFullyEmpty: Boolean
        get() = records.isEmpty() && !isLoading && error == null
}
