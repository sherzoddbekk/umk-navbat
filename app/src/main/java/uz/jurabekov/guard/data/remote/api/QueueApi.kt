package uz.jurabekov.guard.data.remote.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import uz.jurabekov.guard.data.remote.dto.PermitListResponseDto
import uz.jurabekov.guard.data.remote.dto.QueueListResponseDto
import uz.jurabekov.guard.data.remote.dto.QueueRequestDto
import uz.jurabekov.guard.data.remote.dto.QueueSubmitResponseDto

interface QueueApi {

    /**
     * Navbat ro'yxati. `date` parametri ixtiyoriy:
     *  - `null` → backend bugungi sanani qaytaradi (default behavior, eski
     *    kontrakt — `QueueScreen` shu shaklda ishlatadi)
     *  - `"yyyy-MM-dd"` → aniq sana (Navbat boshqaruvi ekranida sana picker)
     *
     * Retrofit `@Query` `null` qiymatda URL'ga `?date=` qo'shmaydi.
     */
    @GET("api/v2/queue")
    suspend fun getQueueList(
        @Query("date") date: String? = null
    ): QueueListResponseDto

    /** Yangi navbat olish. */
    @POST("api/v2/queue")
    suspend fun submitQueue(@Body body: QueueRequestDto): QueueSubmitResponseDto

    /**
     * Bitta navbat uchun ruxsatnomalar — Navbat boshqaruvi ekranida item
     * bosilganda chaqiriladi. `AuthInterceptor` Bearer token avtomatik
     * qo'shadi (path `/auth/` emas).
     *
     * @param queueId Backend `queue.id` (UUID emas — integer ID).
     */
    @GET("api/v2/queue/{id}/permits")
    suspend fun getPermits(
        @Path("id") queueId: Long
    ): PermitListResponseDto
}
