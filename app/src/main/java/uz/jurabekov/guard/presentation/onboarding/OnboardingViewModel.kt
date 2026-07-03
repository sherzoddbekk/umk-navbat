package uz.jurabekov.guard.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.jurabekov.guard.data.preferences.OnboardingPreferences

class OnboardingViewModel(
    private val preferences: OnboardingPreferences
) : ViewModel() {

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            preferences.setOnboardingCompleted()
            onComplete()
        }
    }
}
