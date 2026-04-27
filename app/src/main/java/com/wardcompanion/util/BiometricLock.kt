package com.wardcompanion.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Quick wrapper for fingerprint / face unlock. Call [authenticate] from
 * MainActivity (which must extend FragmentActivity for BiometricPrompt
 * to work — easiest fix is to use AppCompatActivity, but for Compose we
 * stick with ComponentActivity and use this only when needed).
 *
 * Note: ComponentActivity ⊂ FragmentActivity, so casting works.
 */
object BiometricLock {

    fun isAvailable(activity: FragmentActivity): Boolean {
        val mgr = BiometricManager.from(activity)
        return mgr.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
                or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onCancel: () -> Unit = {},
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onCancel()
                }
            },
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Ward Companion")
            .setSubtitle("Unlock to view patient data")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
                    or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()

        prompt.authenticate(info)
    }
}
