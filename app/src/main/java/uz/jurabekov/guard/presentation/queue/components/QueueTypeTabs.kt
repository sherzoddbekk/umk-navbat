package uz.jurabekov.guard.presentation.queue.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.domain.model.VehicleType
import uz.jurabekov.guard.ui.theme.Dimens

/**
 * Zamonaviy segmented tab bar — ikki kategoriya o'rtasida tanlash.
 *
 * Dizayn xususiyatlari:
 *  - Ust-ust qo'yilgan (segmented) ko'rinish — iOS/Material You uyg'unlik.
 *  - Active segment'da fon va matn animatsiyalanadi.
 *  - Icon + text bilan vizual semantika.
 *  - Pill (50% radius) — muloyim, hozirgi UI til bilan mos.
 */
/**
 * @param activeContainerColor tanlangan segment foni. Default — `surface`
 *        (QueueScreen'dagi eski ko'rinish). Navbat boshqaruvi ekranida to'q
 *        rang (masalan Accent) berilib, tanlov aniqroq ajratiladi.
 * @param activeContentColor tanlangan segment matn/ikonasi rangi.
 */
@Composable
fun QueueTypeTabs(
    selected: VehicleType,
    onSelect: (VehicleType) -> Unit,
    modifier: Modifier = Modifier,
    activeContainerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    activeContentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabSegment(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Outlined.LocalShipping,
                label = "Usti ochiq",
                active = selected == VehicleType.OPEN,
                activeContainerColor = activeContainerColor,
                activeContentColor = activeContentColor,
                onClick = { onSelect(VehicleType.OPEN) }
            )
            TabSegment(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Outlined.Warehouse,
                label = "Usti yopiq",
                active = selected == VehicleType.TENT,
                activeContainerColor = activeContainerColor,
                activeContentColor = activeContentColor,
                onClick = { onSelect(VehicleType.TENT) }
            )
        }
    }
}

@Composable
private fun TabSegment(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeContainerColor: androidx.compose.ui.graphics.Color,
    activeContentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(200),
        label = "tab-bg"
    )
    val elevation by animateDpAsState(
        targetValue = if (active) 2.dp else 0.dp,
        animationSpec = tween(200),
        label = "tab-elev"
    )

    val bg = if (active) {
        activeContainerColor.copy(alpha = bgAlpha)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    val fg = if (active) {
        activeContentColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bg,
        shadowElevation = elevation,
        tonalElevation = if (active) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = Dimens.SpaceM)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
            Box(Modifier.size(width = 6.dp, height = 1.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color = fg,
                maxLines = 1
            )
        }
    }
}