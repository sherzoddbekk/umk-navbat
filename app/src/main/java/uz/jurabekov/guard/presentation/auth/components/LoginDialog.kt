package uz.jurabekov.guard.presentation.auth.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uz.jurabekov.guard.presentation.auth.LoginUiEffect
import uz.jurabekov.guard.presentation.auth.LoginUiEvent
import uz.jurabekov.guard.presentation.auth.LoginViewModel
import uz.jurabekov.guard.presentation.queue.components.PrimaryButton
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.R

/**
 * Login modal dialog.
 *
 * === Layout strategiyasi ===
 * QueueSubmitDialog bilan bir xil responsive pattern:
 *  - decorFitsSystemWindows = false → IME insets to'g'ri ishlaydi
 *  - .imePadding() outer Box'da → Card keyboard chiqqanda ko'tariladi
 *  - Card heightIn(max = 92% screen) → hech qachon fullscreen bo'lmaydi
 *  - Sticky bottom buttons → kichik ekranda ham har doim ko'rinadi
 *
 * === Flow ===
 *  1. Foydalanuvchi username + parol kiritadi.
 *  2. "Kirish" bosadi (yoki parol field'ida Done).
 *  3. ViewModel API'ga so'rov yuboradi.
 *  4. Success → LoginSuccess effect → onLoginSuccess() callback → navigation.
 *  5. Error → state.errorMessage'da ko'rsatiladi.
 *
 * @param onDismiss dialog yopilganda (back press, X tugma, click outside)
 * @param onLoginSuccess token saqlanib, asosiy oynaga o'tish kerakligi signali
 */
@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current

    val maxDialogHeight = (configuration.screenHeightDp * 0.92f).dp

    // Effect kollektori — LoginSuccess uchun navigate, Toast uchun show.
    // LaunchedEffect(Unit) — dialog ochilgan davrda yagona collector.
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginUiEffect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()

                LoginUiEffect.LoginSuccess -> {
                    keyboardController?.hide()
                    onLoginSuccess()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!state.isLoading) {
                viewModel.onEvent(LoginUiEvent.DialogDismissed)
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !state.isLoading,
            dismissOnClickOutside = !state.isLoading,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
        ) {
            Card(
                shape = RoundedCornerShape(Dimens.RadiusL),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = Dimens.ElevationDialog
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpaceM)
                    .heightIn(max = maxDialogHeight)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ============ HEADER (fixed) ============
                    LoginHeader(
                        isLoading = state.isLoading,
                        onDismiss = {
                            viewModel.onEvent(LoginUiEvent.DialogDismissed)
                            onDismiss()
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )

                    // ============ SCROLLABLE CONTENT ============
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(
                                horizontal = Dimens.SpaceL,
                                vertical = Dimens.SpaceM
                            )
                    ) {
                        // Username
                        FieldLabel("FOYDALANUVCHI")
                        Spacer(Modifier.height(Dimens.SpaceXS))
                        UsernameField(
                            value = state.username,
                            enabled = !state.isLoading,
                            onValueChange = { viewModel.onEvent(LoginUiEvent.UsernameChanged(it)) },
                            onImeNext = { focusManager.moveFocus(FocusDirection.Next) }
                        )

                        Spacer(Modifier.height(Dimens.SpaceM))

                        // Password
                        FieldLabel("PAROL")
                        Spacer(Modifier.height(Dimens.SpaceXS))
                        PasswordField(
                            value = state.password,
                            visible = state.passwordVisible,
                            enabled = !state.isLoading,
                            onValueChange = { viewModel.onEvent(LoginUiEvent.PasswordChanged(it)) },
                            onToggleVisibility = {
                                viewModel.onEvent(LoginUiEvent.TogglePasswordVisibility)
                            },
                            onImeDone = {
                                keyboardController?.hide()
                                viewModel.onEvent(LoginUiEvent.Submit)
                            }
                        )

                        // Error message (animatsiyali)
                        AnimatedVisibility(
                            visible = state.errorMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            val err = state.errorMessage
                            if (err != null) {
                                ErrorRow(err)
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )

                    // ============ FOOTER (sticky) ============
                    LoginFooter(
                        canSubmit = state.canSubmit,
                        isLoading = state.isLoading,
                        onLoginClick = {
                            keyboardController?.hide()
                            viewModel.onEvent(LoginUiEvent.Submit)
                        }
                    )
                }
            }
        }
    }
}

/* ============================================================
 * Header
 * ============================================================ */

@Composable
private fun LoginHeader(
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.SpaceL,
                end = Dimens.SpaceM,
                top = Dimens.SpaceM,
                bottom = Dimens.SpaceM
            )
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Kirish",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Tizimga kirish ma'lumotlarini kiriting",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onDismiss,
            enabled = !isLoading,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Yopish",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/* ============================================================
 * Input fields
 * ============================================================ */

@Composable
private fun UsernameField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onImeNext: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        placeholder = {
            Text(
                text = "admin",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            // Username: text, lowercase odat. Backend lowercase'ga
            // UseCase'da map qilamiz, lekin foydalanuvchi katta harfda
            // ham yozsa qabul qilamiz.
            capitalization = KeyboardCapitalization.None,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeNext() }
        ),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.InputHeight)
    )
}

@Composable
private fun PasswordField(
    value: String,
    visible: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onImeDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        placeholder = {
            Text(
                text = "••••••••",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onToggleVisibility,
                enabled = enabled
            ) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Outlined.VisibilityOff
                    } else {
                        Icons.Outlined.Visibility
                    },
                    contentDescription = if (visible) "Parolni yashirish" else "Parolni ko'rsatish",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(
            onDone = { onImeDone() }
        ),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.InputHeight)
    )
}

@Composable
private fun ErrorRow(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = Dimens.SpaceS, top = Dimens.SpaceS)
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.size(Dimens.SpaceXS))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/* ============================================================
 * Footer — buttons
 * ============================================================ */

@Composable
private fun LoginFooter(
    canSubmit: Boolean,
    isLoading: Boolean,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.SpaceL,
                vertical = Dimens.SpaceM
            )
    ) {
        // Primary login button
        PrimaryButton(
            text = "Kirish",
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = onLoginClick,
            enabled = canSubmit,
            loading = isLoading
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
