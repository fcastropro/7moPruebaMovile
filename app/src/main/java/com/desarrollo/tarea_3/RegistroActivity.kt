package com.desarrollo.tarea_3

import android.graphics.Color
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

class RegistroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pantallaRegistro)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<Button>(R.id.buttonValidarRegistro).setOnClickListener { validar() }
        findViewById<Button>(R.id.buttonVolverRegistro).setOnClickListener { finish() }
    }

    private fun validar() {
        currentFocus?.let { vista ->
            getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(vista.windowToken, 0)
        }
        val cedula = findViewById<EditText>(R.id.editTextCedulaRegistro).text.toString().trim()
        val correo = findViewById<EditText>(R.id.editTextCorreoRegistro).text.toString().trim()
        val clave = findViewById<EditText>(R.id.editTextClaveRegistro).text.toString()
        val mensajes = mutableListOf<String>()
        if (!Regex("^[0-9]{10}$").matches(cedula)) {
            mensajes.add("La cédula debe tener exactamente 10 dígitos.")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mensajes.add("Ingresa un correo electrónico válido.")
        }
        if (clave.length < 8) {
            mensajes.add("La contraseña debe tener al menos 8 caracteres.")
        }
        if (mensajes.isEmpty()) {
            mensajes.add("Validación exitosa.")
        }
        val resultado = findViewById<TextView>(R.id.textResultadoRegistro)
        val correcto = mensajes.size == 1 && mensajes[0] == "Validación exitosa."
        resultado.text = mensajes.joinToString("\n")
        resultado.setTextColor(Color.parseColor(if (correcto) "#166534" else "#9F1239"))
        resultado.setBackgroundColor(Color.parseColor(if (correcto) "#DCFCE7" else "#FFE4E6"))
        resultado.visibility = android.view.View.VISIBLE
        mensajes.forEach { aviso(it) }
    }

    private fun aviso(mensaje: String) {
        Toast.makeText(applicationContext, mensaje, Toast.LENGTH_LONG).show()
    }
}
