package uz.jurabekov.guard.core.util

object Constants {
    // ===== Network =====
    const val NETWORK_CONNECT_TIMEOUT_SEC = 15L
    const val NETWORK_READ_TIMEOUT_SEC = 20L
    const val NETWORK_WRITE_TIMEOUT_SEC = 20L

    const val DRIVER_NAME_MIN_LENGTH = 3

    // ===== Passport =====
    /** Pasport seriyasi uzunligi (2 ta Lotin harfi: AA, AB, ...). */
    const val PASSPORT_SERIES_LENGTH = 2

    /** Pasport raqami uzunligi (7 ta son: 1234567). */
    const val PASSPORT_NUMBER_LENGTH = 7

    /**
     * Pasport seriyasi uchun tez tanlash variantlari (dropdown).
     * Foydalanuvchi qo'lda boshqa AA-ZZ kombinatsiyasini ham yoza oladi —
     * bu faqat suggestion ro'yxati.
     */
    val PASSPORT_SERIES_OPTIONS = listOf(
        "AA", "AB", "AC", "AD", "AE", "AF", "AG", "AH", "AI", "AJ"
    )

    // ===== Pusher / WebSocket =====
    const val PUSHER_APP_KEY = "oxrana-key"
    const val PUSHER_HOST = "apigate.uzbeksteel.uz"
    const val PUSHER_PORT = 443
    const val PUSHER_USE_TLS = true

    const val PUSHER_CHANNEL = "queue"
    const val PUSHER_EVENT_QUEUE_BOOKED = "QueueBooked"
    const val PUSHER_EVENT_PERMIT_ISSUED = "PermitIssued"

    const val PUSHER_ACTIVITY_TIMEOUT_MS = 120_000L
    const val PUSHER_PONG_TIMEOUT_MS = 30_000L
    const val PUSHER_MAX_RECONNECT_ATTEMPTS = 6
    const val PUSHER_MAX_RECONNECT_GAP_SEC = 30

    // ===== Auto-sync =====
    /** Background tekshiruv intervali — har 10 daqiqada bir. */
    const val AUTO_SYNC_INTERVAL_MS = 10 * 60 * 1000L  // 10 daqiqa

    /**
     * Agar WebSocket'dan oxirgi event'dan beri shu vaqt o'tgan bo'lsa,
     * WS "stale" deb hisoblanadi va REST orqali sync qilinadi.
     */
    const val WS_STALE_THRESHOLD_MS = 10 * 60 * 1000L  // 10 daqiqa
}