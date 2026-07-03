package uz.jurabekov.guard.presentation.scale.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.presentation.scale.ScaleStatusFilter
import uz.jurabekov.guard.presentation.scale.StatusCounts
import uz.jurabekov.guard.ui.theme.Dimens

/**
 * Status filter chip qatori — Jami / Yakunlangan / Ichkarida.
 *
 * **Layout strategiyasi — `horizontalScroll`:**
 *
 * Eski variant `Modifier.weight(1f)` har chipga teng width berdi —
 * "Yakunlangan 8" matn kichik ekranlarda 2 qatorga wrap bo'lib qoldi
 * (constrained width + soft-wrap default).
 *
 * Yangi variant: chiplar `wrapContentWidth()` bilan o'z kontentlariga
 * mos width oladi. Tashqi `Row` `horizontalScroll` — ekranga sig'masa
 * foydalanuvchi scroll qiladi. Matn hech qachon wrap bo'lmaydi.
 *
 * **Defensiv qadamlar:**
 *  - `Text(maxLines = 1, softWrap = false)` — explicit no-wrap
 *  - Chip ichida `wrapContentWidth` — content fit
 *  - `horizontalScroll` parent uchun
 *
 * **Future-proof:** kelajakda 5+ filter qo'shilsa (SKIPPED, DATE_RANGE,
 * va h.k.) ham layout buzilmaydi — scroll handle qiladi.
 *
 * **Accessibility:** scroll content TalkBack bilan to'liq navigatsiya
 * qilinadi (Compose default behavior).
 */
@Composable
fun ScaleStatusFilterRow(
    selected: ScaleStatusFilter,
    counts: StatusCounts,
    onFilterChange: (ScaleStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(
                horizontal = Dimens.SpaceM,
                vertical = 4.dp
            )
    ) {
        ScaleStatusFilter.entries.forEach { filter ->
            val count = counts.countFor(filter)
            FilterChip(
                label = filter.label,
                count = count,
                isSelected = selected == filter,
                isEnabled = count > 0 || filter == ScaleStatusFilter.ALL,
                onClick = { onFilterChange(filter) }
            )
        }
    }
}

/**
 * Bitta filter chip.
 *
 * **Width strategy:** `wrapContentWidth()` — content (label + count)
 * uzunligiga mos width. Parent `horizontalScroll` ichida bo'lgani uchun
 * unbounded width — chip o'z intrinsic measurement'ini oladi.
 *
 * **`maxLines + softWrap`:** ikkala measure ham qo'yilgan — `maxLines=1`
 * yetarli, lekin `softWrap=false` qo'shimcha guarantee (kelajakda kimdir
 * parent modifier'ni o'zgartirsa, text baribir wrap bo'lmaydi).
 */
@Composable
private fun FilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val (bg, fg, badgeFg) = when {
        !isEnabled -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        isSelected -> Triple(
            MaterialTheme.colorScheme.primary,
            Color.White,
            Color.White
        )

        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.primary
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            softWrap = false
        )

        Spacer(Modifier.width(6.dp))

        CountBadge(
            count = count,
            fgColor = badgeFg,
            isSelected = isSelected
        )
    }
}

/**
 * Count badge — chip ichidagi raqam.
 *
 * **Min-width strategy:** 1-2-3 xonali sonlar uchun bir xil ko'rinish.
 * Hech qachon wrap bo'lmaydi (`maxLines = 1`).
 *
 * **Visual contrast:**
 *  - Selected chip ichida: yarim-shaffof oq fon (primary ustida)
 *  - Unselected: surface fon (chip surfaceVariant ustida — kontrast)
 */
@Composable
private fun CountBadge(
    count: Int,
    fgColor: Color,
    isSelected: Boolean
) {
    val badgeBg = if (isSelected) {
        Color.White.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(badgeBg)
            // Min-width: 1 xonali ham 3+ xonali ham stable circle ko'rinish.
            // Content yetarli bo'lsa kengayadi (4 xonali son edge case).
            .widthIn(min = 22.dp)
            .padding(horizontal = 7.dp, vertical = 1.dp)
    ) {
        Text(
            text = "$count",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fgColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
