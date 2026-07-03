package uz.jurabekov.guard.presentation.scale

/**
 * Scale screen user intents.
 *
 * MVI pattern: View → Event → ViewModel → State.
 */
sealed interface ScaleUiEvent {
    /** Date picker dialogini ochish. */
    data object OpenDatePicker : ScaleUiEvent

    /** Date picker dialogini yopish (cancel/dismiss). */
    data object DismissDatePicker : ScaleUiEvent

    /**
     * Yangi sana tanlandi.
     * @param date `yyyy-MM-dd` ISO format
     */
    data class DateSelected(val date: String) : ScaleUiEvent

    /** Yangilash buttoni / pull-to-refresh. */
    data object Refresh : ScaleUiEvent

    /**
     * Status filter o'zgartirildi.
     *
     * Filter o'zgarishi REST chaqiruvi qo'zg'atmaydi — mavjud `records`
     * ustida UI-side filtering. Tezkor, network bo'shliq talab qilmaydi.
     */
    data class FilterChanged(val filter: ScaleStatusFilter) : ScaleUiEvent
}
