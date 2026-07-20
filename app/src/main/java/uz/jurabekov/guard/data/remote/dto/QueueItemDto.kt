package uz.jurabekov.guard.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.QueueItemStatus
import uz.jurabekov.guard.domain.model.VehicleType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
data class QueueItemDto(
    @SerialName("id") val id: Long,
    @SerialName("uuid") val uuid: String,
    @SerialName("date") val date: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("queue_number") val queueNumber: Int,
    @SerialName("plate") val plate: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("has_permit") val hasPermit: Boolean = false,
    @SerialName("cancelled") val cancelled: Boolean = false,
    @SerialName("cancel_reason") val cancelReason: String? = null,
    @SerialName("passport_series") val passportSeries: String? = null,
    @SerialName("passport_number") val passportNumber: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("arrived_at") val arrivedAt: String? = null,
    // Faqat submit response'da keladi (POST /api/v2/queue). List response'da yo'q.
    // Egasi navbatni bekor qilishda (POST .../owner/cancel) ishlatiladi.
    @SerialName("owner_token") val ownerToken: String? = null,
    // Info-tablo chaqiruvi (Navbat boshqaruvi ekrani). Eski backend'da yo'q —
    // default qiymatlar bilan forward/backward compatible.
    @SerialName("info_lane") val infoLane: Int? = null,
    @SerialName("manual_passed") val manualPassed: Boolean = false
) {
    /**
     * Backend signal'larini status'ga aylantirish.
     *
     * Prioritet (eng kuchli signal birinchi):
     *  1. `has_permit = true`        → ENTERED (zavodga kirgan)
     *  2. `cancelled = true`         → SKIPPED (navbati o'tib ketdi)
     *  3. aks holda                  → WAITING (navbatda turibdi)
     *
     * `has_permit` `cancelled`'dan ustun: agar has_permit=true bo'lsa,
     * cancelled qiymati ahamiyatsiz (mashina kirgan).
     *
     * @param status Override uchun (WS handler'da partition logikasi).
     */
    fun toDomain(status: QueueItemStatus = defaultStatus()): QueueItem = QueueItem(
        id = id,
        uuid = uuid,
        queueNumber = queueNumber,
        plate = plate,
        fullName = fullName,
        hasPermit = hasPermit,
        type = VehicleType.fromBackend(type),
        status = status,
        createdAtEpochMs = parseIsoToEpochMs(createdAt),
        arrivedAtHHmm = extractHHmm(arrivedAt),
        date = date,
        ownerToken = ownerToken,
        infoLane = infoLane,
        manualPassed = manualPassed,
    )

    private fun defaultStatus(): QueueItemStatus = when {
        hasPermit -> QueueItemStatus.ENTERED
        cancelled -> QueueItemStatus.SKIPPED
        else -> QueueItemStatus.WAITING
    }
}

private fun extractHHmm(input: String?): String {
    if (input.isNullOrBlank()) return ""
    // "yyyy-MM-dd HH:mm:ss" yoki "yyyy-MM-ddTHH:mm:ss..." — ikkalasini ham qo'llaydi
    val timePart = input.substringAfter(' ', missingDelimiterValue = "")
        .ifEmpty { input.substringAfter('T', missingDelimiterValue = "") }
    // "07:13:27" → "07:13".  Length tekshiruvi defensive.
    return if (timePart.length >= 5) timePart.substring(0, 5) else ""
}

private val ISO_FORMAT_MICROS: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
}

private val ISO_FORMAT_MILLIS: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
}

private val ISO_FORMAT_SECONDS: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
}

private fun parseIsoToEpochMs(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    listOf(ISO_FORMAT_MICROS, ISO_FORMAT_MILLIS, ISO_FORMAT_SECONDS).forEach { fmt ->
        runCatching {
            fmt.get()?.parse(iso)?.time?.let { return it }
        }
    }
    return 0L
}