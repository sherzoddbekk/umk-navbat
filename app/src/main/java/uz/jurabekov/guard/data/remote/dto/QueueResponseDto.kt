package uz.jurabekov.guard.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Backend'dan keladigan javob.
 *
 * Note: backend API tayyor bo'lganda field nomlari moslashtirilishi mumkin.
 * Hozircha taxminiy schema.
 */
@Serializable
data class QueueResponseDto(
    @SerialName("ticket_number") val ticketNumber: Int,
    @SerialName("vehicle_plate") val vehiclePlate: String,
    @SerialName("driver_full_name") val driverFullName: String,
    @SerialName("created_at") val createdAt: Long
) {
//    fun toDomain(): QueueTicket = QueueTicket(
//        ticketNumber = ticketNumber,
//        vehiclePlate = vehiclePlate,
//        driverFullName = driverFullName,
//        createdAt = createdAt
//    )
}
