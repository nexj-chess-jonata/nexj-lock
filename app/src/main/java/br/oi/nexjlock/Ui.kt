package br.oi.nexjlock

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import android.widget.Toast

object Ui {
    val BG = Color.rgb(10, 12, 28)
    val CARD = Color.rgb(24, 31, 56)
    val BLUE = Color.rgb(45, 130, 255)
    val TEXT = Color.rgb(240, 244, 255)
    val MUTED = Color.rgb(150, 165, 200)
    val GREEN = Color.rgb(70, 200, 120)
    val DANGER = Color.rgb(200, 50, 60)
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.bg(color: Int, radiusDp: Int = 14): GradientDrawable {
    val d = GradientDrawable()
    d.setColor(color)
    d.cornerRadius = dp(radiusDp).toFloat()
    return d
}

fun Context.label(t: String, size: Float = 16f, color: Int = Ui.TEXT, bold: Boolean = false): TextView {
    val v = TextView(this)
    v.text = t
    v.textSize = size
    v.setTextColor(color)
    if (bold) v.setTypeface(v.typeface, Typeface.BOLD)
    return v
}

fun Context.toast(t: String) {
    Toast.makeText(this, t, Toast.LENGTH_LONG).show()
}
