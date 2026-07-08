package uz.jurabekov.guard.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Backend xatolik javobining umumiy sxemasi (masalan 422):
 * `{ "success": false, "message": "The plate field must be 8 characters.", "data": null }`
 *
 * `errorBody`'dan `message`'ni ajratib olish uchun ishlatiladi.
 */
@Serializable
data class ApiErrorDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String? = null
)
