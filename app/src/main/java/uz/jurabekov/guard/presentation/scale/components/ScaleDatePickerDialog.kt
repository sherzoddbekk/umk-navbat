package uz.jurabekov.guard.presentation.scale.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Material3 DatePicker dialog wrapper.
 *
 * **UTC ↔ Local date conversion:**
 * Material3 DatePicker `selectedDateMillis` UTC midnight'da bo'ladi (boshqacha
 * aytganda — kun bilan bog'liq, vaqt zonasi neutral). Backend `yyyy-MM-dd`
 * formatda kunni qabul qiladi (TZ-noise yo'q).
 *
 * Conversion: `Date(utcMillis)` ni `SimpleDateFormat` UTC TZ bilan format
 * qilamiz. Lokal TZ ishlatsak — UTC+5 dan oldin/keyin midnight'da bir kun
 * "siljiydi", bug.
 *
 * **`SelectableDates`:** kelajak sanalarni bloklash uchun. Foydalanuvchi
 * ertangi kun tarozi yozuvlarini ko'ra olmaydi (backend ham bermaydi).
 * Buffer: `+24h` — kech kunni hisobga olib.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleDatePickerDialog(
    initialIsoDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val initialMillis = remember(initialIsoDate) {
        runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(initialIsoDate)?.time
        }.getOrNull() ?: System.currentTimeMillis()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Bugungi kun + buffer
                    val maxAllowed = System.currentTimeMillis() + 24L * 3600 * 1000
                    return utcTimeMillis <= maxAllowed
                }
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        onDateSelected(formatToIso(millis))
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Tanlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/** UTC midnight millis → `yyyy-MM-dd` (UTC). */
private fun formatToIso(utcMillis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(Date(utcMillis))
}
