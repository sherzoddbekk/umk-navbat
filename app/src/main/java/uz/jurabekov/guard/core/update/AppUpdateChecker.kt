package uz.jurabekov.guard.core.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Google In-App Updates API'ni o'rab oladi.
 *
 * Strategiyalar:
 *  - [FORCE_ALL_UPDATES] = true  →  Har qanday update majburiy
 *      (IMMEDIATE rejim - user yangilashsiz davom eta olmaydi)
 *  - [FORCE_ALL_UPDATES] = false →  Play Console'dagi `inAppUpdatePriority`
 *      asosida: priority >= 4 → IMMEDIATE, aks holda FLEXIBLE
 *
 * IMMEDIATE rejim Google'ning rasmiy "force update" mexanizmidir va
 * Play Store policy'ga to'liq mos.
 *
 * Cheklovlar:
 *  - Play Store'dan yuklab olingan app'lardagina ishlaydi (sideload emas)
 *  - Telefonda Google Play Services bo'lishi shart
 *  - Bir xil signing key bilan imzolangan versiyalardagina ishlaydi
 */
class AppUpdateChecker(
    private val activity: ComponentActivity,
    private val onFlexibleUpdateReady: () -> Unit,
    private val onImmediateUpdateCancelled: () -> Unit
) {

    private val appUpdateManager: AppUpdateManager =
        AppUpdateManagerFactory.create(activity)

    @Volatile
    private var isImmediateFlowActive: Boolean = false

    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Log.w(TAG, "update flow cancelled or failed: code=${result.resultCode}")
                if (isImmediateFlowActive) {
                    onImmediateUpdateCancelled()
                }
            }
            isImmediateFlowActive = false
        }

    private val flexibleInstallListener: InstallStateUpdatedListener =
        InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> {
                    Log.i(TAG, "flexible update downloaded - ready to install")
                    onFlexibleUpdateReady()
                }
                InstallStatus.FAILED -> {
                    Log.w(TAG, "flexible update failed: code=${state.installErrorCode()}")
                    appUpdateManager.unregisterListener(flexibleInstallListener)
                }
                InstallStatus.INSTALLED -> {
                    appUpdateManager.unregisterListener(flexibleInstallListener)
                }
                else -> { /* DOWNLOADING, PENDING - skip */ }
            }
        }

    /**
     * Asosiy entry point. MainActivity.onCreate()'da chaqiring.
     * Network/Play Services xatolari soft-fail qiladi - app davom etadi.
     */
    fun checkForUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info -> handleUpdateInfo(info) }
            .addOnFailureListener { e ->
                Log.w(TAG, "appUpdateInfo failed (non-fatal): ${e.message}")
            }
    }

    /**
     * MainActivity.onResume()'da chaqirish.
     *
     * 2 ta holat bor:
     *  1. IMMEDIATE update yarim qoldi (user boshqa app'ga o'tib qaytdi) → resume
     *  2. FLEXIBLE update yuklab bo'lgan, lekin install qilinmagan → snackbar
     */
    fun resumeUpdateIfInProgress() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() ==
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    Log.i(TAG, "immediate update was in progress - resuming")
                    startUpdateFlow(info, AppUpdateType.IMMEDIATE)
                }

                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    onFlexibleUpdateReady()
                }
            }
    }

    /**
     * Flexible update yuklab olindi → user "Qayta ishga tushir" dedi.
     * App avtomatik o'chib yana yoqiladi va yangi versiya o'rnatiladi.
     */
    fun completeFlexibleUpdate() {
        appUpdateManager.completeUpdate()
    }

    /**
     * MainActivity.onDestroy()'da chaqirish - listener leak'ning oldini olish.
     */
    fun cleanup() {
        try {
            appUpdateManager.unregisterListener(flexibleInstallListener)
        } catch (_: Exception) { /* not registered */ }
    }

    // ───────────────────────────────────────────────────────────────────

    private fun handleUpdateInfo(info: AppUpdateInfo) {
        if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
            Log.d(TAG, "no update available")
            return
        }

        val priority = info.updatePriority()

        // FORCE_ALL_UPDATES=true bo'lsa - har qanday updateni IMMEDIATE qilamiz
        // (Play Console'da priority belgilashga hojat yo'q)
        val updateType = when {
            FORCE_ALL_UPDATES -> AppUpdateType.IMMEDIATE
            priority >= HIGH_PRIORITY_THRESHOLD -> AppUpdateType.IMMEDIATE
            else -> AppUpdateType.FLEXIBLE
        }

        Log.i(TAG, "update available: priority=$priority, type=$updateType")

        if (!info.isUpdateTypeAllowed(updateType)) {
            // Qurilma bu turdagi update'ni qo'llab-quvvatlamaydi
            // (juda kam holat - masalan instant app)
            Log.w(TAG, "update type $updateType not allowed by Play Store")
            return
        }

        startUpdateFlow(info, updateType)
    }

    private fun startUpdateFlow(info: AppUpdateInfo, type: Int) {
        try {
            if (type == AppUpdateType.FLEXIBLE) {
                appUpdateManager.registerListener(flexibleInstallListener)
            } else {
                isImmediateFlowActive = true
            }

            appUpdateManager.startUpdateFlowForResult(
                info,
                updateLauncher,
                AppUpdateOptions.newBuilder(type).build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "startUpdateFlowForResult failed: ${e.message}", e)
            isImmediateFlowActive = false
        }
    }

    companion object {
        private const val TAG = "AppUpdateChecker"

        /**
         * Sizning talab: "user ilovani yangilamaguncha ishlata olmasin"
         *
         * `true`  → har qanday yangi versiya majburiy (IMMEDIATE).
         *           Play Console'da hech qanday qo'shimcha sozlash kerak emas,
         *           shunchaki yangi AAB yuklang va versionCode'ni oshiring.
         *
         * `false` → Play Console'dagi `inAppUpdatePriority` (0-5) asosida ishlaydi.
         *           Oddiy update'lar FLEXIBLE bo'ladi, faqat priority>=4 da
         *           IMMEDIATE chiqadi. Bu kelajakda kerak bo'lsa, true → false.
         */
        private const val FORCE_ALL_UPDATES = true

        private const val HIGH_PRIORITY_THRESHOLD = 4
    }
}

/**
 * Foydalanuvchini Play Store sahifasiga yo'naltirish (manual link).
 * Asosiy update flow Google API orqali ishlaydi - bu utility kelajakda
 * "Bizni baholang" yoki "Yangilash" link'lari uchun foydali.
 */
fun openPlayStorePage(activity: Activity, packageName: String = activity.packageName) {
    val marketUri = Uri.parse("market://details?id=$packageName")
    val intent = Intent(Intent.ACTION_VIEW, marketUri).apply {
        setPackage("com.android.vending")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    try {
        activity.startActivity(intent)
    } catch (_: Exception) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        activity.startActivity(webIntent)
    }
}
