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

class RecuperarClaveActivity : AppCompatActivity() {
    private val cola by lazy { Volley.newRequestQueue(applicationContext) }
    private var enviando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recuperar_clave)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pantallaRecuperar)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<Button>(R.id.buttonRecuperar).setOnClickListener { recuperar() }
        findViewById<Button>(R.id.buttonVolverRecuperar).setOnClickListener { finish() }
    }

    private fun recuperar() {
        if (enviando) return
        currentFocus?.let { vista ->
            getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(vista.windowToken, 0)
        }
        val cedula = findViewById<EditText>(R.id.editTextCedulaRecuperar).text.toString().trim()
        val correo = findViewById<EditText>(R.id.editTextCorreoRecuperar).text.toString().trim()
        val clave = findViewById<EditText>(R.id.editTextClaveRecuperar).text.toString()
        val confirmacion = findViewById<EditText>(R.id.editTextConfirmarRecuperar).text.toString()
        val error = when {
            !Regex("^[0-9]{10}$").matches(cedula) -> "La cédula debe tener exactamente 10 dígitos."
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> "Ingresa el correo registrado."
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
            .put("correo_persona", correo)
            .put("clave_persona", clave)
            .put("confirmar_clave", confirmacion)
        Red.json(this, cola, Request.Method.POST, "recuperar.php", cuerpo, { respuesta ->
            enviando = false
            if (respuesta.optBoolean("estado", false)) {
                Sesion.cerrar(this)
                Toast.makeText(applicationContext, "Contraseña actualizada correctamente", Toast.LENGTH_LONG).show()
                startActivity(
                    Intent(this, LoginActivity::class.java)
                        .putExtra("aviso", "Contraseña actualizada. Inicia sesión con la nueva clave.")
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                finish()
            } else {
                mostrar(respuesta.optString("mensaje", "No se pudo actualizar la contraseña."), false)
            }
        }, {
            enviando = false
            mostrar(it, false)
        })
    }

    private fun mostrar(texto: String, correcto: Boolean) {
        val resultado = findViewById<TextView>(R.id.textResultadoRecuperar)
        resultado.pintarResultado(texto, correcto)
        Toast.makeText(applicationContext, texto, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        cola.stop()
        super.onDestroy()
    }
}
