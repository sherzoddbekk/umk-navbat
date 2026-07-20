package uz.jurabekov.guard.domain.model

/**
 * Mashina navbat statusi.
 *
 *  WAITING  — Hali kirmagan, navbatda turibdi
 *  ENTERED  — Zavodga kirgan (has_permit=true)
 *  SKIPPED  — Navbati o'tib ketgan (kelmadi-yu, undan keyingi mashina kirib ketdi)
 */
enum class QueueItemStatus {
    WAITING, ENTERED, SKIPPED
}

data class QueueItem(
    val id: Long,
    val uuid: String,
    val queueNumber: Int,
    val plate: String,
    val fullName: String,
    val hasPermit: Boolean,
    val type: VehicleType = VehicleType.OPEN,
    val status: QueueItemStatus = QueueItemStatus.WAITING,
    val createdAtEpochMs: Long,
    val arrivedAtHHmm: String,
    /** Backend `date` (`yyyy-MM-dd`). Odatda faqat submit response'da to'ladi. */
    val date: String? = null,
    /**
     * Egasi navbatni bekor qilish tokeni (64 belgi). FAQAT submit response'da
     * keladi — WS/list item'larda `null`. Local'da saqlanadi (owner cancel uchun).
     */
    val ownerToken: String? = null,
    /**
     * Mashina chaqirilgan yo'l raqami (1..3) yoki `null` — hali chaqirilmagan.
     * Faqat "Navbat boshqaruvi" ekranida ishlatiladi (info-tablo chaqiruvi).
     */
    val infoLane: Int? = null,
    /** Mashina qo'lda o'tkazilgan — tablodan olib tashlangan, yo'l bo'sh. */
    val manualPassed: Boolean = false,
)