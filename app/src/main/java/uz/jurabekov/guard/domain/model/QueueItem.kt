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
)