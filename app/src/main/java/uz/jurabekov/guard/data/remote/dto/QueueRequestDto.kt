package uz.jurabekov.guard.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * POST /api/v2/queue body.
 *
 *   `type = null`   → usti ochiq mashina
 *   `type = "tent"` → usti yopiq (tentli) mashina
 *
 * `type` field server'ga doim yuboriladi (null bo'lsa ham JSON'da chiqadi),
 * chunki backend explicit signal kutmoqda.
 *
 * `passport_series` va `passport_number` — ixtiyoriy.
 * `@EncodeDefault(NEVER)` — agar null bo'lsa, JSON body'ga umuman qo'shilmaydi.
 * Bu null vs missing farqlovchi backend'lar uchun xavfsiz default.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class QueueRequestDto(
    @SerialName("type") val type: String?,
    @SerialName("plate") val plate: String,
    @SerialName("full_name") val fullName: String,

    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("passport_series")
    val passportSeries: String? = null,

    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("passport_number")
    val passportNumber: String? = null
)