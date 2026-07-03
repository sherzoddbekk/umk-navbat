package uz.jurabekov.guard.domain.model

/**
 * Mashina turi — backend payload'idagi `type` field'iga moslashtirilgan.
 *
 *   OPEN  ←→ `type: null`        — usti ochiq mashina
 *   TENT  ←→ `type: "tent"`      — usti yopiq (tentli) mashina
 *
 * Backend yangi tip qo'shsa, shu yerda yangi enum entry qo'shamiz va
 * [fromBackend] / [toBackend] mappingni kengaytiramiz — qolgan logika tegmaydi.
 */
enum class VehicleType {
    OPEN, TENT;

    /** Backend'ga POST qilinadigan format. */
    fun toBackend(): String? = when (this) {
        OPEN -> null
        TENT -> "tent"
    }

    companion object {
        /** Backend'dan kelgan `type` qiymatidan domain enum'ga aylantirish. */
        fun fromBackend(raw: String?): VehicleType = when (raw?.lowercase()) {
            "tent" -> TENT
            else -> OPEN   // null va boshqa noma'lum qiymatlar OPEN deb hisoblanadi
        }
    }
}