/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest

/**
 * App lock helpers: PIN hashing and the device screen lock prompt.
 *
 * The PIN hashing is adapted from June — DenserMeerkat/June (GPL-3.0).
 */
object SecurityUtils {
    /**
     * The PIN is never stored, only this hash. A 4 or 6 digit PIN has too few
     * combinations to survive an offline attack on the raw value, but hashing
     * keeps it out of backups and out of plain sight.
     */
    fun hashPin(pin: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }

    /**
     * Biometric or device credential. BIOMETRIC_WEAK rather than STRONG because
     * the lock guards the UI, not a key: nothing here is bound to a CryptoObject,
     * so a Class 2 face unlock is as good as a Class 3 fingerprint. It is also
     * the only biometric | credential combination androidx.biometric supports on
     * every API level. STRONG | DEVICE_CREDENTIAL is rejected outright on
     * Android 9 and 10: canAuthenticate() answers BIOMETRIC_ERROR_UNSUPPORTED
     * without looking at the device, and PromptInfo.Builder.build() throws. That
     * showed up as "set up a screen lock first" on a phone that had one.
     */
    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /**
     * True when the device can actually authenticate, either with a biometric
     * or with the screen lock PIN, pattern or password.
     */
    fun canUseScreenLock(context: Context): Boolean =
        BiometricManager
            .from(context)
            .canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Shows the system authentication sheet. Device credential is allowed
     * alongside biometrics, which is what "same as screen lock" means.
     */
    fun showScreenLockPrompt(
        activity: FragmentActivity,
        title: String,
        onSuccess: () -> Unit,
        onError: (CharSequence) -> Unit,
    ) {
        val prompt =
            BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        onError(errString)
                    }

                    override fun onAuthenticationFailed() {
                        // A rejected finger is not fatal, the sheet stays up.
                    }
                },
            )

        prompt.authenticate(
            BiometricPrompt
                .PromptInfo
                .Builder()
                .setTitle(title)
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build(),
        )
    }
}
