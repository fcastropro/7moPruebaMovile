package com.desarrollo.tarea_3

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
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
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class RegistroActivity : AppCompatActivity() {
    private val cola by lazy { Volley.newRequestQueue(applicationContext) }
    private var enviando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pantallaRegistro)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<Button>(R.id.buttonRegistrar).setOnClickListener { registrar() }
        findViewById<Button>(R.id.buttonVolverRegistro).setOnClickListener { finish() }
    }

    private fun registrar() {
        if (enviando) return
        ocultarTeclado()
        val cedula = texto(R.id.editTextCedulaRegistro)
        val nombre = texto(R.id.editTextNombreRegistro)
        val apellido = texto(R.id.editTextApellidoRegistro)
        val correo = texto(R.id.editTextCorreoRegistro)
        val clave = findViewById<EditText>(R.id.editTextClaveRegistro).text.toString()
        val confirmacion = findViewById<EditText>(R.id.editTextConfirmarRegistro).text.toString()
        val error = when {
            !Regex("^[0-9]{10}$").matches(cedula) -> "La cédula debe tener exactamente 10 dígitos."
            nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() -> "Completa todos los campos."
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> "Ingresa un correo electrónico válido."
            clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
            clave != confirmacion -> "La confirmación de la contraseña no coincide."
            else -> null
        }
        if (error != null) {
            mostrar(error, false)
            return
        }
        enviando = true
        val cuerpo = JSONObject()
            .put("ci_persona", cedula)
            .put("nom_persona", nombre)
            .put("ape_persona", apellido)
            .put("correo_persona", correo)
            .put("clave_persona", clave)
            .put("confirmar_clave", confirmacion)
        Red.json(this, cola, Request.Method.POST, "registrar.php", cuerpo, { respuesta ->
            enviando = false
            if (respuesta.optBoolean("estado", false)) {
                Toast.makeText(applicationContext, "Cuenta creada correctamente", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginActivity::class.java).putExtra("aviso", "Cuenta creada correctamente. Inicia sesión."))
                finish()
            } else {
                mostrar(respuesta.optString("mensaje", "No se pudo crear la cuenta."), false)
            }
        }, {
            enviando = false
            mostrar(it, false)
        })
    }

    private fun texto(id: Int) = findViewById<EditText>(id).text.toString().trim()

    private fun ocultarTeclado() {
        currentFocus?.let { vista ->
            getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(vista.windowToken, 0)
        }
    }

    private fun mostrar(texto: String, correcto: Boolean) {
        val resultado = findViewById<TextView>(R.id.textResultadoRegistro)
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
}
