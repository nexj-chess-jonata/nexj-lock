package br.oi.nexjlock

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.PowerManager
import android.os.Process

object Perms {
    fun usage(c: Context): Boolean {
        val ops = c.getSystemService(AppOpsManager::class.java)
        return ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun overlay(c: Context): Boolean = android.provider.Settings.canDrawOverlays(c)

    fun notif(c: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun battery(c: Context): Boolean =
        c.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(c.packageName)
}

object Biometric {
    @Suppress("DEPRECATION")
    fun available(c: Context): Boolean {
        val m = c.getSystemService(BiometricManager::class.java) ?: return false
        return m.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun prompt(a: Activity, title: String, onOk: () -> Unit, onEnd: () -> Unit = {}) {
        try {
            val p = BiometricPrompt.Builder(a)
                .setTitle(title)
                .setNegativeButton("Usar PIN", a.mainExecutor, DialogInterface.OnClickListener { _, _ -> onEnd() })
                .build()
            p.authenticate(CancellationSignal(), a.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    onOk()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    onEnd()
                }
            })
        } catch (e: Exception) {
            onEnd()
        }
    }
}
