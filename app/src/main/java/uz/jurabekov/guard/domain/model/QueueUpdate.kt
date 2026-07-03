package uz.jurabekov.guard.domain.model

/**
 * WebSocket'dan keladigan navbat yangiliklarining turlari.
 *
 *  - [Booked]    — Yangi mashina navbatga qo'shildi (single item).
 *  - [Permitted] — Permit holati o'zgardi. Backend ikkala tab uchun ham
 *                  keyingi mashinani bir event'da yuboradi:
 *                    - `nextOpen != null` → OPEN tab uchun yangi banner
 *                    - `nextTent != null` → TENT tab uchun yangi banner
 *                    - har ikki null     → ikkala banner clear (REST refresh kerak)
 *
 *                  Eski format (single item with type) bilan ham mos —
 *                  parser uni mos tab field'iga joylashtiradi.
 */
sealed interface QueueUpdate {
    data class Booked(val item: QueueItem) : QueueUpdate
    data class Permitted(
        val nextOpen: QueueItem?,
        val nextTent: QueueItem?
    ) : QueueUpdate
}