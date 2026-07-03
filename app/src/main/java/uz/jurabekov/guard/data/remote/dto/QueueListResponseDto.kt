package uz.jurabekov.guard.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Backend javob strukturasi:
 * {
 *   "success": true,
 *   "message": "queues",
 *   "data":      { list, next_queue, ... }   ← usti ochiq mashinalar
 *   "data_tent": { list, next_queue, ... }   ← usti yopiq (tent) mashinalar
 * }
 *
 * Ikkala blok bir xil tuzilishga ega — [QueueListDataDto] qayta ishlatiladi.
 * Agar `data_tent` payload'da bo'lmasa, default empty value beriladi
 * (eski client'larda regression bo'lmaydi).
 */
@Serializable
data class QueueListResponseDto(
    @SerialName("success") val success: Boolean = true,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: QueueListDataDto = QueueListDataDto(),
    @SerialName("data_tent") val dataTent: QueueListDataDto = QueueListDataDto()
)

@Serializable
data class QueueListDataDto(
    @SerialName("list") val list: List<QueueItemDto> = emptyList(),
    @SerialName("next_queue") val nextQueue: QueueItemDto? = null,
    @SerialName("server_date") val serverDate: String? = null,
    @SerialName("queue_date") val queueDate: String? = null
)

/**
 * Submit response — tuzilishi taxminiy, hozircha bir xil saqlaymiz.
 */
@Serializable
data class QueueSubmitResponseDto(
    @SerialName("success") val success: Boolean = true,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: QueueItemDto
)