package uz.jurabekov.guard.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST
import uz.jurabekov.guard.data.remote.dto.LoginRequestDto
import uz.jurabekov.guard.data.remote.dto.LoginResponseDto

/**
 * Avtorizatsiya endpointlari.
 *
 * MUHIM: AuthInterceptor `/auth/` path'iga ega so'rovlardan Bearer token'ni
 * olib tashlaydi. Login uchun token kerak emas (chunki bu uni olish so'rovi),
 * va eski token'ni qayta yuborish noto'g'ri 401'larga olib kelishi mumkin.
 */
interface AuthApi {

    @POST("api/v2/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto
}
