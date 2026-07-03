package uz.jurabekov.guard.data.repository

import com.pusher.client.connection.ConnectionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.data.remote.api.QueueApi
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
    private val pusherClient: QueuePusherClient
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
            else -> ApiResult.Error(code = e.code(), message = e.message())
        }
    } catch (e: IOException) {
        ApiResult.NetworkError
    } catch (e: Exception) {
        ApiResult.Error(message = e.localizedMessage ?: "Noma'lum xatolik")
    }
}
