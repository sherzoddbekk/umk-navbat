package uz.jurabekov.guard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.RadiusS),
    small      = RoundedCornerShape(Dimens.RadiusM),
    medium     = RoundedCornerShape(Dimens.RadiusM),
    large      = RoundedCornerShape(Dimens.RadiusL),
    extraLarge = RoundedCornerShape(Dimens.RadiusXL)
)
