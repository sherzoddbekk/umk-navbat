package uz.jurabekov.guard.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import uz.jurabekov.guard.data.preferences.dto.OwnedQueue

/**
 * Foydalanuvchi olgan navbatlarni (`owner_token` bilan) saqlovchi DataStore.
 *
 * **Nega [MyQueuesPreferences]dan alohida?**
 *  `MyQueuesPreferences` faqat UUID `Set` saqlaydi (FCM cross-check uchun). Bu
 *  yerda esa to'liq [OwnedQueue] obyekti (token, plate, queue_number, ...) —
 *  "egasi navbatni bekor qilish" oqimi uchun. Ikki xil retention/semantika,
 *  shuning uchun mustaqil store.
 *
 * **Kun bo'yicha tozalash:**
 *  [add] chaqirilganda yangi yozuvning `date`'idan farq qiladigan (eski kun)
 *  yozuvlar avtomatik olib tashlanadi — DataStore hech qachon eskirgan kun
 *  ma'lumotini ushlab qolmaydi.
 *
 * **Storage:** yagona JSON array string. Parse xato → bo'sh ro'yxat (defensive).
 */
private val Context.ownedQueuesDataStore by preferencesDataStore(name = "owned_queues_prefs")

class OwnedQueuesPreferences(
    private val context: Context,
    private val json: Json
) {

    private val listSerializer = ListSerializer(OwnedQueue.serializer())

    /** Saqlangan navbatlar oqimi. Parse xato → bo'sh ro'yxat. */
    val queues: Flow<List<OwnedQueue>> = context.ownedQueuesDataStore.data.map { prefs ->
        prefs[KEY_JSON]?.takeIf { it.isNotBlank() }?.let { decode(it) } ?: emptyList()
    }

    /** Bir martalik snapshot — dialog ochilganda sinxron o'qish uchun. */
    suspend fun snapshot(): List<OwnedQueue> = queues.first()

    /**
     * Yangi navbat qo'shish (submit success).
     *
     *  - Yangi yozuvning `date`'idan boshqa kunga tegishli yozuvlar tashlab
     *    yuboriladi (kun rollover → eski data yo'q qilinadi).
     *  - Xuddi shu `uuid` avval bo'lsa — almashtiriladi (idempotent re-submit himoyasi).
     */
    suspend fun add(queue: OwnedQueue) {
        context.ownedQueuesDataStore.edit { prefs ->
            val current = prefs[KEY_JSON]?.let { decode(it) } ?: emptyList()
            val sameDay = current.filter { it.date == queue.date && it.uuid != queue.uuid }
            prefs[KEY_JSON] = encode(sameDay + queue)
        }
    }

    /** UUID bo'yicha yozuvni o'chirish (bekor qilingandan keyin). */
    suspend fun remove(uuid: String) {
        context.ownedQueuesDataStore.edit { prefs ->
            val current = prefs[KEY_JSON]?.let { decode(it) } ?: return@edit
            prefs[KEY_JSON] = encode(current.filterNot { it.uuid == uuid })
        }
    }

    private fun decode(raw: String): List<OwnedQueue> = try {
        json.decodeFromString(listSerializer, raw)
    } catch (_: SerializationException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }

    private fun encode(list: List<OwnedQueue>): String =
        json.encodeToString(listSerializer, list)

    private companion object {
        val KEY_JSON = stringPreferencesKey("owned_queues_json")
    }
}
