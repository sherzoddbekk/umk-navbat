package uz.jurabekov.guard.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import uz.jurabekov.guard.ui.theme.Dimens

/**
 * Reusable text input.
 *
 * Label maydonidan tashqarida (yuqorida) joylashtiriladi — bu yerda
 * `label = ""` bo'lsa, default Material label ko'rsatilmaydi.
 *
 * === IME ACTION CONTRACT ===
 *  - `Next`               → fokus keyingi inputga o'tadi (FocusManager).
 *  - `Done|Go|Search|Send` → klaviatura yopiladi va `onImeAction()` chaqiriladi.
 *  - `Previous`           → fokus oldingi inputga qaytadi.
 *
 * Eski `onAny = { onImeAction?.invoke() }` pattern default focus
 * navigation'ni override qilib qo'yardi — natijada `Next` umuman ishlamadi.
 * Yangi versiya har bir action uchun aniq handler taqdim etadi.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "",
    placeholder: String? = null,
    error: String? = null,
    leadingIcon: ImageVector? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = if (label.isNotBlank()) {
                { Text(label, style = MaterialTheme.typography.bodyMedium) }
            } else null,
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (error != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            isError = error != null,
            singleLine = true,
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) },
                onPrevious = { focusManager.moveFocus(FocusDirection.Previous) },
                onDone = {
                    keyboardController?.hide()
                    onImeAction?.invoke()
                },
                onGo = {
                    keyboardController?.hide()
                    onImeAction?.invoke()
                },
                onSearch = {
                    keyboardController?.hide()
                    onImeAction?.invoke()
                },
                onSend = {
                    keyboardController?.hide()
                    onImeAction?.invoke()
                }
            ),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                cursorColor = MaterialTheme.colorScheme.primary,
                errorBorderColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.InputHeight)
        )

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (error != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = Dimens.SpaceS, top = Dimens.SpaceXS)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(Dimens.SpaceXS))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}