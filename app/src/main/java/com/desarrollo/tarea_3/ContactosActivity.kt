package com.desarrollo.tarea_3

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class ContactosActivity : AppCompatActivity() {
    private val cola by lazy { Volley.newRequestQueue(applicationContext) }
    private val adaptador = ContactoAdapter(mutableListOf()) { confirmarEliminacion(it) }
    private lateinit var progreso: ProgressBar
    private lateinit var vacio: TextView
    private lateinit var mensaje: TextView
    private var eliminando = false
    private val crearContacto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val texto = resultado.data?.getStringExtra("mensaje") ?: "Contacto creado correctamente"
            mostrarMensaje(texto, true)
            aviso(texto)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contactos)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pantallaContactos)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        progreso = findViewById(R.id.progressContactos)
        vacio = findViewById(R.id.textContactosVacio)
        mensaje = findViewById(R.id.textMensajeContactos)
        findViewById<RecyclerView>(R.id.recyclerContactos).apply {
            layoutManager = LinearLayoutManager(this@ContactosActivity)
            adapter = adaptador
        }
        findViewById<Button>(R.id.buttonCrearContacto).setOnClickListener {
            crearContacto.launch(Intent(this, CrearContactoActivity::class.java))
        }
        findViewById<Button>(R.id.buttonVolverContactos).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        cargarContactos()
    }

    private fun cargarContactos() {
        progreso.visibility = View.VISIBLE
        val peticion = JsonObjectRequest(
            Request.Method.GET,
            URL_LISTAR,
            null,
            { respuesta ->
                progreso.visibility = View.GONE
                if (!respuesta.optBoolean("estado", false)) {
                    aviso(respuesta.optString("mensaje", "No se pudieron consultar los contactos"))
                    mostrarVacio()
                    return@JsonObjectRequest
                }
                val arreglo = respuesta.optJSONArray("contactos")
                if (arreglo == null) {
                    aviso("El servicio envió una respuesta inválida.")
                    mostrarVacio()
                    return@JsonObjectRequest
                }
                val lista = mutableListOf<Contacto>()
                for (i in 0 until arreglo.length()) {
                    val objeto = arreglo.optJSONObject(i) ?: continue
                    lista.add(
                        Contacto(
                            codContacto = entero(objeto, "cod_contacto"),
                            nombre = objeto.optString("nom_contacto"),
                            apellido = objeto.optString("ape_contacto"),
                            telefono = objeto.optString("telefono_contacto")
                        )
                    )
                }
                adaptador.reemplazar(lista)
                mostrarVacio()
            },
            { error ->
                progreso.visibility = View.GONE
                aviso(mensajeError(error, "No se pudo cargar la lista de contactos."))
                mostrarVacio()
            }
        ).apply {
            setShouldCache(false)
            retryPolicy = DefaultRetryPolicy(8000, 0, 1f)
        }
        cola.add(peticion)
    }

    private fun confirmarEliminacion(contacto: Contacto) {
        if (eliminando) return
        AlertDialog.Builder(this)
            .setTitle("Eliminar contacto")
            .setMessage("¿Eliminar a ${contacto.nombre} ${contacto.apellido}?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ -> eliminar(contacto) }
            .show()
    }

    private fun eliminar(contacto: Contacto) {
        eliminando = true
        val cuerpo = JSONObject().put("cod_contacto", contacto.codContacto)
        val peticion = JsonObjectRequest(
            Request.Method.POST,
            URL_ELIMINAR,
            cuerpo,
            { respuesta ->
                eliminando = false
                if (respuesta.optBoolean("estado", false)) {
                    adaptador.quitar(contacto.codContacto)
                    mostrarVacio()
                    val texto = respuesta.optString("mensaje", "Contacto eliminado correctamente")
                    mostrarMensaje(texto, true)
                    aviso(texto)
                } else {
                    val texto = respuesta.optString("mensaje", "No se pudo eliminar el contacto")
                    mostrarMensaje(texto, false)
                    aviso(texto)
                }
            },
            { error ->
                eliminando = false
                val texto = mensajeError(error, "No se pudo eliminar el contacto.")
                mostrarMensaje(texto, false)
                aviso(texto)
            }
        ).apply {
            setShouldCache(false)
            retryPolicy = DefaultRetryPolicy(8000, 0, 1f)
        }
        cola.add(peticion)
    }

    private fun entero(objeto: JSONObject, clave: String): Int {
        val valor = objeto.opt(clave)
        return when (valor) {
            is Number -> valor.toInt()
            is String -> valor.toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun mensajeError(error: com.android.volley.VolleyError, respaldo: String): String {
        val cuerpo = error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
        if (!cuerpo.isNullOrBlank()) {
            val mensaje = runCatching { JSONObject(cuerpo).optString("mensaje") }.getOrNull()
            if (!mensaje.isNullOrBlank()) return mensaje
        }
        return if (error.networkResponse == null) {
            "No se pudo conectar con el servicio."
        } else {
            respaldo
        }
    }

    private fun mostrarVacio() {
        vacio.visibility = if (adaptador.estaVacio()) View.VISIBLE else View.GONE
    }

    private fun mostrarMensaje(texto: String, correcto: Boolean) {
        mensaje.text = texto
        mensaje.setTextColor(Color.parseColor(if (correcto) "#166534" else "#9F1239"))
        mensaje.setBackgroundColor(Color.parseColor(if (correcto) "#DCFCE7" else "#FFE4E6"))
        mensaje.visibility = View.VISIBLE
    }

    private fun aviso(mensaje: String) {
        Toast.makeText(applicationContext, mensaje, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        cola.stop()
        super.onDestroy()
    }

    companion object {
        private const val URL_LISTAR = "http://192.168.56.56:8088/listar.php"
        private const val URL_ELIMINAR = "http://192.168.56.56:8088/eliminar.php"
    }
}
