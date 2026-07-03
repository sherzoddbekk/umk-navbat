package uz.jurabekov.guard.presentation.scale

import uz.jurabekov.guard.domain.model.ScaleRecord
import uz.jurabekov.guard.domain.model.ScaleStatus

/**
 * Tarozi ro'yxati uchun status filter.
 *
 * **Nima uchun presentation layerda (domain emas)?**
 * Filter — UI concern. Domain `ScaleStatus` enum'i records'larning haqiqiy
 * statusini ifodalaydi (server-side reality). `ScaleStatusFilter` — UI
 * tomondan ro'yxatni qanday ko'rsatish strategiyasi. Mantiqiy ravishda
 * presentation layer.
 *
 * **Open-Closed:** yangi filter qo'shish uchun (`SKIPPED`, `DATE_RANGE`,
 * va h.k.) — faqat enumga yangi qator qo'shing va `apply()`'da branch.
 * UI avtomatik adaptatsiya qiladi (`entries` iteratsiya orqali).
 *
 * **Performance:** `apply()` `List.filter` `O(n)` linear scan. Kunlik
 * 50-100 yozuvda — sub-millisecond. Agar 10,000+ yozuv bo'lsa — `Sequence`
 * yoki backend-side filtering kerak.
 */
enum class ScaleStatusFilter(val label: String) {
    ALL("Jami"),
    COMPLETED("Yakunlangan"),
    INSIDE("Ichkarida");

    /**
     * Records ro'yxatini joriy filter bo'yicha qaytaradi.
     *
     * `ALL` da bir xil reference qaytadi — list yangi allocate qilinmaydi
     * (Compose stability uchun muhim — bir xil filter qayta tanlansa
     * recomposition signal yo'q).
     */
    fun apply(records: List<ScaleRecord>): List<ScaleRecord> = when (this) {
        ALL -> records
        COMPLETED -> records.filter { it.status == ScaleStatus.COMPLETED }
        INSIDE -> records.filter { it.status == ScaleStatus.INSIDE }
    }
}

/**
 * Filter chip uchun ko'rsatiladigan son.
 *
 * Pre-computed counts'lar (`StatusCounts`) bilan birga ishlaydi — har
 * filter o'ziga tegishli countni oladi.
 */
data class StatusCounts(
    val all: Int = 0,
    val completed: Int = 0,
    val inside: Int = 0
) {
    fun countFor(filter: ScaleStatusFilter): Int = when (filter) {
        ScaleStatusFilter.ALL -> all
        ScaleStatusFilter.COMPLETED -> completed
        ScaleStatusFilter.INSIDE -> inside
    }

    companion object {
        /**
         * Bir marta records'larni iteratsiya qilib uchala countni
         * hisoblaydi. Naïve `records.count{}` ni 3 marta chaqirgandan
         * 3x tez (single-pass).
         */
        fun from(records: List<ScaleRecord>): StatusCounts {
            var completed = 0
            var inside = 0
            for (record in records) {
                when (record.status) {
                    ScaleStatus.COMPLETED -> completed++
                    ScaleStatus.INSIDE -> inside++
                    ScaleStatus.UNKNOWN -> Unit  // boshqa filterga kirmaydi
                }
            }
            return StatusCounts(
                all = records.size,
                completed = completed,
                inside = inside
            )
        }
    }
}
