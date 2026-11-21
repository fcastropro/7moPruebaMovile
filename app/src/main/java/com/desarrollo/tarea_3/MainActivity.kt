package com.desarrollo.tarea_3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<Button>(R.id.buttonPregunta9).setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
        findViewById<Button>(R.id.buttonPregunta10).setOnClickListener {
            startActivity(Intent(this, ContactosActivity::class.java))
        }
        findViewById<Button>(R.id.buttonVerificarCedula).setOnClickListener {
            startActivity(Intent(this, VerificarCedulaActivity::class.java))
        }
    }
}
