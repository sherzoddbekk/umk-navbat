package uz.jurabekov.guard.presentation.queue_management.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.QueueItemStatus
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.GuardTheme

/**
 * `QueueManagementItem`ning barcha holatlari — Android Studio Preview'da
 * server va emulatorsiz ko'rish uchun.
 *
 * Preview'lar `main` source set'da: `@Preview` annotation'i `ui-tooling-preview`
 * kutubxonasidan (release'ga qo'shiladi, ~0 KB), render qiluvchi `ui-tooling`
 * esa faqat `debugImplementation` — ya'ni release APK/AAB'ga tegmaydi.
 */

/* ============================================================
 * Fake ma'lumot — faqat preview uchun
 * ============================================================ */
private fun previewItem(
    queueNumber: Int = 3,
    hasPermit: Boolean = true,
    infoLane: Int? = null,
    manualPassed: Boolean = false,
    status: QueueItemStatus = QueueItemStatus.ENTERED
) = QueueItem(
    id = 13299,
    uuid = "preview-$queueNumber-$infoLane",
    queueNumber = queueNumber,
    plate = "80 X 226 BB",
    fullName = "Toyloq",
    hasPermit = hasPermit,
    status = status,
    createdAtEpochMs = 0L,
    arrivedAtHHmm = "07:31",
    infoLane = infoLane,
    manualPassed = manualPassed
)

@Composable
private fun PreviewCase(
    title: String,
    item: QueueItem,
    canManageInfoLane: Boolean = true,
    isActionInProgress: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.SpaceS),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        QueueManagementItem(
            item = item,
            canManageInfoLane = canManageInfoLane,
            isActionInProgress = isActionInProgress,
            onClick = {},
            onLaneCall = {},
            onManualPass = {}
        )
    }
}

/**
 * Barcha holatlar bitta preview'da — bir qarashda taqqoslash oson.
 *
 * Android Studio'da: shu faylni ochib, o'ng tomondagi **Split / Design**
 * tugmasini bosing (build talab qilinmaydi, faqat Gradle sync).
 */
@Preview(name = "Info-tablo holatlari", showBackground = true, widthDp = 400)
@Composable
private fun QueueManagementItemStatesPreview() {
    GuardTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(Dimens.SpaceM)) {

                PreviewCase(
                    title = "1. Ruxsatnoma bor, hali chaqirilmagan → 3 ta yo'l tugmasi",
                    item = previewItem(infoLane = null)
                )

                PreviewCase(
                    title = "2. 1-yo'lga chaqirilgan → badge + O'TKAZILDI",
                    item = previewItem(queueNumber = 4, infoLane = 1)
                )

                PreviewCase(
                    title = "3. So'rov ketmoqda (tugmalar disable, rang o'zgarmaydi)",
                    item = previewItem(queueNumber = 5, infoLane = 2),
                    isActionInProgress = true
                )

                PreviewCase(
                    title = "4. O'tkazilgan (manual_passed = true) → tugma yo'q",
                    item = previewItem(queueNumber = 6, infoLane = 3, manualPassed = true)
                )

                PreviewCase(
                    title = "5. Ruxsatnoma yo'q (navbatda) → tugma yo'q",
                    item = previewItem(
                        queueNumber = 7,
                        hasPermit = false,
                        status = QueueItemStatus.WAITING
                    )
                )

                PreviewCase(
                    title = "6. Roli mos emas (masalan haydovchi) → tugma yo'q",
                    item = previewItem(queueNumber = 8),
                    canManageInfoLane = false
                )
            }
        }
    }
}

/** Waiting (oq) karta ustida tugmalar qanday ko'rinishini alohida tekshirish. */
@Preview(name = "Oq karta + yo'l tugmalari", showBackground = true, widthDp = 400)
@Composable
private fun QueueManagementItemWaitingPreview() {
    GuardTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(Dimens.SpaceM)) {
                QueueManagementItem(
                    item = previewItem(status = QueueItemStatus.WAITING),
                    canManageInfoLane = true,
                    isActionInProgress = false,
                    onClick = {},
                    onLaneCall = {},
                    onManualPass = {}
                )
            }
        }
    }
}
