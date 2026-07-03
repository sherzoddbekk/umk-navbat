package uz.jurabekov.guard.domain.usecase

import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.model.QueueSnapshot
import uz.jurabekov.guard.domain.repository.QueueRepository

class GetQueueListUseCase(
    private val repository: QueueRepository
) {
    suspend operator fun invoke(): ApiResult<QueueSnapshot> = repository.fetchQueue()
}
