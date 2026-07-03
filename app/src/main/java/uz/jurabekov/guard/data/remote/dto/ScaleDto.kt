package uz.jurabekov.guard.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /api/v2/scale?date=YYYY-MM-DD` response.
 *
 * Backend strukturasi:
 * ```
 * {
 *   "data": {
 *     "date": "2026-05-14",
 *     "total": 12,
 *     "records": [ ... ]
 *   }
 * }
 * ```
 *
 * `success` field optional — boshqa endpoint'lar (auth, queue) `success`
 * qaytaradi, scale spec'da yo'q. Defensive: `success = null` bo'lsa va
 * `data` non-null bo'lsa — muvaffaqiyatli deb hisoblaymiz.
 */
@Serializable
data class ScaleListResponseDto(
    @SerialName("success") val success: Boolean? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: ScaleDataDto? = null
)

@Serializable
data class ScaleDataDto(
    @SerialName("date") val date: String,
    @SerialName("total") val total: Int = 0,
    @SerialName("records") val records: List<ScaleRecordDto> = emptyList()
)

/**
 * Bitta tarozi yozuvi.
 *
 * Hamma field nullable + default — backend ba'zan field'ni umuman
 * yubormasligi yoki `"-"` qaytarishi mumkin (mashina hali ichkarida).
 * Defensive deserializatsiya — crash o'rniga `null`.
 */
@Serializable
data class ScaleRecordDto(
    @SerialName("plate") val plate: String = "",
    @SerialName("kpp") val kpp: String = "",
    @SerialName("entry_time") val entryTime: String? = null,
    @SerialName("exit_time") val exitTime: String? = null,
    @SerialName("brutto") val brutto: String? = null,
    @SerialName("tara") val tara: String? = null,
    @SerialName("netto") val netto: String? = null,
    @SerialName("status") val status: String? = null
)
