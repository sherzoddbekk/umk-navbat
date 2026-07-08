package uz.jurabekov.guard.data.preferences.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Foydalanuvchi olgan bitta navbat — `owner_token` bilan birga local'da saqlanadi.
 *
 * **Maqsad:**
 *  Submit success'dan keyin backend qaytargan to'liq navbat obyekti (jumladan
 *  64-belgili `owner_token`) DataStore'da array sifatida saqlanadi. Keyinchalik
 *  foydalanuvchi navbatni bekor qilmoqchi bo'lsa, shu ro'yxatdan tanlab
 *  `POST /api/v2/queue/owner/cancel` ga token+plate yuboriladi.
 *
 *  Bir foydalanuvchi kun davomida bir nechta navbat olishi mumkin — shuning
 *  uchun array. Yangi kun boshlanganda eski kun yozuvlari tozalanadi
 *  ([uz.jurabekov.guard.data.preferences.OwnedQueuesPreferences.add] ichida).
 *
 * Backend submit response schema'siga to'liq mos (`@SerialName`).
 */
@Serializable
data class OwnedQueue(
    @SerialName("id") val id: Long,
    @SerialName("uuid") val uuid: String,
    @SerialName("date") val date: String,
    @SerialName("type") val type: String,
    @SerialName("queue_number") val queueNumber: Int,
    @SerialName("plate") val plate: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("passport_series") val passportSeries: String? = null,
    @SerialName("passport_number") val passportNumber: String? = null,
    @SerialName("has_permit") val hasPermit: Boolean = false,
    @SerialName("cancelled") val cancelled: Boolean = false,
    @SerialName("cancel_reason") val cancelReason: String? = null,
    @SerialName("owner_token") val ownerToken: String
)
