package uz.jurabekov.guard.presentation.queue.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.jurabekov.guard.ui.theme.Dimens

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceXXL)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    StateMessage(
        icon = Icons.Outlined.Inbox,
        title = "Bugun navbat yo'q",
        message = "Birinchi bo'lib navbat oling",
        modifier = modifier
    )
}

/**
 * Banner va waiting bo'sh, lekin tarix mavjud (entered/skipped'lar bor).
 * Foydalanuvchi tarixini toggle orqali ko'rishi mumkin.
 */
@Composable
fun QueueEmptyState(modifier: Modifier = Modifier) {
    StateMessage(
        icon = Icons.Outlined.Inbox,
        title = "Navbatda mashina yo'q",
        message = "Hozircha kutayotgan mashina yo'q",
        modifier = modifier
    )
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    StateMessage(
        icon = Icons.Outlined.CloudOff,
        title = "Yuklab bo'lmadi",
        message = message,
        actionText = "Qayta urinish",
        onAction = onRetry,
        modifier = modifier
    )
}

@Composable
private fun StateMessage(
    icon: ImageVector,
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = Dimens.SpaceXL, horizontal = Dimens.SpaceL)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(Dimens.RadiusL))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(Dimens.SpaceM))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.SpaceXS))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(Dimens.SpaceS))
            TextButton(onClick = onAction) {
                Text(actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}