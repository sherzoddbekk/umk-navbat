package uz.jurabekov.guard.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query
import uz.jurabekov.guard.data.remote.dto.ScaleListResponseDto

/**
 * Tarozi endpointlari.
 *
 * `AuthInterceptor` `/auth/` path'idan tashqari barcha so'rovlarga
 * `Authorization: Bearer <token>` qo'shadi — bu endpoint authenticated.
 */
interface ScaleApi {

    /**
     * Sana bo'yicha tarozi ro'yxati.
     *
     * `date` — `yyyy-MM-dd` formatda. `null` yuborilsa backend bugungi
     * sanani qaytaradi (server-side default). Bu yerda always non-null
     * yuboramiz — UI hech qachon `null` send qilmasligi kerak.
     *
     * @param date `yyyy-MM-dd` — ISO local date format
     */
    @GET("api/v2/scale")
    suspend fun getScaleList(
        @Query("date") date: String
    ): ScaleListResponseDto
}
