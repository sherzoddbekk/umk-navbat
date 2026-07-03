package uz.jurabekov.guard.presentation.queue.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import uz.jurabekov.guard.ui.theme.Dimens

/**
 * Industrial muhitda asosiy CTA tugmasi.
 *  - 60dp balandlik (qo'lqop bilan ham bosish oson)
 *  - loading state'da progress indicator
 *  - leading va trailing icon qo'llab-quvvatlanadi
 *  - smooth disabled animation
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    val containerColor by animateColorAsState(
        targetValue = if (enabled && !loading) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        },
        animationSpec = tween(200),
        label = "btnContainer"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = containerColor,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ButtonHeight)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                modifier = Modifier.wrapContentSize()
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
                if (trailingIcon != null) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
