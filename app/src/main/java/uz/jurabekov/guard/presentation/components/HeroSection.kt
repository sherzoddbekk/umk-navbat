package uz.jurabekov.guard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.PrimaryGradBot
import uz.jurabekov.guard.ui.theme.PrimaryGradTop

/**
 * Yuqori dekorativ bo'lim — gradient background, truck icon, sarlavha.
 * Industrial muhit uchun aniq vizual identitet beradi.
 */
@Composable
fun HeroSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryGradTop, PrimaryGradBot)
                ),
                shape = RoundedCornerShape(
                    bottomStart = Dimens.RadiusXL,
                    bottomEnd = Dimens.RadiusXL
                )
            )
            .padding(
                horizontal = Dimens.SpaceL,
                vertical = Dimens.SpaceXL
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon container — yuqori-tortishish hisi
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Dimens.HeroIconBox)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(Dimens.RadiusXL)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Dimens.HeroIconSize)
                )
            }

            Spacer(Modifier.height(Dimens.SpaceL))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(Dimens.SpaceS))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}
