package uz.jurabekov.guard.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import uz.jurabekov.guard.data.preferences.AuthPreferences

/**
 * Bearer token interceptor.
 *
 * Har bir HTTP so'rovga avtomatik `Authorization: Bearer <token>` header
 * qo'shadi, agar token saqlangan bo'lsa.
 *
 * === Skip ro'yxati ===
 * /auth/ pathi ostidagi so'rovlardan token olib tashlanadi:
 *  - /api/v2/auth/login — token bo'lmasligi kerak (uni olish jarayoni)
 *  - kelajakdagi /auth/refresh, /auth/forgot — shu zonalarga ham mos
 *
 * === Concurrency note ===
 * `runBlocking` OkHttp dispatch thread'ida ishlaydi (NOT main thread).
 * DataStore o'qishi memory-mapped → odatda 1-3 ms. Bu interceptor
 * pattern OkHttp dokumentatsiyasida ham tavsiya etilgan.
 *
 * Alternative: in-memory `AtomicReference<String?>` cache + Repository
 * yangilash uchun explicit sync. Hozirgi yondashuv sodda va to'g'ri.
 */
class AuthInterceptor(
    private val preferences: AuthPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Auth endpointlari uchun token qo'shmaymiz — login uchun mavjud
        // bo'lgan eski token serverda 401 ga olib kelishi mumkin.
        if (original.url.encodedPath.contains(AUTH_PATH_SEGMENT)) {
            return chain.proceed(original)
        }

        val token = runBlocking { preferences.getToken() }

        val authedRequest = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .addHeader(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
                .build()
        } else {
            original
        }

        return chain.proceed(authedRequest)
    }

    private companion object {
        const val AUTH_PATH_SEGMENT = "/auth/"
        const val HEADER_AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
