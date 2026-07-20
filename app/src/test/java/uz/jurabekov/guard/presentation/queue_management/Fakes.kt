package uz.jurabekov.guard.presentation.queue_management

import com.pusher.client.connection.ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.model.AuthSession
import uz.jurabekov.guard.domain.model.Permit
import uz.jurabekov.guard.domain.model.QueueData
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.QueueSnapshot
import uz.jurabekov.guard.domain.model.QueueUpdate
import uz.jurabekov.guard.domain.model.User
import uz.jurabekov.guard.domain.model.VehicleType
import uz.jurabekov.guard.domain.repository.AuthRepository
import uz.jurabekov.guard.domain.repository.QueueRepository

/**
 * Server o'rniga ishlatiladigan soxta repository.
 *
 * Info-tablo action'lari **backend kabi** o'zini tutadi: muvaffaqiyatli
 * chaqiruvdan keyin ichki snapshot ham yangilanadi — shu tufayli VM'ning
 * `silentRefresh()` reconcile qadamini ham realistik tekshira olamiz.
 */
class FakeQueueRepository(
    initialItems: List<QueueItem> = emptyList()
) : QueueRepository {

    /** Backend "bazasi" — action'lar shu ro'yxatni o'zgartiradi. */
    var items: List<QueueItem> = initialItems

    /** Testda xato stsenariylarini modellash uchun almashtiriladi. */
    var callLaneResult: ApiResult<String> = ApiResult.Success("Mashina yo'lga chaqirildi")
    var manualEntryResult: ApiResult<String> = ApiResult.Success("Mashina o'tkazildi")

    /** `> 0` bo'lsa so'rov "sekin" bo'ladi — single-flight testi uchun. */
    var actionDelayMs: Long = 0

    /** Chaqiruv tarixi — API haqiqatan chaqirilganini tekshirish uchun. */
    val laneCalls = mutableListOf<Pair<Long, Int>>()
    val manualEntries = mutableListOf<Long>()

    private val updates = MutableSharedFlow<QueueUpdate>()
    private val _wsState = MutableStateFlow(ConnectionState.CONNECTED)

    override suspend fun fetchQueue(date: String?): ApiResult<QueueSnapshot> =
        ApiResult.Success(
            QueueSnapshot(
                open = QueueData(items = items, nextQueue = null),
                tent = QueueData(items = emptyList(), nextQueue = null),
                queueDate = date
            )
        )

    override suspend fun callInfoLane(queueId: Long, lane: Int): ApiResult<String> {
        if (actionDelayMs > 0) delay(actionDelayMs)
        laneCalls += queueId to lane
        if (callLaneResult is ApiResult.Success) {
            items = items.map { if (it.id == queueId) it.copy(infoLane = lane) else it }
        }
        return callLaneResult
    }

    override suspend fun markManualEntry(queueId: Long): ApiResult<String> {
        if (actionDelayMs > 0) delay(actionDelayMs)
        manualEntries += queueId
        if (manualEntryResult is ApiResult.Success) {
            items = items.map { if (it.id == queueId) it.copy(manualPassed = true) else it }
        }
        return manualEntryResult
    }

    override fun observeUpdates(): Flow<QueueUpdate> = updates

    override suspend fun fetchPermits(queueId: Long): ApiResult<List<Permit>> =
        ApiResult.Success(emptyList())

    override suspend fun submitQueue(
        type: VehicleType,
        plate: String,
        fullName: String,
        passportSeries: String?,
        passportNumber: String?
    ): ApiResult<QueueItem> = error("Testda ishlatilmaydi")

    override suspend fun cancelOwnerQueue(ownerToken: String, plate: String): ApiResult<Unit> =
        ApiResult.Success(Unit)

    override val wsConnectionState: StateFlow<ConnectionState> = _wsState.asStateFlow()
    override val wsLastEventAt: StateFlow<Long> = MutableStateFlow(0L).asStateFlow()
    override fun reconnectWebSocket() = Unit
}

/** Rol gating testlari uchun — `currentUser` qiymatini testda beramiz. */
class FakeAuthRepository(user: User?) : AuthRepository {
    private val _user = MutableStateFlow(user)

    override val isLoggedIn: Flow<Boolean> = MutableStateFlow(user != null)
    override val currentUser: Flow<User?> = _user.asStateFlow()

    override suspend fun login(username: String, password: String): ApiResult<AuthSession> =
        error("Testda ishlatilmaydi")

    override suspend fun logout() {
        _user.value = null
    }
}

/** `role_code` bo'yicha test useri. */
fun testUser(roleCode: String) = User(
    id = 1,
    fullName = "Test User",
    username = "test",
    roleCode = roleCode,
    status = "active",
    permissions = listOf("queue.view")
)
