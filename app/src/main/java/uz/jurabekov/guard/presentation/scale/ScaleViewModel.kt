package uz.jurabekov.guard.presentation.scale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.usecase.GetScaleListUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tarozi ViewModel.
 *
 * **Concurrency design:**
 *  - `loadJob` — single-flight pattern. Yangi sana tanlanganda eski
 *    inflight so'rov cancel bo'ladi.
 *  - `viewModelScope` — onCleared'da avtomatik cancel.
 *
 * **State emission strategiyasi:**
 *  - Sana o'zgarganda: `isLoading = true`, `records = emptyList()`,
 *    `statusCounts` reset — stale datani ko'rsatmaymiz.
 *  - Refresh paytida: `isRefreshing = true`, `records` saqlanadi —
 *    foydalanuvchi mavjud ma'lumotlar ustida ishlayotganini biladi.
 *  - Filter changed: faqat `statusFilter` o'zgaradi, network chaqirilmaydi.
 *
 * **Counts pre-computation:**
 *  Records yangilanganda `StatusCounts.from()` chaqirilib state'da
 *  saqlanadi. UI chip'lari count'larni `O(1)` read qiladi — har recompose
 *  hisoblash isrof bo'lmaydi.
 *
 * **Error handling:**
 *  - `Unauthorized` (401) — token expire. UI'da error ko'rsatamiz.
 *    Auto-logout interceptor kelajakda qo'shiladi.
 *
 * **Filter state persistence:**
 *  Filter holatini sana o'zgarganda saqlash kerakmi?
 *  - Qoldirsak: foydalanuvchi "Ichkarida" tanlagan, ertangi kunga o'tdi —
 *    yana "Ichkarida" — predictable.
 *  - Reset qilsak: har sana yangi kontekst — `ALL` boshlanadi.
 *  **Tanlov: saqlanadi (intuitive — filter UX kontekst-stable).**
 */
class ScaleViewModel(
    private val getScaleList: GetScaleListUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(
        ScaleUiState(selectedDate = today(), isLoading = true)
    )
    val state: StateFlow<ScaleUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadRecords(isRefresh = false)
    }

    fun onEvent(event: ScaleUiEvent) {
        when (event) {
            ScaleUiEvent.OpenDatePicker -> _state.update {
                it.copy(showDatePicker = true)
            }

            ScaleUiEvent.DismissDatePicker -> _state.update {
                it.copy(showDatePicker = false)
            }

            is ScaleUiEvent.DateSelected -> selectDate(event.date)

            ScaleUiEvent.Refresh -> loadRecords(isRefresh = true)

            is ScaleUiEvent.FilterChanged -> applyFilter(event.filter)
        }
    }

    /**
     * Filter o'zgartirish — local state update.
     *
     * Validation: count = 0 bo'lgan filter tanlanmasligi kerak (UI'da
     * disabled chip). Defensive: agar shunday bo'lsa ham state set qilamiz,
     * UI esa bo'sh list ko'rsatadi.
     *
     * No-op: bir xil filter qayta bosilsa update yo'q (distinct emit
     * StateFlow tomondan ham handle bo'ladi, lekin explicit early return
     * micro-optimization).
     */
    private fun applyFilter(filter: ScaleStatusFilter) {
        if (_state.value.statusFilter == filter) return
        _state.update { it.copy(statusFilter = filter) }
    }

    private fun selectDate(date: String) {
        // Bir xil sana qayta tanlansa — request yubormaymiz.
        if (_state.value.selectedDate == date && _state.value.records.isNotEmpty()) {
            _state.update { it.copy(showDatePicker = false) }
            return
        }

        _state.update {
            it.copy(
                selectedDate = date,
                showDatePicker = false,
                records = emptyList(),
                statusCounts = StatusCounts(),  // counts reset
                // statusFilter saqlanadi — UX kontekst-stable
                error = null
            )
        }
        loadRecords(isRefresh = false)
    }

    /**
     * REST chaqiruv — single-flight + race-guard.
     */
    private fun loadRecords(isRefresh: Boolean) {
        loadJob?.cancel()

        val date = _state.value.selectedDate

        _state.update {
            if (isRefresh) it.copy(isRefreshing = true, error = null)
            else it.copy(isLoading = true, error = null)
        }

        loadJob = viewModelScope.launch {
            val result = getScaleList(date)

            // Race condition guard.
            if (_state.value.selectedDate != date) return@launch

            when (result) {
                is ApiResult.Success -> {
                    val records = result.data.records
                    // Counts pre-compute — bir marta single-pass iteration.
                    val counts = StatusCounts.from(records)

                    _state.update {
                        // Filter validation: agar saqlangan filter ostida
                        // count=0 bo'lsa, avtomatik ALL'ga qaytamiz.
                        // (Misol: oldingi kunda "Ichkarida" tanlangan, yangi
                        // kunda hech kim ichkarida emas — bo'sh ro'yxat
                        // chiqarib turish noma'qul UX.)
                        val safeFilter = if (counts.countFor(it.statusFilter) == 0
                            && it.statusFilter != ScaleStatusFilter.ALL
                        ) ScaleStatusFilter.ALL else it.statusFilter

                        it.copy(
                            records = records,
                            statusCounts = counts,
                            statusFilter = safeFilter,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
                }

                is ApiResult.Error -> _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = result.message
                    )
                }

                ApiResult.NetworkError -> _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = MSG_NO_INTERNET
                    )
                }

                ApiResult.Unauthorized -> _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = MSG_UNAUTHORIZED
                    )
                }
            }
        }
    }

    private companion object {
        const val MSG_NO_INTERNET = "Internet aloqasi yo'q"
        const val MSG_UNAUTHORIZED = "Sessiya muddati tugagan, qayta kiring"

        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(Date())
    }
}
