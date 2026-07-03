package uz.jurabekov.guard.presentation.queue.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.ui.theme.Dimens

@Composable
fun LoginIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
            modifier = Modifier.padding(
                horizontal = Dimens.SpaceM,
                vertical = Dimens.SpaceS
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Login,
                contentDescription = "Kirish",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(Dimens.SpaceXS))
            Text(
                text = "Kirish",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
