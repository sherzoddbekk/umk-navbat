package uz.jurabekov.guard.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import uz.jurabekov.guard.data.preferences.dto.SavedDriverData

/**
 * "Eslab qolish" tanlovi orqali driver ma'lumotlarini saqlovchi DataStore.
 *
 * **Maqsad:**
 *  QueueSubmitDialog'da foydalanuvchi "Eslab qolish" checkbox'ni belgilab Yuborish
 *  bosganda mashina/haydovchi ma'lumotlari saqlanadi va keyingi dialog ochilganda
 *  avtomatik to'ldiriladi. UX → takroriy data entry'ni yo'q qiladi.
 *
 * **Storage strategiyasi:**
 *  - Ikkita kalit yagona DataStore faylida:
 *      `KEY_REMEMBER`  : Boolean — checkbox holati (UI restoration uchun)
 *      `KEY_DATA_JSON` : String  — `SavedDriverData` JSON
 *  - **Atomic write**: `saveSession()` ikkalasini bitta `edit{}` ichida yozadi —
 *    yarim-yangilangan state imkonsiz.
 *  - **Parse failure → null** (defensive): corrupt JSON / schema o'zgargani
 *    holatda crash bermasdan recovery. Foydalanuvchi qayta kiritadi va qayta saqlaydi.
 *
 * **Nega alohida DataStore fayli (`saved_driver_prefs`)?**
 *  Separation of concerns: `MyQueuesPreferences` UUID set saqlaydi (FCM cross-check),
 *  bu yerda esa form snapshot. Mustaqil retention semantikasi — logout
 *  remembered driver'ni tozalamasligi mumkin (business decision: hozir tozalamayman).
 *
 * **Nega DataStore (SharedPreferences emas)?**
 *  - Coroutine-friendly (`suspend` + Flow)
 *  - Atomic transaction garansi
 *  - Loyiha bo'yicha konsistent (AuthPreferences, OnboardingPreferences, MyQueuesPreferences)
 *
 * **Xavfsizlik eslatmasi:**
 *  Plain text. Auth token bilan bir xil threat model. Yuqori talablarda →
 *  EncryptedSharedPreferences yoki Keystore+AES-GCM migration.
 */
private val Context.savedDriverDataStore by preferencesDataStore(name = "saved_driver_prefs")

class SavedDriverPreferences(
    private val context: Context,
    private val json: Json
) {

    /**
     * "Eslab qolish" tanlovi oqimi.
     *
     * `true`  → dialog ochilganda saqlangan ma'lumotlar avtomatik to'ldiriladi
     *           va checkbox belgilangan holatda chiqadi.
     * `false` → har gal bo'sh dialog (default behaviour).
     *
     * Default `false` — opt-in privacy: foydalanuvchi explicit roziligisiz
     * hech narsa saqlanmaydi.
     */
    val rememberMe: Flow<Boolean> = context.savedDriverDataStore.data.map { prefs ->
        prefs[KEY_REMEMBER] ?: false
    }.distinctUntilChanged()

    /**
     * Saqlangan driver snapshot. Parse error / mavjud emas → `null`.
     *
     * Defensive parsing — `SerializationException` (schema o'zgardi, JSON corrupt)
     * yoki `IllegalArgumentException` (invalid format) holatlarida crash o'rniga
     * `null` qaytaramiz. Foydalanuvchi shunchaki qayta kiritadi.
     */
    val driverData: Flow<SavedDriverData?> = context.savedDriverDataStore.data.map { prefs ->
        val raw = prefs[KEY_DATA_JSON]?.takeIf { it.isNotBlank() } ?: return@map null
        try {
            json.decodeFromString(SavedDriverData.serializer(), raw)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }.distinctUntilChanged()

    /**
     * Bir martalik snapshot — ViewModel `OpenDialog` event'ida sinxron olish uchun.
     *
     * **Performance**: DataStore in-memory cache — birinchi o'qishdan keyin
     * keyingi `first()` chaqiruvlar instant. Dialog ochilishini bloklamaydi.
     */
    suspend fun snapshot(): Pair<Boolean, SavedDriverData?> {
        val remember = rememberMe.first()
        val data = if (remember) driverData.first() else null
        return remember to data
    }

    /**
     * Driver ma'lumotlarini saqlash — Submit success'dan keyin chaqiriladi.
     *
     * **Atomic** — `KEY_REMEMBER` va `KEY_DATA_JSON` bitta `edit{}` ichida.
     * Race condition: concurrent reader yarim-saqlangan state ko'rmaydi.
     */
    suspend fun save(data: SavedDriverData) {
        val payload = json.encodeToString(SavedDriverData.serializer(), data)
        context.savedDriverDataStore.edit { prefs ->
            prefs[KEY_REMEMBER] = true
            prefs[KEY_DATA_JSON] = payload
        }
    }

    /**
     * Tozalash — foydalanuvchi "Eslab qolish"'ni o'chirib Submit qilganda.
     *
     * Faqat flag o'chmaydi, JSON ham olib tashlanadi — keyingi snapshot
     * to'liq bo'sh (`null`) qaytishi kerak. Aks holda flag=false bo'lsa-da
     * JSON qoladi va boshqa kontekstda noto'g'ri tarqalishi mumkin.
     */
    suspend fun clear() {
        context.savedDriverDataStore.edit { prefs ->
            prefs.remove(KEY_REMEMBER)
            prefs.remove(KEY_DATA_JSON)
        }
    }

    private companion object {
        val KEY_REMEMBER = booleanPreferencesKey("remember_me")
        val KEY_DATA_JSON = stringPreferencesKey("driver_data_json")
    }
}