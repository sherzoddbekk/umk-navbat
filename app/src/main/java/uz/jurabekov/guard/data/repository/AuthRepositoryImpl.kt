package uz.jurabekov.guard.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.data.preferences.AuthPreferences
import uz.jurabekov.guard.data.preferences.dto.StoredUser
import uz.jurabekov.guard.data.remote.api.AuthApi
import uz.jurabekov.guard.data.remote.dto.LoginDataDto
import uz.jurabekov.guard.data.remote.dto.LoginRequestDto
import uz.jurabekov.guard.data.remote.dto.UserDto
import uz.jurabekov.guard.domain.model.AuthSession
import uz.jurabekov.guard.domain.model.User
import uz.jurabekov.guard.domain.repository.AuthRepository
import java.io.IOException

/**
 * AuthRepository implementatsiyasi.
 *
 * Login flow:
 *  1. API ga so'rov
 *  2. Success → atomic saqlash: `saveSession(token, user)`
 *     - `isLoggedIn` Flow `true`'ga o'tadi
 *     - `currentUser` Flow yangilanadi
 *  3. Domain modeli qaytariladi
 *
 * Mapping:
 *  - DTO (`UserDto`)         → API'dan keladi
 *  - StoredUser              → DataStore JSON
 *  - User (domain)           → presentation/use-case
 *
 * Uch turdagi modelni saqlash — overhead emas, balki Clean Architecture
 * isolation. API schema o'zgarsa faqat DTO mapper o'zgaradi.
 */
class AuthRepositoryImpl(
    private val api: AuthApi,
    private val preferences: AuthPreferences
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = preferences.isLoggedIn

    override val currentUser: Flow<User?> = preferences.user.map { it?.toDomain() }

    override suspend fun login(
        username: String,
        password: String
    ): ApiResult<AuthSession> = try {
        val response = api.login(LoginRequestDto(username = username, password = password))

        when {
            !response.success -> ApiResult.Error(
                code = null,
                message = response.message?.takeIf { it.isNotBlank() } ?: MSG_GENERIC
            )

            response.data == null -> ApiResult.Error(message = MSG_GENERIC)

            else -> {
                // ATOMIC: token + user bitta DataStore edit'da yoziladi.
                // Race condition (token saqlandi, user JSON yo'q) — imkonsiz.
                preferences.saveSession(
                    token = response.data.token,
                    user = response.data.user.toStored()
                )
                ApiResult.Success(response.data.toDomain())
            }
        }
    } catch (e: CancellationException) {
        // Structured concurrency — rethrow.
        throw e
    } catch (e: HttpException) {
        when (e.code()) {
            401 -> ApiResult.Error(401, MSG_INVALID_CREDENTIALS)
            403 -> ApiResult.Error(403, MSG_FORBIDDEN)
            in 400..499 -> ApiResult.Error(e.code(), MSG_GENERIC)
            else -> ApiResult.Error(e.code(), MSG_SERVER_ERROR)
        }
    } catch (e: IOException) {
        ApiResult.NetworkError
    } catch (e: Exception) {
        ApiResult.Error(message = e.localizedMessage ?: MSG_GENERIC)
    }

    override suspend fun logout() {
        preferences.clear()
    }

    /* ===================== Mappers ===================== */

    private fun LoginDataDto.toDomain(): AuthSession = AuthSession(
        token = token,
        user = user.toDomain()
    )

    private fun UserDto.toDomain(): User = User(
        id = id,
        fullName = fullName,
        username = username,
        roleCode = roleCode,
        status = status,
        permissions = permissions
    )

    /** DTO → Storage modeli (presentation'ga emas, DataStore'ga). */
    private fun UserDto.toStored(): StoredUser = StoredUser(
        id = id,
        fullName = fullName,
        username = username,
        roleCode = roleCode,
        status = status,
        permissions = permissions
    )

    /** Storage → Domain modeli. */
    private fun StoredUser.toDomain(): User = User(
        id = id,
        fullName = fullName,
        username = username,
        roleCode = roleCode,
        status = status,
        permissions = permissions
    )

    private companion object {
        const val MSG_INVALID_CREDENTIALS = "Login yoki parol noto'g'ri"
        const val MSG_FORBIDDEN = "Sizga kirish ruxsat berilmagan"
        const val MSG_SERVER_ERROR = "Server xatosi, qayta urinib ko'ring"
        const val MSG_GENERIC = "Xatolik yuz berdi, qayta urinib ko'ring"
    }
}
