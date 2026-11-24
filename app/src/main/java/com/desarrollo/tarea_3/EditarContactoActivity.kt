package com.desarrollo.tarea_3

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class EditarContactoActivity : AppCompatActivity() {
    private val cola by lazy { Volley.newRequestQueue(applicationContext) }
    private var enviando = false
    private var codigo = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Sesion.leer(this) == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        codigo = intent.getIntExtra("cod_contacto", 0)
        if (codigo < 1) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_editar_contacto)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pantallaEditarContacto)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<EditText>(R.id.editTextNombreEditar).setText(intent.getStringExtra("nombre").orEmpty())
        findViewById<EditText>(R.id.editTextApellidoEditar).setText(intent.getStringExtra("apellido").orEmpty())
        findViewById<EditText>(R.id.editTextTelefonoEditar).setText(intent.getStringExtra("telefono").orEmpty())
        findViewById<EditText>(R.id.editTextEmailEditar).setText(intent.getStringExtra("correo").orEmpty())
        findViewById<Button>(R.id.buttonGuardarEditar).setOnClickListener { guardar() }
        findViewById<Button>(R.id.buttonVolverEditar).setOnClickListener { finish() }
    }

    private fun guardar() {
        if (enviando) return
        currentFocus?.let { vista ->
            getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(vista.windowToken, 0)
        }
        val nombre = findViewById<EditText>(R.id.editTextNombreEditar).text.toString().trim()
        val apellido = findViewById<EditText>(R.id.editTextApellidoEditar).text.toString().trim()
        val telefono = findViewById<EditText>(R.id.editTextTelefonoEditar).text.toString().trim()
        val correo = findViewById<EditText>(R.id.editTextEmailEditar).text.toString().trim()
        val error = when {
            nombre.isEmpty() || apellido.isEmpty() -> "El nombre y el apellido son obligatorios."
            correo.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> "Ingresa un correo electrónico válido."
            telefono.length > 45 || correo.length > 45 -> "El teléfono o el correo es demasiado largo."
            else -> null
        }
        if (error != null) {
            mostrar(error, false)
            return
        }
        enviando = true
        val cuerpo = JSONObject()
            .put("cod_contacto", codigo)
            .put("nom_contacto", nombre)
            .put("ape_contacto", apellido)
            .put("telefono_contacto", telefono)
            .put("email_contacto", correo)
        Red.json(this, cola, Request.Method.POST, "editar.php", cuerpo, { respuesta ->
            enviando = false
            if (respuesta.optBoolean("estado", false)) {
                val texto = respuesta.optString("mensaje", "Contacto actualizado correctamente")
                setResult(RESULT_OK, Intent().putExtra("mensaje", texto))
                finish()
            } else {
                mostrar(respuesta.optString("mensaje", "No se pudo actualizar el contacto."), false)
            }
        }, {
            enviando = false
            if (it.contains("sesión", ignoreCase = true)) {
                Sesion.cerrar(this)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                mostrar(it, false)
            }
        })
    }

    private fun mostrar(texto: String, correcto: Boolean) {
        val resultado = findViewById<TextView>(R.id.textResultadoEditar)
        resultado.pintarResultado(texto, correcto)
        Toast.makeText(applicationContext, texto, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        cola.stop()
        super.onDestroy()
    }
}
