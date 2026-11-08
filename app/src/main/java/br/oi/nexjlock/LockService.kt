package br.oi.nexjlock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * Serviço em primeiro plano que descobre qual app está na tela (Acesso de uso)
 * e abre a tela de PIN por cima (permissão de sobreposição).
 */
class LockService : Service() {

    companion object {
        private const val CHANNEL = "nexjlock"
        private const val POLL_MS = 300L

        @Volatile
        var instance: LockService? = null

        fun start(c: Context) {
            c.startForegroundService(Intent(c, LockService::class.java))
        }

        fun stop(c: Context) {
            c.stopService(Intent(c, LockService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastQuery = 0L
    private var current: String? = null
    private val unlocked = HashSet<String>()
    private val leftAt = HashMap<String, Long>()
    private var lastShown = 0L
    private var lastShownPkg: String? = null

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            unlocked.clear()
            leftAt.clear()
            current = null
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            try {
                poll()
            } catch (e: Exception) {
                // nunca derruba o serviço
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val ch = NotificationChannel(CHANNEL, "NexJ Lock ativo", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("NexJ Lock ativo")
            .setContentText("Protegendo seus apps")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }

        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
        lastQuery = System.currentTimeMillis()
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        try {
            unregisterReceiver(screenOff)
        } catch (e: Exception) {
        }
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Chamado pela tela de PIN quando o usuário acerta. */
    fun unlock(pkg: String) {
        unlocked.add(pkg)
        leftAt.remove(pkg)
        current = pkg
    }

    @Suppress("DEPRECATION")
    private fun poll() {
        val now = System.currentTimeMillis()
        val usm = getSystemService(UsageStatsManager::class.java)
        val events = usm.queryEvents(lastQuery - 1000, now)
        lastQuery = now
        val e = UsageEvents.Event()
        var pkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) pkg = e.packageName
        }
        if (pkg != null) onForeground(pkg, now)
    }

    private fun onForeground(pkg: String, now: Long) {
        if (pkg == packageName) return // nossa própria tela de PIN / painel

        if (pkg != current) {
            current?.let { if (it in unlocked) leftAt[it] = now }
            current = pkg
            val left = leftAt.remove(pkg)
            if (left != null && now - left > Prefs.relockMs(this)) unlocked.remove(pkg)
        }

        if (pkg in Prefs.lockedApps(this) && pkg !in unlocked) showLock(pkg, now)
    }

    private fun showLock(pkg: String, now: Long) {
        if (pkg == lastShownPkg && now - lastShown < 700) return
        lastShown = now
        lastShownPkg = pkg
        val i = Intent(this, LockActivity::class.java)
            .putExtra("pkg", pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(i)
    }
}
