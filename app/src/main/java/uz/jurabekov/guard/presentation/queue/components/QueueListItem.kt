package uz.jurabekov.guard.presentation.queue.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.QueueItemStatus
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.Success500

/**
 * Bitta navbat elementi.
 *
 * Status'ga qarab vizual:
 *  WAITING  — Oq fon, primary number box
 *  ENTERED  — Slate-gray fon, yashil ✓ icon
 *  SKIPPED  — Qizil fon ozroq, ✗ icon, "Navbat o'tib ketdi" qizil yozuv,
 *             plate strikethrough
 *
 * @param onClick `null` bo'lsa item passive (eski QueueScreen ishlatadi).
 *                Non-null bo'lsa ripple effect bilan clickable bo'ladi
 *                (Navbat boshqaruvi ekranida permit dialog ochish uchun).
 * @param actions Karta ichida, ma'lumot qatoridan pastda chiziladigan
 *                ixtiyoriy action qatori (Navbat boshqaruvidagi yo'l
 *                tugmalari). `null` — eski ko'rinish (QueueScreen).
 * @param compact Shrift/paddinglarni bir oz kichraytiradi (Navbat boshqaruvi
 *                ekranida zichroq ro'yxat). `false` — QueueScreen ko'rinishi.
 * @param containerColor Karta fonini majburan belgilaydi (Navbat boshqaruvida
 *                bo'limlarni rang bilan ajratish uchun). `null` — status
 *                bo'yicha standart rang (QueueScreen). SKIPPED holatida
 *                e'tiborga olinmaydi (qizil ogohlantirish saqlanadi).
 * @param borderColor Karta atrofidagi ingichka chegara rangi. `null` —
 *                HistoryItem'da chegara yo'q (standart). WaitingItem doim
 *                o'z chegarasiga ega.
 */
@Composable
fun QueueListItem(
    item: QueueItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    compact: Boolean = false,
    containerColor: Color? = null,
    borderColor: Color? = null
) {
    // Clickable modifier — onClick mavjud bo'lsa qo'shamiz. Bu Card'ning
    // ichidagi padding va shape ustida ishlaydi (ripple to'g'ri renderlanadi).
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    when (item.status) {
        QueueItemStatus.WAITING -> WaitingItem(item, cardModifier, actions, compact, containerColor)
        QueueItemStatus.ENTERED -> HistoryItem(item, cardModifier, skipped = false, actions = actions, compact = compact, containerOverride = containerColor, borderColor = borderColor)
        QueueItemStatus.SKIPPED -> HistoryItem(item, cardModifier, skipped = true, actions = actions, compact = compact, containerOverride = containerColor, borderColor = borderColor)
    }
}

/**
 * Compact rejimidagi o'lchamlar — bitta joyda, WaitingItem va HistoryItem
 * o'rtasida bir xil bo'lishi uchun.
 */
private class ItemSizes(compact: Boolean) {
    val name = if (compact) 13.sp else 14.sp
    val plate = if (compact) 11.sp else 12.sp
    val numberBox = if (compact) 34.dp else 38.dp
    val numberFont = if (compact) 13.sp else 14.sp
    val rowPadV = if (compact) 5.dp else 6.dp
}

/**
 * Action slot'ini karta ichida, ma'lumot qatoridan pastda chizadi.
 * `null` bo'lsa hech narsa qo'shmaydi — karta balandligi eskicha qoladi.
 *
 * Padding YO'Q — content o'z chetlanishini o'zi belgilaydi. Shu tufayli
 * chaqiruv tugmalari inset bo'lishi, "O'tkazilgan" bari esa full-bleed
 * (chetdan-chetga) bo'lishi mumkin. Karta o'z shape'iga clip qilgani uchun
 * full-bleed bar pastki yumaloq burchaklarga mos qirqiladi.
 */
@Composable
private fun CardActions(actions: (@Composable () -> Unit)?) {
    if (actions == null) return
    actions()
}

/* ============================================================
 * Waiting (kirmagan) - oq fon
 * ============================================================ */
@Composable
private fun WaitingItem(
    item: QueueItem,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null,
    compact: Boolean = false,
    containerOverride: Color? = null
) {
    val sizes = ItemSizes(compact)
    Card(
        shape = RoundedCornerShape(Dimens.RadiusM),
        colors = CardDefaults.cardColors(
            containerColor = containerOverride ?: MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpaceM, vertical = sizes.rowPadV)
            ) {
                NumberBox(
                    number = item.queueNumber,
                    bg = MaterialTheme.colorScheme.primaryContainer,
                    fg = MaterialTheme.colorScheme.onPrimaryContainer,
                    sizes = sizes
                )

                Spacer(Modifier.width(Dimens.SpaceM))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fullName,
                        fontSize = sizes.name,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = item.plate,
                        fontSize = sizes.plate,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            CardActions(actions)
        }
    }
}

/* ============================================================
 * History item — entered yoki skipped
 * ============================================================ */
@Composable
private fun HistoryItem(
    item: QueueItem,
    modifier: Modifier = Modifier,
    skipped: Boolean,
    actions: (@Composable () -> Unit)? = null,
    compact: Boolean = false,
    containerOverride: Color? = null,
    borderColor: Color? = null
) {
    val sizes = ItemSizes(compact)
    val containerColor = when {
        skipped -> Color(0xFFFEE2E2)          // Soft red-50 (override e'tiborga olinmaydi)
        else -> containerOverride ?: Color(0xFFCBD5DC)  // Slate (default)
    }

    val numberBg = if (skipped) Color(0xFFFCA5A5) else Color(0xFFA8B7C2)
    val numberFg = if (skipped) Color(0xFF7F1D1D) else Color(0xFF1E2A38)

    val titleColor = if (skipped) Color(0xFF991B1B) else Color(0xFF334155)
    val secondaryColor = if (skipped) Color(0xFFB91C1C) else Color(0xFF566472)

    Card(
        shape = RoundedCornerShape(Dimens.RadiusM),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = borderColor?.let { BorderStroke(0.8.dp, it) },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpaceM, vertical = sizes.rowPadV)
            ) {
                NumberBox(number = item.queueNumber, bg = numberBg, fg = numberFg, sizes = sizes)

                Spacer(Modifier.width(Dimens.SpaceM))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fullName,
                        fontSize = sizes.name,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(1.dp))

                    if (skipped) {
                        // Plate + status matn
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.plate,
                                fontSize = sizes.plate,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.6.sp,
                                color = secondaryColor,
                                textDecoration = TextDecoration.LineThrough,
                                maxLines = 1
                            )
                            Text(
                                text = "  ·  Navbat o'tib ketdi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626),
                                maxLines = 1
                            )
                        }
                    } else {
                        Text(
                            text = item.plate,
                            fontSize = sizes.plate,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.6.sp,
                            color = secondaryColor,
                            maxLines = 1
                        )
                    }
                }

                // Status icon / vaqt badge
                StatusBadge(item = item, skipped = skipped)
            }

            CardActions(actions)
        }
    }
}
@Composable
private fun StatusBadge(item: QueueItem, skipped: Boolean) {
    if (skipped) {
        // SKIPPED — qizil ✗ icon (eski logika)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFDC2626).copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Navbat o'tib ketdi",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(12.dp)
            )
        }
    } else {
        // ENTERED — yashil pill ichida "07:13"
        // Vaqt yo'q bo'lsa, fallback ✓ icon (defensive)
        if (item.arrivedAtHHmm.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Success500.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = item.arrivedAtHHmm,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.4.sp,
                    color = Color(0xFF1E7C3F),
                    maxLines = 1
                )
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Success500.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Kirgan",
                    tint = Color(0xFF1E7C3F),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}


@Composable
private fun NumberBox(number: Int, bg: Color, fg: Color, sizes: ItemSizes) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizes.numberBox)
            .clip(RoundedCornerShape(Dimens.RadiusS))
            .background(bg)
    ) {
        Text(
            text = "$number",
            fontSize = sizes.numberFont,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}
