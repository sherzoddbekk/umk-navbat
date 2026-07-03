package uz.jurabekov.guard.presentation.queue_management

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pusher.client.connection.ConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.model.Permit
import uz.jurabekov.guard.domain.model.QueueData
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.QueueItemStatus
import uz.jurabekov.guard.domain.model.QueueSnapshot
import uz.jurabekov.guard.domain.model.QueueUpdate
import uz.jurabekov.guard.domain.model.VehicleType
import uz.jurabekov.guard.domain.repository.QueueRepository
import uz.jurabekov.guard.domain.usecase.GetPermitsUseCase
import uz.jurabekov.guard.domain.usecase.GetQueueByDateUseCase
import uz.jurabekov.guard.presentation.queue.TabState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Navbat boshqaruvi ekrani VM.
 *
 * **`QueueViewModel`'dan farqi:**
 *  - Sana parametri qo'llab-quvvatlanadi (tarixiy kunlar)
 *  - WS event'lari faqat `selectedDate == today()` bo'lganda apply qilinadi
 *  - Submit (yangi navbat olish) funksiyasi yo'q (faqat ko'rish)
 *  - Item click → permit yuklash + dialog state
 *
 * **WS scope guard mantiqi:**
 *  Foydalanuvchi tarixiy sanani tanlasa — WS event'lari (`Booked`, `Permitted`)
 *  bugungi sana uchun keladi. Ularni tarixiy snapshot ustiga apply qilmaymiz
 *  (consistency: foydalanuvchi 2026-05-15 ko'rmoqda, lekin 2026-05-20'da
 *  yangi mashina navbatga turdi — uni ko'rsatish noto'g'ri).
 *
 *  Solution: `.filter { isToday() }` collect oqimida. Re-emission yo'q —
 *  faqat skip. Foydalanuvchi bugunga qaytsa → `silentRefresh()` REST orqali
 *  fresh snapshot oladi.
 *
 * **Concurrency:**
 *  - `loadJob` single-flight: sana o'zgarsa eski request cancel.
 *  - `permitJob` single-flight: ikkita item ketma-ket bosilsa, eski request
 *    cancel — race condition'da eski permit dialog'da chiqib qolishi mumkin.
 */
class QueueManagementViewModel(
    private val getQueueByDate: GetQueueByDateUseCase,
    private val getPermits: GetPermitsUseCase,
    private val repository: QueueRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        QueueManagementUiState(selectedDate = today(), isLoading = true)
    )
    val state: StateFlow<QueueManagementUiState> = _state.asStateFlow()

    private val _effect = Channel<QueueManagementUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var loadJob: Job? = null
    private var permitJob: Job? = null

    /**
     * `LifecycleResumeEffect` initial composition'da ham chaqiriladi — burst
     * REST oldini olish uchun throttle. `QueueViewModel` bilan bir xil pattern.
     */
    @Volatile
    private var lastResumeRefreshAt: Long = 0L

    init {
        lastResumeRefreshAt = System.currentTimeMillis()
        loadQueue(initial = true)
        listenForWsUpdates()
        observeWsReconnect()
    }

    fun onEvent(event: QueueManagementUiEvent) {
        when (event) {
            QueueManagementUiEvent.OpenDatePicker -> _state.update {
                it.copy(showDatePicker = true)
            }
            QueueManagementUiEvent.DismissDatePicker -> _state.update {
                it.copy(showDatePicker = false)
            }
            is QueueManagementUiEvent.DateSelected -> selectDate(event.date)

            QueueManagementUiEvent.Refresh -> loadQueue(initial = false)
            QueueManagementUiEvent.AppResumed -> handleAppResumed()

            is QueueManagementUiEvent.TabSelected -> {
                _state.update { it.copy(selectedTab = event.type) }
                // Bugungi sana ustida ishlasak, tab almashganda silent refresh —
                // WS yo'qolgan event'larni "ushlab oladi". Tarixiy sanada
                // qayta yuklamaymiz (snapshot stable).
                if (isToday()) {
                    viewModelScope.launch { silentRefresh() }
                }
            }

            is QueueManagementUiEvent.ItemClicked -> openPermitDialog(event.item)
            QueueManagementUiEvent.RetryPermit -> retryPermit()
            QueueManagementUiEvent.DismissPermitDialog -> dismissPermit()
        }
    }

    /* ============================================================
     * Date selection
     * ============================================================ */
    private fun selectDate(date: String) {
        // Bir xil sana — request yubormaymiz, dialog'ni yopamiz xolos.
        if (_state.value.selectedDate == date) {
            _state.update { it.copy(showDatePicker = false) }
            return
        }

        _state.update {
            it.copy(
                selectedDate = date,
                showDatePicker = false,
                // Stale ma'lumotni ko'rsatmaymiz — sana o'zgardi.
                open = TabState(),
                tent = TabState(),
                queueDate = null,
                listError = null
            )
        }
        loadQueue(initial = true)
    }

    /* ============================================================
     * REST loaders
     * ============================================================ */
    private fun loadQueue(initial: Boolean) {
        loadJob?.cancel()
        val date = _state.value.selectedDate

        _state.update {
            if (initial) it.copy(isLoading = true, listError = null)
            else it.copy(isRefreshing = true, listError = null)
        }

        loadJob = viewModelScope.launch {
            // Backend kontraktiga ko'ra `null` → bugungi sana. Ammo biz doim
            // explicit `yyyy-MM-dd` yuboramiz — backend bug'lariga immune.
            val result = getQueueByDate(date)

            // Race-condition guard: response kelguniga foydalanuvchi sana
            // o'zgartirgan bo'lsa, response'ni e'tibordan chetda qoldiramiz.
            if (_state.value.selectedDate != date) return@launch

            when (result) {
                is ApiResult.Success -> _state.update {
                    it.applySnapshot(result.data).copy(
                        isLoading = false,
                        isRefreshing = false,
                        listError = null
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        listError = result.message
                    )
                }
                ApiResult.NetworkError -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            listError = if (it.open.isFullyEmpty && it.tent.isFullyEmpty) MSG_NO_INTERNET else null
                        )
                    }
                    if (!initial) emitToast(MSG_NO_INTERNET)
                }
                ApiResult.Unauthorized -> _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        listError = MSG_UNAUTHORIZED
                    )
                }
            }
        }
    }

    private suspend fun silentRefresh() {
        val date = _state.value.selectedDate
        when (val result = getQueueByDate(date)) {
            is ApiResult.Success -> {
                if (_state.value.selectedDate != date) return
                _state.update { it.applySnapshot(result.data) }
            }
            else -> Log.w(TAG, "silentRefresh failed: $result")
        }
    }

    /* ============================================================
     * Permit dialog
     * ============================================================ */
    private fun openPermitDialog(item: QueueItem) {
        permitJob?.cancel()

        _state.update {
            it.copy(
                permitDialog = PermitDialogState.Loading(
                    queueId = item.id,
                    baseItem = item
                )
            )
        }

        permitJob = viewModelScope.launch {
            val result = getPermits(item.id)

            // Race: foydalanuvchi dialog'ni yopib ulgursa — apply qilmaymiz.
            val current = _state.value.permitDialog
            if (current?.queueId != item.id) return@launch

            _state.update { it.copy(permitDialog = mapResult(result, item)) }
        }
    }

    private fun retryPermit() {
        val ds = _state.value.permitDialog ?: return
        val item = ds.baseItem
        permitJob?.cancel()

        _state.update {
            it.copy(
                permitDialog = PermitDialogState.Loading(
                    queueId = ds.queueId,
                    baseItem = item
                )
            )
        }

        permitJob = viewModelScope.launch {
            val result = getPermits(ds.queueId)
            val current = _state.value.permitDialog
            if (current?.queueId != ds.queueId) return@launch
            _state.update { it.copy(permitDialog = mapResult(result, item)) }
        }
    }

    private fun mapResult(
        result: ApiResult<List<Permit>>,
        item: QueueItem?
    ): PermitDialogState {
        val queueId = item?.id ?: 0L
        return when (result) {
            is ApiResult.Success -> {
                val first = result.data.firstOrNull()
                if (first == null) {
                    PermitDialogState.Empty(queueId = queueId, baseItem = item)
                } else {
                    PermitDialogState.Loaded(queueId = queueId, baseItem = item, permit = first)
                }
            }
            is ApiResult.Error -> PermitDialogState.Error(
                queueId = queueId,
                baseItem = item,
                message = result.message
            )
            ApiResult.NetworkError -> PermitDialogState.Error(
                queueId = queueId,
                baseItem = item,
                message = MSG_NO_INTERNET
            )
            ApiResult.Unauthorized -> PermitDialogState.Error(
                queueId = queueId,
                baseItem = item,
                message = MSG_UNAUTHORIZED
            )
        }
    }

    private fun dismissPermit() {
        permitJob?.cancel()
        _state.update { it.copy(permitDialog = null) }
    }

    /* ============================================================
     * WebSocket — faqat joriy sanada apply
     * ============================================================ */
    private fun listenForWsUpdates() {
        viewModelScope.launch {
            repository.observeUpdates()
                .filter { isToday() }   // tarixiy sanada e'tiborga olmaymiz
                .collect { update ->
                    when (update) {
                        is QueueUpdate.Booked -> handleBooked(update.item)
                        is QueueUpdate.Permitted -> handlePermitted(
                            update.nextOpen,
                            update.nextTent
                        )
                    }
                }
        }
    }

    /**
     * WS reconnect → silent REST refresh. `QueueViewModel`'dagi pattern.
     * Reconcile-on-reconnect (Pusher at-most-once delivery uchun).
     */
    private fun observeWsReconnect() {
        viewModelScope.launch {
            var hasBeenConnected = false
            repository.wsConnectionState.collect { state ->
                if (state == ConnectionState.CONNECTED) {
                    if (hasBeenConnected) {
                        Log.i(TAG, "📡 WS reconnected → reconcile via REST (date=${_state.value.selectedDate})")
                        if (isToday()) silentRefresh()
                    } else {
                        hasBeenConnected = true
                    }
                }
            }
        }
    }

    private fun handleAppResumed() {
        val now = System.currentTimeMillis()
        if (now - lastResumeRefreshAt < RESUME_REFRESH_THROTTLE_MS) return
        lastResumeRefreshAt = now

        viewModelScope.launch {
            silentRefresh()  // sana qaysi bo'lsa ham — fresh ma'lumot foydali.

            // WS reconnect faqat bugungi sanada va WS sog'lom emas bo'lsa.
            if (isToday() &&
                repository.wsConnectionState.value != ConnectionState.CONNECTED
            ) {
                repository.reconnectWebSocket()
            }
        }
    }

    private fun handleBooked(newItem: QueueItem) {
        if (newItem.hasPermit) return  // BOOKED'da has_permit=true bo'lmaydi normally

        _state.update { current ->
            current.updateTab(newItem.type) { tab ->
                if (tab.containsItem(newItem.uuid)) tab
                else promoteToBannerIfFirst(tab, newItem)
            }
        }
    }

    private fun handlePermitted(
        nextOpen: QueueItem?,
        nextTent: QueueItem?
    ) {
        var needsRefresh = false

        _state.update { current ->
            var updated = current

            updated = when {
                nextOpen != null -> updated.updateTab(VehicleType.OPEN) {
                    applyPermitted(it, nextOpen)
                }
                current.open.currentlyEntering != null -> {
                    needsRefresh = true
                    updated.updateTab(VehicleType.OPEN) { it.copy(currentlyEntering = null) }
                }
                else -> updated
            }

            updated = when {
                nextTent != null -> updated.updateTab(VehicleType.TENT) {
                    applyPermitted(it, nextTent)
                }
                current.tent.currentlyEntering != null -> {
                    needsRefresh = true
                    updated.updateTab(VehicleType.TENT) { it.copy(currentlyEntering = null) }
                }
                else -> updated
            }

            updated
        }

        if (needsRefresh && isToday()) {
            viewModelScope.launch { silentRefresh() }
        }
    }

    private fun emitToast(message: String) {
        viewModelScope.launch { _effect.send(QueueManagementUiEffect.ShowToast(message)) }
    }

    /* ============================================================
     * State helpers — `QueueViewModel`'dan duplikat
     * ============================================================
     * NOTE: Bu helper'lar `QueueViewModel`'da ham bor (private). Hozircha
     * duplikatsiya — kelajakda `presentation/queue/common/TabStateOps.kt`
     * deb shared file'ga ko'chirish kerak. Production'da risk minimal —
     * pure funksiyalar, behavior identical.
     */

    private fun QueueManagementUiState.applySnapshot(snapshot: QueueSnapshot): QueueManagementUiState = copy(
        open = snapshot.open.toTabState(),
        tent = snapshot.tent.toTabState(),
        queueDate = snapshot.queueDate
    )

    private fun QueueData.toTabState(): TabState {
        val nextQ = nextQueue.takeBannerOrNull()
        val (waiting, history) = items.partitionForUi(nextQ)
        return TabState(
            currentlyEntering = nextQ,
            waitingItems = waiting,
            enteredItems = history
        )
    }

    private fun QueueItem?.takeBannerOrNull(): QueueItem? =
        this?.takeIf { it.status == QueueItemStatus.WAITING }

    private fun List<QueueItem>.partitionForUi(
        currentEntering: QueueItem?
    ): Pair<List<QueueItem>, List<QueueItem>> {
        val waiting = ArrayList<QueueItem>(size)
        val history = ArrayList<QueueItem>(size)

        for (item in this) {
            if (item.uuid == currentEntering?.uuid) continue
            when (item.status) {
                QueueItemStatus.WAITING -> waiting += item
                QueueItemStatus.ENTERED, QueueItemStatus.SKIPPED -> history += item
            }
        }

        waiting.sortBy { it.queueNumber }
        history.sortBy { it.queueNumber }
        return waiting to history
    }

    private fun QueueManagementUiState.updateTab(
        type: VehicleType,
        block: (TabState) -> TabState
    ): QueueManagementUiState = when (type) {
        VehicleType.OPEN -> copy(open = block(open))
        VehicleType.TENT -> copy(tent = block(tent))
    }

    private fun TabState.containsItem(uuid: String): Boolean =
        waitingItems.any { it.uuid == uuid } ||
                enteredItems.any { it.uuid == uuid } ||
                currentlyEntering?.uuid == uuid

    private fun promoteToBannerIfFirst(tab: TabState, newItem: QueueItem): TabState {
        val shouldBeBanner = tab.currentlyEntering == null && run {
            val minWaitingQ = tab.waitingItems.minOfOrNull { it.queueNumber }
            minWaitingQ == null || newItem.queueNumber <= minWaitingQ
        }

        return if (shouldBeBanner) {
            tab.copy(currentlyEntering = newItem.copy(status = QueueItemStatus.WAITING))
        } else {
            tab.copy(
                waitingItems = (tab.waitingItems + newItem.copy(status = QueueItemStatus.WAITING))
                    .sortedBy { it.queueNumber }
            )
        }
    }

    private fun applyPermitted(tab: TabState, nextItem: QueueItem): TabState {
        val previousEntering = tab.currentlyEntering
            ?.takeIf { it.uuid != nextItem.uuid }
            ?.copy(status = QueueItemStatus.ENTERED, hasPermit = true)

        val (skipped, stillWaiting) = tab.waitingItems.partition {
            it.queueNumber < nextItem.queueNumber && it.uuid != nextItem.uuid
        }

        val skippedItems = skipped.map { it.copy(status = QueueItemStatus.SKIPPED) }

        val newHistory = buildList {
            addAll(tab.enteredItems)
            if (previousEntering != null) add(previousEntering)
            addAll(skippedItems)
        }.sortedBy { it.queueNumber }

        val newWaiting = stillWaiting.filterNot { it.uuid == nextItem.uuid }

        return tab.copy(
            currentlyEntering = nextItem.copy(status = QueueItemStatus.WAITING),
            waitingItems = newWaiting,
            enteredItems = newHistory
        )
    }

    private fun isToday(): Boolean = _state.value.selectedDate == today()

    private companion object {
        const val TAG = "QueueMgmtVM"
        const val RESUME_REFRESH_THROTTLE_MS = 5_000L

        const val MSG_NO_INTERNET = "Internet aloqasi yo'q"
        const val MSG_UNAUTHORIZED = "Sessiya muddati tugagan, qayta kiring"

        fun today(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
