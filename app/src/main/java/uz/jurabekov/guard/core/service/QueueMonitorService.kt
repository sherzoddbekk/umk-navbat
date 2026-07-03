package uz.jurabekov.guard.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import uz.jurabekov.guard.core.notification.NotificationHelper
import uz.jurabekov.guard.data.preferences.MyQueuesPreferences
import uz.jurabekov.guard.data.remote.websocket.QueuePusherClient
import uz.jurabekov.guard.domain.model.QueueUpdate
import uz.jurabekov.guard.domain.repository.QueueRepository

/**
 * Foreground Service:
 *  - Pusher WS'ni doim ulangan ushlab turadi
 *  - PermitIssued event keldi'da UUID'ni saqlangan UUID'lar bilan solishtiradi
 *  - Mos kelsa - notification ko'rsatadi
 *
 * MUHIM: Endi Application.onCreate'dan emas, **MainActivity'dan** yoki
 * navbat olingandan keyin (foreground'da) ishga tushadi.
 *
 * Battery: agar foydalanuvchining kutayotgan navbati qolmasa
 * (UUID lar bo'sh), service o'zini to'xtatadi → battery yemaydi va
 * Android 14 dataSync 6-soatlik limitiga umuman tushib qolmaydi.
 */
class QueueMonitorService : Service() {

    private val pusherClient: QueuePusherClient by inject()
    private val repository: QueueRepository by inject()
    private val myQueuesPrefs: MyQueuesPreferences by inject()
    private val notificationHelper: NotificationHelper by inject()

    private val serviceJob = SupervisorJob()
    // IO dispatcher - flow collect va DataStore o'qish/yozish uchun
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "service created")

        // Foreground'ga 5 sek ichida o'tish majburiy
        startForeground(
            NotificationHelper.NOTIFICATION_ID_SERVICE,
            notificationHelper.buildServiceNotification()
        )

        pusherClient.connect()
        observePermitEvents()
        observeQueuesEmpty()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        // Edge case: service start qilindi, lekin pending navbat yo'q
        // (masalan eski intent qayta yuborildi). Darhol to'xtaymiz.
        serviceScope.launch {
            val hasPending = myQueuesPrefs.getUuids().isNotEmpty()
            if (!hasPending) {
                Log.i(TAG, "no pending queues at start → stopping")
                stopSelfCleanly()
            }
        }

        // START_NOT_STICKY: agar sistema o'ldirsa, qayta tiriltirmaydi.
        // Foydalanuvchi o'zi navbat olsa, biz qayta start qilamiz.
        // Bu STICKY'dan ko'ra battery va FGS limit'lar uchun yaxshi.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "service destroyed")
        serviceScope.cancel()
    }

    /**
     * PermitIssued event'lar oqimi - foydalanuvchining navbati kelganda
     * notification chiqarish.
     *
     * `nextItem == null` (navbat tugadi yoki cancelled) holatida notification
     * kerak emas — hech kimga ruxsat berilmadi, faqat banner clear signal.
     */
    private fun observePermitEvents() {
        serviceScope.launch {
            repository.observeUpdates()
                .filterIsInstance<QueueUpdate.Permitted>()
                .collect { update -> handlePermitEvent(update) }
        }
    }

    /**
     * UUID set'i bo'sh bo'lib qolganda service'ni o'chirish.
     * `drop(1)` - dastlabki holat (boshlang'ich set) shu yerda
     * tushunmovchilik qilmasligi uchun, faqat o'zgarishlarga reaksiya.
     */
    private fun observeQueuesEmpty() {
        serviceScope.launch {
            myQueuesPrefs.uuids
                .distinctUntilChanged()
                .drop(1)
                .filter { it.isEmpty() }
                .collect {
                    Log.i(TAG, "all queues processed → stopping service")
                    stopSelfCleanly()
                }
        }
    }

    private suspend fun handlePermitEvent(event: QueueUpdate.Permitted) {
        // Backend endi ikkala tab uchun ham keyingi mashinani bir event'da yuboradi.
        // Foydalanuvchining UUID'i qaysi tabning next'iga kirsa — notification.
        val candidates = listOfNotNull(event.nextOpen, event.nextTent)
        if (candidates.isEmpty()) {
            Log.d(TAG, "permit: ikkala tab ham null → no notify (queue cleared)")
            return
        }

        val savedUuids = myQueuesPrefs.getUuids()
        Log.d(TAG, "permit: ${candidates.size} candidate(s), saved=${savedUuids.size}")

        for (item in candidates) {
            if (savedUuids.contains(item.uuid)) {
                Log.i(TAG, "🔔 NOTIFY: ${item.plate} (#${item.queueNumber}, type=${item.type})")

                notificationHelper.showMyTurnNotification(
                    plate = item.plate,
                    queueNumber = item.queueNumber,
                    uuid = item.uuid
                )

                // UUID'ni o'chiramiz → observeQueuesEmpty trigger bo'lib
                // service to'xtaydi (agar boshqa pending qolmagan bo'lsa).
                myQueuesPrefs.remove(item.uuid)
            }
        }
    }

    private fun stopSelfCleanly() {
        // Foreground'dan chiqib, notification'ni olib tashlaymiz, keyin to'xtaymiz
        @Suppress("DEPRECATION")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    companion object {
        private const val TAG = "QueueMonitorSvc"
    }
}