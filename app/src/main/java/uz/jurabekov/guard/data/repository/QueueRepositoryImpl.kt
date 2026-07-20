package uz.jurabekov.guard.data.repository

import com.pusher.client.connection.ConnectionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.data.remote.api.QueueApi
import uz.jurabekov.guard.data.remote.dto.ApiErrorDto
import uz.jurabekov.guard.data.remote.dto.InfoLaneActionResponseDto
import uz.jurabekov.guard.data.remote.dto.InfoLaneCallRequestDto
import uz.jurabekov.guard.data.remote.dto.QueueCancelRequestDto
import uz.jurabekov.guard.data.remote.dto.QueueListDataDto
import uz.jurabekov.guard.data.remote.dto.QueueRequestDto
import uz.jurabekov.guard.data.remote.websocket.QueuePusherClient
import uz.jurabekov.guard.domain.model.Permit
import uz.jurabekov.guard.domain.model.QueueData
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.QueueSnapshot
import uz.jurabekov.guard.domain.model.QueueUpdate
import uz.jurabekov.guard.domain.model.VehicleType
import uz.jurabekov.guard.domain.repository.QueueRepository
import java.io.IOException

class QueueRepositoryImpl(
    private val api: QueueApi,
    private val pusherClient: QueuePusherClient,
    private val json: Json
) : QueueRepository {

    override val wsConnectionState: StateFlow<ConnectionState>
        get() = pusherClient.connectionState

    override val wsLastEventAt: StateFlow<Long>
        get() = pusherClient.lastEventAt

    override fun reconnectWebSocket() = pusherClient.reconnect()

    override suspend fun fetchQueue(date: String?): ApiResult<QueueSnapshot> = safeCall {
        val response = api.getQueueList(date)
        QueueSnapshot(
            open = response.data.toDomain(),
            tent = response.dataTent.toDomain(),
            // Ikkala blok ham bir xil sanani qaytaradi — `data`'ni ustun olamiz,
            // bo'sh bo'lsa (eski API yoki backend bug) — `data_tent` ga fallback.
            queueDate = response.data.queueDate ?: response.dataTent.queueDate
        )
    }

    override suspend fun fetchPermits(queueId: Long): ApiResult<List<Permit>> = safeCall {
        api.getPermits(queueId).data.map { it.toDomain() }
    }

    override suspend fun cancelOwnerQueue(
        ownerToken: String,
        plate: String
    ): ApiResult<Unit> = safeCall {
        val response = api.cancelOwnerQueue(
            QueueCancelRequestDto(ownerToken = ownerToken, plate = plate)
        )
        // Backend 200 qaytarib, ammo `success=false` bo'lishi mumkin
        // (masalan, allaqachon bekor qilingan). Buni xatolik deb hisoblaymiz.
        if (!response.success) {
            throw IllegalStateException(response.message ?: "Navbatni bekor qilib bo'lmadi")
        }
    }

    override suspend fun callInfoLane(queueId: Long, lane: Int): ApiResult<String> = safeCall {
        api.callInfoLane(queueId, InfoLaneCallRequestDto(lane = lane)).requireMessage()
    }

    override suspend fun markManualEntry(queueId: Long): ApiResult<String> = safeCall {
        api.markManualEntry(queueId).requireMessage()
    }

    /**
     * HTTP 200 + `success=false` — backend biznes-xatoligi (masalan, yo'l band).
     * `safeCall` uni `ApiResult.Error`ga aylantiradi, UI toast ko'rsatadi.
     */
    private fun InfoLaneActionResponseDto.requireMessage(): String {
        if (!success) throw IllegalStateException(message ?: "Amalni bajarib bo'lmadi")
        return message.orEmpty()
    }

    private fun QueueListDataDto.toDomain(): QueueData = QueueData(
        items = list.map { it.toDomain() },
        nextQueue = nextQueue?.toDomain()
    )

    override fun observeUpdates(): Flow<QueueUpdate> = pusherClient.updates.map { raw ->
        when (raw) {
            is QueuePusherClient.RawUpdate.Booked ->
                QueueUpdate.Booked(raw.item.toDomain())

            is QueuePusherClient.RawUpdate.Permitted ->
                QueueUpdate.Permitted(
                    nextOpen = raw.nextOpen?.toDomain(),
                    nextTent = raw.nextTent?.toDomain()
                )
        }
    }

    override suspend fun submitQueue(
        type: VehicleType,
        plate: String,
        fullName: String,
        passportSeries: String?,
        passportNumber: String?
    ): ApiResult<QueueItem> = safeCall {
        api.submitQueue(
            QueueRequestDto(
                type = type.toBackend(),
                plate = plate,
                fullName = fullName,
                passportSeries = passportSeries,
                passportNumber = passportNumber
            )
        ).data.toDomain()
    }

    /**
     * Network call'ni o'rab oladi va xatoliklarni `ApiResult`'ga aylantiradi.
     *
     * MUHIM: `CancellationException` REthrow qilinadi.
     * Aks holda, ekran yopilgan paytda coroutine cancel bo'lsa,
     * structured concurrency buziladi va `ApiResult.Error` yuzaga kelib,
     * yopilgan ViewModel'ning state'i noto'g'ri yangilanishi mumkin.
     */
    private inline fun <T> safeCall(block: () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        // Coroutine cancellation - hech qachon ushlamaymiz
        throw e
    } catch (e: HttpException) {
        when (e.code()) {
            401 -> ApiResult.Unauthorized
            // Backend `errorBody`'dagi `message`'ni ustun olamiz (422 validatsiya
            // xatolari), bo'lmasa HTTP reason phrase'ga fallback.
            else -> ApiResult.Error(
                code = e.code(),
                message = extractErrorMessage(e) ?: e.message()
            )
        }
    } catch (e: IOException) {
        ApiResult.NetworkError
    } catch (e: Exception) {
        ApiResult.Error(message = e.localizedMessage ?: "Noma'lum xatolik")
    }

    /**
     * HTTP xatolik javobidan (`errorBody`) `message` maydonini ajratib oladi.
     * Parse imkonsiz bo'lsa (bo'sh yoki noto'g'ri JSON) — `null` qaytadi.
     *
     * `errorBody` faqat bir marta o'qiladi; barcha xatolar defensive tarzda
     * yutiladi (bu yerda crash bo'lmasligi kerak).
     */
    private fun extractErrorMessage(e: HttpException): String? = runCatching {
        val raw = e.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return null
        json.decodeFromString(ApiErrorDto.serializer(), raw).message?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
