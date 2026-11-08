package br.oi.nexjlock

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Teclado numérico reutilizado na tela de bloqueio e no painel. */
class PinPadView(
    ctx: Context,
    private val onSubmit: (String) -> Unit,
    private val onBio: (() -> Unit)? = null
) : LinearLayout(ctx) {

    private val pin = StringBuilder()
    val header = LinearLayout(ctx)
    private val title: TextView = ctx.label("", 20f, bold = true)
    private val dots: TextView = ctx.label("", 26f)
    private val msg: TextView = ctx.label("", 14f, Ui.MUTED)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Ui.BG)
        setPadding(ctx.dp(16), ctx.dp(24), ctx.dp(16), ctx.dp(24))

        header.orientation = VERTICAL
        header.gravity = Gravity.CENTER_HORIZONTAL
        title.gravity = Gravity.CENTER
        dots.gravity = Gravity.CENTER
        msg.gravity = Gravity.CENTER

        addView(header)
        addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(dots, LayoutParams(LayoutParams.MATCH_PARENT, ctx.dp(44)))
        addView(msg, LayoutParams(LayoutParams.MATCH_PARENT, ctx.dp(32)))

        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
        for (r in rows) {
            val row = newRow()
            for (d in r) row.addView(key(d) { digit(d) })
            addView(row)
        }
        val last = newRow()
        if (onBio != null) {
            last.addView(key("Digital", 13f) { onBio.invoke() })
        } else {
            last.addView(placeholder())
        }
        last.addView(key("0") { digit("0") })
        last.addView(key("←") { back() })
        addView(last)

        val ok = Button(ctx)
        ok.text = "Confirmar"
        ok.isAllCaps = false
        ok.textSize = 17f
        ok.setTextColor(Ui.TEXT)
        ok.background = ctx.bg(Ui.BLUE, 28)
        ok.setOnClickListener { submit() }
        val lp = LayoutParams(ctx.dp(246), ctx.dp(50))
        lp.topMargin = ctx.dp(8)
        addView(ok, lp)

        refresh()
    }

    private fun newRow(): LinearLayout {
        val row = LinearLayout(context)
        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER
        return row
    }

    private fun cellParams(): LayoutParams {
        val p = LayoutParams(context.dp(66), context.dp(66))
        p.setMargins(context.dp(8), context.dp(5), context.dp(8), context.dp(5))
        return p
    }

    private fun placeholder(): View {
        val v = View(context)
        v.layoutParams = cellParams()
        return v
    }

    private fun key(t: String, size: Float = 22f, onClick: () -> Unit): Button {
        val b = Button(context)
        b.text = t
        b.textSize = size
        b.isAllCaps = false
        b.setTextColor(Ui.TEXT)
        b.background = context.bg(Ui.CARD, 40)
        b.layoutParams = cellParams()
        b.setOnClickListener { onClick() }
        return b
    }

    private fun digit(d: String) {
        if (pin.length < 8) {
            pin.append(d)
            refresh()
        }
    }

    private fun back() {
        if (pin.isNotEmpty()) {
            pin.deleteCharAt(pin.length - 1)
            refresh()
        }
    }

    private fun submit() {
        if (pin.length < 4) {
            setMessage("Use pelo menos 4 dígitos.")
            return
        }
        onSubmit(pin.toString())
    }

    private fun refresh() {
        dots.text = if (pin.isEmpty()) " " else List(pin.length) { "●" }.joinToString(" ")
    }

    fun reset() {
        pin.setLength(0)
        refresh()
    }

    fun setTitle(s: String) {
        title.text = s
    }

    fun setMessage(s: String) {
        msg.text = s
    }
}
