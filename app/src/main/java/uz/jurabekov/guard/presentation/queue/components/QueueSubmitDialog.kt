package uz.jurabekov.guard.presentation.queue.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uz.jurabekov.guard.core.util.Constants
import uz.jurabekov.guard.domain.model.VehicleType
import uz.jurabekov.guard.presentation.components.AppTextField
import uz.jurabekov.guard.presentation.components.PrimaryButton
import uz.jurabekov.guard.presentation.queue.QueueUiState
import uz.jurabekov.guard.ui.theme.Dimens

/**
 * Modal dialog: navbat olish.
 *
 * === Responsive layout strategiyasi ===
 *
 * Dialog uchta layer'dan iborat:
 *   ┌─────────────────────────────────────┐
 *   │ Box (fullscreen) — IME inset host    │
 *   │  ┌──────────────────────────────┐   │
 *   │  │ Card (max 92% screen height) │   │ ← centered, .imePadding()
 *   │  │ ┌──────────────────────────┐ │   │
 *   │  │ │ Header (fixed)           │ │   │
 *   │  │ ├──────────────────────────┤ │   │
 *   │  │ │ Form fields              │ │   │ ← weight(1f, fill=false)
 *   │  │ │ (verticalScroll)         │ │   │   + verticalScroll
 *   │  │ ├──────────────────────────┤ │   │
 *   │  │ │ Submit button (fixed)    │ │   │
 *   │  │ └──────────────────────────┘ │   │
 *   │  └──────────────────────────────┘   │
 *   └─────────────────────────────────────┘
 *
 * Bu pattern quyidagi muammolarni hal qiladi:
 *
 *  1) **Submit button hamisha ko'rinadi** (sticky bottom).
 *     Kichik ekran + ochiq keyboard holatida ham foydalanuvchi yuborish
 *     tugmasiga yetib boradi.
 *
 *  2) **Content overflow → vertical scroll**.
 *     Forma uzunligi card height'idan oshsa, faqat o'rta soha aylantiriladi —
 *     header va submit joyida qoladi.
 *
 *  3) **Keyboard avoidance — `decorFitsSystemWindows = false` + `imePadding()`**.
 *     `Dialog` default holatda IME insetlarni tarqatmaydi. Bu flag yoqilsa,
 *     keyboard chiqqanda `imePadding()` Card'ni keyboard ustiga ko'taradi.
 *
 *  4) **System bars padding** — edge-to-edge rejimda status/nav bars ostida
 *     content qolib ketmaydi.
 *
 * === Form tartibi ===
 *  1. Mashina turi tanlash — segmented control (Usti ochiq / Usti yopiq)
 *  2. Mashina raqami
 *  3. Haydovchi F.I.Sh
 *  4. Pasport ma'lumotlari (IXTIYORIY)
 *  5. Submit
 */
@Composable
fun QueueSubmitDialog(
    state: QueueUiState,
    onTypeChange: (VehicleType) -> Unit,
    onPlateChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onPassportSeriesChange: (String) -> Unit,
    onPassportNumberChange: (String) -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current

    // Card hech qachon ekranni butunlay egallamaydi — 92% upper bound.
    // Bu past resolution / katta font / katta display scale qurilmalarda
    // ham minimal "breathing room" qoldiradi va keyboard chiqqanda Card
    // pastdan qisqarish o'rniga butun ekrandan "yopiq" ko'rinmasligini
    // ta'minlaydi.
    val maxDialogHeight = (configuration.screenHeightDp * 0.92f).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !state.isSubmitting,
            dismissOnClickOutside = !state.isSubmitting,
            usePlatformDefaultWidth = false,
            // CRITICAL: bu flagsiz `imePadding()` Dialog ichida ishlamaydi.
            // Default `true` da Compose dialog window'ga IME insetlarni
            // dispatch qilmaydi.
            decorFitsSystemWindows = false
        )
    ) {
        // Outer Box — fullscreen, system bars va IME insetlarni hisobga oladi.
        // Box ichidagi Card centered bo'ladi. Keyboard chiqqanda available
        // vertical space qisqaradi → Card avtomatik shrink + scroll engages.
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
                    .padding(horizontal = Dimens.SpaceS)
                    .heightIn(max = maxDialogHeight)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ============ FIXED HEADER ============
                    DialogHeader(
                        isSubmitting = state.isSubmitting,
                        onDismiss = onDismiss
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )

                    // ============ SCROLLABLE CONTENT ============
                    // `weight(1f, fill = false)` — agar content card'dan kichik
                    // bo'lsa, qo'shimcha bo'sh joy ochmaydi (fill=false). Agar
                    // content uzun bo'lsa, faqat available space oladi va
                    // vertical scroll engages.
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(
                                horizontal = Dimens.SpaceM,
                                vertical = Dimens.SpaceS
                            )
                    ) {
                        // Vehicle type — SINGLE ROW: label chap, radio o'ng.
                        // RadioGroup `weight(1f)` bilan available joyni egallaydi —
                        // label esa intrinsic kenglik (qisqa "MASHINA TURI" matni).
                        // Bu vertikal joyni tejaydi (~32dp).
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(Modifier.size(Dimens.SpaceS))
                            VehicleTypeRadioGroup(
                                selected = state.selectedType,
                                enabled = !state.isSubmitting,
                                onSelect = onTypeChange,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(Dimens.SpaceS))

                        // Plate
                        FieldLabel("MASHINA RAQAMI")
                        Spacer(Modifier.height(Dimens.SpaceXXS))
                        AppTextField(
                            value = state.plate,
                            onValueChange = onPlateChange,
                            placeholder = "01 W 571 QA",
                            leadingIcon = Icons.Outlined.DirectionsCar,
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next,
                            error = state.plateError,
                            enabled = !state.isSubmitting
                        )

                        Spacer(Modifier.height(Dimens.SpaceS))

                        // Name
                        FieldLabel("HAYDOVCHI F.I.SH")
                        Spacer(Modifier.height(Dimens.SpaceXXS))
                        AppTextField(
                            value = state.fullName,
                            onValueChange = onNameChange,
                            placeholder = "Aliyev Vali Akmalovich",
                            leadingIcon = Icons.Outlined.Person,
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                            error = state.nameError,
                            enabled = !state.isSubmitting
                        )

                        Spacer(Modifier.height(Dimens.SpaceS))

                        // Passport — IXTIYORIY
                        FieldLabel("PASPORT MA'LUMOTLARI (IXTIYORIY)")
                        Spacer(Modifier.height(Dimens.SpaceXXS))
                        PassportRow(
                            series = state.passportSeries,
                            number = state.passportNumber,
                            onSeriesChange = onPassportSeriesChange,
                            onNumberChange = onPassportNumberChange,
                            enabled = !state.isSubmitting,
                            onDone = {
                                keyboardController?.hide()
                                if (state.isFormValid && !state.isSubmitting) onSubmit()
                            }
                        )

                        AnimatedVisibility(
                            visible = state.passportError != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            val err = state.passportError
                            if (err != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(
                                        start = Dimens.SpaceS,
                                        top = Dimens.SpaceXS
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.size(Dimens.SpaceXS))
                                    Text(
                                        text = err,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )

                    // ============ FIXED FOOTER (sticky submit) ============
                    // "Eslab qolish" checkbox + Yuborish button vertikal stacked.
                    // Checkbox row tight padding bilan — submit tugmasiga
                    // imkon qadar yaqin tursin.
                    Column(
                        modifier = Modifier.padding(
                            horizontal = Dimens.SpaceM,
                            vertical = Dimens.SpaceS
                        )
                    ) {
                        RememberMeRow(
                            checked = state.rememberMe,
                            enabled = !state.isSubmitting,
                            onCheckedChange = onRememberMeChange
                        )

                        Spacer(Modifier.height(Dimens.SpaceXXXS))

                        PrimaryButton(
                            text = "Yuborish",
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = {
                                keyboardController?.hide()
                                onSubmit()
                            },
                            enabled = state.isFormValid,
                            loading = state.isSubmitting
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(
    isSubmitting: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.SpaceM,
                end = Dimens.SpaceS,
                top = Dimens.SpaceS,
                bottom = Dimens.SpaceS
            )
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Navbat olish",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Mashina ma'lumotlarini kiriting",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onDismiss,
            enabled = !isSubmitting,
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

/**
 * Pasport seriyasi + raqami yagona qatorda.
 *
 * Layout:
 *  ┌──────────┐  ┌──────────────────────┐
 *  │ AA  ▼   │  │ 1234567              │
 *  └──────────┘  └──────────────────────┘
 *   weight=1       weight=2 (raqam kengroq)
 *
 * Seriya: ExposedDropdownMenuBox. IME action = Next → fokus raqamga.
 * Raqam: numeric keyboard, IME action = Done → submit form (agar valid).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassportRow(
    series: String,
    number: String,
    onSeriesChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    enabled: Boolean,
    onDone: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Series — Exposed dropdown
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { if (enabled) dropdownExpanded = !dropdownExpanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = series,
                onValueChange = { newValue ->
                    onSeriesChange(newValue)
                    if (dropdownExpanded) dropdownExpanded = false
                },
                placeholder = {
                    Text(
                        text = "AA",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Tanlash",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = enabled)
                    .fillMaxWidth()
                    .height(Dimens.InputHeight)
            )

            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                Constants.PASSPORT_SERIES_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = {
                            onSeriesChange(option)
                            dropdownExpanded = false
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = Dimens.SpaceM,
                            vertical = Dimens.SpaceS
                        )
                    )
                }
            }
        }

        // Number — numeric input, Done → submit
        AppTextField(
            value = number,
            onValueChange = onNumberChange,
            placeholder = "1234567",
            leadingIcon = Icons.Outlined.Numbers,
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
            onImeAction = onDone,
            enabled = enabled,
            modifier = Modifier.weight(2f)
        )
    }
}

/**
 * Mashina turi tanlash uchun ikki kartochka.
 *
 * `modifier` parametri orqali tashqi container kenglikni kontrol qiladi
 * (single-row layoutda `weight(1f)` bilan ishlatiladi).
 */
@Composable
private fun VehicleTypeRadioGroup(
    selected: VehicleType,
    enabled: Boolean,
    onSelect: (VehicleType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
        modifier = modifier
    ) {
        VehicleTypeOption(
            modifier = Modifier.weight(1f),
            label = "Usti ochiq avto",
            selected = selected == VehicleType.OPEN,
            enabled = enabled,
            onClick = { onSelect(VehicleType.OPEN) }
        )
        VehicleTypeOption(
            modifier = Modifier.weight(1f),
            label = "Usti yopiq avto",
            selected = selected == VehicleType.TENT,
            enabled = enabled,
            onClick = { onSelect(VehicleType.TENT) }
        )
    }
}

@Composable
private fun VehicleTypeOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.3f,
        animationSpec = tween(200),
        label = "option-border"
    )

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha)
    }

    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.RadiusM),
        color = containerColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = borderColor
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.SpaceS, horizontal = Dimens.SpaceXS)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
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

/**
 * "Eslab qolish" checkbox qatori — Submit tugma ustida joylashadi.
 *
 * **UX qoidasi:**
 *  - Butun qator clickable (faqat checkbox emas) — touch target kengayadi,
 *    qo'lqop bilan ham bosish oson.
 *  - `indication = null` — Material ripple Checkbox'da allaqachon bor.
 *    Qator + Checkbox ikki ripple bir vaqtda — visual noise.
 *  - Disabled holatda (submit jarayonida) toggle ishlamaydi — race oldini olish.
 *
 * **Performance:** `MutableInteractionSource` har recomposition'da reuse —
 * `remember{}` bilan instance stabil. Allocation overhead yo'q.
 */
@Composable
private fun RememberMeRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = Dimens.SpaceXXXS)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Text(
            text = "Eslab qolish",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
        )
    }
}