package uz.jurabekov.guard.presentation.queue_management.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.QueueItemStatus
import uz.jurabekov.guard.presentation.queue.components.QueueListItem
import uz.jurabekov.guard.presentation.queue_management.QueueSection
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.GuardTheme
import uz.jurabekov.guard.ui.theme.Neutral100
import uz.jurabekov.guard.ui.theme.Neutral300
import uz.jurabekov.guard.ui.theme.Primary100

/**
 * "Navbat boshqaruvi" bo'lim tablari va bo'limga xos item ko'rinishlari —
 * Android Studio Preview'da (server/emulatorsiz) tekshirish uchun.
 */

private fun sampleItem(
    queueNumber: Int,
    fullName: String,
    plate: String,
    hasPermit: Boolean,
    manualPassed: Boolean = false,
    status: QueueItemStatus = if (hasPermit) QueueItemStatus.ENTERED else QueueItemStatus.WAITING
) = QueueItem(
    id = queueNumber.toLong(),
    uuid = "u-$queueNumber",
    queueNumber = queueNumber,
    plate = plate,
    fullName = fullName,
    hasPermit = hasPermit,
    status = status,
    createdAtEpochMs = 0L,
    arrivedAtHHmm = "10:33",
    manualPassed = manualPassed
)

/** Bo'lim tablari — 3 ta (darvoza roli) va 2 ta (oddiy rol) variant. */
@Preview(name = "Bo'lim tablari", showBackground = true, widthDp = 400)
@Composable
private fun QueueSectionTabsPreview() {
    GuardTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(vertical = Dimens.SpaceM),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
            ) {
                var selectedAdmin by remember { mutableStateOf(QueueSection.GATE_QUEUE) }
                Label("admin / nazoratchi — 3 ta bo'lim")
                QueueSectionTabs(
                    sections = QueueSection.availableFor("admin"),
                    selected = selectedAdmin,
                    onSelect = { selectedAdmin = it }
                )

                var selectedOperator by remember { mutableStateOf(QueueSection.PERMIT_QUEUE) }
                Label("operator / mehmon — Darvoza navbati yo'q")
                QueueSectionTabs(
                    sections = QueueSection.availableFor("operator"),
                    selected = selectedOperator,
                    onSelect = { selectedOperator = it }
                )

                var selectedGate by remember { mutableStateOf(QueueSection.GATE_QUEUE) }
                Label("darvoza_tekshiruv — Ruxsatnoma navbati yo'q")
                QueueSectionTabs(
                    sections = QueueSection.availableFor("darvoza_tekshiruv"),
                    selected = selectedGate,
                    onSelect = { selectedGate = it }
                )
            }
        }
    }
}

/** Har bir bo'limdagi item ko'rinishi. */
@Preview(name = "Bo'lim item'lari", showBackground = true, widthDp = 400)
@Composable
private fun SectionItemsPreview() {
    GuardTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(Dimens.SpaceM),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
            ) {
                Label("Ruxsatnoma navbati (ruxsatnomasiz)")
                QueueListItem(
                    item = sampleItem(7, "Ismoilov Aziz", "01 A 123 AA", hasPermit = false),
                    compact = true,
                    onClick = {}
                )

                Label("Darvoza navbati (chaqiruv tugmalari)")
                QueueManagementItem(
                    item = sampleItem(1, "Ormonaliyev Sherzodbek", "40 N 782 SB", hasPermit = true),
                    canManageInfoLane = true,
                    isActionInProgress = false,
                    containerColor = Neutral100,
                    borderColor = Neutral300,
                    onClick = {}, onLaneCall = {}, onManualPass = {}
                )

                Label("Berilgan ruxsatnomalar — O'tkazilgan")
                val passedItem = sampleItem(13, "Kojambergenov Ruslan", "95246 HBA", hasPermit = true, manualPassed = true)
                QueueListItem(
                    item = passedItem,
                    compact = true,
                    onClick = {},
                    actions = { ItemStatusBar(passedItem) }
                )

                Label("Berilgan ruxsatnomalar — 2-YO'LGA chaqirilgan")
                val calledItem = sampleItem(15, "Aliyev Botir", "70 A 900 CA", hasPermit = true).copy(infoLane = 2)
                QueueListItem(
                    item = calledItem,
                    compact = true,
                    containerColor = Primary100,
                    onClick = {},
                    actions = { ItemStatusBar(calledItem) }
                )

                Label("Berilgan ruxsatnomalar — Darvozani kutmoqda")
                val waitingItem = sampleItem(12, "Toshpo'latov Olim", "30 D 550 CA", hasPermit = true)
                QueueListItem(
                    item = waitingItem,
                    compact = true,
                    containerColor = Primary100,
                    onClick = {},
                    actions = { ItemStatusBar(waitingItem) }
                )
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Dimens.SpaceM)
    )
}
