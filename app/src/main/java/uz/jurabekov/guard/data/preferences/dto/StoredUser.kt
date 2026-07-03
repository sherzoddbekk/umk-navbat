package uz.jurabekov.guard.data.preferences.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DataStore'da JSON sifatida saqlanadigan user snapshot.
 *
 * **Nega alohida `StoredUser`, `User` domain model'i emas?**
 * Clean Architecture: domain layer framework/serialization-agnostic.
 * `@Serializable` annotation `kotlinx.serialization` ga bog'lanish bo'lardi.
 *
 * Mapping `AuthRepositoryImpl` ichida bo'ladi:
 *  domain.User ↔ data.StoredUser ↔ JSON ↔ DataStore
 *
 * Field naming: snake_case backend bilan tekshirib turishga ehtiyoj yo'q —
 * bu **lokal storage format**, server'ga yetib bormaydi. Camel case ham ok,
 * lekin backend `UserDto` bilan bir xil snake_case'da qoldiramiz —
 * `LoginResponseDto.user`'dan to'g'ridan-to'g'ri map qilish oson bo'ladi.
 */
@Serializable
data class StoredUser(
    @SerialName("id") val id: Long,
    @SerialName("full_name") val fullName: String,
    @SerialName("username") val username: String,
    @SerialName("role_code") val roleCode: String,
    @SerialName("status") val status: String,
    @SerialName("permissions") val permissions: List<String> = emptyList()
)
