package uz.jurabekov.guard.presentation.queue_management.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Navbat boshqaruvi ekrani uchun yuqori bar.
 *
 * Layout:
 *  ┌────────────────────────────────────┐
 *  │ 📅 18.05.2026         Jami: 17 ta │
 *  └────────────────────────────────────┘
 *
 *  - Sol: clickable date chip (CalendarMonth icon + dd.MM.yyyy)
 *  - O'ng: Jami counter pill (OPEN+TENT permitted total)
 *
 * Pull-to-refresh ekranning asosiy qismida (LazyColumn ustida) bo'lgani
 * uchun bu yerda refresh icon yo'q — UI sodda qoladi.
 */
@Composable
fun QueueDateBar(
    isoDate: String,
    totalPermittedCount: Int,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayDate = remember(isoDate) { formatDisplayDate(isoDate) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.SpaceM,
                vertical = Dimens.SpaceS
            )
    ) {
        // === Date chip ===
        Surface(
            onClick = onDateClick,
            shape = RoundedCornerShape(Dimens.RadiusM),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                modifier = Modifier.padding(
                    horizontal = Dimens.SpaceM,
                    vertical = 10.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = displayDate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // === Jami counter (jami: OPEN + TENT permitted) ===
        TotalCountPill(count = totalPermittedCount)
    }
}

/**
 * "Jami: N ta" pill. `count = 0` bo'lsa ham ko'rsatamiz — foydalanuvchiga
 * "ma'lumot yo'q" emas, "0 ta kirgan" axborot.
 */
@Composable
private fun TotalCountPill(count: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusM))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Jami: $count ta",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun formatDisplayDate(iso: String): String = runCatching {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.US)
    formatter.format(parser.parse(iso)!!)
}.getOrDefault(iso)
