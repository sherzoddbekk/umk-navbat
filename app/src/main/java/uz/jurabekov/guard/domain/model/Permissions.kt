package uz.jurabekov.guard.domain.model

/**
 * Backend permission konstantlari.
 *
 * **Nima uchun `object` (enum emas)?**
 * Backend kelajakda yangi permission qo'shsa (`reports.view`, `audit.view`),
 * eski app build'lar JSON deserialize paytida crash bermasin — `List<String>`
 * shaklida olamiz, string-match qilamiz. Enum bo'lganida `serialization`
 * `IllegalArgumentException` tashlardi.
 *
 * Bu **forward-compatible** strategiya — server-driven RBAC modelining
 * standart yondashuvi.
 */
object Permissions {
    const val DASHBOARD_VIEW = "dashboard.view"

    const val CAMERA_VIEW = "camera.view"
    const val CAMERA_MANAGE = "camera.manage"

    const val USERS_VIEW = "users.view"
    const val USERS_MANAGE = "users.manage"

    const val QUEUE_VIEW = "queue.view"

    const val SCALE_VIEW = "scale.view"
}

/**
 * Backend rol kodlari (`user.role_code`).
 *
 * Permission'lardan farqli o'laroq, ba'zi amallar rol darajasida cheklangan
 * (backend hozircha ular uchun alohida permission bermaydi) — masalan
 * info-tablo chaqiruvi.
 */
object Roles {
    const val ADMIN = "admin"
    const val GATE_INSPECTOR = "darvoza_tekshiruv"
}

/**
 * Info-tablo yo'l chaqiruvi va "o'tkazildi" amallarini bajara oladimi.
 * Faqat `admin` va `darvoza_tekshiruv` rollari.
 */
val User?.canManageInfoLane: Boolean
    get() = this != null && (roleCode == Roles.ADMIN || roleCode == Roles.GATE_INSPECTOR)

/**
 * Permission tekshirish — `User.permissions` `List<String>` uchun.
 *
 * Inline + reified yoki extension — ikkalasi ham ishlaydi; bu yerda
 * extension ishlatamiz, chunki call site sodda: `user.has(Permissions.SCALE_VIEW)`.
 *
 * Performance: `List.contains()` `O(n)`. Permissions ro'yxati odatda
 * 10-20 element, demak set'ga aylantirish overkill (allocation > linear scan).
 * Agar 100+ permission bo'lsa — `User.permissions: Set<String>` ga o'tish kerak.
 */
fun User.has(permission: String): Boolean = permissions.contains(permission)

/** Hech bir permission'i bormi (drawer empty state uchun). */
fun User.hasAny(vararg perms: String): Boolean = perms.any { permissions.contains(it) }
