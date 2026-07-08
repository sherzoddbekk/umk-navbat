package uz.jurabekov.guard.presentation.queue

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pusher.client.connection.ConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.core.util.Constants
import uz.jurabekov.guard.core.util.InputValidator
import uz.jurabekov.guard.data.preferences.AnnouncementPreferences
import uz.jurabekov.guard.data.preferences.MyQueuesPreferences
import uz.jurabekov.guard.data.preferences.OwnedQueuesPreferences
import uz.jurabekov.guard.data.preferences.SavedDriverPreferences
import uz.jurabekov.guard.data.preferences.dto.OwnedQueue
import uz.jurabekov.guard.data.preferences.dto.SavedDriverData
import uz.jurabekov.guard.domain.model.QueueData
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.QueueItemStatus
import uz.jurabekov.guard.domain.model.QueueSnapshot
import uz.jurabekov.guard.domain.model.QueueUpdate
import uz.jurabekov.guard.domain.model.VehicleType
import uz.jurabekov.guard.domain.repository.QueueRepository
import uz.jurabekov.guard.domain.usecase.CancelOwnerQueueUseCase
import uz.jurabekov.guard.domain.usecase.GetQueueListUseCase
import uz.jurabekov.guard.domain.usecase.ObserveQueueUpdatesUseCase
import uz.jurabekov.guard.domain.usecase.SubmitQueueUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Queue ViewModel — ikki tab (OPEN / TENT) holatini parallel boshqaradi.
 *
 * Asosiy qoidalar:
 *  - REST snapshot ikkala tabni atomic ravishda yangilaydi.
 *  - WS event'lar `item.type` ga qarab tegishli tabga marshrutlanadi.
 *  - Submit foydalanuvchi tanlagan tip bilan yuboriladi (dialog'dagi radio).
 */
class QueueViewModel(
    private val getQueueList: GetQueueListUseCase,
    private val submitQueue: SubmitQueueUseCase,
    private val observeUpdates: ObserveQueueUpdatesUseCase,
    private val cancelOwnerQueue: CancelOwnerQueueUseCase,
    private val repository: QueueRepository,
    private val myQueuesPreferences: MyQueuesPreferences,
    private val ownedQueuesPreferences: OwnedQueuesPreferences,
    private val savedDriverPreferences: SavedDriverPreferences,
    private val announcementPreferences: AnnouncementPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(QueueUiState(isLoading = true))
    val state: StateFlow<QueueUiState> = _state.asStateFlow()

    private val _effect = Channel<QueueUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var autoSyncJob: Job? = null

    /**
     * App resume refresh throttle — `LifecycleResumeEffect` initial composition'da
     * ham bir marta chaqiriladi (init bilan birga). Tab switch yoki dialog
     * close'larda burst REST chaqiruvlarni oldini oladi.
     */
    @Volatile
    private var lastResumeRefreshAt: Long = 0L

    init {
        Log.d(TAG, "init")
        // KRITIK: throttle timestamp'ni init'da o'rnatamiz. Aks holda birinchi
        // composition'dagi `LifecycleResumeEffect` `loadQueue(initial=true)`
        // bilan parallel REST'ni ishga tushiradi — duplicate va keraksiz.
        // Foydalanuvchi keyin background'dan qaytganda 5+ sek o'tgani uchun
        // throttle to'g'ri ishlaydi.
        lastResumeRefreshAt = System.currentTimeMillis()

        loadQueue(initial = true)
        listenForUpdates()
        observeConnectionState()
        startAutoSync()
        maybeShowAnnouncement()
    }

    /**
     * "Yangilik!" e'loni — FAQAT birinchi marta ko'rsatiladi. Flag DataStore'da
     * saqlanadi ([AnnouncementPreferences]); ko'rsatilgach qayta chiqmaydi.
     */
    private fun maybeShowAnnouncement() {
        viewModelScope.launch {
            if (!announcementPreferences.isCancelFeatureShown()) {
                _state.update { it.copy(showAnnouncement = true) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoSyncJob?.cancel()
    }

    fun onEvent(event: QueueUiEvent) {
        when (event) {
            QueueUiEvent.LoginClicked -> _state.update {
                it.copy(showLoginDialog = true)
            }

            QueueUiEvent.DismissLoginDialog -> _state.update {
                it.copy(showLoginDialog = false)
            }
            QueueUiEvent.Refresh -> loadQueue(initial = false)

            QueueUiEvent.AppResumed -> handleAppResumed()

            is QueueUiEvent.TabSelected -> {
                _state.update { it.copy(selectedTab = event.type) }
                // Tab almashtirilganda darhol fresh ma'lumot olamiz —
                // WS yo'qolgan event'larni "ushlab oladi", UX freshness oshadi.
                viewModelScope.launch { silentRefresh() }
            }

            QueueUiEvent.OpenDialog -> {
                // Optimistic open — darhol bo'sh state ko'rsatamiz, parallel
                // ravishda saqlangan ma'lumotni o'qiymiz. DataStore in-memory
                // cache → odatda mikrosekundlarda qaytadi, ammo cold start
                // birinchi o'qishi I/O bo'lishi mumkin. UI freeze yo'q.
                _state.update {
                    it.copy(
                        showDialog = true,
                        plate = "",
                        fullName = "",
                        passportSeries = "",
                        passportNumber = "",
                        selectedType = it.selectedTab,
                        plateError = null,
                        nameError = null,
                        passportError = null,
                        rememberMe = false
                    )
                }
                loadRememberedDriver()
            }
            QueueUiEvent.DismissDialog -> _state.update {
                if (it.isSubmitting) it else it.copy(showDialog = false)
            }

            is QueueUiEvent.RememberMeChanged -> _state.update {
                it.copy(rememberMe = event.value)
            }

            is QueueUiEvent.TypeChanged -> _state.update {
                it.copy(selectedType = event.type)
            }
            is QueueUiEvent.PlateChanged -> {
                // Filter Kirilcha harflar va simbollarni darhol olib tashlaydi.
                // Faqat ASCII Lotin (A-Z, a-z), raqam (0-9), bo'sh joy qoladi.
                // Uppercase filter'dan keyin — barcha belgilar allaqachon ASCII.
                val filtered = InputValidator.filterPlate(event.value)
                _state.update {
                    it.copy(
                        plate = filtered.value.uppercase(),
                        plateError = if (filtered.filteredOut) MSG_PLATE_INVALID_CHARS else null
                    )
                }
            }
            is QueueUiEvent.NameChanged -> {
                // Filter raqam va simbollarni olib tashlaydi.
                // Har qanday tildagi harf (Lotin, Kirill, ...) + bo'sh joy + ' + - qoladi.
                val filtered = InputValidator.filterName(event.value)
                _state.update {
                    it.copy(
                        fullName = filtered.value,
                        nameError = if (filtered.filteredOut) MSG_NAME_INVALID_CHARS else null
                    )
                }
            }
            is QueueUiEvent.PassportSeriesChanged -> {
                // Faqat 2 ta ASCII Lotin harfi, avtomatik uppercase.
                val filtered = InputValidator.filterPassportSeries(event.value)
                _state.update {
                    it.copy(
                        passportSeries = filtered.value,
                        passportError = if (filtered.filteredOut) MSG_PASSPORT_SERIES_INVALID else null
                    )
                }
            }
            is QueueUiEvent.PassportNumberChanged -> {
                // Faqat 7 ta son.
                val filtered = InputValidator.filterPassportNumber(event.value)
                _state.update {
                    it.copy(
                        passportNumber = filtered.value,
                        passportError = if (filtered.filteredOut) MSG_PASSPORT_NUMBER_INVALID else null
                    )
                }
            }

            QueueUiEvent.Submit -> submit()

            QueueUiEvent.DismissSuccess -> _state.update {
                it.copy(successItem = null, showDialog = false)
            }

            QueueUiEvent.DismissAnnouncement -> dismissAnnouncement()

            QueueUiEvent.OpenCancelSheet -> openCancelSheet()
            QueueUiEvent.DismissCancelSheet -> _state.update {
                if (it.isCancelling) it else it.copy(showCancelSheet = false)
            }
            is QueueUiEvent.CancelSelectionChanged -> _state.update {
                it.copy(selectedCancelUuid = event.uuid)
            }
            QueueUiEvent.ConfirmCancel -> confirmCancel()
        }
    }

    /* ============================================================
     * "Yangilik!" e'loni
     * ============================================================ */
    private fun dismissAnnouncement() {
        // Idempotent — allaqachon yopiq bo'lsa qayta yozmaymiz.
        if (!_state.value.showAnnouncement) return
        _state.update { it.copy(showAnnouncement = false) }
        viewModelScope.launch { announcementPreferences.markCancelFeatureShown() }
    }

    /* ============================================================
     * Navbatni bekor qilish (owner cancel)
     * ============================================================ */

    /**
     * Sheet ochiladi va local'da saqlangan BUGUNGI navbatlar yuklanadi.
     *
     * === RECONCILE-ON-OPEN (admin bekor qilishini hisobga olish) ===
     * Local'da saqlangan navbat "eskirgan" bo'lishi mumkin: admin foydalanuvchi
     * kirishini bekor qilgan yoki mashina allaqachon kirib bo'lgan bo'lishi
     * mumkin. Bunda WS event'i yo'qolgan bo'lishi mumkin (app background'da
     * edi — at-most-once delivery), shuning uchun WS'ga tayanmaymiz.
     *
     * Yechim: sheet ochilishidan oldin REST'dan yangi snapshot olamiz va faqat
     * hali ham BEKOR QILSA BO'LADIGAN (server'da `WAITING` — navbatda turgan)
     * navbatlarni ko'rsatamiz. Server'da endi mavjud bo'lmagan yoki bekor
     * qilingan/kirib bo'lgan yozuvlar ko'rsatilmaydi va local'dan tozalanadi.
     *
     *  - Eski kunlar (kun rollover) — `date` bo'yicha filtr.
     *  - Bo'sh bo'lsa — sheet ochilmaydi, toast ko'rsatiladi.
     */
    private fun openCancelSheet() {
        viewModelScope.launch {
            // 1) REST — authoritative. Muvaffaqiyatsiz bo'lsa (offline) oxirgi
            //    ma'lum state'ga tayanamiz (stale, ammo tozalash o'tkazilmaydi).
            val refreshed = when (val result = getQueueList()) {
                is ApiResult.Success -> {
                    _state.update { it.applySnapshot(result.data) }
                    true
                }
                else -> false
            }

            val today = _state.value.queueDate ?: todayIso()
            val cancellable = _state.value.cancellableUuids()

            val stored = ownedQueuesPreferences.snapshot().filter { it.date == today }
            val owned = stored
                .filter { it.uuid in cancellable }
                .sortedBy { it.queueNumber }

            // 2) Faqat ISHONCHLI (muvaffaqiyatli) snapshot bo'lganda stale
            //    yozuvlarni local'dan tozalaymiz — offline'da noto'g'ri
            //    o'chirib qo'ymaslik uchun.
            if (refreshed) {
                stored.filterNot { it.uuid in cancellable }
                    .forEach { ownedQueuesPreferences.remove(it.uuid) }
            }

            if (owned.isEmpty()) {
                emitToast(
                    if (!refreshed && stored.isNotEmpty()) MSG_NO_INTERNET
                    else MSG_NO_OWNED_QUEUES
                )
                return@launch
            }

            _state.update {
                it.copy(
                    showCancelSheet = true,
                    ownedQueues = owned,
                    // Default tanlov — birinchi (odatda faol) navbat.
                    selectedCancelUuid = owned.first().uuid
                )
            }
        }
    }

    /**
     * Hozir BEKOR QILSA BO'LADIGAN navbatlar uuid'lari — ikkala tab bo'yicha
     * server'da `WAITING` (navbatda turgan) holatdagilar: banner (currentlyEntering)
     * + waiting ro'yxati. `ENTERED`/`SKIPPED` (kirib bo'lgan yoki admin bekor
     * qilgan) va ro'yxatda umuman yo'q navbatlar bu to'plamга kirmaydi.
     */
    private fun QueueUiState.cancellableUuids(): Set<String> {
        val result = HashSet<String>()
        for (tab in listOf(open, tent)) {
            tab.currentlyEntering
                ?.takeIf { it.status == QueueItemStatus.WAITING }
                ?.let { result += it.uuid }
            tab.waitingItems.forEach {
                if (it.status == QueueItemStatus.WAITING) result += it.uuid
            }
        }
        return result
    }

    private fun confirmCancel() {
        val s = _state.value
        if (s.isCancelling) return

        val selected = s.ownedQueues.firstOrNull { it.uuid == s.selectedCancelUuid }
        if (selected == null) {
            emitToast(MSG_SELECT_QUEUE)
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCancelling = true) }

            when (val result = cancelOwnerQueue(selected.ownerToken, selected.plate)) {
                is ApiResult.Success -> {
                    // Local'dan o'chiramiz — qayta ko'rsatilmasin.
                    ownedQueuesPreferences.remove(selected.uuid)

                    val remaining = s.ownedQueues.filterNot { it.uuid == selected.uuid }
                    _state.update {
                        it.copy(
                            isCancelling = false,
                            ownedQueues = remaining,
                            selectedCancelUuid = remaining.firstOrNull()?.uuid,
                            // Ro'yxat bo'shab qolsa sheet'ni yopamiz.
                            showCancelSheet = remaining.isNotEmpty()
                        )
                    }
                    emitToast(MSG_CANCEL_SUCCESS)
                    // Backend holati o'zgardi — ro'yxatni yangilaymiz.
                    silentRefresh()
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isCancelling = false) }
                    emitToast(result.message)
                }
                ApiResult.NetworkError -> {
                    _state.update { it.copy(isCancelling = false) }
                    emitToast(MSG_NO_INTERNET)
                }
                ApiResult.Unauthorized -> {
                    _state.update { it.copy(isCancelling = false) }
                    emitToast(MSG_UNAUTHORIZED)
                }
            }
        }
    }

    private fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /**
     * Backend (inglizcha) xatolik xabarlarini foydalanuvchiga ko'rsatiladigan
     * o'zbekcha matnga o'giradi. Ma'lum bo'lmagan xabarlar o'zgarishsiz qaytadi
     * (raw backend matni — hech bo'lmasa biror ma'lumot).
     */
    private fun localizeSubmitError(raw: String): String {
        val n = raw.trim().lowercase()
        return when {
            // "The plate field must be 8 characters."
            n.contains("plate") && n.contains("8 character") -> MSG_PLATE_LENGTH_8
            else -> raw
        }
    }

    /** Xabar mashina raqamiga taalluqlimi (inline plateError uchun). */
    private fun isPlateError(raw: String): Boolean =
        raw.contains("plate", true) || raw.contains("raqam", true)

    /* ============================================================
     * WS Connection observer — RECONCILE-ON-RECONNECT pattern
     * ============================================================
     *
     * Pusher (va deyarli barcha WebSocket protokollari) — at-most-once
     * delivery: agar mijoz uzilgan paytda backend event yuborsa, u
     * abadiy yo'qoladi. Replay yo'q.
     *
     * Yechim: har gal ulanish "RECONNECTED" holatga o'tganda darhol REST
     * orqali fresh snapshot tortib olish — missed event'larni kompensatsiya
     * qiladi.
     *
     * MUHIM nyuans:
     *  - Initial CONNECTED ni o'tkazib yuboramiz (init blokda allaqachon
     *    `loadQueue(initial=true)` chaqirilgan, double-fetch keraksiz).
     *  - Faqat haqiqiy reconnect — `hasBeenConnected` flag orqali aniqlaymiz.
     *  - `distinctUntilChanged` shart emas, chunki Pusher state changes
     *    duplicate emit qilmaydi, ammo defensive uchun mantiqiy.
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            var hasBeenConnected = false
            repository.wsConnectionState.collect { state ->
                if (state == ConnectionState.CONNECTED) {
                    if (hasBeenConnected) {
                        Log.i(TAG, "📡 WS reconnected → reconciling via REST")
                        silentRefresh()
                    } else {
                        hasBeenConnected = true
                    }
                }
            }
        }
    }

    /* ============================================================
     * Auto-sync
     * ============================================================ */
    private fun startAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(Constants.AUTO_SYNC_INTERVAL_MS)
                runAutoSync()
            }
        }
    }

    private suspend fun runAutoSync() {
        val wsState = repository.wsConnectionState.value
        val wsConnected = wsState == ConnectionState.CONNECTED

        Log.i(TAG, "auto-sync: ws=$wsState, connected=$wsConnected")

        // Reconnect FAQAT WS haqiqatan uzilgan bo'lsa.
        //
        // ESKI BUG: `isStale = sinceLastEvent > 10 min` ham trigger qilardi.
        // Bu false positive: kechqurun zavod ishlamaganda event yo'q, lekin
        // WS sog'lom. Har 10 minutda force reconnect → `Already subscribed`
        // exception → Pusher DISCONNECTED → event'lar abadiy yo'qoladi.
        //
        // Endi: Pusher Java client allaqachon `activityTimeout` orqali avtomatik
        // ping/reconnect qiladi. Manual reconnect FAQAT state DISCONNECTED bo'lsa.
        // `lastEventAt` esa observability/debug uchun saqlanadi (UI'da
        // "WebSocket healthy" indicator qilish mumkin).
        if (!wsConnected) {
            Log.i(TAG, "🔌 WS not connected — triggering manual reconnect")
            repository.reconnectWebSocket()
        }

        // REST refresh DOIM bajariladi — bu authoritative source.
        // Missed event'lar har 10 daqiqada ushlanib qoladi.
        silentRefresh()
    }

    private suspend fun silentRefresh() {
        when (val result = getQueueList()) {
            is ApiResult.Success -> {
                _state.update { it.applySnapshot(result.data) }
            }
            else -> Log.w(TAG, "silent refresh failed")
        }
    }

    /* ============================================================
     * Lifecycle — app foreground transition handler
     * ============================================================
     *
     * MUAMMO: Android'da app background'ga ketganda:
     *  - Doze Mode TCP packet'larni cheklaydi
     *  - App Standby fonda turgan ilovalarni "uxlatadi"
     *  - Pusher WS technically CONNECTED bo'lib qolishi mumkin, lekin
     *    backend "idle" deb event yuborishni to'xtatishi yoki ulanishni
     *    yopishi mumkin (silent stale)
     *  - `observeConnectionState` bunday holatni detect qila olmaydi
     *    (state CONNECTED ko'rinadi)
     *
     * YECHIM: app foreground'ga qaytganda:
     *  1. REST orqali fresh snapshot tortib olish (missed BOOKED/PERMITTED)
     *  2. WS state DISCONNECTED bo'lsa — manual reconnect
     *
     * THROTTLE: 5 sek ichida qayta chaqiriqlar ignore qilinadi. Bu tab
     * switch yoki dialog close holatlarida foydali — `LifecycleResumeEffect`
     * bunday paytda ham chaqiriladi.
     */
    private fun handleAppResumed() {
        val now = System.currentTimeMillis()
        if (now - lastResumeRefreshAt < RESUME_REFRESH_THROTTLE_MS) {
            Log.d(TAG, "app resumed: throttled (${(now - lastResumeRefreshAt) / 1000}s ago)")
            return
        }
        lastResumeRefreshAt = now

        viewModelScope.launch {
            Log.i(TAG, "📲 app resumed — refreshing snapshot")
            silentRefresh()

            // WS sog'lom emas bo'lsa — manual reconnect.
            // Sog'lom bo'lsa tegmaymiz (Already subscribed exception risk).
            val wsState = repository.wsConnectionState.value
            if (wsState != ConnectionState.CONNECTED) {
                Log.i(TAG, "📲 app resumed: WS state=$wsState — reconnecting")
                repository.reconnectWebSocket()
            }
        }
    }

    /* ============================================================
     * Manual / initial load
     * ============================================================ */
    private fun loadQueue(initial: Boolean) {
        viewModelScope.launch {
            _state.update {
                if (initial) it.copy(isLoading = true, listError = null)
                else it.copy(isRefreshing = true, listError = null)
            }

            when (val result = getQueueList()) {
                is ApiResult.Success -> {
                    _state.update {
                        it.applySnapshot(result.data).copy(
                            isLoading = false,
                            isRefreshing = false,
                            listError = null
                        )
                    }
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, isRefreshing = false, listError = result.message)
                }
                ApiResult.NetworkError -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            listError = if (it.isFullyEmpty) MSG_NO_INTERNET else null
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

    private fun QueueUiState.applySnapshot(snapshot: QueueSnapshot): QueueUiState = copy(
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

    /* ============================================================
     * WebSocket
     * ============================================================ */
    private fun listenForUpdates() {
        viewModelScope.launch {
            observeUpdates().collect { update ->
                when (update) {
                    is QueueUpdate.Booked -> handleBooked(update.item)
                    is QueueUpdate.Permitted -> handlePermitted(update.nextOpen, update.nextTent)
                }
            }
        }
    }

    private fun handleBooked(newItem: QueueItem) {
        Log.i(TAG, "🟢 Booked: type=${newItem.type}, q=${newItem.queueNumber}")

        if (newItem.hasPermit) return

        _state.update { current ->
            current.updateTab(newItem.type) { tab ->
                if (tab.containsItem(newItem.uuid)) tab
                else promoteToBannerIfFirst(tab, newItem)
            }
        }
    }

    /**
     * `PermitIssued` event'i — backend ikkala tab uchun ham keyingi banner'ni yuboradi.
     *
     * Har tab uchun:
     *  - `nextItem != null` → applyPermitted (eski banner history'ga, yangisi banner'ga)
     *  - `nextItem == null` va banner mavjud → banner clear + REST refresh trigger
     *  - `nextItem == null` va banner ham yo'q → no-op
     *
     * REST refresh faqat bir marta chaqiriladi (ikkala tab clear bo'lsa ham).
     */
    private fun handlePermitted(nextOpen: QueueItem?, nextTent: QueueItem?) {
        Log.i(
            TAG,
            "🟠 Permitted: open=${nextOpen?.queueNumber} tent=${nextTent?.queueNumber}"
        )

        var needsRefresh = false

        _state.update { current ->
            var updated = current

            // OPEN tab
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

            // TENT tab
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

        if (needsRefresh) {
            viewModelScope.launch { silentRefresh() }
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

    /* ============================================================
     * Submit
     * ============================================================ */
    private fun submit() {
        if (_state.value.isSubmitting) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSubmitting = true,
                    plateError = null,
                    nameError = null,
                    passportError = null
                )
            }

            val s = _state.value
            val result = submitQueue(
                type = s.selectedType,
                plate = s.plate,
                fullName = s.fullName,
                passportSeries = s.passportSeries,
                passportNumber = s.passportNumber
            )
            when (result) {
                is ApiResult.Success -> {
                    val newItem = result.data.copy(
                        status = QueueItemStatus.WAITING,
                        type = s.selectedType
                    )

                    // UUID DataStore'ga saqlanadi - v1.1'da FCM data
                    // payload kelganida qaysi UUID joriy qurilmaga tegishli
                    // ekanligini aniqlash uchun ishlatiladi.
                    myQueuesPreferences.add(newItem.uuid)

                    // Owner cancel uchun to'liq navbatni (token bilan) saqlaymiz.
                    persistOwnedQueue(newItem, s)

                    // "Eslab qolish" — submit muvaffaqiyatli bo'lgandan keyin
                    // ATOMIC saqlaymiz. Submit fail bo'lsa hech narsa
                    // saqlanmaydi — bu to'g'ri, chunki noto'g'ri ma'lumotni
                    // remember qilish mantiqsiz.
                    //
                    // Fire-and-forget: viewModelScope ichida launch — submit
                    // success UI flow'ini bloklamaydi. DataStore atomic write
                    // garansi mavjud.
                    persistRememberPreference(s)

                    _state.update { current ->
                        val updated = current.updateTab(newItem.type) { tab ->
                            if (tab.containsItem(newItem.uuid)) tab
                            else promoteToBannerIfFirst(tab, newItem)
                        }
                        updated.copy(
                            isSubmitting = false,
                            successItem = newItem,
                            // Foydalanuvchi yangi navbat olgan tab'ga avtomatik o'tamiz
                            selectedTab = newItem.type
                        )
                    }
                }
                is ApiResult.Error -> {
                    // Backend xabarini o'zbekchaga o'giramiz (masalan 422 validatsiya).
                    val raw = result.message
                    val msg = localizeSubmitError(raw)

                    // Xabar qaysi maydonga taalluqli — inline error uchun.
                    val plateErr = if (isPlateError(raw)) msg else null
                    val nameErr = if (raw.contains("F.I", true)) msg else null
                    val passErr = if (raw.contains("pasport", true)) msg else null

                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            plateError = plateErr,
                            nameError = nameErr,
                            passportError = passErr
                        )
                    }
                    emitToast(msg)
                }
                ApiResult.NetworkError -> {
                    _state.update { it.copy(isSubmitting = false) }
                    emitToast(MSG_NO_INTERNET)
                }
                ApiResult.Unauthorized -> {
                    _state.update { it.copy(isSubmitting = false) }
                    emitToast(MSG_UNAUTHORIZED)
                }
            }
        }
    }

    private fun emitToast(message: String) {
        viewModelScope.launch { _effect.send(QueueUiEffect.ShowToast(message)) }
    }

    /**
     * Submit success'dan keyin olingan navbatni (`owner_token` bilan) local'ga
     * saqlash — keyinchalik egasi bekor qilishi uchun.
     *
     * `owner_token` faqat submit response'da keladi. Agar yo'q bo'lsa (eski
     * backend), bekor qilib bo'lmaydi — saqlashning ma'nosi yo'q, o'tkazamiz.
     *
     * Passport ma'lumotlari form state'dan olinadi (response'da null keladi).
     * Fire-and-forget: DataStore atomic write.
     */
    private fun persistOwnedQueue(item: QueueItem, form: QueueUiState) {
        val token = item.ownerToken?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            ownedQueuesPreferences.add(
                OwnedQueue(
                    id = item.id,
                    uuid = item.uuid,
                    date = item.date ?: todayIso(),
                    type = item.type.toBackend() ?: "open",
                    queueNumber = item.queueNumber,
                    plate = item.plate,
                    fullName = item.fullName,
                    passportSeries = form.passportSeries.ifBlank { null },
                    passportNumber = form.passportNumber.ifBlank { null },
                    hasPermit = item.hasPermit,
                    cancelled = false,
                    cancelReason = null,
                    ownerToken = token
                )
            )
        }
    }

    /* ============================================================
     * "Eslab qolish" — remembered driver flow
     * ============================================================
     *
     * 1) Dialog ochilganda (`OpenDialog`):
     *    - DataStore'dan `(rememberMe, data)` snapshot olamiz
     *    - `rememberMe=true` va `data!=null` → form to'ldiriladi, checkbox belgilanadi
     *    - Aks holda — bo'sh dialog (default state)
     *
     * 2) Submit success:
     *    - `rememberMe=true`  → joriy form ma'lumotlarini saqlaymiz
     *    - `rememberMe=false` → har qanday saqlangan ma'lumotni tozalaymiz
     *      (idempotent — yo'q bo'lsa ham xatolik yo'q)
     *
     * **Race condition mulohazasi**: dialog ochilganda foydalanuvchi
     * snapshot kelishidan oldin biror maydonni o'zgartirishi mumkin. Bu
     * holatda saqlangan ma'lumot tipgan tekstni "overwrite" qilmaslik
     * kerak. `loadRememberedDriver` har maydonda **bo'sh ekanligini** tekshirib
     * yozadi — defensive merge.
     */
    private fun loadRememberedDriver() {
        viewModelScope.launch {
            val (remember, data) = savedDriverPreferences.snapshot()
            if (!remember || data == null) return@launch

            // VehicleType valueOf — agar string buzilgan bo'lsa OPEN'ga fallback.
            val vehicleType = runCatching { VehicleType.valueOf(data.vehicleType) }
                .getOrDefault(VehicleType.OPEN)

            _state.update { current ->
                // Faqat dialog hali ochiq bo'lsa apply qilamiz — foydalanuvchi
                // tez yopib ulgursa, eski state'ga snapshot yozmaymiz.
                if (!current.showDialog) return@update current

                // Defensive merge — agar user allaqachon biror maydonga
                // yozayotgan bo'lsa (snapshot kelganda), uning kiritmasini
                // o'chirmaymiz. Bu race window kichik (DataStore in-memory
                // cache), ammo bardosh uchun.
                current.copy(
                    rememberMe = true,
                    selectedType = vehicleType,
                    plate = if (current.plate.isBlank()) data.plate else current.plate,
                    fullName = if (current.fullName.isBlank()) data.fullName else current.fullName,
                    passportSeries = if (current.passportSeries.isBlank())
                        data.passportSeries else current.passportSeries,
                    passportNumber = if (current.passportNumber.isBlank())
                        data.passportNumber else current.passportNumber
                )
            }
        }
    }

    /**
     * Submit success'dan keyin "Eslab qolish" preferencesini sinxronlash.
     *
     * Fire-and-forget: viewModelScope'da launch. DataStore atomic write —
     * partial state imkonsiz.
     */
    private fun persistRememberPreference(snapshot: QueueUiState) {
        viewModelScope.launch {
            if (snapshot.rememberMe) {
                savedDriverPreferences.save(
                    SavedDriverData(
                        vehicleType = snapshot.selectedType.name,
                        plate = snapshot.plate,
                        fullName = snapshot.fullName,
                        passportSeries = snapshot.passportSeries,
                        passportNumber = snapshot.passportNumber
                    )
                )
            } else {
                // Idempotent — yo'q bo'lsa ham edit{} no-op. Safe.
                savedDriverPreferences.clear()
            }
        }
    }

    /* ============================================================
     * Helpers — TabState manipulation
     * ============================================================ */

    private fun QueueUiState.updateTab(
        type: VehicleType,
        block: (TabState) -> TabState
    ): QueueUiState = when (type) {
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

    private companion object {
        const val TAG = "QueueVM"

        /** Foreground transition'da resume-refresh chaqiruvlar oralig'i. */
        const val RESUME_REFRESH_THROTTLE_MS = 5_000L

        const val MSG_IN_PROGRESS = "Jarayonda"
        const val MSG_NO_INTERNET = "Internet aloqasi yo'q"
        const val MSG_UNAUTHORIZED = "Avtorizatsiya talab qilinadi"
        const val MSG_PLATE_LENGTH_8 = "Mashina raqamingiz 8ta belgidan iborat bo'lishi shart"
        const val MSG_NO_OWNED_QUEUES = "Bekor qilinadigan navbat yo'q"
        const val MSG_SELECT_QUEUE = "Bekor qilish uchun navbatni tanlang"
        const val MSG_CANCEL_SUCCESS = "Navbat bekor qilindi"
        const val MSG_PLATE_INVALID_CHARS = "Faqat lotin harf va raqam kiriting"
        const val MSG_NAME_INVALID_CHARS = "Faqat harflar bilan yozing"
        const val MSG_PASSPORT_SERIES_INVALID = "Seriyada faqat 2 ta lotin harfi"
        const val MSG_PASSPORT_NUMBER_INVALID = "Raqamda faqat son (7 ta)"
    }
}