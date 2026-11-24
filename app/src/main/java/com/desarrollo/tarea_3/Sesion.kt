package com.desarrollo.tarea_3

import android.content.Context

object Sesion {
    private const val ARCHIVO = "agenda_sesion"
    private const val TOKEN = "token"

    fun guardar(context: Context, token: String) {
        preferencias(context).edit().putString(TOKEN, token).apply()
    }

    fun leer(context: Context): String? {
        return preferencias(context).getString(TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    fun cerrar(context: Context) {
        preferencias(context).edit().clear().apply()
    }

    private fun preferencias(context: Context) =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
}
