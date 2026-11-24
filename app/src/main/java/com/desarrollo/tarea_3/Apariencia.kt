package com.desarrollo.tarea_3

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView

internal fun TextView.pintarResultado(texto: String, correcto: Boolean) {
    this.text = texto
    setTextColor(Color.parseColor(if (correcto) "#15803D" else "#BE123C"))
    background = GradientDrawable().apply {
        cornerRadius = 16f * resources.displayMetrics.density
        setColor(Color.parseColor(if (correcto) "#DCFCE7" else "#FFE4E6"))
    }
    visibility = View.VISIBLE
}
