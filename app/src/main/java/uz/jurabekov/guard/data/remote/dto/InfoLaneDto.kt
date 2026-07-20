package uz.jurabekov.guard.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `POST /api/v2/queue/{id}/info-lane/call` body — mashinani 1/2/3-yo'lga chaqirish.
 */
@Serializable
data class InfoLaneCallRequestDto(
    @SerialName("lane") val lane: Int
)

/**
 * Info-tablo action'lari uchun umumiy javob (chaqiruv + "o'tkazildi").
 *
 * Ikkala endpoint ham bir xil envelope qaytaradi; farq faqat `data` ichida
 * (`entry_id`, `source`, `entered_at` — "o'tkazildi"da). UI'ga faqat
 * `message` va `lane` kerak, qolganini o'qimaymiz (forward-compatible).
 */
@Serializable
data class InfoLaneActionResponseDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: InfoLaneActionDataDto? = null
)

@Serializable
data class InfoLaneActionDataDto(
    @SerialName("queue_id") val queueId: Long? = null,
    @SerialName("plate") val plate: String? = null,
    @SerialName("lane") val lane: Int? = null
)
