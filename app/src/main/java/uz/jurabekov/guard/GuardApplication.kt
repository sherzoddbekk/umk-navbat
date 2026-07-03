package uz.jurabekov.guard

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import uz.jurabekov.guard.core.di.appModule
import uz.jurabekov.guard.core.di.networkModule

/**
 * Application entrypoint.
 *
 * v1.0: faqat DI initialization. Notification, foreground service yo'q.
 * v1.1: FCM initialization shu yerga qo'shiladi (FirebaseApp.initializeApp +
 *       token registratsiyasi).
 */
class GuardApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@GuardApplication)
            modules(networkModule, appModule)
        }
    }
}
