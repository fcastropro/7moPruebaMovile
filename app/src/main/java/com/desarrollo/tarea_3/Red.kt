package com.desarrollo.tarea_3

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

object Red {
    const val BASE = "http://192.168.56.56:8088/"

    fun encabezados(context: Context): MutableMap<String, String> {
        val encabezados = mutableMapOf("Accept" to "application/json")
        Sesion.leer(context)?.let {
            encabezados["Authorization"] = "Bearer $it"
            encabezados["X-Sesion"] = it
        }
        return encabezados
    }

    fun json(
        context: Context,
        cola: RequestQueue,
        metodo: Int,
        ruta: String,
        cuerpo: JSONObject?,
        alResponder: (JSONObject) -> Unit,
        alFallar: (String) -> Unit
    ) {
        val peticion = object : JsonObjectRequest(
            metodo,
            BASE + ruta,
            cuerpo,
            { alResponder(it) },
            { alFallar(mensaje(it)) }
        ) {
            override fun getHeaders(): MutableMap<String, String> = encabezados(context)
        }.apply {
            setShouldCache(false)
            retryPolicy = DefaultRetryPolicy(8000, 0, 1f)
        }
        cola.add(peticion)
    }

    fun mensaje(error: VolleyError): String {
        val crudo = error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
        val mensaje = crudo?.let { runCatching { JSONObject(it).optString("mensaje") }.getOrNull() }
        if (!mensaje.isNullOrBlank()) return mensaje
        return if (error.networkResponse == null) {
            "No se pudo conectar con el servicio."
        } else {
            "No se pudo completar la operación."
        }
    }
}
