package uz.jurabekov.guard.domain.repository

import com.pusher.client.connection.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.model.Permit
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.QueueSnapshot
import uz.jurabekov.guard.domain.model.QueueUpdate
import uz.jurabekov.guard.domain.model.VehicleType

interface QueueRepository {

    /**
     * Navbat snapshotini olish.
     *
     * @param date `null` — backend bugungi sanani qaytaradi (default).
     *             `"yyyy-MM-dd"` — aniq sana (Navbat boshqaruvi ekranida).
     *
     * Default qiymat interfacedagina belgilanadi — bu mavjud `QueueScreen`'ning
     * `fetchQueue()` chaqiruvini buzmaydi.
     */
    suspend fun fetchQueue(date: String? = null): ApiResult<QueueSnapshot>

    fun observeUpdates(): Flow<QueueUpdate>

    suspend fun submitQueue(
        type: VehicleType,
        plate: String,
        fullName: String,
        passportSeries: String?,
        passportNumber: String?
    ): ApiResult<QueueItem>

    /** Bitta navbat uchun barcha permit'lar (joriy specga ko'ra 0..1 ta). */
    suspend fun fetchPermits(queueId: Long): ApiResult<List<Permit>>

    /** Pusher connection holati. */
    val wsConnectionState: StateFlow<ConnectionState>

    /** Pusher'dan oxirgi event qachon kelgan (epoch ms). 0 = hech qachon. */
    val wsLastEventAt: StateFlow<Long>

    /** Pusher'ni qayta ulash. */
    fun reconnectWebSocket()
}
