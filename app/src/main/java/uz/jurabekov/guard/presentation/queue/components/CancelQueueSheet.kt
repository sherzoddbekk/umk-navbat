package uz.jurabekov.guard.presentation.queue.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.data.preferences.dto.OwnedQueue
import uz.jurabekov.guard.ui.theme.Accent100
import uz.jurabekov.guard.ui.theme.Accent500
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.Error500

/**
 * "Navbatni bekor qilish" — modal bottom sheet.
 *
 * Local'da saqlangan (bugungi) navbatlar ro'yxati. Foydalanuvchi bittasini
 * radio orqali tanlaydi va bekor qiladi. Faol (hozir kirayotgan) navbat
 * "Faol" pill bilan, qolganlari "Kutish" bilan belgilanadi.
 *
 * @param activeUuids Hozir "navbatdagi" (banner) navbatlar uuid'lari.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelQueueSheet(
    queues: List<OwnedQueue>,
    selectedUuid: String?,
    activeUuids: Set<String>,
    isCancelling: Boolean,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isCancelling) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.SpaceL)
        ) {
            Text(
                text = "Navbatni bekor qilish",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Dimens.SpaceXS))
            Text(
                text = "Bugun olgan navbatlaringiz. Bekor qilmoqchi bo'lgan bittasini tanlang.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Dimens.SpaceM))

            queues.forEach { queue ->
                QueueRow(
                    queue = queue,
                    selected = queue.uuid == selectedUuid,
                    isActive = queue.uuid in activeUuids,
                    enabled = !isCancelling,
                    onClick = { onSelect(queue.uuid) }
                )
                Spacer(Modifier.height(Dimens.SpaceS))
            }

            Spacer(Modifier.height(Dimens.SpaceS))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isCancelling,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.ButtonHeight)
                ) {
                    Text("Yopish", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = onConfirm,
                    enabled = !isCancelling && selectedUuid != null,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Error500,
                        contentColor = Color.White,
                        disabledContainerColor = Error500.copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(Dimens.ButtonHeight)
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Block,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(Dimens.SpaceS))
                        Text(
                            text = "Navbatni bekor qilish",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.SpaceM))
        }
    }
}

@Composable
private fun QueueRow(
    queue: OwnedQueue,
    selected: Boolean,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusM))
            .background(containerColor)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Dimens.SpaceS, vertical = Dimens.SpaceSM)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )

        Spacer(Modifier.width(Dimens.SpaceS))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = queue.plate,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = queue.fullName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        StatusChip(isActive = isActive)
    }
}

@Composable
private fun StatusChip(isActive: Boolean) {
    val (bg, fg, label) = if (isActive) {
        Triple(Accent100, Accent500, "Faol")
    } else {
        Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Kutish"
        )
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}
