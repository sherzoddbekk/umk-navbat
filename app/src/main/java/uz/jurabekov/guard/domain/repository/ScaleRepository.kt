package uz.jurabekov.guard.domain.repository

import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.model.ScaleDay

/**
 * Tarozi repository kontrakti.
 *
 * Hozircha REST-only (no real-time updates). Kelajakda WebSocket subscription
 * qo'shilsa — `observeUpdates(date: String): Flow<ScaleDay>` qo'shiladi.
 */
interface ScaleRepository {

    /**
     * Berilgan sana bo'yicha tarozi yozuvlari.
     *
     * @param date `yyyy-MM-dd`
     */
    suspend fun getScaleList(date: String): ApiResult<ScaleDay>
}
