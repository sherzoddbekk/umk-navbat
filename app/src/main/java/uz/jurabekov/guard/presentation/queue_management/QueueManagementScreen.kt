package uz.jurabekov.guard.presentation.queue_management

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
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
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.presentation.queue.components.ErrorState
import uz.jurabekov.guard.presentation.queue.components.LoadingState
import uz.jurabekov.guard.presentation.queue.components.QueueEmptyState
import uz.jurabekov.guard.presentation.queue.components.QueueListItem
import uz.jurabekov.guard.presentation.queue.components.QueueTypeTabs
import uz.jurabekov.guard.presentation.queue_management.components.ItemStatusBar
import uz.jurabekov.guard.presentation.queue_management.components.PermitDialog
import uz.jurabekov.guard.presentation.queue_management.components.QueueDateBar
import uz.jurabekov.guard.presentation.queue_management.components.QueueManagementItem
import uz.jurabekov.guard.presentation.queue_management.components.QueueSectionTabs
import uz.jurabekov.guard.presentation.queue_management.components.hasStatusBar
import uz.jurabekov.guard.presentation.scale.components.ScaleDatePickerDialog
import uz.jurabekov.guard.ui.theme.Accent500
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.Neutral0
import uz.jurabekov.guard.ui.theme.Neutral100
import uz.jurabekov.guard.ui.theme.Neutral300
import uz.jurabekov.guard.ui.theme.Primary100

/**
 * Navbat boshqaruvi ekrani.
 *
 * **Layout (yuqoridan-pastga):**
 *  1. `QueueDateBar`      — sana picker + Jami counter
 *  2. `QueueSectionTabs`  — Ruxsatnoma navbati / Darvoza navbati / Berilgan
 *                           ruxsatnomalar (rolga qarab 2 yoki 3 ta)
 *  3. `QueueTypeTabs`     — Usti ochiq / Usti yopiq
 *  4. `PullToRefreshBox`  + tanlangan bo'lim ro'yxati
 *
 * **Bo'limlar** (`QueueSection` — `has_permit`/`manual_passed` bo'yicha):
 *  - Ruxsatnoma navbati — ruxsatnomasiz kutayotgan mashinalar (oq karta)
 *  - Darvoza navbati    — ruxsatnoma bor, 1/2/3-yo'lga chaqirish tugmalari
 *  - Berilgan ruxsatnomalar — barcha berilgan ruxsatnomalar; kirganlar
 *    "O'tkazilgan" bilan
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueManagementScreen(
    viewModel: QueueManagementViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val listState = rememberLazyListState()

    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(QueueManagementUiEvent.AppResumed)
        onPauseOrDispose { /* no-op */ }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QueueManagementUiEffect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Bo'lim yoki yo'nalish almashsa — yuqoriga scroll.
    LaunchedEffect(state.selectedSection, state.selectedTab) {
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

            QueueDateBar(
                isoDate = state.selectedDate,
                totalPermittedCount = state.totalPermittedCount,
                onDateClick = { viewModel.onEvent(QueueManagementUiEvent.OpenDatePicker) }
            )

            QueueSectionTabs(
                sections = state.availableSections,
                selected = state.selectedSection,
                onSelect = { viewModel.onEvent(QueueManagementUiEvent.SectionSelected(it)) }
            )

            // Bo'lim tablari ko'k (primary) bo'lgani uchun bu yerda to'q sariq
            // (Accent) — ikki qatlam bir-biridan aniq ajraladi.
            QueueTypeTabs(
                selected = state.selectedTab,
                onSelect = { viewModel.onEvent(QueueManagementUiEvent.TabSelected(it)) },
                activeContainerColor = Accent500,
                activeContentColor = Neutral0
            )

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.onEvent(QueueManagementUiEvent.Refresh) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val items = state.visibleItems

                when {
                    state.isLoading && state.hasNoData -> LoadingState()

                    state.listError != null && state.hasNoData -> ErrorState(
                        message = state.listError ?: "Xatolik",
                        onRetry = { viewModel.onEvent(QueueManagementUiEvent.Refresh) }
                    )

                    items.isEmpty() -> QueueEmptyState()

                    else -> SectionList(
                        section = state.selectedSection,
                        items = items,
                        canManageInfoLane = state.canManageInfoLane,
                        laneActionInProgressId = state.laneActionInProgressId,
                        listState = listState,
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }
    }
}

/**
 * Tanlangan bo'lim ro'yxati. Item ko'rinishi bo'limga bog'liq:
 *  - GATE_QUEUE     → `QueueManagementItem` (1/2/3-yo'l tugmalari bilan)
 *  - GIVEN_PERMITS  → `QueueListItem` + kirganlarga "O'tkazilgan"
 *  - PERMIT_QUEUE   → oddiy `QueueListItem` (permit dialogga bosiladi)
 */
@Composable
private fun SectionList(
    section: QueueSection,
    items: List<QueueItem>,
    canManageInfoLane: Boolean,
    laneActionInProgressId: Long?,
    listState: LazyListState,
    onEvent: (QueueManagementUiEvent) -> Unit
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
        itemsIndexed(
            items = items,
            key = { _, item -> "${section.name}-${item.uuid}" }
        ) { index, item ->
            when (section) {
                // Darvoza navbati: tartib raqami 1'dan (backend queue_number emas).
                // Fon engil kulrang + ingichka chegara — kartalar aniq ajraladi.
                QueueSection.GATE_QUEUE -> QueueManagementItem(
                    item = item.copy(queueNumber = index + 1),
                    canManageInfoLane = canManageInfoLane,
                    isActionInProgress = laneActionInProgressId == item.id,
                    containerColor = Neutral100,
                    borderColor = Neutral300,
                    onClick = { onEvent(QueueManagementUiEvent.ItemClicked(item)) },
                    onLaneCall = { lane ->
                        onEvent(QueueManagementUiEvent.LaneCallClicked(item, lane))
                    },
                    onManualPass = { onEvent(QueueManagementUiEvent.ManualPassClicked(item)) }
                )

                // Berilgan: o'tkazilgan → kulrang, aks holda (kutmoqda/chaqirilgan)
                // → ochiq ko'k. Pastda status bari (O'tkazilgan / N-YO'LGA chaqirilgan).
                QueueSection.GIVEN_PERMITS -> QueueListItem(
                    item = item,
                    compact = true,
                    containerColor = if (item.manualPassed) null else Primary100,
                    onClick = { onEvent(QueueManagementUiEvent.ItemClicked(item)) },
                    actions = if (item.hasStatusBar()) {
                        { ItemStatusBar(item) }
                    } else null
                )

                QueueSection.PERMIT_QUEUE -> QueueListItem(
                    item = item,
                    compact = true,
                    onClick = { onEvent(QueueManagementUiEvent.ItemClicked(item)) }
                )
            }
        }
    }
}
