package br.oi.nexjlock

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.Manifest

class MainActivity : Activity() {

    private class AppItem(val pkg: String, val name: String, val icon: Drawable)

    private var authed = false
    private var leftAt = 0L
    private var prompting = false
    private var apps: List<AppItem>? = null
    private lateinit var root: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG
        root = FrameLayout(this)
        root.setBackgroundColor(Ui.BG)
        setContentView(root)
        loadApps()
    }

    override fun onStart() {
        super.onStart()
        if (authed && SystemClock.elapsedRealtime() - leftAt > 60_000) authed = false
        render()
        if (Prefs.hasPin(this) && !authed && Prefs.bioOn(this) && Biometric.available(this)) askBio()
    }

    override fun onStop() {
        super.onStop()
        leftAt = SystemClock.elapsedRealtime()
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = pm.queryIntentActivities(main, 0)
                .filter { it.activityInfo.packageName != packageName }
                .distinctBy { it.activityInfo.packageName }
                .map { AppItem(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            runOnUiThread {
                apps = list
                if (authed) render()
            }
        }.start()
    }

    private fun render() {
        root.removeAllViews()
        when {
            !Prefs.hasPin(this) -> root.addView(setupView {
                authed = true
                render()
            })
            !authed -> root.addView(verifyView())
            else -> {
                root.addView(mainView())
                ensureService()
            }
        }
    }

    // ---------- PIN ----------
    private fun setupView(onDone: () -> Unit): View {
        var first: String? = null
        lateinit var pad: PinPadView
        pad = PinPadView(this, onSubmit = { p ->
            val f = first
            if (f == null) {
                first = p
                pad.reset()
                pad.setTitle("Repita o PIN")
                pad.setMessage("")
            } else if (f == p) {
                Prefs.setPin(this, p)
                onDone()
            } else {
                first = null
                pad.reset()
                pad.setTitle("Crie um PIN")
                pad.setMessage("Os PINs não coincidem. Tente de novo.")
            }
        })
        pad.setTitle("Crie um PIN")
        pad.setMessage("4 a 8 números. Protege seus apps e este painel.")
        return pad
    }

    private fun verifyView(): View {
        val bioReady = Prefs.bioOn(this) && Biometric.available(this)
        lateinit var pad: PinPadView
        pad = PinPadView(
            this,
            onSubmit = { p ->
                val err = PinGate.verify(this, p)
                if (err == null) {
                    authed = true
                    render()
                } else {
                    pad.reset()
                    pad.setMessage(err)
                }
            },
            onBio = if (bioReady) ({ askBio() }) else null
        )
        pad.setTitle("NexJ Lock")
        pad.setMessage("Digite seu PIN")
        return pad
    }

    private fun askBio() {
        if (prompting) return
        prompting = true
        Biometric.prompt(this, "Abrir NexJ Lock", onOk = {
            prompting = false
            authed = true
            render()
        }, onEnd = {
            prompting = false
        })
    }

    // ---------- painel ----------
    private fun mainView(): View {
        val sv = ScrollView(this)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(20), dp(16), dp(32))
        sv.addView(col)

        col.addView(label("NexJ Lock", 26f, bold = true))
        col.addView(label("Bloqueio de apps. Sem internet, sem anúncios.", 14f, Ui.MUTED))

        // proteção
        val on = Prefs.serviceOn(this)
        val status = card()
        status.addView(label(if (on) "Proteção ATIVA" else "Proteção DESLIGADA", 18f,
            if (on) Ui.GREEN else Ui.DANGER, true))
        status.addView(button(if (on) "Desligar proteção" else "Ligar proteção",
            if (on) Ui.DANGER else Ui.BLUE) { toggleService() })
        col.addView(status, gap())

        // permissões
        col.addView(section("Permissões"))
        col.addView(permRow("Acesso de uso (obrigatória)",
            "Para saber qual app foi aberto.", Perms.usage(this)) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }, gap())
        col.addView(permRow("Exibir sobre outros apps (obrigatória)",
            "Para mostrar a tela de PIN por cima.", Perms.overlay(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }, gap())
        col.addView(permRow("Notificações (recomendada)",
            "Mostra o aviso fixo de que a proteção está ligada.", Perms.notif(this)) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }, gap())
        col.addView(permRow("Sem limite de bateria (recomendada)",
            "Evita que o Android desligue a proteção.", Perms.battery(this)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")))
        }, gap())

        // opções
        col.addView(section("Opções"))
        val opts = card()
        if (Biometric.available(this)) {
            val sw = Switch(this)
            sw.text = "Desbloquear com biometria"
            sw.setTextColor(Ui.TEXT)
            sw.isChecked = Prefs.bioOn(this)
            sw.setOnCheckedChangeListener { _, v -> Prefs.setBioOn(this, v) }
            opts.addView(sw)
        } else {
            opts.addView(label("Biometria indisponível ou não cadastrada neste aparelho.", 14f, Ui.MUTED))
        }
        val relockBtn = button("Bloquear de novo: " + Prefs.RELOCK_LABELS[Prefs.relockIndex(this)], Ui.CARD) {
            val next = (Prefs.relockIndex(this) + 1) % Prefs.RELOCK_SECONDS.size
            Prefs.setRelockIndex(this, next)
            render()
        }
        opts.addView(relockBtn)
        opts.addView(label("Tempo em que o app continua liberado depois que você sai dele.", 12f, Ui.MUTED))
        opts.addView(button("Alterar PIN", Ui.CARD) {
            root.removeAllViews()
            root.addView(setupView {
                toast("PIN alterado.")
                render()
            })
        })
        col.addView(opts, gap())

        // apps
        col.addView(section("Apps protegidos"))
        val list = apps
        if (list == null) {
            col.addView(label("Carregando apps...", 14f, Ui.MUTED))
        } else {
            val locked = Prefs.lockedApps(this)
            val sorted = list.sortedWith(
                compareBy<AppItem>({ it.pkg !in locked }, { it.name.lowercase() })
            )
            for (a in sorted) col.addView(appRow(a, a.pkg in locked), gap(6))
        }
        return sv
    }

    private fun appRow(a: AppItem, locked: Boolean): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = bg(Ui.CARD, 12)
        row.setPadding(dp(12), dp(10), dp(12), dp(10))

        val iv = ImageView(this)
        iv.setImageDrawable(a.icon)
        row.addView(iv, LinearLayout.LayoutParams(dp(40), dp(40)))

        val name = label(a.name, 16f)
        val nlp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        nlp.leftMargin = dp(12)
        row.addView(name, nlp)

        val sw = Switch(this)
        sw.isChecked = locked
        sw.setOnCheckedChangeListener { _, v -> Prefs.setLocked(this, a.pkg, v) }
        row.addView(sw)
        row.setOnClickListener { sw.toggle() }
        return row
    }

    private fun permRow(title: String, desc: String, ok: Boolean, action: () -> Unit): View {
        val c = card()
        c.addView(label(title, 15f, bold = true))
        c.addView(label(desc, 12f, Ui.MUTED))
        if (ok) {
            c.addView(label("Concedida", 13f, Ui.GREEN, true))
        } else {
            c.addView(button("Conceder", Ui.BLUE) { action() })
        }
        return c
    }

    private fun toggleService() {
        if (Prefs.serviceOn(this)) {
            Prefs.setServiceOn(this, false)
            LockService.stop(this)
        } else {
            if (!Perms.usage(this) || !Perms.overlay(this)) {
                toast("Conceda as duas permissões obrigatórias primeiro.")
                return
            }
            Prefs.setServiceOn(this, true)
            LockService.start(this)
        }
        render()
    }

    private fun ensureService() {
        if (Prefs.serviceOn(this) && Perms.usage(this) && Perms.overlay(this) &&
            LockService.instance == null
        ) {
            LockService.start(this)
        }
    }

    // ---------- pequenos helpers de layout ----------
    private fun card(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.background = bg(Ui.CARD, 14)
        l.setPadding(dp(14), dp(12), dp(14), dp(12))
        return l
    }

    private fun section(t: String): View {
        val v = label(t, 13f, Ui.MUTED, true)
        v.setPadding(0, dp(22), 0, dp(6))
        return v
    }

    private fun gap(top: Int = 8): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        p.topMargin = dp(top)
        return p
    }

    private fun button(t: String, color: Int, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = t
        b.isAllCaps = false
        b.setTextColor(Ui.TEXT)
        b.background = bg(color, 12)
        b.setOnClickListener { onClick() }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46)
        )
        lp.topMargin = dp(8)
        b.layoutParams = lp
        return b
    }
}
