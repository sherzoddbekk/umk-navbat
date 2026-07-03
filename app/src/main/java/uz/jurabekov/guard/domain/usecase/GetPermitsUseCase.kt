package uz.jurabekov.guard.domain.usecase

import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.model.Permit
import uz.jurabekov.guard.domain.repository.QueueRepository

/**
 * Bitta navbat uchun ruxsatnomalarni olish.
 *
 * Repository to'g'ridan-to'g'ri ham chaqirilishi mumkin edi, ammo use case
 * mavjud bo'lsa: (1) testlash oson — mock UseCase yetadi; (2) kelajakda
 * caching/business rules qo'shiladi (masalan, "ENTERED bo'lmagan item'larga
 * permit ko'rsatma") — bir joyda.
 */
class GetPermitsUseCase(
    private val repository: QueueRepository
) {
    suspend operator fun invoke(queueId: Long): ApiResult<List<Permit>> =
        repository.fetchPermits(queueId)
}
