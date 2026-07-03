package uz.jurabekov.guard.domain.model

/**
 * Joriy kun navbatining to'liq snapshoti — ikki kategoriya:
 *
 *   - [open]      →  usti ochiq mashinalar
 *   - [tent]      →  usti yopiq mashinalar
 *   - [queueDate] →  navbat qaysi kun uchun (ISO `yyyy-MM-dd`),
 *                    backend `queue_date` field'idan keladi.
 *                    Null bo'lsa (eski API yoki xato) — UI o'z fallback'ini ishlatadi.
 */
data class QueueSnapshot(
    val open: QueueData,
    val tent: QueueData,
    val queueDate: String? = null
) {
    fun forType(type: VehicleType): QueueData = when (type) {
        VehicleType.OPEN -> open
        VehicleType.TENT -> tent
    }
}

/**
 * Bitta kategoriya navbat ma'lumotlari.
 *
 * @param items     kunning hamma navbatlari (cancelled, entered va waiting'lar)
 * @param nextQueue hozir kirmoqda yoki keyingi banner — null bo'lishi mumkin
 */
data class QueueData(
    val items: List<QueueItem>,
    val nextQueue: QueueItem?
)