package uz.jurabekov.guard.data.preferences.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DataStore'da JSON sifatida saqlanadigan "Eslab qolish" driver snapshot.
 *
 * **Saqlanish strategiyasi:**
 *  - Yagona JSON record — barcha maydonlar atomic yoziladi (`edit{}` block ichida).
 *  - Yarim-yangilangan state imkonsiz (concurrent reader race condition yo'q).
 *  - Forward compat: yangi maydon qo'shilsa `default value` orqali — schema migration shart emas
 *    (`@SerialName` + default qiymat → eski JSON parse muvaffaqiyatli).
 *
 * **Nega `VehicleType` o'rniga `String`?**
 *  - Domain enum'i to'g'ridan-to'g'ri saqlansa, kelajakda renamed qilinsa eski clientlarda
 *    `SerializationException` bo'ladi va data yo'qoladi.
 *  - String saqlash + `VehicleType.valueOf(...)` defensive (catch) → bardoshli.
 *  - `runCatching` mapping `SavedDriverPreferences` ichida.
 *
 * **Maydonlar:**
 *  - `vehicleType`   : "OPEN" yoki "TENT" (enum name)
 *  - `plate`         : avtomobil raqami (ASCII upper-case, masalan "01W571QA")
 *  - `fullName`      : haydovchi F.I.Sh
 *  - `passportSeries`: 2 ta Lotin harf yoki bo'sh (ixtiyoriy)
 *  - `passportNumber`: 7 ta raqam yoki bo'sh (ixtiyoriy)
 *
 * **Xavfsizlik eslatmasi:**
 *  Pasport ma'lumotlari plain text. Threat model qattiqlashsa →
 *  EncryptedDataStore yoki Keystore+AES-GCM migration kerak.
 */
@Serializable
data class SavedDriverData(
    @SerialName("vehicle_type") val vehicleType: String = "OPEN",
    @SerialName("plate") val plate: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("passport_series") val passportSeries: String = "",
    @SerialName("passport_number") val passportNumber: String = ""
)