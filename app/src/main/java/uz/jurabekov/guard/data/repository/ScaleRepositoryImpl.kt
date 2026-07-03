package uz.jurabekov.guard.data.repository

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.data.remote.api.ScaleApi
import uz.jurabekov.guard.data.remote.dto.ScaleDataDto
import uz.jurabekov.guard.data.remote.dto.ScaleRecordDto
import uz.jurabekov.guard.domain.model.ScaleDay
import uz.jurabekov.guard.domain.model.ScaleRecord
import uz.jurabekov.guard.domain.model.ScaleStatus
import uz.jurabekov.guard.domain.repository.ScaleRepository
import java.io.IOException

/**
 * ScaleRepository implementatsiyasi.
 *
 * Error mapping konsistentligi `AuthRepositoryImpl` bilan bir xil — agar
 * keyinchalik ko'p endpoint qo'shilsa, error mapping `abstract base class`
 * yoki `safeApiCall` helper'ga ko'chirilishi mumkin (hozir 2 ta repo'da
 * — DRY hali zarur emas).
 */
class ScaleRepositoryImpl(
    private val api: ScaleApi
) : ScaleRepository {

    override suspend fun getScaleList(date: String): ApiResult<ScaleDay> = try {
        val response = api.getScaleList(date)

        when {
            // `success` non-null va `false` — error
            response.success == false -> ApiResult.Error(
                message = response.message?.takeIf { it.isNotBlank() } ?: MSG_GENERIC
            )

            response.data == null -> ApiResult.Error(message = MSG_GENERIC)

            else -> ApiResult.Success(response.data.toDomain())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        when (e.code()) {
            401 -> ApiResult.Unauthorized
            403 -> ApiResult.Error(403, MSG_FORBIDDEN)
            in 400..499 -> ApiResult.Error(e.code(), MSG_GENERIC)
            else -> ApiResult.Error(e.code(), MSG_SERVER_ERROR)
        }
    } catch (e: IOException) {
        ApiResult.NetworkError
    } catch (e: Exception) {
        ApiResult.Error(message = e.localizedMessage ?: MSG_GENERIC)
    }

    /* ===================== Mappers ===================== */

    private fun ScaleDataDto.toDomain(): ScaleDay = ScaleDay(
        date = date,
        total = total,
        records = records.map { it.toDomain() }
    )

    private fun ScaleRecordDto.toDomain(): ScaleRecord = ScaleRecord(
        plate = plate,
        kpp = kpp,
        entryTime = entryTime.normalizeDash(),
        exitTime = exitTime.normalizeDash(),
        brutto = brutto.normalizeDash(),
        tara = tara.normalizeDash(),
        netto = netto.normalizeDash(),
        status = ScaleStatus.fromRaw(status)
    )

    /**
     * Backend `"-"` placeholder → `null`.
     *
     * UI rendering sodda bo'ladi: `record.tara?.let { Text("Tara $it") }`
     * o'rniga `if (record.tara != "-") Text("Tara ${record.tara}")` —
     * birinchisi idiomatic Kotlin.
     */
    private fun String?.normalizeDash(): String? = when {
        this == null -> null
        isBlank() -> null
        trim() == "-" -> null
        else -> this
    }

    private companion object {
        const val MSG_FORBIDDEN = "Ruxsat berilmagan"
        const val MSG_SERVER_ERROR = "Server xatosi, qayta urinib ko'ring"
        const val MSG_GENERIC = "Ma'lumotlarni olib bo'lmadi"
    }
}
