package uz.jurabekov.guard.core.network

/**
 * Network natijasini wrap qilish uchun sealed type.
 * UseCase / Repository qatlamida exception throw qilish o'rniga shu type qaytariladi.
 *
 * Bu pattern:
 * - UI'da xatoliklarni explicit handle qilishga majbur qiladi
 * - Try/catch'larni har joyga tarqatishga yo'l qo'ymaydi
 * - Test yozish oson
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val code: Int? = null, val message: String) : ApiResult<Nothing>
    data object NetworkError : ApiResult<Nothing>
    data object Unauthorized : ApiResult<Nothing>
}
