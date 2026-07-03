package uz.jurabekov.guard.core.util

/**
 * QueueSubmitDialog form input filtering.
 *
 * Ikki qatlamli yondashuv:
 *  1. **Filter** — `onValueChange` da noto'g'ri belgilarni darhol olib tashlaydi.
 *     State doim "clean" bo'lib qoladi, paste qilingan ifloslik ham
 *     avtomatik tozalanadi.
 *  2. **Detection** — agar filter biror belgini olib tashlagan bo'lsa
 *     ([filteredOut] = true), ViewModel transient error message
 *     ko'rsatadi. Keyingi to'g'ri keystroke error'ni o'chiradi.
 *
 * Performance: regex ishlatmaymiz — character iteration arzonroq, allocation
 * bitta `StringBuilder` (capacity hint bilan). Hot path emas, ammo idiomatic.
 *
 * Single responsibility: bu class faqat character-level filtering. Length,
 * format va boshqa rule'lar boshqa joyda (PlateFormatter / submit validation).
 */
object InputValidator {

    /** Filter natijasi: tozalangan qiymat va biror belgi olib tashlanganmi. */
    data class Result(val value: String, val filteredOut: Boolean)

    /**
     * Mashina raqami: faqat ASCII Lotin harf (A-Z, a-z), raqam (0-9), bo'sh joy.
     *
     * MUHIM: Kirilcha 'А' (U+0410) — Lotin 'A' (U+0041) bilan vizual bir xil,
     * lekin codepoint farqli. `Char.isLetter()` har ikkalasiga ham true qaytaradi,
     * shuning uchun ASCII range'ni explicit tekshiramiz.
     */
    fun filterPlate(input: String): Result {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch.isAsciiLatinOrDigit() || ch == ' ') sb.append(ch)
        }
        return Result(sb.toString(), filteredOut = sb.length < input.length)
    }

    /**
     * F.I.Sh: ruxsat etilgan belgilar:
     *  - har qanday tildagi harf (Lotin, Kirill, Arab, va h.k.)
     *  - bo'sh joy (ism va familiya orasida)
     *  - apostrof (G'afur, Jo'rabekov)
     *  - tire (qo'shma familiyalar: Anna-Maria)
     *
     * Rad etiladi: raqamlar va boshqa simbollar (!, @, #, va h.k.).
     *
     * `Char.isLetter()` Unicode-aware — barcha tildagi harflarni qabul qiladi.
     * Apostrofning ikki varianti qo'llab-quvvatlanadi:
     *  - ASCII `'` (U+0027) — standart klaviatura
     *  - Unicode `'` (U+2019) — iOS smart-quote, ko'pincha autocorrect
     */
    fun filterName(input: String): Result {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch.isLetter() || ch == ' ' || ch.isAllowedNamePunctuation()) sb.append(ch)
        }
        return Result(sb.toString(), filteredOut = sb.length < input.length)
    }

    /**
     * Pasport seriyasi: faqat 2 ta ASCII Lotin harfi.
     * Avtomatik uppercase. Maksimum [Constants.PASSPORT_SERIES_LENGTH] belgi.
     *
     * `filteredOut = true` faqat ruxsat etilmagan belgi (raqam, simbol, Kirill)
     * uchurilgan bo'lsa. Length cap silently — bu false positive emas.
     */
    fun filterPassportSeries(input: String): Result {
        var hadInvalid = false
        val sb = StringBuilder(2)
        for (ch in input) {
            val isLatin = ch in 'A'..'Z' || ch in 'a'..'z'
            if (!isLatin) {
                hadInvalid = true
                continue
            }
            if (sb.length < 2) sb.append(ch.uppercaseChar())
            // Length cap: 2 dan ortig'i silently tashlanadi, error emas
        }
        return Result(sb.toString(), filteredOut = hadInvalid)
    }

    /**
     * Pasport raqami: faqat 0-9. Maksimum [Constants.PASSPORT_NUMBER_LENGTH] raqam.
     *
     * `filteredOut = true` faqat raqam emas belgi uchurilgan bo'lsa.
     */
    fun filterPassportNumber(input: String): Result {
        var hadInvalid = false
        val sb = StringBuilder(7)
        for (ch in input) {
            if (ch !in '0'..'9') {
                hadInvalid = true
                continue
            }
            if (sb.length < 7) sb.append(ch)
        }
        return Result(sb.toString(), filteredOut = hadInvalid)
    }

    private fun Char.isAsciiLatinOrDigit(): Boolean =
        this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9'

    private fun Char.isAllowedNamePunctuation(): Boolean =
        this == '\'' || this == '\u2019' || this == '-'
}