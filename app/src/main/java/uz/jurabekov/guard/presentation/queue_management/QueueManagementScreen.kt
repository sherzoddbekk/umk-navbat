package uz.jurabekov.guard.presentation.queue_management

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uz.jurabekov.guard.presentation.queue.TabState
import uz.jurabekov.guard.presentation.queue.components.EmptyState
import uz.jurabekov.guard.presentation.queue.components.ErrorState
import uz.jurabekov.guard.presentation.queue.components.LoadingState
import uz.jurabekov.guard.presentation.queue.components.NowEnteringBanner
import uz.jurabekov.guard.presentation.queue.components.QueueEmptyState
import uz.jurabekov.guard.presentation.queue.components.QueueListItem
import uz.jurabekov.guard.presentation.queue.components.QueueTypeTabs
import uz.jurabekov.guard.presentation.queue_management.components.PermitDialog
import uz.jurabekov.guard.presentation.queue_management.components.QueueDateBar
import uz.jurabekov.guard.presentation.scale.components.ScaleDatePickerDialog
import uz.jurabekov.guard.ui.theme.Dimens

/**
 * Navbat boshqaruvi ekrani — Main screen ichidagi nested route.
 *
 * **Layout (yuqoridan-pastga):**
 *  1. `QueueDateBar`   — sana picker + Jami counter
 *  2. `QueueTypeTabs`  — OPEN ↔ TENT
 *  3. `PullToRefreshBox` + Unified scroll list:
 *      - entered/skipped tarix (yuqorida, sorted by queueNumber)
 *      - currentlyEntering banner (highlight)
 *      - waiting items (pastda)
 *      Hammasi clickable — itemga bosilsa permit dialog ochiladi.
 *
 * **`QueueScreen`'dan farqi:**
 *  - Top bar va "Navbat olish" tugmasi yo'q (Navbat boshqaruvchi UI)
 *  - History (entered/skipped) toggle yo'q — DOIM ko'rinadi (talab)
 *  - Item bosilganda dialog (QueueScreen'da passive list)
 *
 * **Dialog'lar:**
 *  - `ScaleDatePickerDialog` — qayta ishlatamiz (DRY, identical API).
 *  - `PermitDialog` — yangi, QR + tafsilotlar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueManagementScreen(
    viewModel: QueueManagementViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val listState = rememberLazyListState()

    // App foreground qaytishida silent refresh (QueueScreen pattern).
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(QueueManagementUiEvent.AppResumed)
        onPauseOrDispose { /* no-op */ }
    }

    // One-shot effect'lar (toast)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QueueManagementUiEffect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Tab almashganda yuqoriga scroll
    LaunchedEffect(state.selectedTab) {
        listState.animateScrollToItem(0)
    }

    // === Date picker dialog (Scale ekranniki qayta ishlatamiz) ===
    if (state.showDatePicker) {
        ScaleDatePickerDialog(
            initialIsoDate = state.selectedDate,
            onDismiss = { viewModel.onEvent(QueueManagementUiEvent.DismissDatePicker) },
            onDateSelected = { viewModel.onEvent(QueueManagementUiEvent.DateSelected(it)) }
        )
    }

    // === Permit dialog ===
    state.permitDialog?.let { ds ->
        PermitDialog(
            state = ds,
            onDismiss = { viewModel.onEvent(QueueManagementUiEvent.DismissPermitDialog) },
            onRetry = { viewModel.onEvent(QueueManagementUiEvent.RetryPermit) }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // === 1. Date bar + counter ===
            QueueDateBar(
                isoDate = state.selectedDate,
                totalPermittedCount = state.totalPermittedCount,
                onDateClick = { viewModel.onEvent(QueueManagementUiEvent.OpenDatePicker) }
            )

            // === 2. Tabs ===
            QueueTypeTabs(
                selected = state.selectedTab,
                onSelect = { viewModel.onEvent(QueueManagementUiEvent.TabSelected(it)) }
            )

            // === 3. List ===
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.onEvent(QueueManagementUiEvent.Refresh) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val tab = state.activeTab

                when {
                    state.isLoading && tab.isFullyEmpty -> LoadingState()

                    state.listError != null && tab.isFullyEmpty -> ErrorState(
                        message = state.listError ?: "Xatolik",
                        onRetry = { viewModel.onEvent(QueueManagementUiEvent.Refresh) }
                    )

                    tab.isFullyEmpty -> EmptyState()

                    tab.isQueueEmpty && tab.enteredItems.isEmpty() -> QueueEmptyState()

                    else -> UnifiedClickableList(
                        tab = tab,
                        listState = listState,
                        onItemClick = { item ->
                            viewModel.onEvent(QueueManagementUiEvent.ItemClicked(item))
                        }
                    )
                }
            }
        }
    }
}

/**
 * Joriy navbatchidan oldingi (entered/skipped) + banner + keyingi (waiting)
 * mashinalar bir LazyColumn'da. Hammasi clickable.
 *
 * Tartib:
 *  1. enteredItems (sortlangan queueNumber asc) — eng eski yuqorida
 *  2. currentlyEntering banner — highlight
 *  3. waitingItems (sortlangan queueNumber asc) — keyingi mashinalar
 *
 * Banner ham clickable (NowEnteringBanner'ni clickable Box ichiga o'rab).
 */
@Composable
private fun UnifiedClickableList(
    tab: TabState,
    listState: LazyListState,
    onItemClick: (uz.jurabekov.guard.domain.model.QueueItem) -> Unit
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = Dimens.SpaceM,
            end = Dimens.SpaceM,
            top = 0.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // === History: entered + skipped ===
        items(
            items = tab.enteredItems,
            key = { "history-${it.uuid}" }
        ) { item ->
            QueueListItem(
                item = item,
                onClick = { onItemClick(item) }
            )
        }

        // === Banner: currently entering ===
        tab.currentlyEntering?.let { item ->
            item(key = "banner-${item.uuid}") {
                Spacer(Modifier.height(2.dp))
                BannerClickable(
                    item = item,
                    onClick = { onItemClick(item) }
                )
                Spacer(Modifier.height(2.dp))
            }
        }

        // === Waiting items ===
        items(
            items = tab.waitingItems,
            key = { "waiting-${it.uuid}" }
        ) { item ->
            QueueListItem(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

/**
 * `NowEnteringBanner`'ni clickable wrapper. Banner komponenti o'zi
 * onClick qabul qilmaydi (QueueScreen passive ishlatadi) — wrapper.
 *
 * Material ripple banner gradient ustida ham yaxshi ko'rinadi (tactile feedback
 * UX uchun foydali). `onClickLabel` accessibility uchun.
 */
@Composable
private fun BannerClickable(
    item: uz.jurabekov.guard.domain.model.QueueItem,
    onClick: () -> Unit
) {
    NowEnteringBanner(
        item = item,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Ruxsatnomani ochish",
                onClick = onClick
            )
    )
}
