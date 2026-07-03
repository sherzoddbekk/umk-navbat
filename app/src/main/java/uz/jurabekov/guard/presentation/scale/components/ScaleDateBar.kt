package uz.jurabekov.guard.presentation.scale.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Sana ko'rsatkichi va yangilash buttoni qatori.
 *
 * **Layout:**
 *  - Sol: clickable "sana" chip (CalendarMonth icon + dd.MM.yyyy)
 *  - O'ng: refresh icon button
 *
 * **Total counter olib tashlandi** — `ScaleStatusFilterRow` ichidagi
 * "Hammasi N" chip uni o'rnini bosadi. Duplicate ma'lumot ekrandan
 * tozalanadi.
 *
 * **States:**
 *  - `isRefreshing = true` — refresh icon o'rniga progress indicator
 *  - `isLoading = true` — refresh button disabled
 */
@Composable
fun ScaleDateBar(
    isoDate: String,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onDateClick: () -> Unit,
    onRefreshClick: () -> Unit,
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
        // === Date chip (clickable) ===
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

        // === Refresh button ===
        RefreshIconButton(
            isRefreshing = isRefreshing,
            enabled = !isLoading && !isRefreshing,
            onClick = onRefreshClick
        )
    }
}

/* ============================================================
 * Refresh button — animatsiyali
 * ============================================================ */
@Composable
private fun RefreshIconButton(
    isRefreshing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "refresh-rotation"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.RadiusM),
        color = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Yangilash",
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}

/* ============================================================
 * Helpers
 * ============================================================ */
private fun formatDisplayDate(iso: String): String = runCatching {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.US)
    formatter.format(parser.parse(iso)!!)
}.getOrDefault(iso)
