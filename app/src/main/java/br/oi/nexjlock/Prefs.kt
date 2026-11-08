package br.oi.nexjlock

import android.content.Context
import android.os.SystemClock
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Prefs {
    val RELOCK_SECONDS = intArrayOf(0, 5, 30, 120)
    val RELOCK_LABELS = arrayOf("Imediato", "5 segundos", "30 segundos", "2 minutos")

    private fun sp(c: Context) = c.getSharedPreferences("nexjlock", Context.MODE_PRIVATE)

    private fun hash(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)).encoded

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }

    private fun unhex(s: String) =
        ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    fun hasPin(c: Context) = sp(c).contains("pin_hash")

    fun setPin(c: Context, pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        sp(c).edit()
            .putString("pin_salt", hex(salt))
            .putString("pin_hash", hex(hash(pin, salt)))
            .apply()
    }

    fun checkPin(c: Context, pin: String): Boolean {
        val s = sp(c)
        val salt = s.getString("pin_salt", null) ?: return false
        val h = s.getString("pin_hash", null) ?: return false
        return MessageDigest.isEqual(hash(pin, unhex(salt)), unhex(h))
    }

    fun lockedApps(c: Context): Set<String> =
        HashSet(sp(c).getStringSet("locked", emptySet()) ?: emptySet())

    fun setLocked(c: Context, pkg: String, on: Boolean) {
        val set = HashSet(lockedApps(c))
        if (on) set.add(pkg) else set.remove(pkg)
        sp(c).edit().putStringSet("locked", set).apply()
    }

    fun serviceOn(c: Context) = sp(c).getBoolean("service_on", false)
    fun setServiceOn(c: Context, v: Boolean) = sp(c).edit().putBoolean("service_on", v).apply()

    fun bioOn(c: Context) = sp(c).getBoolean("bio_on", false)
    fun setBioOn(c: Context, v: Boolean) = sp(c).edit().putBoolean("bio_on", v).apply()

    fun relockIndex(c: Context) = sp(c).getInt("relock_idx", 1).coerceIn(0, RELOCK_SECONDS.size - 1)
    fun setRelockIndex(c: Context, i: Int) = sp(c).edit().putInt("relock_idx", i).apply()
    fun relockMs(c: Context): Long = RELOCK_SECONDS[relockIndex(c)] * 1000L
}

/** Controle de tentativas erradas, compartilhado por todas as telas de PIN. */
object PinGate {
    private var fails = 0
    private var blockedUntil = 0L

    /** Retorna null se o PIN está certo; senão, a mensagem de erro. */
    fun verify(c: Context, pin: String): String? {
        val now = SystemClock.elapsedRealtime()
        if (now < blockedUntil) return "Aguarde ${(blockedUntil - now) / 1000 + 1}s."
        if (Prefs.checkPin(c, pin)) {
            fails = 0
            return null
        }
        fails++
        if (fails >= 5) {
            fails = 0
            blockedUntil = now + 30_000
            return "Muitas tentativas. Aguarde 30s."
        }
        return "PIN incorreto."
    }
}
