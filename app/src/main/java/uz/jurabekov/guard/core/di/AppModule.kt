package uz.jurabekov.guard.core.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import uz.jurabekov.guard.data.preferences.AnnouncementPreferences
import uz.jurabekov.guard.data.preferences.AuthPreferences
import uz.jurabekov.guard.data.preferences.MyQueuesPreferences
import uz.jurabekov.guard.data.preferences.OnboardingPreferences
import uz.jurabekov.guard.data.preferences.OwnedQueuesPreferences
import uz.jurabekov.guard.data.preferences.SavedDriverPreferences
import uz.jurabekov.guard.data.repository.AuthRepositoryImpl
import uz.jurabekov.guard.data.repository.QueueRepositoryImpl
import uz.jurabekov.guard.data.repository.ScaleRepositoryImpl
import uz.jurabekov.guard.domain.repository.AuthRepository
import uz.jurabekov.guard.domain.repository.QueueRepository
import uz.jurabekov.guard.domain.repository.ScaleRepository
import uz.jurabekov.guard.domain.usecase.CancelOwnerQueueUseCase
import uz.jurabekov.guard.domain.usecase.GetPermitsUseCase
import uz.jurabekov.guard.domain.usecase.GetQueueByDateUseCase
import uz.jurabekov.guard.domain.usecase.GetQueueListUseCase
import uz.jurabekov.guard.domain.usecase.GetScaleListUseCase
import uz.jurabekov.guard.domain.usecase.LoginUseCase
import uz.jurabekov.guard.domain.usecase.ObserveQueueUpdatesUseCase
import uz.jurabekov.guard.domain.usecase.SubmitQueueUseCase
import uz.jurabekov.guard.presentation.auth.LoginViewModel
import uz.jurabekov.guard.presentation.main.MainViewModel
import uz.jurabekov.guard.presentation.onboarding.OnboardingViewModel
import uz.jurabekov.guard.presentation.queue.QueueViewModel
import uz.jurabekov.guard.presentation.queue_management.QueueManagementViewModel
import uz.jurabekov.guard.presentation.scale.ScaleViewModel

val appModule = module {

    // ===== Repositories =====
    // QueueRepository singleton — bir nechta ekran (QueueScreen va
    // QueueManagementScreen) bir xil Pusher instance'idan event'larni
    // collect qiladi. SharedFlow tomonidan multi-collector qo'llab-quvvatlanadi.
    single<QueueRepository> {
        QueueRepositoryImpl(api = get(), pusherClient = get(), json = get())
    }

    single<AuthRepository> {
        AuthRepositoryImpl(api = get(), preferences = get())
    }

    single<ScaleRepository> {
        ScaleRepositoryImpl(api = get())
    }

    // ===== Preferences =====
    single { OnboardingPreferences(androidContext()) }
    single { MyQueuesPreferences(androidContext()) }
    single { AnnouncementPreferences(androidContext()) }
    // Olingan navbatlar (owner_token bilan) — bekor qilish oqimi uchun.
    single { OwnedQueuesPreferences(context = androidContext(), json = get()) }
    // AuthPreferences endi Json'ni inject qiladi — session JSON serialize
    // qilish uchun. Json `networkModule`'da `single` sifatida ro'yxatda.
    single { AuthPreferences(context = androidContext(), json = get()) }
    // "Eslab qolish" — driver form snapshot. AuthPreferences bilan bir xil
    // JSON+DataStore pattern. Json singleton reuse qilinadi.
    single { SavedDriverPreferences(context = androidContext(), json = get()) }

    // ===== Use cases =====
    factory { GetQueueListUseCase(repository = get()) }
    factory { ObserveQueueUpdatesUseCase(repository = get()) }
    factory { SubmitQueueUseCase(repository = get()) }
    factory { CancelOwnerQueueUseCase(repository = get()) }
    factory { LoginUseCase(repository = get()) }
    factory { GetScaleListUseCase(repository = get()) }
    // Navbat boshqaruvi use case'lari
    factory { GetQueueByDateUseCase(repository = get()) }
    factory { GetPermitsUseCase(repository = get()) }

    // ===== ViewModels =====
    viewModelOf(::QueueViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::ScaleViewModel)
    // Navbat boshqaruvi VM — MainScreen NavHost ichida har NavBackStackEntry
    // uchun yangi instance (ViewModelStoreOwner alohida bo'lgani uchun).
    viewModelOf(::QueueManagementViewModel)
}