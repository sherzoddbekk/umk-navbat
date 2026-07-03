package uz.jurabekov.guard.presentation.scale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uz.jurabekov.guard.domain.model.ScaleRecord
import uz.jurabekov.guard.presentation.queue.components.EmptyState
import uz.jurabekov.guard.presentation.queue.components.ErrorState
import uz.jurabekov.guard.presentation.queue.components.LoadingState
import uz.jurabekov.guard.presentation.scale.components.ScaleDateBar
import uz.jurabekov.guard.presentation.scale.components.ScaleDatePickerDialog
import uz.jurabekov.guard.presentation.scale.components.ScaleListItem
import uz.jurabekov.guard.presentation.scale.components.ScaleStatusFilterRow
import uz.jurabekov.guard.ui.theme.Dimens

/**
 * Tarozi ekrani — Main screen ichidagi nested route.
 *
 * **Layout (yuqoridan-pastga):**
 *  1. `ScaleDateBar`         — date picker chip + refresh button
 *  2. `ScaleStatusFilterRow` — Hammasi / Yakunlangan / Ichkarida chiplari
 *  3. `LazyColumn`           — filtered yozuvlar (faqat shu scroll bo'ladi)
 *
 * **`displayRecords` derivation:**
 *  `remember(records, filter)` — records yoki filter o'zgarganda qayta
 *  hisoblanadi. ViewModel state'ida saqlanmaydi (presentation concern).
 *
 * **Scroll reset on filter change:**
 *  Filter o'zgarganda LazyColumn yuqoriga scroll bo'ladi. Aks holda
 *  foydalanuvchi "ichkarida"'ga o'tib, pastda turgan bo'sh joyni ko'rishi
 *  mumkin (count kichik bo'lsa). `LaunchedEffect(filter)` orqali.
 *
 * **Performance:**
 *  - `key = "plate-entryTime-idx"` — stable diffing
 *  - `LazyColumn` virtualizatsiya
 *  - Filter switching — list copy, lekin 50-100 yozuvda sub-ms
 *  - Counts pre-computed — chip recomposition zero-cost
 */
@Composable
fun ScaleScreen(
    viewModel: ScaleViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // === Derived state: filtered records ===
    // ViewModel'da emas, UI'da — presentation concern.
    val displayRecords = remember(state.records, state.statusFilter) {
        state.statusFilter.apply(state.records)
    }

    // Filter o'zgarganda ro'yxat boshiga scroll. `animateScrollToItem` smooth.
    LaunchedEffect(state.statusFilter) {
        if (displayRecords.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    if (state.showDatePicker) {
        ScaleDatePickerDialog(
            initialIsoDate = state.selectedDate,
            onDismiss = { viewModel.onEvent(ScaleUiEvent.DismissDatePicker) },
            onDateSelected = { viewModel.onEvent(ScaleUiEvent.DateSelected(it)) }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // === Top: Date bar ===
            ScaleDateBar(
                isoDate = state.selectedDate,
                isLoading = state.isLoading,
                isRefreshing = state.isRefreshing,
                onDateClick = { viewModel.onEvent(ScaleUiEvent.OpenDatePicker) },
                onRefreshClick = { viewModel.onEvent(ScaleUiEvent.Refresh) }
            )

            // === Filter chips — faqat records mavjud bo'lsa ko'rinadi ===
            //
            // Loading/error/empty holatlarda filter row noma'qul (filter
            // qiladigan narsa yo'q). UI clean qoladi.
            if (state.records.isNotEmpty()) {
                ScaleStatusFilterRow(
                    selected = state.statusFilter,
                    counts = state.statusCounts,
                    onFilterChange = {
                        viewModel.onEvent(ScaleUiEvent.FilterChanged(it))
                    }
                )
            }

            // === Content (scrollable area) ===
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.isLoading && state.records.isEmpty() -> LoadingState()

                    state.error != null && state.records.isEmpty() -> ErrorState(
                        message = state.error ?: "Xatolik",
                        onRetry = { viewModel.onEvent(ScaleUiEvent.Refresh) }
                    )

                    state.isFullyEmpty -> EmptyState()

                    else -> ScaleList(
                        records = displayRecords,
                        listState = listState
                    )
                }
            }
        }
    }
}

@Composable
private fun ScaleList(
    records: List<ScaleRecord>,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = Dimens.SpaceM,
            end = Dimens.SpaceM,
            top = 4.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        itemsIndexed(
            items = records,
            key = { idx, record -> "${record.plate}-${record.entryTime}-$idx" }
        ) { idx, record ->
            ScaleListItem(
                index = idx + 1,
                record = record
            )
        }
    }
}
