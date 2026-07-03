package uz.jurabekov.guard.data.remote.websocket

import android.util.Log
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.ChannelEventListener
import com.pusher.client.channel.PusherEvent
import com.pusher.client.channel.SubscriptionEventListener
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import uz.jurabekov.guard.BuildConfig
import uz.jurabekov.guard.core.util.Constants
import uz.jurabekov.guard.data.remote.dto.QueueItemDto
import java.util.concurrent.atomic.AtomicBoolean

class QueuePusherClient(
    private val json: Json
) {

    sealed interface RawUpdate {
        data class Booked(val item: QueueItemDto) : RawUpdate

        /**
         * Backend payload variantlari:
         *   - YANGI: `{"next_queue": {"next_queue": ..., "next_queue_tent": ...}}` —
         *            ikkala tab uchun ham keyingi mashina bir event'da.
         *   - ESKI:  `{"next_queue": {<single item with type field>}}` —
         *            bitta tab uchun.
         *   - NULL:  `{"next_queue": null}` — ikkala tab clear.
         *
         * Ikkala holat ham `nextOpen` va `nextTent` orqali ifoda qilinadi:
         * eski format'da bitta non-null, NEW formatda ikkalasi ham bo'lishi mumkin.
         */
        data class Permitted(
            val nextOpen: QueueItemDto?,
            val nextTent: QueueItemDto?
        ) : RawUpdate
    }

    private val pusher: Pusher by lazy {
        val options = PusherOptions().apply {
            setHost(Constants.PUSHER_HOST)
            setWsPort(Constants.PUSHER_PORT)
            setWssPort(Constants.PUSHER_PORT)
            isUseTLS = Constants.PUSHER_USE_TLS
            activityTimeout = Constants.PUSHER_ACTIVITY_TIMEOUT_MS
            pongTimeout = Constants.PUSHER_PONG_TIMEOUT_MS
            maxReconnectionAttempts = Constants.PUSHER_MAX_RECONNECT_ATTEMPTS
            maxReconnectGapInSeconds = Constants.PUSHER_MAX_RECONNECT_GAP_SEC
        }
        Pusher(Constants.PUSHER_APP_KEY, options)
    }

    private val connected = AtomicBoolean(false)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** Oxirgi event qachon kelganligi (System.currentTimeMillis()). 0 = hech qachon. */
    private val _lastEventAt = MutableStateFlow(0L)
    val lastEventAt: StateFlow<Long> = _lastEventAt.asStateFlow()

    private val _updates = MutableSharedFlow<RawUpdate>(
        replay = 0,
        extraBufferCapacity = EVENTS_BUFFER_SIZE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val updates: Flow<RawUpdate> = _updates.asSharedFlow()

    fun connect() {
        // KRITIK: Pusher haqiqiy holatini tekshiramiz, faqat o'z flag'imizga
        // tayanmaymiz. Reconnect zanjiri exception bilan tugagan bo'lsa,
        // flag va Pusher state desync bo'lishi mumkin (eski bug).
        val pusherState = runCatching { pusher.connection.state }.getOrNull()
        if (pusherState == ConnectionState.CONNECTED || pusherState == ConnectionState.CONNECTING) {
            // Flag'ni Pusher state bilan synxronlaymiz (defensive against past desync).
            connected.set(true)
            logI("connect: already $pusherState — skip")
            return
        }

        if (!connected.compareAndSet(false, true)) {
            logI("connect: another connect() in progress")
            return
        }

        try {
            logI("⇄ Pusher initializing endpoint=${endpointUrl()}")

            // KRITIK: Subscribe va bind QILINADI `pusher.connect()`'dan OLDIN.
            //
            // Sababi: `pusher.connect()` asynchronous. Mobil tarmog'ida handshake
            // 100-300ms ichida tugashi mumkin. Agar `subscribe`+`bind` keyinroq
            // chaqirilsa, dastlabki event'lar listener'siz qoladi va silently
            // dropped bo'ladi.
            //
            // Pusher Java client subscribe'ni queue qiladi va handshake tugagach
            // avtomatik apply qiladi — bu yondashuv race window'ni yopadi.
            val channel = pusher.subscribe(Constants.PUSHER_CHANNEL, channelLifecycleListener)
            channel.bind(Constants.PUSHER_EVENT_QUEUE_BOOKED, bookedListener())
            channel.bind(Constants.PUSHER_EVENT_PERMIT_ISSUED, permittedListener())
            logI(
                "📡 channel='${Constants.PUSHER_CHANNEL}' bound: " +
                        "${Constants.PUSHER_EVENT_QUEUE_BOOKED}, " +
                        Constants.PUSHER_EVENT_PERMIT_ISSUED
            )

            // Endi WebSocket'ni boshlaymiz — subscriptions handshake'dan keyin auto-apply.
            pusher.connect(connectionListener, ConnectionState.ALL)
        } catch (e: Exception) {
            // KRITIK: exception bo'lsa flag'ni MAJBURAN rollback qilish kerak.
            // Aks holda keyingi `connect()` chaqiriqlari "already in progress"
            // deb skip qiladi va Pusher abadiy DISCONNECTED qoladi.
            // Bu loglardagi 'Already subscribed' bug'ining asosiy keng tarqalish sababi.
            connected.set(false)
            logE("⚠ connect failed: ${e.message}", e)
        }
    }

    /**
     * Channel lifecycle event'lar — subscription succeeded confirmation muhim,
     * shu signal kelguncha biz "subscribed" deb o'ylamasligimiz kerak.
     */
    private val channelLifecycleListener = object : ChannelEventListener {
        override fun onSubscriptionSucceeded(channelName: String) {
            logI("✅ subscribed & ready: $channelName")
        }

        override fun onEvent(event: PusherEvent) {
            // Bu lifecycle listener — event handling alohida `bookedListener` /
            // `permittedListener`'da.
        }
    }

    /**
     * Force reconnect — sync logikasi WS uzilgan deb topganda chaqirsa bo'ladi.
     *
     * KRITIK FIX: avval `unsubscribe()` chaqirilishi SHART. Pusher Java client'da
     * channel internal map'da saqlanadi, va `disconnect()` uni TOZALAMAYDI.
     * Keyingi `connect()` ichida `pusher.subscribe(...)` chaqirilganda
     * `IllegalArgumentException("Already subscribed to a channel...")` otadi
     * va Pusher abadiy DISCONNECTED qoladi.
     *
     * Production loglarda aniq ko'rilgan bug:
     *   reconnect() → disconnect() → connect() → subscribe() → CRASH
     *   → DISCONNECTED qoladi → barcha keyingi event'lar yo'qoladi.
     */
    fun reconnect() {
        logI("⟳ force reconnect requested")
        try {
            // Subscription'ni olib tashlash. Idempotent: agar mavjud bo'lmasa - OK.
            runCatching { pusher.unsubscribe(Constants.PUSHER_CHANNEL) }
                .onFailure { logD("unsubscribe ignored: ${it.message}") }

            pusher.disconnect()
        } catch (e: Exception) {
            logW("reconnect: cleanup error (continuing)", e)
        } finally {
            // Har qanday holatda flag'ni reset qilamiz, keyin connect() ishlay oladi.
            connected.set(false)
        }

        connect()
    }

    fun disconnect() {
        if (!connected.compareAndSet(true, false)) return
        try {
            pusher.unsubscribe(Constants.PUSHER_CHANNEL)
            pusher.disconnect()
        } catch (e: Exception) {
            logW("disconnect error", e)
        }
    }

    /**
     * `QueueBooked` — yangi mashina navbatga qo'shildi.
     * Payload doim mashina ma'lumoti bo'lishi kerak; null bo'lsa — bu noto'g'ri va log qilamiz.
     */
    private fun bookedListener() = object : SubscriptionEventListener {
        override fun onEvent(event: PusherEvent) {
            // INFO level — production'da ham log'larda ko'rinadi (tent debug uchun muhim)
            logI("📥 BOOKED event arrived | data=${event.data}")
            val dto = parseDto(event.data, primaryKey = "queue")
            if (dto == null) {
                logE("❌ Booked parse failed: ${event.data}")
                return
            }
            logI("✅ BOOKED parsed | type=${dto.type} q=${dto.queueNumber} plate=${dto.plate}")
            _lastEventAt.value = System.currentTimeMillis()
            _updates.tryEmit(RawUpdate.Booked(dto))
        }
    }

    /**
     * `PermitIssued` — permit holati o'zgardi.
     *
     * Payload variantlari (backend kelajakda format o'zgartirsa, bu yerga qo'shiladi):
     *  - **NEW**:  `{"next_queue": {"next_queue": <open|null>, "next_queue_tent": <tent|null>}}`
     *  - **OLD**:  `{"next_queue": {<single item with type field>}}`
     *  - **NULL**: `{"next_queue": null}`  → ikkala banner clear
     */
    private fun permittedListener() = object : SubscriptionEventListener {
        override fun onEvent(event: PusherEvent) {
            logI("📥 PERMITTED event arrived | data=${event.data}")
            _lastEventAt.value = System.currentTimeMillis()

            val parsed = parsePermittedPayload(event.data)
            if (parsed == null) {
                logE("❌ PERMITTED parse failed: ${event.data}")
                return
            }

            logI(
                "✅ PERMITTED parsed | open=${parsed.nextOpen?.queueNumber} " +
                        "tent=${parsed.nextTent?.queueNumber}"
            )
            _updates.tryEmit(parsed)
        }
    }

    /**
     * Permitted event payload'ini parse qiladi — ikki format'ni qo'llab-quvvatlaydi.
     *
     * Detection strategiyasi:
     *  1. `obj["next_queue"]` ni unwrap qilamiz (outer wrapper).
     *  2. Agar JsonNull → ikkala tab clear.
     *  3. Inner JsonObject'da:
     *      - `id` field bor → ESKI format (single item), `type` bo'yicha tab routing.
     *      - `next_queue` yoki `next_queue_tent` field bor → YANGI format (per-tab).
     */
    private fun parsePermittedPayload(rawData: String?): RawUpdate.Permitted? {
        if (rawData.isNullOrBlank()) return null

        return runCatching {
            val element = json.parseToJsonElement(rawData)
            val obj = element as? JsonObject ?: return@runCatching null

            // Outer wrapper unwrap. Ba'zi formatlarda flat ham bo'lishi mumkin —
            // shu sabab fallback qilamiz: yo `next_queue` keyni olamiz, yo `obj`'ni o'zini.
            val inner = obj["next_queue"] ?: obj

            // Holat NULL: explicit null payload
            if (inner is kotlinx.serialization.json.JsonNull) {
                return@runCatching RawUpdate.Permitted(nextOpen = null, nextTent = null)
            }

            val innerObj = inner as? JsonObject ?: return@runCatching null

            // ESKI format detection: inner ichida bevosita item field'lari (id, uuid, ...).
            if (innerObj.containsKey("id")) {
                val item = json.decodeFromJsonElement(QueueItemDto.serializer(), inner)
                return@runCatching when (item.type?.lowercase()) {
                    "tent" -> RawUpdate.Permitted(nextOpen = null, nextTent = item)
                    else -> RawUpdate.Permitted(nextOpen = item, nextTent = null)
                }
            }

            // YANGI format: per-tab keys.
            val openItem = innerObj["next_queue"]?.toItemDtoOrNull()
            val tentItem = innerObj["next_queue_tent"]?.toItemDtoOrNull()
            RawUpdate.Permitted(nextOpen = openItem, nextTent = tentItem)
        }.getOrNull()
    }

    /**
     * JsonElement'ni QueueItemDto'ga aylantiradi, JsonNull / parse fail bo'lsa null qaytaradi.
     */
    private fun kotlinx.serialization.json.JsonElement.toItemDtoOrNull(): QueueItemDto? {
        if (this is kotlinx.serialization.json.JsonNull) return null
        return runCatching {
            json.decodeFromJsonElement(QueueItemDto.serializer(), this)
        }.getOrNull()
    }

    /**
     * `{"next_queue": null}` formatdagi payload'ni aniqlaydi.
     * (Eski parseDto ishlatadigan funksiya — bu yerda saqlab qolamiz, lekin
     *  yangi `parsePermittedPayload` o'zi null'ni handle qiladi.)
     */
    private fun isExplicitlyNull(rawData: String?, key: String): Boolean {
        if (rawData.isNullOrBlank()) return false
        return runCatching {
            val element = json.parseToJsonElement(rawData) as? JsonObject ?: return false
            element[key] is kotlinx.serialization.json.JsonNull
        }.getOrDefault(false)
    }

    private val connectionListener = object : ConnectionEventListener {
        override fun onConnectionStateChange(change: ConnectionStateChange) {
            _connectionState.value = change.currentState
            logI("⇄ state: ${change.previousState} → ${change.currentState}")

            if (change.currentState == ConnectionState.CONNECTED) {
                logI("🟢 Pusher CONNECTED — events expected on '${Constants.PUSHER_CHANNEL}'")
            }
        }

        override fun onError(message: String?, code: String?, e: Exception?) {
            logE("⚠ pusher error: code=$code, msg=$message", e)
        }
    }

    private fun parseDto(rawData: String?, primaryKey: String): QueueItemDto? {
        if (rawData.isNullOrBlank()) return null

        runCatching { return json.decodeFromString(QueueItemDto.serializer(), rawData) }

        runCatching {
            val element = json.parseToJsonElement(rawData)
            val obj = element as? JsonObject ?: return null

            // JsonNull non-null JsonElement — Kotlin `?:` fall through qilmaydi.
            val candidates = listOf(primaryKey, "queue", "next_queue", "data", "item")
            val nested = candidates
                .mapNotNull { obj[it] }
                .firstOrNull { it !is kotlinx.serialization.json.JsonNull }
                ?: return null

            return json.decodeFromJsonElement(QueueItemDto.serializer(), nested)
        }
        return null
    }

    private fun endpointUrl(): String =
        "${if (Constants.PUSHER_USE_TLS) "wss" else "ws"}://" +
                "${Constants.PUSHER_HOST}:${Constants.PUSHER_PORT}"

    private fun logD(msg: String) { if (BuildConfig.DEBUG) Log.d(TAG, msg) }
    private fun logI(msg: String) = Log.i(TAG, msg)
    private fun logW(msg: String, e: Throwable? = null) = Log.w(TAG, msg, e)
    private fun logE(msg: String, e: Throwable? = null) = Log.e(TAG, msg, e)

    private companion object {
        const val TAG = "QueuePusher"
        const val EVENTS_BUFFER_SIZE = 32
    }
}