package com.desarrollo.tarea_3

import android.content.Intent
import android.os.Bundle
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

class LoginActivity : AppCompatActivity() {
    private val cola by lazy { Volley.newRequestQueue(applicationContext) }
    private var enviando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Sesion.leer(this) != null) {
            startActivity(Intent(this, ContactosActivity::class.java))
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pantallaLogin)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        intent.getStringExtra("aviso")?.let { mostrar(it, true) }
        findViewById<Button>(R.id.buttonIngresar).setOnClickListener { ingresar() }
        findViewById<Button>(R.id.buttonCrearDesdeLogin).setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
        findViewById<Button>(R.id.buttonRecuperarDesdeLogin).setOnClickListener {
            startActivity(Intent(this, RecuperarClaveActivity::class.java))
        }
        findViewById<Button>(R.id.buttonVolverLogin).setOnClickListener { finish() }
    }

    private fun ingresar() {
        if (enviando) return
        currentFocus?.let { vista ->
            getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(vista.windowToken, 0)
        }
        val cedula = findViewById<EditText>(R.id.editTextCedulaLogin).text.toString().trim()
        val clave = findViewById<EditText>(R.id.editTextClaveLogin).text.toString()
        if (!Regex("^[0-9]{10}$").matches(cedula) || clave.isEmpty()) {
            mostrar("Cédula o contraseña incorrecta.", false)
            return
        }
        enviando = true
        val cuerpo = JSONObject().put("ci_persona", cedula).put("clave_persona", clave)
        Red.json(this, cola, Request.Method.POST, "login.php", cuerpo, { respuesta ->
            enviando = false
            val token = respuesta.optJSONObject("datos")?.optString("token").orEmpty()
            if (respuesta.optBoolean("estado", false) && token.length == 64) {
                Sesion.guardar(this, token)
                startActivity(Intent(this, ContactosActivity::class.java))
                finish()
            } else {
                mostrar(respuesta.optString("mensaje", "Cédula o contraseña incorrecta."), false)
            }
        }, {
            enviando = false
            mostrar(it, false)
        })
    }

    private fun mostrar(texto: String, correcto: Boolean) {
        val resultado = findViewById<TextView>(R.id.textResultadoLogin)
        resultado.pintarResultado(texto, correcto)
        if (!correcto) Toast.makeText(applicationContext, texto, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        cola.stop()
        super.onDestroy()
    }
}
