package uz.jurabekov.guard.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Onboarding va boshqa lokal afzalliklar uchun DataStore.
 *
 * Nima uchun DataStore (SharedPreferences emas):
 *  - Coroutine-friendly (suspend funksiyalar)
 *  - Type-safe Preferences keys
 *  - Atomic transaksiyalar
 *  - Compose'ning Flow'lariga to'g'ri keladi
 */
private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding_prefs"
)

class OnboardingPreferences(private val context: Context) {

    private val dataStore: DataStore<Preferences>
        get() = context.onboardingDataStore

    /** Onboarding tugatilganmi - Flow sifatida (Compose'da kuzatish uchun). */
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    /**
     * Onboarding tugatildi deb belgilash.
     * Suspend - background thread'da yoziladi (main thread'ni bloklamaydi).
     */
    suspend fun setOnboardingCompleted() {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    private companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
