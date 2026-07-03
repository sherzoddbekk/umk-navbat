package uz.jurabekov.guard.domain.model

/**
 * Vaqtinchalik ruxsatnoma — `GET /api/v2/queue/{id}/permits` javobidagi
 * bitta yozuv (joriy specga ko'ra bitta queue uchun bitta permit).
 *
 * **Domain qoidasi:** UI-specific format'lar (`dd.MM.yyyy HH:mm` ko'rinishi)
 * domain'da YO'Q — bu yerda faqat raw qiymatlar va parse qilingan epoch ms.
 * Format presentation qatlamida amalga oshiriladi.
 *
 * Null bo'lishi mumkin bo'lgan fieldlar (`recipient`, `workDoneTime`,
 * `validDuration`, `purpose`, `shiftGuard`) — backend ham `null` qaytaradi,
 * UI'da "—" placeholder ko'rsatiladi.
 *
 * @param issuedAtEpochMs `issued_at` ISO string'dan parse qilingan UTC ms.
 *                        Parse fail bo'lsa 0L (UI fallback ishlatadi).
 */
data class Permit(
    val id: Long,
    val uuid: String,
    val queueId: Long,
    val year: Int,
    val number: Int,
    val fullName: String,
    val organization: String?,
    val passportSeries: String?,
    val passportNumber: String?,
    val destination: String?,
    val recipient: String?,
    val workDoneTime: String?,
    val issuedAtEpochMs: Long,
    val issuedAtRaw: String?,
    val dutyOfficer: String?,
    val validDuration: String?,
    val vehicleBrand: String?,
    val plate: String,
    val purpose: String?,
    val shiftGuard: String?
) {
    /** Permit raqami `YYYY/NNNN` formatda (UI'da header'da ko'rsatiladi). */
    val displayNumber: String
        get() = "$year/$number"
}
