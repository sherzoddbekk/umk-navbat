package uz.jurabekov.guard.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uz.jurabekov.guard.domain.model.Permit
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * `GET /api/v2/queue/{id}/permits` response.
 *
 * Backend doim `data` arrayini qaytaradi. Joriy specga ko'ra bitta queue
 * uchun bir permit, ammo array sifatida keladi — kelajakda backend qayta
 * chiqarish (re-entry) qo'shsa o'zgarishsiz ishlaydi.
 */
@Serializable
data class PermitListResponseDto(
    @SerialName("success") val success: Boolean = true,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: List<PermitDto> = emptyList()
)

@Serializable
data class PermitDto(
    @SerialName("id") val id: Long,
    @SerialName("uuid") val uuid: String,
    @SerialName("queue_id") val queueId: Long,
    @SerialName("year") val year: Int = 0,
    @SerialName("number") val number: Int = 0,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("organization") val organization: String? = null,
    @SerialName("passport_series") val passportSeries: String? = null,
    @SerialName("passport_number") val passportNumber: String? = null,
    @SerialName("destination") val destination: String? = null,
    @SerialName("recipient") val recipient: String? = null,
    @SerialName("work_done_time") val workDoneTime: String? = null,
    @SerialName("issued_at") val issuedAt: String? = null,
    @SerialName("duty_officer") val dutyOfficer: String? = null,
    @SerialName("valid_duration") val validDuration: String? = null,
    @SerialName("vehicle_brand") val vehicleBrand: String? = null,
    @SerialName("plate") val plate: String = "",
    @SerialName("purpose") val purpose: String? = null,
    @SerialName("shift_guard") val shiftGuard: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toDomain(): Permit = Permit(
        id = id,
        uuid = uuid,
        queueId = queueId,
        year = year,
        number = number,
        fullName = fullName,
        organization = organization,
        passportSeries = passportSeries,
        passportNumber = passportNumber,
        destination = destination,
        recipient = recipient,
        workDoneTime = workDoneTime,
        issuedAtEpochMs = parseIsoToEpochMs(issuedAt),
        issuedAtRaw = issuedAt,
        dutyOfficer = dutyOfficer,
        validDuration = validDuration,
        vehicleBrand = vehicleBrand,
        plate = plate,
        purpose = purpose,
        shiftGuard = shiftGuard
    )
}

/* ============================================================
 * ISO date parser — `QueueItemDto`'dagi format'lar bilan mos.
 *
 * NOTE: duplikatsiya. Kelajakda bu helper'larni `core/util/IsoDate.kt`'ga
 * ko'chirish kerak (hozircha 2 ta DTO faylida — minimal refactor).
 * ============================================================ */

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
