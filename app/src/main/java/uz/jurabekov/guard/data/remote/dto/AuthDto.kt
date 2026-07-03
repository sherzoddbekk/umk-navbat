package uz.jurabekov.guard.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * POST /api/v2/auth/login body.
 *
 * Foydalanuvchi username va parol yuboradi. Parol JSON ichida raw bo'ladi —
 * shu sababli TLS (HTTPS) majburiy. Logger interceptor release'da disabled
 * (BuildConfig.DEBUG = false → NONE) — parol logga tushib qolmaydi.
 */
@Serializable
data class LoginRequestDto(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String
)

/**
 * Backend javob strukturasi:
 * {
 *   "success": true,
 *   "data": {
 *     "token": "eyJ...",
 *     "user": { id, full_name, username, role_code, status, permissions }
 *   }
 * }
 *
 * `data` nullable — backend `success: false` qaytarganda data umuman
 * bo'lmasligi mumkin. Repository qatlamida explicit null check qilamiz.
 */
@Serializable
data class LoginResponseDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: LoginDataDto? = null
)

@Serializable
data class LoginDataDto(
    @SerialName("token") val token: String,
    @SerialName("user") val user: UserDto
)

@Serializable
data class UserDto(
    @SerialName("id") val id: Long,
    @SerialName("full_name") val fullName: String,
    @SerialName("username") val username: String,
    @SerialName("role_code") val roleCode: String,
    @SerialName("status") val status: String,
    @SerialName("permissions") val permissions: List<String> = emptyList()
)
