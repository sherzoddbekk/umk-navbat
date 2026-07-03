package uz.jurabekov.guard.domain.model

/**
 * Login muvaffaqiyatli bo'lganda olinadigan session.
 *
 * UI qatlami DTO'larni emas, domain model'larni ko'radi — bu Clean
 * Architecture qoidasi va backend refactoring'ga immune bo'lishni
 * ta'minlaydi.
 */
data class AuthSession(
    val token: String,
    val user: User
)

data class User(
    val id: Long,
    val fullName: String,
    val username: String,
    val roleCode: String,
    val status: String,
    val permissions: List<String>
)
