package uz.jurabekov.guard.presentation.queue_management.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.presentation.queue_management.QueueSection
import uz.jurabekov.guard.ui.theme.Dimens

/**
 * Asosiy bo'lim tanlagichi — "Ruxsatnoma navbati", "Darvoza navbati",
 * "Berilgan ruxsatnomalar".
 *
 * [QueueTypeTabs] bilan bir uslub (segmented pill), lekin:
 *  - 2 yoki 3 segment (rolga qarab — GATE_QUEUE ba'zan yashiringan)
 *  - matn ikki qatorgacha (yorliqlar uzun), ikonasiz
 *
 * @param sections ko'rsatiladigan bo'limlar (allaqachon rol bo'yicha filtrlangan)
 */
@Composable
fun QueueSectionTabs(
    sections: List<QueueSection>,
    selected: QueueSection,
    onSelect: (QueueSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceXS),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(Dimens.RadiusM)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sections.forEach { section ->
                SectionSegment(
                    label = section.title,
                    active = section == selected,
                    onClick = { onSelect(section) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun SectionSegment(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tanlangan segment — to'ldirilgan primary (ko'k), oq matn. Tanlanmagan —
    // shaffof, muted matn. Oq fon o'rniga to'q rang aniqroq ajratadi.
    val bg by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary
        else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(200),
        label = "section-bg"
    )
    val fg by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "section-fg"
    )
    val elevation by animateDpAsState(
        targetValue = if (active) 2.dp else 0.dp,
        animationSpec = tween(200),
        label = "section-elev"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.RadiusS),
        color = bg,
        shadowElevation = elevation,
        tonalElevation = if (active) 2.dp else 0.dp,
        modifier = modifier
    ) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = if (active) androidx.compose.ui.text.font.FontWeight.SemiBold
                else androidx.compose.ui.text.font.FontWeight.Medium,
                color = fg,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
