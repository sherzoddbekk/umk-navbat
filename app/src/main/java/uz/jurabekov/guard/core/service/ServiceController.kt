package uz.jurabekov.guard.core.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * QueueMonitorService'ni boshqarish uchun tor scope'li helper.
 *
 * Service'ni faqat **foreground'da** start qilamiz (Activity yoki ViewModel
 * effect orqali), shunda Android 12+ FGS-from-background restriction'iga
 * tushib qolmaymiz.
 *
 * Service o'zi foydalanuvchining kutayotgan navbatlari (UUID'lari) bo'lmasa
 * `stopSelf()` qiladi - shuning uchun bu yerda fine-grained start/stop
 * mantiqi kerak emas.
 */
object ServiceController {

    private const val TAG = "ServiceController"

    /**
     * Service'ni ishga tushiradi.
     *
     * Foydalanuvchi:
     *  - Yangi navbat olganida (submit success)
     *  - Cold start'da agar pending navbat bo'lsa (MainActivity onCreate)
     */
    fun start(context: Context) {
        try {
            val intent = Intent(context, QueueMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
            Log.i(TAG, "service start requested")
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException va boshqalar
            // Production'da crashlytics'ga yozish kerak
            Log.e(TAG, "failed to start service: ${e.message}", e)
        }
    }

    /** Service'ni majburiy to'xtatish. Odatda kerak emas - service o'zi to'xtaydi. */
    fun stop(context: Context) {
        try {
            val intent = Intent(context, QueueMonitorService::class.java)
            context.stopService(intent)
            Log.i(TAG, "service stop requested")
        } catch (e: Exception) {
            Log.w(TAG, "stop service error: ${e.message}")
        }
    }
}
