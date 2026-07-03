# ============================================================
# Crash stack trace'larni tanib olish uchun (Play Console crash report)
# ============================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Generic signature'lar (Retrofit response type'larini topish uchun)
-keepattributes Signature, Exceptions, *Annotation*, InnerClasses, EnclosingMethod

# ============================================================
# Kotlinx Serialization
# ============================================================
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Companion object'lardagi serializer() metodi
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Internal $serializer class'lari
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Loyihaga tegishli barcha @Serializable class'lar (DTO + domain)
-keep,includedescriptorclasses class uz.jurabekov.guard.**$$serializer { *; }
-keepclassmembers class uz.jurabekov.guard.** {
    *** Companion;
}
-keepclasseswithmembers class uz.jurabekov.guard.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# DTO va domain model'lar reflection orqali ishlatiladi
-keep class uz.jurabekov.guard.data.remote.dto.** { *; }
-keep class uz.jurabekov.guard.domain.model.** { *; }

# ============================================================
# Retrofit
# ============================================================
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Retrofit interface'lar (suspend fun bilan)
-keep,allowobfuscation interface uz.jurabekov.guard.data.remote.api.** { *; }

# Retrofit ichki ehtiyojlari
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit

# Retrofit checks (kotlin reflection)
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# ============================================================
# OkHttp / Okio
# ============================================================
-dontwarn okhttp3.internal.platform.**
-dontwarn okhttp3.internal.platform.android.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okio.**

# ============================================================
# Pusher Java Client (WebSocket)
# Pusher Gson va reflection ishlatadi - hammasini saqlaymiz
# ============================================================
-keep class com.pusher.** { *; }
-keep interface com.pusher.** { *; }
-keepclassmembers class com.pusher.** { *; }
-dontwarn com.pusher.**

# Pusher ichidagi Gson dependency
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keepattributes EnclosingMethod
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# Pusher tomonidan ishlatiladigan Java-WebSocket
-keep class org.java_websocket.** { *; }
-dontwarn org.java_websocket.**

# Pusher slf4j (kerak emas, lekin warning'larni jim qilish uchun)
-dontwarn org.slf4j.**

# ============================================================
# Koin DI
# ============================================================
-keep class org.koin.** { *; }
-keep interface org.koin.** { *; }
-dontwarn org.koin.**

# ViewModel constructor'larini saqlaymiz (Koin reflection orqali topadi)
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# ============================================================
# Coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    *** invoke(...);
}
-keepclassmembers class kotlinx.coroutines.android.AndroidDispatcherFactory {
    *** invoke(...);
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.flow.**

# ============================================================
# AndroidX DataStore
# ============================================================
-keep class androidx.datastore.*.** { *; }

# ============================================================
# Compose
# ============================================================
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# ============================================================
# AndroidX Navigation
# ============================================================
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ============================================================
# Kotlin Reflection (kotlinx.serialization ishlatadi)
# ============================================================
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ============================================================
# Loyiha umumiy qoidalari
# ============================================================
# Application va Activity'larni saqlash
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Enum'larni saqlash
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
