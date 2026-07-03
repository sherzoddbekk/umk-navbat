package uz.jurabekov.guard.domain.usecase

import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.model.QueueSnapshot
import uz.jurabekov.guard.domain.repository.QueueRepository

/**
 * Sana bo'yicha navbat snapshotini olish — Navbat boshqaruvi ekranida
 * date picker tanloviga ko'ra chaqiriladi.
 *
 * @param date `null` → backend bugungi sanani qaytaradi.
 *             `"yyyy-MM-dd"` → aniq sana.
 */
class GetQueueByDateUseCase(
    private val repository: QueueRepository
) {
    suspend operator fun invoke(date: String?): ApiResult<QueueSnapshot> =
        repository.fetchQueue(date)
}
