package uz.jurabekov.guard.domain.model

/**
 * Tarozi statuslari.
 *
 * Backend `"Yakunlangan"` yoki `"Ichkarida"` (Cyrillic-free, o'zbek lotin)
 * qaytaradi. Domain tomonida sealed enum — UI rendering uchun stable kontrakt.
 *
 * `UNKNOWN` — kelajakda yangi status qo'shilsa (`"Bekor qilindi"` va h.k.)
 * crash bo'lmasin uchun fallback. Defensive design.
 */
enum class ScaleStatus(val raw: String) {
    COMPLETED("Yakunlangan"),
    INSIDE("Ichkarida"),
    UNKNOWN("");

    companion object {
        fun fromRaw(raw: String?): ScaleStatus = when (raw?.trim()) {
            COMPLETED.raw -> COMPLETED
            INSIDE.raw -> INSIDE
            else -> UNKNOWN
        }
    }
}

/**
 * Tarozi yozuvi.
 *
 * Backend ko'p maydonlarni `"-"` deb qaytaradi (chiqmagan mashinada
 * `exit_time`, `tara`, `netto` yo'q). Domain modeli `"-"` ni `null`'ga
 * normalize qiladi — UI rendering logikasi sodda bo'ladi (`?.let { ... }`).
 *
 * `weight` (brutto/tara/netto) raw string'lar — backend "32.5 t" formatda
 * yuboradi. Parsing UI tomonida emas, raw display sodda va format-agnostic.
 * Agar kelajakda `sortBy { netto }` kerak bo'lsa — domain modelni numeric
 * `Double` ga aylantirishimiz mumkin (lekin hozircha YAGNI).
 */
data class ScaleRecord(
    val plate: String,
    val kpp: String,
    val entryTime: String?,
    val exitTime: String?,
    val brutto: String?,
    val tara: String?,
    val netto: String?,
    val status: ScaleStatus
)

/**
 * `GET /api/v2/scale?date=...` response'ining domain wrapper'i.
 *
 * `date` — backend ekspozitsiyasi (so'ralgan sana — request bilan bir xil
 * bo'lishi kerak). UI display uchun emas (bizda local `selectedDate` bor),
 * ammo response validation uchun foydali.
 */
data class ScaleDay(
    val date: String,
    val total: Int,
    val records: List<ScaleRecord>
)
