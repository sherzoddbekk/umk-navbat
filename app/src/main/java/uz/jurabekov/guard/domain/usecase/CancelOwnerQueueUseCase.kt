package uz.jurabekov.guard.domain.usecase

import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.repository.QueueRepository

/**
 * Egasi navbatni bekor qilish.
 *
 * Backend token + plate'ni birga tekshiradi. Plate probel bilan yoki
 * probelsiz yuborilishi mumkin — bu yerda saqlangan ko'rinishda (probel bilan)
 * yuboramiz, backend ikkalasini ham qabul qiladi.
 */
class CancelOwnerQueueUseCase(
    private val repository: QueueRepository
) {
    suspend operator fun invoke(ownerToken: String, plate: String): ApiResult<Unit> {
        if (ownerToken.isBlank() || plate.isBlank()) {
            return ApiResult.Error(message = ERR_INVALID)
        }
        return repository.cancelOwnerQueue(ownerToken = ownerToken, plate = plate.trim())
    }

    companion object {
        const val ERR_INVALID = "Navbat ma'lumotlari topilmadi"
    }
}
