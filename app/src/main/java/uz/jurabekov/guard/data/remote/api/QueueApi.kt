package uz.jurabekov.guard.data.remote.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import uz.jurabekov.guard.data.remote.dto.InfoLaneActionResponseDto
import uz.jurabekov.guard.data.remote.dto.InfoLaneCallRequestDto
import uz.jurabekov.guard.data.remote.dto.PermitListResponseDto
import uz.jurabekov.guard.data.remote.dto.QueueCancelRequestDto
import uz.jurabekov.guard.data.remote.dto.QueueCancelResponseDto
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
     * Egasi navbatni bekor qilish — `owner_token` + `plate` bilan.
     * Auth talab qilinmaydi (token o'zi identifikatsiya qiladi).
     */
    @POST("api/v2/queue/owner/cancel")
    suspend fun cancelOwnerQueue(@Body body: QueueCancelRequestDto): QueueCancelResponseDto

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

    /**
     * Mashinani info-tabloda 1/2/3-yo'lga chaqirish.
     * Faqat ruxsatnoma berilgan (`has_permit=true`) navbat uchun ma'noli.
     */
    @POST("api/v2/queue/{id}/info-lane/call")
    suspend fun callInfoLane(
        @Path("id") queueId: Long,
        @Body body: InfoLaneCallRequestDto
    ): InfoLaneActionResponseDto

    /**
     * Mashina qo'lda o'tkazildi — tablodan olib tashlanadi, yo'l bo'shaydi.
     * Body talab qilinmaydi (bo'sh POST).
     */
    @POST("api/v2/queue/{id}/entry/manual")
    suspend fun markManualEntry(
        @Path("id") queueId: Long
    ): InfoLaneActionResponseDto
}
