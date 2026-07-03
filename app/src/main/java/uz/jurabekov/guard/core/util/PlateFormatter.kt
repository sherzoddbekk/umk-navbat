package uz.jurabekov.guard.core.util

/**
 * Mashina raqami uchun minimal sanity check va sanitize.
 *
 * Strict regex ishlatmaymiz — chunki real hayotda format'lar har xil:
 *  - "01 W 571 QA" (klassik O'zbekiston)
 *  - "071 A 789 AB" (yangi)
 *  - "T 1234 RU" (tranzit)
 *  - "AS 12345" (qisqa format)
 *  - va boshqa variantlar
 *
 * Backend o'zi validate qiladi. Frontend faqat:
 *  - Bo'sh emasligini tekshiradi
 *  - Trim qiladi
 *  - Uppercase'ga keltiradi
 *  - Bir nechta bo'sh joylarni bittaga qisqartiradi
 */
object PlateFormatter {

    private const val MIN_PLATE_LENGTH = 4
    private const val MAX_PLATE_LENGTH = 20

    /**
     * Foydalanuvchi inputini tozalaydi:
     *  - boshlash/oxirdagi probellarni olib tashlaydi
     *  - bir nechta probelni bittaga qisqartiradi
     *  - uppercase'ga keltiradi
     *
     * "  01w  571 qa  " -> "01W 571 QA"
     */
    fun sanitize(input: String): String =
        input.trim()
            .replace(MULTI_SPACE, " ")
            .uppercase()

    /**
     * Minimum check: bo'sh emas va juda qisqa emas.
     */
    fun isValid(plate: String): Boolean {
        val cleaned = plate.trim()
        return cleaned.length in MIN_PLATE_LENGTH..MAX_PLATE_LENGTH
    }

    private val MULTI_SPACE = Regex("\\s+")
}