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
import uz.jurabekov.guard.domain.model.QueueSnapshot
import uz.jurabekov.guard.domain.model.QueueUpdate
import uz.jurabekov.guard.domain.model.VehicleType
import uz.jurabekov.guard.domain.repository.AuthRepository
import uz.jurabekov.guard.domain.repository.QueueRepository
import uz.jurabekov.guard.domain.usecase.CallInfoLaneUseCase
import uz.jurabekov.guard.domain.usecase.GetPermitsUseCase
import uz.jurabekov.guard.domain.usecase.GetQueueByDateUseCase
import uz.jurabekov.guard.domain.usecase.MarkManualEntryUseCase
import uz.jurabekov.guard.domain.usecase.ReleaseInfoLaneUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Navbat boshqaruvi ekrani VM.
 *
 * **`QueueViewModel`'dan farqi:**
 *  - Sana parametri qo'llab-quvvatlanadi (tarixiy kunlar)
 *  - Uch bo'lim ([QueueSection]) — permit / gate / given; xom item ro'yxatlaridan
 *    UI tomonida filtrlanadi (`QueueScreen`'dagi status-partition emas)
 *  - Item click → permit yuklash + dialog
 *  - Info-tablo: 1/2/3-yo'lga chaqirish + "O'tkazildi"
 *
 * **WS scope guard:** WS event'lari faqat `selectedDate == today()` bo'lganda
 * apply qilinadi (tarixiy snapshot ustiga bugungi o'zgarishlarni yozmaymiz).
 *
 * **Concurrency:** `loadJob` / `permitJob` / `laneJob` — har biri single-flight.
 */
class QueueManagementViewModel(
    private val getQueueByDate: GetQueueByDateUseCase,
    private val getPermits: GetPermitsUseCase,
    private val callInfoLane: CallInfoLaneUseCase,
    private val markManualEntry: MarkManualEntryUseCase,
    private val releaseInfoLane: ReleaseInfoLaneUseCase,
    private val repository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        QueueManagementUiState(selectedDate = today(), isLoading = true)
    )
    val state: StateFlow<QueueManagementUiState> = _state.asStateFlow()

    private val _effect = Channel<QueueManagementUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var loadJob: Job? = null
    private var permitJob: Job? = null
    private var laneJob: Job? = null

    /** `LifecycleResumeEffect` initial composition'da ham chaqiriladi — throttle. */
    @Volatile
    private var lastResumeRefreshAt: Long = 0L

    init {
        lastResumeRefreshAt = System.currentTimeMillis()
        loadQueue(initial = true)
        listenForWsUpdates()
        observeWsReconnect()
        observeUserRole()
    }

    fun onEvent(event: QueueManagementUiEvent) {
        when (event) {
            QueueManagementUiEvent.OpenDatePicker -> _state.update { it.copy(showDatePicker = true) }
            QueueManagementUiEvent.DismissDatePicker -> _state.update { it.copy(showDatePicker = false) }
            is QueueManagementUiEvent.DateSelected -> selectDate(event.date)

            QueueManagementUiEvent.Refresh -> loadQueue(initial = false)
            QueueManagementUiEvent.AppResumed -> handleAppResumed()

            is QueueManagementUiEvent.SectionSelected ->
                _state.update { it.copy(selectedSection = event.section) }

            is QueueManagementUiEvent.TabSelected -> {
                _state.update { it.copy(selectedTab = event.type) }
                // Bugungi sanada tab almashganda silent refresh — WS yo'qotgan
                // event'larni "ushlab oladi". Tarixiy sanada snapshot stable.
                if (isToday()) viewModelScope.launch { silentRefresh() }
            }

            is QueueManagementUiEvent.ItemClicked -> openPermitDialog(event.item)
            QueueManagementUiEvent.RetryPermit -> retryPermit()
            QueueManagementUiEvent.DismissPermitDialog -> dismissPermit()

            is QueueManagementUiEvent.LaneCallClicked -> runLaneAction(event.item.id) {
                callInfoLane(event.item.id, event.lane) to { item: QueueItem ->
                    item.copy(infoLane = event.lane)
                }
            }

            is QueueManagementUiEvent.ManualPassClicked -> runLaneAction(event.item.id) {
                markManualEntry(event.item.id) to { item: QueueItem ->
                    item.copy(manualPassed = true)
                }
            }

            is QueueManagementUiEvent.LaneReleaseClicked -> runLaneAction(event.item.id) {
                releaseInfoLane(event.item.id) to { item: QueueItem ->
                    item.copy(infoLane = null)
                }
            }
        }
    }

    /* ============================================================
     * Date selection
     * ============================================================ */
    private fun selectDate(date: String) {
        if (_state.value.selectedDate == date) {
            _state.update { it.copy(showDatePicker = false) }
            return
        }
        _state.update {
            it.copy(
                selectedDate = date,
                showDatePicker = false,
                // Stale ma'lumotni ko'rsatmaymiz — sana o'zgardi.
                openItems = emptyList(),
                tentItems = emptyList(),
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
            val result = getQueueByDate(date)

            // Race-condition guard: response kelguncha sana o'zgargan bo'lsa — tashlaymiz.
            if (_state.value.selectedDate != date) return@launch

            when (result) {
                is ApiResult.Success -> _state.update {
                    it.applySnapshot(result.data).copy(
                        isLoading = false, isRefreshing = false, listError = null
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, isRefreshing = false, listError = result.message)
                }
                ApiResult.NetworkError -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            listError = if (it.hasNoData) MSG_NO_INTERNET else null
                        )
                    }
                    if (!initial) emitToast(MSG_NO_INTERNET)
                }
                ApiResult.Unauthorized -> _state.update {
                    it.copy(isLoading = false, isRefreshing = false, listError = MSG_UNAUTHORIZED)
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
            it.copy(permitDialog = PermitDialogState.Loading(queueId = item.id, baseItem = item))
        }
        permitJob = viewModelScope.launch {
            val result = getPermits(item.id)
            if (_state.value.permitDialog?.queueId != item.id) return@launch
            _state.update { it.copy(permitDialog = mapResult(result, item)) }
        }
    }

    private fun retryPermit() {
        val ds = _state.value.permitDialog ?: return
        val item = ds.baseItem
        permitJob?.cancel()
        _state.update {
            it.copy(permitDialog = PermitDialogState.Loading(queueId = ds.queueId, baseItem = item))
        }
        permitJob = viewModelScope.launch {
            val result = getPermits(ds.queueId)
            if (_state.value.permitDialog?.queueId != ds.queueId) return@launch
            _state.update { it.copy(permitDialog = mapResult(result, item)) }
        }
    }

    private fun mapResult(result: ApiResult<List<Permit>>, item: QueueItem?): PermitDialogState {
        val queueId = item?.id ?: 0L
        return when (result) {
            is ApiResult.Success -> result.data.firstOrNull()
                ?.let { PermitDialogState.Loaded(queueId, item, it) }
                ?: PermitDialogState.Empty(queueId, item)
            is ApiResult.Error -> PermitDialogState.Error(queueId, item, result.message)
            ApiResult.NetworkError -> PermitDialogState.Error(queueId, item, MSG_NO_INTERNET)
            ApiResult.Unauthorized -> PermitDialogState.Error(queueId, item, MSG_UNAUTHORIZED)
        }
    }

    private fun dismissPermit() {
        permitJob?.cancel()
        _state.update { it.copy(permitDialog = null) }
    }

    /* ============================================================
     * Info-tablo: yo'lga chaqirish / o'tkazildi
     * ============================================================ */

    /**
     * Ikkala action uchun umumiy oqim:
     *  1. Rol + single-flight guard
     *  2. So'rov (progress → chaqiruv tugmalari disable)
     *  3. Success → optimistik local patch + toast + silent refresh (reconcile)
     *  4. Error → faqat toast; local state tegilmaydi
     *
     * @param request so'rovni bajaradi va success holida item'ga qo'llaniladigan
     *                optimistik transform'ni qaytaradi.
     */
    private fun runLaneAction(
        queueId: Long,
        request: suspend () -> Pair<ApiResult<String>, (QueueItem) -> QueueItem>
    ) {
        if (!_state.value.canManageInfoLane) return
        if (laneJob?.isActive == true) return

        _state.update { it.copy(laneActionInProgressId = queueId) }

        laneJob = viewModelScope.launch {
            val (result, transform) = request()

            when (result) {
                is ApiResult.Success -> {
                    _state.update { it.updateItem(queueId, transform) }
                    result.data.takeIf { it.isNotBlank() }?.let(::emitToast)
                    silentRefresh()
                }
                is ApiResult.Error -> emitToast(result.message.ifBlank { MSG_ACTION_FAILED })
                ApiResult.NetworkError -> emitToast(MSG_NO_INTERNET)
                ApiResult.Unauthorized -> emitToast(MSG_UNAUTHORIZED)
            }

            _state.update { it.copy(laneActionInProgressId = null) }
        }
    }

    /* ============================================================
     * Rol kuzatuvi
     * ============================================================ */
    private fun observeUserRole() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                val role = user?.roleCode
                _state.update { st ->
                    val sections = QueueSection.availableFor(role)
                    // Tanlangan bo'lim rolga ko'rinmasa — birinchi mavjudiga.
                    val selected = if (st.selectedSection in sections) st.selectedSection
                    else sections.first()
                    st.copy(userRole = role, selectedSection = selected)
                }
            }
        }
    }

    /* ============================================================
     * WebSocket — faqat joriy sanada apply
     * ============================================================ */
    private fun listenForWsUpdates() {
        viewModelScope.launch {
            repository.observeUpdates()
                .filter { isToday() }
                .collect { update ->
                    when (update) {
                        is QueueUpdate.Booked -> handleBooked(update.item)
                        // Ruxsatnoma berilishi bo'lim a'zoligini o'zgartiradi
                        // (permit → gate). Eng ishonchli yo'l — REST reconcile.
                        is QueueUpdate.Permitted -> silentRefresh()
                    }
                }
        }
    }

    /** Yangi navbatga turgan mashina (ruxsatnomasiz) — mos ro'yxatga qo'shamiz. */
    private fun handleBooked(newItem: QueueItem) {
        _state.update { st ->
            val exists = (st.openItems + st.tentItems).any { it.uuid == newItem.uuid }
            if (exists) st else st.addItem(newItem)
        }
    }

    /**
     * WS reconnect → silent REST refresh (Pusher at-most-once delivery uchun).
     */
    private fun observeWsReconnect() {
        viewModelScope.launch {
            var hasBeenConnected = false
            repository.wsConnectionState.collect { state ->
                if (state == ConnectionState.CONNECTED) {
                    if (hasBeenConnected) {
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
            silentRefresh()
            if (isToday() &&
                repository.wsConnectionState.value != ConnectionState.CONNECTED
            ) {
                repository.reconnectWebSocket()
            }
        }
    }

    private fun emitToast(message: String) {
        viewModelScope.launch { _effect.send(QueueManagementUiEffect.ShowToast(message)) }
    }

    /* ============================================================
     * State helpers — xom ro'yxat ustida
     * ============================================================ */

    private fun QueueManagementUiState.applySnapshot(snapshot: QueueSnapshot) = copy(
        openItems = snapshot.open.allItems(),
        tentItems = snapshot.tent.allItems(),
        queueDate = snapshot.queueDate
    )

    /**
     * `list` + `next_queue`'ni bitta ro'yxatga birlashtiradi (next_queue ba'zan
     * alohida keladi). Dedup `uuid` bo'yicha.
     */
    private fun QueueData.allItems(): List<QueueItem> {
        val next = nextQueue ?: return items
        return if (items.any { it.uuid == next.uuid }) items else items + next
    }

    /** Yangi item'ni type'iga qarab tegishli ro'yxatga qo'shadi. */
    private fun QueueManagementUiState.addItem(item: QueueItem) = when (item.type) {
        VehicleType.OPEN -> copy(openItems = openItems + item)
        VehicleType.TENT -> copy(tentItems = tentItems + item)
    }

    /** `id` bo'yicha item'ni ikkala ro'yxatda ham yangilaydi. */
    private fun QueueManagementUiState.updateItem(
        id: Long,
        transform: (QueueItem) -> QueueItem
    ) = copy(
        openItems = openItems.map { if (it.id == id) transform(it) else it },
        tentItems = tentItems.map { if (it.id == id) transform(it) else it }
    )

    private fun isToday(): Boolean = _state.value.selectedDate == today()

    private companion object {
        const val TAG = "QueueMgmtVM"
        const val RESUME_REFRESH_THROTTLE_MS = 5_000L

        const val MSG_NO_INTERNET = "Internet aloqasi yo'q"
        const val MSG_UNAUTHORIZED = "Sessiya muddati tugagan, qayta kiring"
        const val MSG_ACTION_FAILED = "Amalni bajarib bo'lmadi"

        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
