package uz.jurabekov.guard.presentation.queue_management.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.usecase.CallInfoLaneUseCase
import uz.jurabekov.guard.presentation.queue.components.QueueListItem
import uz.jurabekov.guard.ui.theme.Accent100
import uz.jurabekov.guard.ui.theme.Accent600
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.Neutral0
import uz.jurabekov.guard.ui.theme.Primary100
import uz.jurabekov.guard.ui.theme.Primary700
import uz.jurabekov.guard.ui.theme.Success500

/**
 * "Navbat boshqaruvi" ekranidagi navbat elementi — `QueueListItem` kartasi
 * ichiga info-tablo action'lari (`actions` slot) joylashtirilgan.
 *
 * **Tugmalar mantiqi** (`item` maydonlariga qarab):
 *
 * | Holat                                   | Ko'rinadi                    |
 * |-----------------------------------------|------------------------------|
 * | `manualPassed = true`                   | — (hech qanday tugma yo'q)   |
 * | `infoLane = null`                        | 1 YO'L / 2 YO'L / 3 YO'L     |
 * | `infoLane = 1..3`                        | "N YO'L" badge + O'TKAZILDI  |
 *
 * Qo'shimcha shartlar: tugmalar faqat ruxsatnoma berilgan (`hasPermit`)
 * mashinalarda va faqat `admin` / `darvoza_tekshiruv` rollarida chiqadi.
 *
 * @param isActionInProgress shu item uchun so'rov ketmoqda — tugmalar disable.
 */
@Composable
fun QueueManagementItem(
    item: QueueItem,
    canManageInfoLane: Boolean,
    isActionInProgress: Boolean,
    onClick: () -> Unit,
    onLaneCall: (Int) -> Unit,
    onManualPass: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showActions = canManageInfoLane && item.hasPermit && !item.manualPassed

    QueueListItem(
        item = item,
        modifier = modifier,
        onClick = onClick,
        actions = if (!showActions) null else {
            {
                InfoLaneActions(
                    assignedLane = item.infoLane,
                    enabled = !isActionInProgress,
                    onLaneCall = onLaneCall,
                    onManualPass = onManualPass
                )
            }
        }
    )
}

/**
 * Action qatori. Balandlik ikkala holatda bir xil — chaqiruvdan keyin
 * ro'yxat "sakramaydi".
 */
@Composable
private fun InfoLaneActions(
    assignedLane: Int?,
    enabled: Boolean,
    onLaneCall: (Int) -> Unit,
    onManualPass: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ACTION_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
    ) {
        if (assignedLane == null) {
            // Uchala yo'l bir xil vaznda — birortasi ustunlik qilmaydi.
            CallInfoLaneUseCase.LANES.forEach { lane ->
                ActionButton(
                    text = "$lane YO'L",
                    container = Primary100,
                    content = Primary700,
                    enabled = enabled,
                    onClick = { onLaneCall(lane) }
                )
            }
        } else {
            // Chaqirilgan yo'l — bosilmaydigan holat indikatori (disabled tugma
            // rangi o'zgarmaydi: `disabledContainerColor` explicit berilgan).
            ActionButton(
                text = "$assignedLane YO'L",
                container = Accent100,
                content = Accent600,
                enabled = false,
                onClick = {}
            )
            ActionButton(
                text = "O'TKAZILDI",
                container = Success500,
                content = Neutral0,
                enabled = enabled,
                withCheckIcon = true,
                weight = 2f,
                onClick = onManualPass
            )
        }
    }
}

@Composable
private fun RowScope.ActionButton(
    text: String,
    container: Color,
    content: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    withCheckIcon: Boolean = false,
    weight: Float = 1f
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.RadiusS),
        elevation = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            // Disabled holatda ham rang o'zgarmaydi — "chaqirilgan yo'l" badge
            // va so'rov ketayotgan tugma o'qilishli qoladi.
            disabledContainerColor = container,
            disabledContentColor = content
        ),
        contentPadding = ACTION_PADDING,
        modifier = Modifier
            .weight(weight)
            .height(ACTION_HEIGHT)
    ) {
        if (withCheckIcon) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            fontSize = ACTION_FONT_SIZE,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = if (withCheckIcon) Dimens.SpaceXS else 0.dp)
        )
    }
}

private val ACTION_HEIGHT = 40.dp
private val ACTION_FONT_SIZE = 13.sp

/** Standart Button padding (24dp) qisqa tugmalarda matnni kesadi. */
private val ACTION_PADDING = PaddingValues(horizontal = Dimens.SpaceXS)
