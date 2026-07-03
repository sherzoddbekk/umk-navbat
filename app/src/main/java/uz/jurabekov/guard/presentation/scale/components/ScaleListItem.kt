package uz.jurabekov.guard.presentation.scale.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.domain.model.ScaleRecord
import uz.jurabekov.guard.domain.model.ScaleStatus
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.Success500
import uz.jurabekov.guard.ui.theme.Warning500

/**
 * Bitta tarozi yozuvi.
 *
 * Layout strategiyasi:
 *  - Sol: tartib raqami (NumberBox — QueueListItem'dagi pattern)
 *  - Markaz: plate (monospace, prominent) + KPP + time range
 *  - O'ng: status pill
 *  - Pastki qator: brutto/tara/netto badge'lari (faqat mavjudlar)
 *
 * **Padding strategiyasi:** kichik — foydalanuvchi so'rovi bo'yicha
 * (`padding va marginlarni kichikroq qilib ber`). Vertical 8dp, horizontal
 * 12dp. Tarozi yozuvi tarkibi zich, oson o'qiladi.
 *
 * **Status color mapping:**
 *  - COMPLETED → yashil (Success500) — yakunlangan
 *  - INSIDE    → sariq (Warning500) — diqqat, hali hududda
 *  - UNKNOWN   → kulrang — defensive fallback
 */
@Composable
fun ScaleListItem(
    index: Int,
    record: ScaleRecord,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(Dimens.RadiusM),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // === Yuqori qator: index + plate/kpp + status ===
            Row(verticalAlignment = Alignment.CenterVertically) {
                IndexBox(number = index)

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.plate.ifBlank { "—" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = record.kpp.ifBlank { "—" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                StatusPill(status = record.status)
            }

            // === O'rta qator: vaqt range ===
            if (record.entryTime != null || record.exitTime != null) {
                Spacer(Modifier.height(6.dp))
                TimeRangeRow(
                    entry = record.entryTime,
                    exit = record.exitTime
                )
            }

            // === Pastki qator: tortish badge'lari ===
            val hasWeights = record.brutto != null ||
                    record.tara != null ||
                    record.netto != null

            if (hasWeights) {
                Spacer(Modifier.height(6.dp))
                WeightsRow(
                    brutto = record.brutto,
                    tara = record.tara,
                    netto = record.netto
                )
            }
        }
    }
}

/* ============================================================
 * Index — chap tomondagi tartib raqami
 * ============================================================ */
@Composable
private fun IndexBox(number: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 32.dp, height = 32.dp)
            .clip(RoundedCornerShape(Dimens.RadiusS))
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            text = "$number",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/* ============================================================
 * Status Pill
 * ============================================================ */
@Composable
private fun StatusPill(status: ScaleStatus) {
    val (bg, fg, label) = when (status) {
        ScaleStatus.COMPLETED -> Triple(
            Success500.copy(alpha = 0.15f),
            Success500,
            "Yakunlangan"
        )
        ScaleStatus.INSIDE -> Triple(
            Warning500.copy(alpha = 0.18f),
            Color(0xFFB45309),  // amber-700 — kontrast yaxshilash
            "Ichkarida"
        )
        ScaleStatus.UNKNOWN -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "—"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

/* ============================================================
 * Time range: 08:42 → 11:15  yoki  08:42 → —
 * ============================================================ */
@Composable
private fun TimeRangeRow(entry: String?, exit: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 42.dp)  // IndexBox + spacer hizalash
    ) {
        Icon(
            imageVector = Icons.Outlined.AccessTime,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = entry ?: "—",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(10.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = exit ?: "—",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = if (exit == null) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

/* ============================================================
 * Brutto / Tara / Netto badge'lari
 * ============================================================ */
@Composable
private fun WeightsRow(
    brutto: String?,
    tara: String?,
    netto: String?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 42.dp)  // IndexBox hizalash
    ) {
        brutto?.let { WeightBadge(label = "Brutto", value = it) }
        tara?.let { WeightBadge(label = "Tara", value = it) }
        netto?.let { WeightBadge(label = "Netto", value = it, emphasize = true) }
    }
}

@Composable
private fun WeightBadge(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    val bg = if (emphasize) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val labelColor = if (emphasize) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val valueColor = if (emphasize) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
    }
}
