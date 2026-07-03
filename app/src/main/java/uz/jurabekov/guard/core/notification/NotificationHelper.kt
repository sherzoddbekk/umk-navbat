package uz.jurabekov.guard.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import uz.jurabekov.guard.MainActivity
import uz.jurabekov.guard.R

/**
 * Notification yaratish va boshqarish.
 *
 *  CHANNEL_ID            — HIGH importance, ovoz + vibratsiya, lock screen'da ko'rinadi
 *  CHANNEL_ID_SERVICE    — LOW priority, foreground service uchun ongoing
 */
class NotificationHelper(private val context: Context) {

    init {
        createChannelsIfNeeded()
    }

    /**
     * "Sizning navbatingiz keldi" notification.
     *
     * @param plate mashina raqami (matnda)
     * @param queueNumber navbat raqami
     * @param uuid notification ID uchun (har navbat — alohida notification)
     */
    fun showMyTurnNotification(plate: String, queueNumber: Int, uuid: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            uuid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Sizning navbatingiz keldi!")
            .setContentText("$plate raqamli mashinangiz joriy navbatda")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Sizning $plate raqamli mashinangiz joriy navbatda. " +
                            "Iltimos zavod kirish darvozasiga yo'naling. (Navbat #$queueNumber)"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)  // sound + vibrate
            .build()

        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?: return

        manager.notify(uuid.hashCode(), notification)
    }

    /** Foreground service uchun ongoing notification. */
    fun buildServiceNotification(): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Guard")
            .setContentText("Navbatlar kuzatilmoqda")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createChannelsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?: return

        val alertChannel = NotificationChannel(
            CHANNEL_ID,
            "Navbat haqida xabar",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Sizning mashinangiz navbati kelganida xabardor qiladi"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC

            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(sound, audioAttrs)
        }
        manager.createNotificationChannel(alertChannel)

        val serviceChannel = NotificationChannel(
            CHANNEL_ID_SERVICE,
            "Background xizmat",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Navbat yangilanishlarini kuzatish uchun fon xizmati"
            setShowBadge(false)
        }
        manager.createNotificationChannel(serviceChannel)
    }

    companion object {
        const val CHANNEL_ID = "queue_alert"
        const val CHANNEL_ID_SERVICE = "queue_service"
        const val NOTIFICATION_ID_SERVICE = 999
    }
}
