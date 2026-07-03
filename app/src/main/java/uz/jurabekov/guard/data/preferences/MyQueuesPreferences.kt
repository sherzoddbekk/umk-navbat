package uz.jurabekov.guard.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Foydalanuvchining olgan navbatlari UUID'larini saqlovchi DataStore.
 *
 * v1.0: faqat submit'da add() chaqiriladi - persistent saqlanadi.
 * v1.1: FCM data payload kelganda UUID cross-check (qaysi navbat qaysi
 *       qurilmaga tegishli) - shu yerdan o'qiladi va remove() chaqiriladi.
 */
private val Context.myQueuesDataStore by preferencesDataStore(name = "my_queues_prefs")

class MyQueuesPreferences(private val context: Context) {

    private val uuidSetKey = stringSetPreferencesKey("queue_uuids")

    /** Saqlangan UUID'lar oqimi. */
    val uuids: Flow<Set<String>> = context.myQueuesDataStore.data.map { prefs ->
        prefs[uuidSetKey] ?: emptySet()
    }

    /** Sync versiyasi - service ichida foydalanish uchun. */
    suspend fun getUuids(): Set<String> = uuids.first()

    /** Yangi UUID qo'shish (submit success). Set - duplikat yo'q. */
    suspend fun add(uuid: String) {
        context.myQueuesDataStore.edit { prefs ->
            val current = prefs[uuidSetKey] ?: emptySet()
            prefs[uuidSetKey] = current + uuid
        }
    }

    /** UUID o'chirish (notification ko'rsatildi → qayta xabar bermaslik). */
    suspend fun remove(uuid: String) {
        context.myQueuesDataStore.edit { prefs ->
            val current = prefs[uuidSetKey] ?: emptySet()
            prefs[uuidSetKey] = current - uuid
        }
    }

    /** Hammasini tozalash (debug / kun boshida). */
    suspend fun clear() {
        context.myQueuesDataStore.edit { prefs ->
            prefs.remove(uuidSetKey)
        }
    }
}
