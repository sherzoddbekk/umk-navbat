package uz.jurabekov.guard.presentation.queue_management.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uz.jurabekov.guard.domain.model.Permit
import uz.jurabekov.guard.presentation.queue_management.PermitDialogState
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.Error500
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Vaqtinchalik ruxsatnoma dialogi.
 *
 * **Sizing:**
 *  `usePlatformDefaultWidth = false` — Compose default 280dp dialog'ni
 *  bypass qilamiz, o'zimizning width'ni qo'yamiz (90%vw).
 */
@Composable
fun PermitDialog(
    state: PermitDialogState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(Dimens.RadiusL),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = Dimens.ElevationDialog,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(min = 200.dp, max = 720.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Header(onClose = onDismiss)

                // Body — har holatga qarab
                when (state) {
                    is PermitDialogState.Loading -> LoadingBody()
                    is PermitDialogState.Empty -> EmptyBody()
                    is PermitDialogState.Error -> ErrorBody(
                        message = state.message,
                        onRetry = onRetry
                    )
                    is PermitDialogState.Loaded -> LoadedBody(permit = state.permit)
                }
            }
        }
    }
}

/* ============================================================
 * Header
 * ============================================================ */

@Composable
private fun Header(onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.SpaceL, end = Dimens.SpaceS, top = Dimens.SpaceM, bottom = Dimens.SpaceS)
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(Dimens.SpaceS))
        Text(
            text = "Vaqtinchalik ruxsatnoma",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Yopish",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalLine()
}

@Composable
private fun HorizontalLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}

/* ============================================================
 * Loaded body — QR + tafsilotlar
 * ============================================================ */
@Composable
private fun LoadedBody(permit: Permit) {
    val scrollState = rememberScrollState()
    val qrContent = remember(permit.uuid) { "https://gate.uzbeksteel.uz/verify/${permit.uuid}" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS)
    ) {
        // === № raqami + QR kod — bitta qatorda ===
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            modifier = Modifier.fillMaxWidth()
        ) {
            QrCodeImage(
                content = qrContent,
                size = 120.dp
            )

            Text(
                text = "№ ${permit.displayNumber}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(Dimens.SpaceS))

        // === Ma'lumotlar — label: value bir qatorda ===
        DetailRow(label = "Ism sharifi", value = permit.fullName)
        DetailRow(label = "Tashkilot nomi", value = permit.organization)
        DetailRow(
            label = "Passport",
            value = formatPassport(permit.passportSeries, permit.passportNumber)
        )
        DetailRow(label = "Borayotgan joyi", value = permit.destination)
        DetailRow(label = "Qabul qiluvchi", value = permit.recipient)
        DetailRow(
            label = "Avtomashina",
            value = formatVehicle(permit.vehicleBrand, permit.plate)
        )
        DetailRow(label = "Ruxsat etiladi", value = permit.purpose)
        DetailRow(
            label = "Berilgan vaqti",
            value = formatDateTime(permit.issuedAtEpochMs, permit.issuedAtRaw)
        )
        DetailRow(label = "Amal qilish vaqti", value = permit.validDuration)
        DetailRow(label = "R.B. navbatchisi", value = permit.dutyOfficer)
        DetailRow(label = "Smena katta qorovuli", value = permit.shiftGuard)

        Spacer(Modifier.height(Dimens.SpaceS))
    }
}

/* ============================================================
 * Loading / Empty / Error bodies
 * ============================================================ */
@Composable
private fun LoadingBody() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .padding(Dimens.SpaceL)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun EmptyBody() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .padding(Dimens.SpaceL)
    ) {
        Text(
            text = "Ruxsatnoma hali chiqarilmagan",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.SpaceXS))
        Text(
            text = "Bu mashinaga ruxsatnoma ko'rinmadi",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .padding(Dimens.SpaceL)
    ) {
        Text(
            text = "Yuklab bo'lmadi",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Error500,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.SpaceXS))
        Text(
            text = message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.SpaceS))
        TextButton(onClick = onRetry) {
            Text("Qayta urinish")
        }
    }
}

/* ============================================================
 * DetailRow — label: value bir qatorda (compact)
 *
 * Joy yetmasa value pastga wrap bo'ladi (Compose Text default).
 * Padding minimal — dialog'ga ko'proq field sig'ishi uchun.
 * ============================================================ */
@Composable
private fun DetailRow(label: String, value: String?) {
    val displayValue = if (value.isNullOrBlank()) "—" else value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = "$label: ",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = displayValue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/* ============================================================
 * Formatting helpers
 * ============================================================ */

private fun formatPassport(series: String?, number: String?): String? {
    if (series.isNullOrBlank() && number.isNullOrBlank()) return null
    return listOfNotNull(series, number)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

private fun formatVehicle(brand: String?, plate: String?): String? {
    return when {
        brand.isNullOrBlank() && plate.isNullOrBlank() -> null
        brand.isNullOrBlank() -> plate
        plate.isNullOrBlank() -> brand
        else -> "$brand · $plate"
    }
}

/**
 * Berilgan vaqti formatlash.
 *  - Parsed epoch ms mavjud (>0) → "dd.MM.yyyy HH:mm" local TZ
 *  - Aks holda → raw string (defensive — debug uchun)
 */
private fun formatDateTime(epochMs: Long, raw: String?): String? {
    if (epochMs > 0) {
        return runCatching {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).apply {
                // Asia/Tashkent UTC+5 — Toshkent vaqtida ko'rsatamiz.
                // Foydalanuvchining qurilma TZ'sini ham olish mumkin
                // (TimeZone.getDefault()), lekin korxona local context'da
                // doim UZ vaqtida o'qish kerak — biz hardcode qilamiz.
                timeZone = TimeZone.getTimeZone("Asia/Tashkent")
            }.format(Date(epochMs))
        }.getOrDefault(raw)
    }
    return raw?.takeIf { it.isNotBlank() }
}
