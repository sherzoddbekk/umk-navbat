package uz.jurabekov.guard.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `POST /api/v2/queue/owner/cancel` body.
 *
 * Backend `owner_token` + `plate`'ni birga tekshiradi. `plate` probel bilan
 * yoki probelsiz yuborilsa ham qabul qilinadi.
 */
@Serializable
data class QueueCancelRequestDto(
    @SerialName("owner_token") val ownerToken: String,
    @SerialName("plate") val plate: String
)

@Serializable
data class QueueCancelResponseDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String? = null
)
