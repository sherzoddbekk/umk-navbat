package uz.jurabekov.guard.presentation.queue_management

import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.Roles

/**
 * "Navbat boshqaruvi" ekranidagi asosiy bo'lim (yuqori tab).
 *
 * Har bir mashina `has_permit` va `manual_passed` maydonlariga qarab bo'limga
 * tushadi:
 *  - [PERMIT_QUEUE]  — ruxsatnoma hali berilmagan (navbatda kutmoqda)
 *  - [GATE_QUEUE]    — ruxsatnoma bor, darvozadan kirishni kutmoqda
 *                      (1/2/3-yo'lga chaqirish shu yerda)
 *  - [GIVEN_PERMITS] — berilgan barcha ruxsatnomalar jurnali; kirib bo'lganlar
 *                      ("O'tkazilgan") ham shu yerda
 *
 * **Ko'rinishi rolga bog'liq** ([isVisibleFor]) — backend `role_code`ni
 * belgilaydi. Matritsa:
 *
 * | Rol               | Ruxsatnoma | Darvoza | Berilgan |
 * |-------------------|:---------:|:-------:|:--------:|
 * | admin             |     ✓     |    ✓    |    ✓     |
 * | nazoratchi        |     ✓     |    ✓    |    ✓     |
 * | operator          |     ✓     |    —    |    ✓     |
 * | mehmon            |     ✓     |    —    |    ✓     |
 * | darvoza_tekshiruv |     —     |    ✓    |    ✓     |
 */
enum class QueueSection(val title: String) {
    PERMIT_QUEUE(title = "Ruxsatnoma navbati"),
    GATE_QUEUE(title = "Darvoza navbati"),
    GIVEN_PERMITS(title = "Berilgan ruxsatnomalar");

    /** Mashina shu bo'limga tegishlimi. */
    fun matches(item: QueueItem): Boolean = when (this) {
        PERMIT_QUEUE -> !item.hasPermit
        GATE_QUEUE -> item.hasPermit && !item.manualPassed
        GIVEN_PERMITS -> item.hasPermit
    }

    /** Bu bo'limdagi item'lar yo'l boshqaruvi (chaqiruv) tugmalarini ko'rsatadimi. */
    val showsLaneActions: Boolean get() = this == GATE_QUEUE

    /** Berilgan rol bu bo'limni ko'ra oladimi. */
    fun isVisibleFor(roleCode: String?): Boolean = when (this) {
        GIVEN_PERMITS -> true                              // barcha rollar
        PERMIT_QUEUE -> roleCode != Roles.GATE_INSPECTOR   // darvoza_tekshiruvdan boshqa
        GATE_QUEUE -> roleCode in Roles.GATE_MANAGERS      // admin / nazoratchi / darvoza_tekshiruv
    }

    companion object {
        /** Rolga ko'ra ko'rinadigan bo'limlar (tab tartibida). */
        fun availableFor(roleCode: String?): List<QueueSection> =
            entries.filter { it.isVisibleFor(roleCode) }
    }
}
