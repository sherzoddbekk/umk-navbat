package uz.jurabekov.guard.presentation.onboarding

import androidx.annotation.DrawableRes

data class OnboardingPage(
    @DrawableRes val imageRes: Int,
    val title: String,
    val description: String
)
