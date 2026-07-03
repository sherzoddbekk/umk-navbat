package uz.jurabekov.guard.domain.usecase

import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.model.AuthSession
import uz.jurabekov.guard.domain.repository.AuthRepository

/**
 * Login UseCase — bitta entry point.
 *
 * Hozircha bevosita repository.login() ga delegate qilamiz. Kelajakda
 * bu yerda qo'shimcha logika joylashtirilishi mumkin:
 *  - input normalization (lowercase username, trim whitespace)
 *  - audit logging
 *  - analytics events (login_attempted / login_failed)
 *  - FCM token registratsiyasi (login success keyin)
 */
class LoginUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        username: String,
        password: String
    ): ApiResult<AuthSession> {
        // Normalize: trim + lowercase username
        val cleanUsername = username.trim().lowercase()
        return repository.login(cleanUsername, password)
    }
}
