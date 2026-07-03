package uz.jurabekov.guard.core.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import uz.jurabekov.guard.BuildConfig
import uz.jurabekov.guard.core.network.AuthInterceptor
import uz.jurabekov.guard.core.util.Constants
import uz.jurabekov.guard.data.remote.api.AuthApi
import uz.jurabekov.guard.data.remote.api.QueueApi
import uz.jurabekov.guard.data.remote.api.ScaleApi
import uz.jurabekov.guard.data.remote.websocket.QueuePusherClient
import java.util.concurrent.TimeUnit

/**
 * Network qatlamining barcha singleton'lari.
 *
 * Interceptor zanjiri:
 *   1. AuthInterceptor  → Bearer token (login pathlardan tashqari)
 *   2. HttpLoggingInterceptor → log (release'da NONE)
 *
 * `Json` single — `AuthPreferences` da session JSON serialize uchun ham
 * ishlatiladi (`AppModule`'da inject qilinadi).
 */
val networkModule = module {

    single<Json> {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
            isLenient = true
        }
    }

    single<HttpLoggingInterceptor> {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    single<AuthInterceptor> {
        AuthInterceptor(preferences = get())
    }

    single<OkHttpClient> {
        OkHttpClient.Builder()
            .connectTimeout(Constants.NETWORK_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(Constants.NETWORK_READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(Constants.NETWORK_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(get<AuthInterceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }

    single<Retrofit> {
        val json = get<Json>()
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    // ===== API services =====
    single<QueueApi> { get<Retrofit>().create(QueueApi::class.java) }
    single<AuthApi> { get<Retrofit>().create(AuthApi::class.java) }
    single<ScaleApi> { get<Retrofit>().create(ScaleApi::class.java) }

    // ===== WebSocket =====
    single<QueuePusherClient> { QueuePusherClient(json = get()) }
}
