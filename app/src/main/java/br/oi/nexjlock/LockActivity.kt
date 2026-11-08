package br.oi.nexjlock

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout

/** Tela de PIN/biometria que aparece por cima do app protegido. */
class LockActivity : Activity() {

    private var target = ""
    private var prompting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG
        bind(intent.getStringExtra("pkg"))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        bind(intent?.getStringExtra("pkg"))
    }

    private fun bind(pkg: String?) {
        if (pkg == null) {
            finish()
            return
        }
        target = pkg
        val pm = packageManager
        val name = try {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
        val icon: Drawable? = try {
            pm.getApplicationIcon(pkg)
        } catch (e: Exception) {
            null
        }

        val bioReady = Prefs.bioOn(this) && Biometric.available(this)
        lateinit var pad: PinPadView
        pad = PinPadView(
            this,
            onSubmit = { pin ->
                val err = PinGate.verify(this, pin)
                if (err == null) {
                    success()
                } else {
                    pad.reset()
                    pad.setMessage(err)
                }
            },
            onBio = if (bioReady) ({ askBio() }) else null
        )

        if (icon != null) {
            val iv = ImageView(this)
            iv.setImageDrawable(icon)
            val lp = LinearLayout.LayoutParams(dp(64), dp(64))
            lp.bottomMargin = dp(12)
            pad.header.removeAllViews()
            pad.header.addView(iv, lp)
        }
        pad.setTitle("Desbloquear $name")
        pad.setMessage("Digite seu PIN")
        setContentView(pad)

        if (bioReady) Handler(Looper.getMainLooper()).postDelayed({ askBio() }, 250)
    }

    private fun askBio() {
        if (prompting) return
        prompting = true
        Biometric.prompt(this, "Desbloquear app", onOk = {
            prompting = false
            success()
        }, onEnd = {
            prompting = false
        })
    }

    private fun success() {
        LockService.instance?.unlock(target)
        finish()
    }

    @Deprecated("Voltar leva para a tela inicial, nunca para o app protegido")
    override fun onBackPressed() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(home)
        finish()
    }
}
