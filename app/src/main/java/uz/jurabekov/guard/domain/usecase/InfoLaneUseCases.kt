package uz.jurabekov.guard.domain.usecase

import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.repository.QueueRepository

/**
 * Mashinani info-tabloda 1/2/3-yo'lga chaqirish.
 *
 * Yo'l raqami domain qoidasi shu yerda tekshiriladi — UI xato qiymat
 * yubora olmasin (server'ga keraksiz 422 bormaydi).
 */
class CallInfoLaneUseCase(
    private val repository: QueueRepository
) {
    suspend operator fun invoke(queueId: Long, lane: Int): ApiResult<String> {
        require(lane in LANES) { "Yo'l raqami 1..3 oralig'ida bo'lishi kerak: $lane" }
        return repository.callInfoLane(queueId, lane)
    }

    companion object {
        /** Info-tablodagi mavjud yo'llar. UI tugmalari ham shundan quriladi. */
        val LANES = 1..3
    }
}

/** Mashina qo'lda o'tkazildi — tablodan ketadi, yo'l bo'shaydi. */
class MarkManualEntryUseCase(
    private val repository: QueueRepository
) {
    suspend operator fun invoke(queueId: Long): ApiResult<String> =
        repository.markManualEntry(queueId)
}

/** Yo'l chaqiruvini bekor qiladi — mashina 1/2/3-yo'l tanlash holatiga qaytadi. */
class ReleaseInfoLaneUseCase(
    private val repository: QueueRepository
) {
    suspend operator fun invoke(queueId: Long): ApiResult<String> =
        repository.releaseInfoLane(queueId)
}
