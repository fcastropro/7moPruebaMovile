package com.desarrollo.tarea_3

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
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
        val cedula = findViewById<EditText>(R.id.editTextCedulaRegistro).text.toString().trim()
        val correo = findViewById<EditText>(R.id.editTextCorreoRegistro).text.toString().trim()
        val clave = findViewById<EditText>(R.id.editTextClaveRegistro).text.toString()
        var valido = true
        if (!Regex("^[0-9]{10}$").matches(cedula)) {
            aviso("La cédula debe tener exactamente 10 dígitos.")
            valido = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            aviso("Ingresa un correo electrónico válido.")
            valido = false
        }
        if (clave.length < 8) {
            aviso("La contraseña debe tener al menos 8 caracteres.")
            valido = false
        }
        if (valido) {
            aviso("Validación exitosa.")
        }
    }

    private fun aviso(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }
}
