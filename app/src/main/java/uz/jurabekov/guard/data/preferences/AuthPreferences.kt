package uz.jurabekov.guard.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import uz.jurabekov.guard.data.preferences.dto.StoredUser

/**
 * Avtorizatsiya state'ini saqlovchi DataStore.
 *
 * v2 (multi-key session):
 *  - `auth_token`  — Bearer token (interceptor uchun)
 *  - `auth_user`   — `StoredUser` JSON (drawer + permission gating uchun)
 *
 * **Atomic write garansi**: ikkala kalit bitta `edit{}` block ichida yoziladi.
 * DataStore mutex bilan himoyalangan — concurrent reader yarim-yangilangan
 * state ko'rmaydi. Login race condition (token saqlandi, user esa yo'q)
 * imkonsiz.
 *
 * **Forward compat**: yangi kalit qo'shilsa (masalan, `last_login_at`)
 * eski kalitlar saqlanib qoladi, schema migration shart emas.
 *
 * **Xavfsizlik**:
 *  - `allowBackup=false` + `dataExtractionRules` — cloud va adb backup yo'q
 *  - Release build'da HTTP logger NONE — token logga tushmaydi
 *  - Plain text DataStore yetarli (internal ERP, mobile banking emas)
 *
 * Yuqori xavfsizlik kerak bo'lsa — `EncryptedSharedPreferences` yoki
 * Android Keystore + AES-GCM.
 */
private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

class AuthPreferences(
    private val context: Context,
    private val json: Json
) {

    /** Bearer token oqimi — interceptor va router uchun. */
    val token: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[TOKEN_KEY]?.takeIf { it.isNotBlank() }
    }.distinctUntilChanged()

    /**
     * Saqlangan user oqimi. JSON parse xatosida `null` qaytaramiz va
     * bo'sh kalit qoldiramiz (`clear()` chaqirmaymiz — token hali validmi
     * yo'qmi bilmaymiz; explicit logout faqat foydalanuvchidan keladi).
     *
     * `SerializationException` — schema o'zgargan / corrupt JSON holatlari.
     * Defensive parsing — crash bermasdan recover.
     */
    val user: Flow<StoredUser?> = context.authDataStore.data.map { prefs ->
        val raw = prefs[USER_KEY]?.takeIf { it.isNotBlank() } ?: return@map null
        try {
            json.decodeFromString(StoredUser.serializer(), raw)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }.distinctUntilChanged()

    /**
     * Login holati — ikkala kalit ham bo'lishi shart.
     *
     * `token != null && user != null` — bittasi bo'lmasa unauthorized.
     * `combine` distinct emit qiladi (oxirgi `distinctUntilChanged` bilan).
     */
    val isLoggedIn: Flow<Boolean> = combine(token, user) { t, u ->
        t != null && u != null
    }.distinctUntilChanged()

    /**
     * Bir martalik token snapshot — `AuthInterceptor` (suspending context,
     * `runBlocking` ichida) uchun.
     */
    suspend fun getToken(): String? = token.first()

    /**
     * Login muvaffaqiyatli — token va user atomic saqlanadi.
     *
     * Race condition oldini olish: ikkala kalit bitta `edit{}` ichida.
     * Yangi sessiya boshlanganda eski state to'liq yoziladi (overwrite).
     */
    suspend fun saveSession(token: String, user: StoredUser) {
        val userJson = json.encodeToString(StoredUser.serializer(), user)
        context.authDataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_KEY] = userJson
        }
    }

    /** Logout — barcha kalitlar tozalanadi. */
    suspend fun clear() {
        context.authDataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_KEY)
        }
    }

    private companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val USER_KEY = stringPreferencesKey("auth_user")
    }
}
