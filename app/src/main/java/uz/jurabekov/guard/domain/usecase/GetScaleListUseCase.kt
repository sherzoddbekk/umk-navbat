package uz.jurabekov.guard.domain.usecase

import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.model.ScaleDay
import uz.jurabekov.guard.domain.repository.ScaleRepository

/**
 * Tarozi ro'yxatini olish.
 *
 * Thin wrapper hozircha. Kelajakda business logikasi qo'shilishi mumkin:
 *  - audit logging
 *  - cache strategy (last 7 days)
 *  - merge with offline-stored entries
 *  - analytics events
 */
class GetScaleListUseCase(
    private val repository: ScaleRepository
) {
    suspend operator fun invoke(date: String): ApiResult<ScaleDay> =
        repository.getScaleList(date)
}
