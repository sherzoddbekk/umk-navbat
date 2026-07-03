package uz.jurabekov.guard.domain.usecase

import kotlinx.coroutines.flow.Flow
import uz.jurabekov.guard.domain.model.QueueUpdate
import uz.jurabekov.guard.domain.repository.QueueRepository

class ObserveQueueUpdatesUseCase(
    private val repository: QueueRepository
) {
    operator fun invoke(): Flow<QueueUpdate> = repository.observeUpdates()
}
