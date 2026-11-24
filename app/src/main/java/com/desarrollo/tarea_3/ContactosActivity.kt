package com.desarrollo.tarea_3

import android.content.Intent
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
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class ContactosActivity : AppCompatActivity() {
    private val cola by lazy { Volley.newRequestQueue(applicationContext) }
    private lateinit var adaptador: ContactoAdapter
    private lateinit var progreso: ProgressBar
    private lateinit var vacio: TextView
    private lateinit var mensaje: TextView
    private lateinit var contador: TextView
    private var eliminando = false
    private val crearContacto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val texto = resultado.data?.getStringExtra("mensaje") ?: "Contacto creado correctamente"
            mostrarMensaje(texto, true)
            aviso(texto)
        }
    }
    private val editarContacto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val texto = resultado.data?.getStringExtra("mensaje") ?: "Contacto actualizado correctamente"
            mostrarMensaje(texto, true)
            aviso(texto)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Sesion.leer(this) == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
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
        contador = findViewById(R.id.textContadorContactos)
        adaptador = ContactoAdapter(mutableListOf(), cola, ::abrirEdicion, ::confirmarEliminacion)
        findViewById<RecyclerView>(R.id.recyclerContactos).apply {
            layoutManager = LinearLayoutManager(this@ContactosActivity)
            adapter = adaptador
        }
        findViewById<Button>(R.id.buttonCrearContacto).setOnClickListener {
            crearContacto.launch(Intent(this, CrearContactoActivity::class.java))
        }
        findViewById<Button>(R.id.buttonCerrarSesion).setOnClickListener { cerrarSesion() }
    }

    override fun onResume() {
        super.onResume()
        if (Sesion.leer(this) == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        cargarContactos()
    }

    private fun cargarContactos() {
        progreso.visibility = View.VISIBLE
        Red.json(this, cola, Request.Method.GET, "listar.php", null, { respuesta ->
            progreso.visibility = View.GONE
            val arreglo = respuesta.optJSONArray("contactos")
            if (!respuesta.optBoolean("estado", false) || arreglo == null) {
                aviso(respuesta.optString("mensaje", "No se pudieron consultar los contactos"))
                mostrarVacio()
            } else {
            val lista = mutableListOf<Contacto>()
            for (i in 0 until arreglo.length()) {
                val objeto = arreglo.optJSONObject(i) ?: continue
                lista.add(
                    Contacto(
                        codContacto = entero(objeto, "cod_contacto"),
                        nombre = objeto.optString("nom_contacto"),
                        apellido = objeto.optString("ape_contacto"),
                        telefono = objeto.optString("telefono_contacto"),
                        correo = objeto.optString("email_contacto").takeUnless { it == "null" }.orEmpty(),
                        foto = objeto.optString("foto_contacto").takeUnless { it == "null" }.orEmpty()
                    )
                )
            }
            adaptador.reemplazar(lista)
            mostrarVacio()
            }
        }, {
            progreso.visibility = View.GONE
            if (it.contains("sesión", ignoreCase = true)) {
                irAlLogin()
            } else {
                aviso(it)
                mostrarVacio()
            }
        })
    }

    private fun abrirEdicion(contacto: Contacto) {
        editarContacto.launch(
            Intent(this, EditarContactoActivity::class.java)
                .putExtra("cod_contacto", contacto.codContacto)
                .putExtra("nombre", contacto.nombre)
                .putExtra("apellido", contacto.apellido)
                .putExtra("telefono", contacto.telefono)
                .putExtra("correo", contacto.correo)
        )
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
        Red.json(this, cola, Request.Method.POST, "eliminar.php", cuerpo, { respuesta ->
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
        }, {
            eliminando = false
            mostrarMensaje(it, false)
            aviso(it)
        })
    }

    private fun cerrarSesion() {
        Red.json(this, cola, Request.Method.POST, "cerrar.php", JSONObject(), {
            irAlLogin()
        }, {
            irAlLogin()
        })
    }

    private fun irAlLogin() {
        Sesion.cerrar(this)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun entero(objeto: JSONObject, clave: String): Int {
        val valor = objeto.opt(clave)
        return when (valor) {
            is Number -> valor.toInt()
            is String -> valor.toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun mostrarVacio() {
        val total = adaptador.itemCount
        vacio.visibility = if (total == 0) View.VISIBLE else View.GONE
        contador.text = if (total == 1) "1 contacto" else "$total contactos"
    }

    private fun mostrarMensaje(texto: String, correcto: Boolean) {
        mensaje.pintarResultado(texto, correcto)
    }

    private fun aviso(mensaje: String) {
        Toast.makeText(applicationContext, mensaje, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        cola.stop()
        super.onDestroy()
    }
}
