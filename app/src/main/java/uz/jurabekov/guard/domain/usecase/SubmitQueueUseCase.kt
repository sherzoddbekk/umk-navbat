package uz.jurabekov.guard.domain.usecase

import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.core.util.Constants
import uz.jurabekov.guard.core.util.PlateFormatter
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.VehicleType
import uz.jurabekov.guard.domain.repository.QueueRepository

class SubmitQueueUseCase(
    private val repository: QueueRepository
) {
    suspend operator fun invoke(
        type: VehicleType,
        plate: String,
        fullName: String,
        passportSeries: String,
        passportNumber: String
    ): ApiResult<QueueItem> {

        val sanitizedPlate = PlateFormatter.sanitize(plate)
        if (!PlateFormatter.isValid(sanitizedPlate)) {
            return ApiResult.Error(message = ERR_PLATE)
        }

        val name = fullName.trim()
        if (name.length < Constants.DRIVER_NAME_MIN_LENGTH) {
            return ApiResult.Error(message = ERR_NAME)
        }

        // Pasport — atomic optional:
        //  - ikkalasi ham bo'sh → null yuboriladi (foydalanuvchi pasport kiritmagan)
        //  - ikkalasi ham to'liq → backend'ga yuboriladi
        //  - boshqa hollar (qisman to'la) → xato
        val seriesTrim = passportSeries.trim().uppercase()
        val numberTrim = passportNumber.trim()
        val seriesEmpty = seriesTrim.isEmpty()
        val numberEmpty = numberTrim.isEmpty()

        val (finalSeries, finalNumber) = when {
            seriesEmpty && numberEmpty -> {
                // Pasport umuman kiritilmagan — null yuboriladi (DTO'da default)
                null to null
            }
            seriesTrim.length == Constants.PASSPORT_SERIES_LENGTH &&
                    numberTrim.length == Constants.PASSPORT_NUMBER_LENGTH -> {
                seriesTrim to numberTrim
            }
            else -> {
                return ApiResult.Error(message = ERR_PASSPORT_INCOMPLETE)
            }
        }

        return repository.submitQueue(type, sanitizedPlate, name, finalSeries, finalNumber)
    }

    companion object {
        const val ERR_PLATE = "Mashina raqami juda qisqa"
        const val ERR_NAME = "Haydovchi F.I.Sh kamida 3 ta belgi bo'lishi kerak"
        const val ERR_PASSPORT_INCOMPLETE = "Pasport ma'lumotlarini to'liq kiriting yoki bo'sh qoldiring"
    }
}