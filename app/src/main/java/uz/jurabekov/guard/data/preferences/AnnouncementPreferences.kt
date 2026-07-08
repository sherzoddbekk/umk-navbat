package uz.jurabekov.guard.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Bir martalik "Yangilik!" e'lonlari ko'rsatildimi — flag'larni saqlaydi.
 *
 * Har bir e'lon uchun alohida boolean kalit. Yangi e'lon qo'shilsa yangi
 * kalit qo'shiladi — eski flag'lar tegilmaydi.
 *
 * Hozircha: `cancel_feature` — "navbatni bekor qilish" imkoniyati e'loni.
 * Faqat birinchi marta (flag=false) ko'rsatiladi, `markCancelFeatureShown()`
 * chaqirilgach qayta chiqmaydi.
 */
private val Context.announcementDataStore by preferencesDataStore(name = "announcement_prefs")

class AnnouncementPreferences(private val context: Context) {

    /** "Navbatni bekor qilish" e'loni allaqachon ko'rsatilganmi. */
    suspend fun isCancelFeatureShown(): Boolean =
        context.announcementDataStore.data
            .map { it[KEY_CANCEL_FEATURE] ?: false }
            .first()

    /** E'lon ko'rsatildi deb belgilash — qayta chiqmaydi. */
    suspend fun markCancelFeatureShown() {
        context.announcementDataStore.edit { it[KEY_CANCEL_FEATURE] = true }
    }

    private companion object {
        val KEY_CANCEL_FEATURE = booleanPreferencesKey("cancel_feature_shown")
    }
}
