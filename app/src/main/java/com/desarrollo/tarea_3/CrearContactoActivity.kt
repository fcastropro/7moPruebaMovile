package com.desarrollo.tarea_3

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class CrearContactoActivity : AppCompatActivity() {
    private val cola by lazy { Volley.newRequestQueue(applicationContext) }
    private var guardando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crear_contacto)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pantallaCrearContacto)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<Button>(R.id.buttonGuardarContacto).setOnClickListener { guardar() }
        findViewById<Button>(R.id.buttonVolverCrear).setOnClickListener { finish() }
    }

    private fun guardar() {
        if (guardando) return
        currentFocus?.let { vista ->
            getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(vista.windowToken, 0)
        }
        val nombre = findViewById<EditText>(R.id.editTextNombreContacto).text.toString().trim()
        val apellido = findViewById<EditText>(R.id.editTextApellidoContacto).text.toString().trim()
        val telefono = findViewById<EditText>(R.id.editTextTelefonoContacto).text.toString().trim()
        val email = findViewById<EditText>(R.id.editTextEmailContacto).text.toString().trim()
        if (nombre.isEmpty() || apellido.isEmpty()) {
            mostrar("El nombre y el apellido son obligatorios.", false)
            return
        }
        guardando = true
        val cuerpo = JSONObject()
            .put("nom_contacto", nombre)
            .put("ape_contacto", apellido)
            .put("telefono_contacto", telefono)
            .put("email_contacto", email)
        val peticion = object : StringRequest(
            Request.Method.POST,
            URL_CREAR,
            { cuerpoRespuesta ->
                guardando = false
                val respuesta = runCatching { JSONObject(cuerpoRespuesta.trim()) }.getOrNull()
                if (respuesta?.optBoolean("estado", false) == true) {
                    val texto = respuesta.optString("mensaje").ifBlank { "Contacto creado correctamente" }
                    setResult(RESULT_OK, Intent().putExtra("mensaje", texto))
                    finish()
                } else {
                    val texto = respuesta?.optString("mensaje")?.takeIf { it.isNotBlank() }
                        ?: "No se pudo crear el contacto."
                    mostrar(texto, false)
                }
            },
            { error ->
                guardando = false
                val crudo = error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
                Log.e(
                    "CrearContacto",
                    "tipo=${error.javaClass.simpleName} codigo=${error.networkResponse?.statusCode} causa=${error.cause} cuerpo=$crudo",
                    error
                )
                val texto = crudo?.let {
                    runCatching { JSONObject(it).optString("mensaje") }.getOrNull()
                }?.takeIf { it.isNotBlank() }
                    ?: if (error.networkResponse == null) {
                        "No se pudo conectar con el servicio."
                    } else {
                        "No se pudo crear el contacto."
                    }
                mostrar(texto, false)
            }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = cuerpo.toString().toByteArray(Charsets.UTF_8)
        }.apply {
            setShouldCache(false)
            retryPolicy = DefaultRetryPolicy(8000, 0, 1f)
        }
        cola.add(peticion)
    }

    private fun mostrar(texto: String, correcto: Boolean) {
        val resultado = findViewById<TextView>(R.id.textResultadoCrear)
        resultado.text = texto
        resultado.setTextColor(Color.parseColor(if (correcto) "#166534" else "#9F1239"))
        resultado.setBackgroundColor(Color.parseColor(if (correcto) "#DCFCE7" else "#FFE4E6"))
        resultado.visibility = View.VISIBLE
        Toast.makeText(applicationContext, texto, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        cola.stop()
        super.onDestroy()
    }

    companion object {
        private const val URL_CREAR = "http://192.168.56.56:8088/crear.php"
    }
}
