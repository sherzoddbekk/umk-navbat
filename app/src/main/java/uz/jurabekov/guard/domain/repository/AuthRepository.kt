package uz.jurabekov.guard.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.model.AuthSession
import uz.jurabekov.guard.domain.model.User

/**
 * Avtorizatsiya repository kontrakti.
 *
 * v2 — session ma'lumotlarini ham expose qiladi:
 *  - `isLoggedIn` — splash router uchun
 *  - `currentUser` — drawer va permission gating uchun
 */
interface AuthRepository {

    /** Token + user ikkalasi ham saqlanganmi. */
    val isLoggedIn: Flow<Boolean>

    /**
     * Joriy foydalanuvchi snapshot'i (drawer ichida ko'rsatish + permission
     * gating). `null` — login bo'lmagan yoki session corrupt.
     */
    val currentUser: Flow<User?>

    /**
     * Login. Muvaffaqiyatli — token + user atomic saqlanadi.
     *
     * Xato:
     *  - HTTP 401      → `ApiResult.Error(401, ...)`
     *  - IOException   → `ApiResult.NetworkError`
     *  - success=false → `ApiResult.Error(message=...)`
     */
    suspend fun login(username: String, password: String): ApiResult<AuthSession>

    /** Session tozalash — token va user kalit ikkalasi. */
    suspend fun logout()
}
