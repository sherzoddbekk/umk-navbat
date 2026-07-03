package uz.jurabekov.guard.presentation.queue

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uz.jurabekov.guard.R
import uz.jurabekov.guard.presentation.components.PrimaryButton
import uz.jurabekov.guard.presentation.queue.components.AppInfoDialog
import uz.jurabekov.guard.presentation.queue.components.EmptyState
import uz.jurabekov.guard.presentation.queue.components.ErrorState
import uz.jurabekov.guard.presentation.queue.components.LoadingState
import uz.jurabekov.guard.presentation.queue.components.LoginIconButton
import uz.jurabekov.guard.presentation.queue.components.NowEnteringBanner
import uz.jurabekov.guard.presentation.queue.components.QueueEmptyState
import uz.jurabekov.guard.presentation.queue.components.QueueListItem
import uz.jurabekov.guard.presentation.queue.components.QueueSubmitDialog
import uz.jurabekov.guard.presentation.queue.components.QueueTypeTabs
import uz.jurabekov.guard.presentation.queue.components.SuccessDialog
import uz.jurabekov.guard.presentation.auth.components.LoginDialog
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.Success500
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onLoginSuccess: () -> Unit = {},                 // <-- YANGI
    viewModel: QueueViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // History filter chip holati. `rememberSaveable` — configuration
    // change'larda (rotation, theme switch, process death) ham saqlanadi.
    var enteredVisible by rememberSaveable { mutableStateOf(false) }

    var showInfoDialog by remember { mutableStateOf(false) }

    val stickyListState = rememberLazyListState()
    val unifiedListState = rememberLazyListState()
    val activeListState = if (enteredVisible) unifiedListState else stickyListState

    val isScrolled by remember {
        derivedStateOf {
            activeListState.firstVisibleItemIndex > 0 ||
                    activeListState.firstVisibleItemScrollOffset > 0
        }
    }

    val tab = state.activeTab

    // History filter'larni reset qilish — FAQAT tab almashtirilganda.
    //
    // OLD bug: `tab.currentlyEntering?.uuid` ham dependency edi, demak har
    // gal banner mashinasi o'zgarganda (PERMITTED event) chip yopilib qolardi.
    // Real-time tarix ko'rishda — bu UX buzilishi.
    //
    // FIX: faqat tab kontekstida reset (Open ↔ Tent). Banner o'zgarishi —
    // tabiiy realtime hodisa, foydalanuvchi tarix oynasini ko'rishda davom
    // etmoqda. Filter holatini saqlaymiz.
    LaunchedEffect(state.selectedTab) {
        enteredVisible = false
    }

    /*
     * App foreground'ga qaytganda silent refresh chaqiramiz.
     *
     * SABAB: Android'da app background'ga ketganda Doze Mode / App Standby
     * tufayli WS event'lar yo'qoladi yoki backend "idle" deb event yuborishni
     * to'xtatadi. WS state CONNECTED ko'rinadi, lekin haqiqatan stale.
     *
     * `LifecycleResumeEffect` har gal screen ON_RESUME bo'lganda chaqiriladi:
     *  - Initial composition (`init {}` bilan birga) — ViewModel throttle ushlaydi
     *  - Background → foreground (asosiy holat — bug fix)
     *  - Configuration change (rotation) — throttle ushlaydi
     *
     * ViewModel ichida 5 sek throttle, demak burst chaqiruvlar zarar qilmaydi.
     */
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(QueueUiEvent.AppResumed)
        onPauseOrDispose { /* no-op */ }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QueueUiEffect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (state.showDialog && state.successItem == null) {
        QueueSubmitDialog(
            state = state,
            onTypeChange = { viewModel.onEvent(QueueUiEvent.TypeChanged(it)) },
            onPlateChange = { viewModel.onEvent(QueueUiEvent.PlateChanged(it)) },
            onNameChange = { viewModel.onEvent(QueueUiEvent.NameChanged(it)) },
            onPassportSeriesChange = { viewModel.onEvent(QueueUiEvent.PassportSeriesChanged(it)) },
            onPassportNumberChange = { viewModel.onEvent(QueueUiEvent.PassportNumberChanged(it)) },
            onRememberMeChange = { viewModel.onEvent(QueueUiEvent.RememberMeChanged(it)) },
            onSubmit = { viewModel.onEvent(QueueUiEvent.Submit) },
            onDismiss = { viewModel.onEvent(QueueUiEvent.DismissDialog) }
        )
    }

    state.successItem?.let { item ->
        SuccessDialog(
            item = item,
            onDismiss = { viewModel.onEvent(QueueUiEvent.DismissSuccess) }
        )
    }

    if (showInfoDialog) {
        AppInfoDialog(onDismiss = { showInfoDialog = false })
    }

    if (state.showLoginDialog) {
        LoginDialog(
            onDismiss = { viewModel.onEvent(QueueUiEvent.DismissLoginDialog) },
            onLoginSuccess = {
                viewModel.onEvent(QueueUiEvent.DismissLoginDialog)
                onLoginSuccess()
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(modifier = Modifier.fillMaxSize()) {

                TopBar(
                    onLoginClick = { viewModel.onEvent(QueueUiEvent.LoginClicked) },
                    onInfoClick = { showInfoDialog = true },
                    isScrolled = isScrolled
                )

                // Tab bar — TopBar pastida, har doim ko'rinadi
                QueueTypeTabs(
                    selected = state.selectedTab,
                    onSelect = { viewModel.onEvent(QueueUiEvent.TabSelected(it)) }
                )

                Header(
                    queueDate = state.queueDate,
                    enteredCount = tab.enteredOnlyCount,
                    enteredVisible = enteredVisible,
                    onToggleEntered = { enteredVisible = !enteredVisible }
                )

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.onEvent(QueueUiEvent.Refresh) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        state.isLoading && tab.isFullyEmpty -> LoadingState()

                        state.listError != null && tab.isFullyEmpty -> ErrorState(
                            message = state.listError ?: "Xatolik",
                            onRetry = { viewModel.onEvent(QueueUiEvent.Refresh) }
                        )

                        tab.isFullyEmpty -> EmptyState()

                        enteredVisible -> UnifiedScrollList(
                            tab = tab,
                            listState = unifiedListState,
                            showEntered = true
                        )

                        // Banner va waiting bo'sh — lekin tarix bor.
                        tab.isQueueEmpty -> QueueEmptyState()

                        else -> StickyBannerList(
                            tab = tab,
                            listState = stickyListState
                        )
                    }
                }
            }

            BottomCta(
                onClick = { viewModel.onEvent(QueueUiEvent.OpenDialog) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun TopBar(
    onLoginClick: () -> Unit,
    onInfoClick: () -> Unit,  // ⬅️ YANGI parametr
    isScrolled: Boolean,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(
        targetValue = if (isScrolled) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "topbar-elevation"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0.4f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "topbar-border"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = elevation,
        tonalElevation = if (isScrolled) 2.dp else 0.dp
    ) {
        Box(modifier = Modifier.statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS)
            ) {
                // Logo + matn (chap tomon)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Company logo",
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "UMK-Navbat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.2.sp
                    )
                }

                // Info + Login (o'ng tomon)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
                ) {
                    InfoIconButton(onClick = onInfoClick)
                    LoginIconButton(onClick = onLoginClick)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha)
                    )
            )
        }
    }
}

/**
 * Info icon button - dumaloq, LoginIconButton bilan uyg'un.
 * Login bilan farqi: matnsiz, faqat icon (kichikroq).
 */
@Composable
private fun InfoIconButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Ilova haqida",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun Header(
    queueDate: String?,
    enteredCount: Int,
    enteredVisible: Boolean,
    onToggleEntered: () -> Unit
) {
    val displayDate = remember(queueDate) {
        formatQueueDate(queueDate)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceM)
    ) {
        Text(
            text = displayDate,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.weight(1f))

        // ✓ Kirgan mashinalar (Success rangi)
        if (enteredCount > 0) {
            HistoryFilterChip(
                icon = Icons.Default.Check,
                contentDescription = "Kirgan mashinalar",
                count = enteredCount,
                active = enteredVisible,
                activeColor = Success500,
                onClick = onToggleEntered
            )
        }
    }
}

/**
 * Tarix filtri uchun chip-stildagi tugma.
 *
 *  Inactive: surfaceVariant fon, neutral matn — diqqat tortmaydi.
 *  Active:   `activeColor` fon (Success/Error), oq matn — selektsiya aniq ko'rinadi.
 *
 * Bir komponent ikki kontekstda — ✓ va ✗ — qayta ishlatiladi: rang
 * va ikon parametr orqali.
 */
@Composable
private fun HistoryFilterChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    count: Int,
    active: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val (bg, fg) = if (active) {
        activeColor to androidx.compose.ui.graphics.Color.White
    } else {
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

@Composable
private fun StickyBannerList(
    tab: TabState,
    listState: LazyListState
) {
    Column(modifier = Modifier.fillMaxSize()) {

        tab.currentlyEntering?.let { item ->
            Box(modifier = Modifier.padding(horizontal = Dimens.SpaceM)) {
                NowEnteringBanner(item = item)
            }
            Spacer(Modifier.height(Dimens.SpaceS))
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = Dimens.SpaceM,
                end = Dimens.SpaceM,
                top = 0.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(
                items = tab.waitingItems,
                key = { "waiting-${it.uuid}" }
            ) { item ->
                QueueListItem(item = item)
            }
        }
    }
}

@Composable
private fun UnifiedScrollList(
    tab: TabState,
    listState: LazyListState,
    showEntered: Boolean
) {
    val historyToShow = remember(tab.enteredItems, showEntered) {
        if (showEntered) {
            tab.enteredItems.filter {
                it.status == uz.jurabekov.guard.domain.model.QueueItemStatus.ENTERED
            }
        } else {
            emptyList()
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = Dimens.SpaceM,
            end = Dimens.SpaceM,
            top = 0.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = historyToShow,
            key = { "history-${it.uuid}" }
        ) { item ->
            QueueListItem(item = item)
        }

        tab.currentlyEntering?.let { item ->
            item(key = "banner-${item.uuid}") {
                Spacer(Modifier.height(2.dp))
                NowEnteringBanner(item = item)
                Spacer(Modifier.height(2.dp))
            }
        }

        items(
            items = tab.waitingItems,
            key = { "waiting-${it.uuid}" }
        ) { item ->
            QueueListItem(item = item)
        }
    }
}

@Composable
private fun BottomCta(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = Dimens.SpaceM,
                vertical = Dimens.SpaceM
            )
    ) {
        PrimaryButton(
            text = "Navbat olish",
            leadingIcon = Icons.Default.Add,
            onClick = onClick
        )
    }
}

/* ============================================================
 * Date formatting helpers
 * ============================================================ */

/**
 * Backend `queue_date` (`yyyy-MM-dd`) ni Header uchun (`dd.MM.yyyy`) ga aylantiradi.
 *
 * Fallback'lar (defensive — Header hech qachon bo'sh bo'lmasligi kerak):
 *  - null/blank   → qurilma sanasi
 *  - parse fail   → input string'ni o'zi (xato format'ni yashirmaymiz, ammo crash ham yo'q)
 *
 * `Locale.US` — backend ISO format ham, output ham raqamli (lokalga bog'liq emas).
 */
private fun formatQueueDate(queueDate: String?): String {
    if (queueDate.isNullOrBlank()) {
        return SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())
    }
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.US)
        formatter.format(parser.parse(queueDate)!!)
    }.getOrDefault(queueDate)
}