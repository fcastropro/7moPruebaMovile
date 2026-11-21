package com.desarrollo.tarea_3

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.imageview.ShapeableImageView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class CrearContactoActivity : AppCompatActivity() {
    private val cola by lazy { Volley.newRequestQueue(applicationContext) }
    private var guardando = false
    private var foto: ImagenSeleccionada? = null
    private val elegirFoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        aplicarFotografia(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crear_contacto)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pantallaCrearContacto)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<Button>(R.id.buttonSeleccionarFoto).setOnClickListener {
            elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        findViewById<Button>(R.id.buttonGuardarContacto).setOnClickListener { guardar() }
        findViewById<Button>(R.id.buttonVolverCrear).setOnClickListener { finish() }
    }

    private fun aplicarFotografia(uri: Uri) {
        val bytes = runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null) {
            mostrar("No se pudo leer la fotografía.", false)
            return
        }
        val imagen = interpretarImagen(bytes)
        if (imagen == null) {
            foto = null
            findViewById<ShapeableImageView>(R.id.imageVistaPrevia).setImageResource(R.drawable.ic_avatar)
            mostrar(
                if (bytes.size > TAMANO_MAXIMO) {
                    "La fotografía no puede superar 2 MB."
                } else {
                    "Selecciona una imagen JPG, PNG o WebP."
                },
                false
            )
            return
        }
        foto = imagen
        findViewById<ShapeableImageView>(R.id.imageVistaPrevia).setImageURI(uri)
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
        val imagen = foto
        if (imagen == null) {
            enviarJson(nombre, apellido, telefono, email)
        } else {
            enviarConFotografia(nombre, apellido, telefono, email, imagen)
        }
    }

    private fun enviarJson(nombre: String, apellido: String, telefono: String, email: String) {
        val cuerpo = JSONObject()
            .put("nom_contacto", nombre)
            .put("ape_contacto", apellido)
            .put("telefono_contacto", telefono)
            .put("email_contacto", email)
        val peticion = object : StringRequest(
            Request.Method.POST,
            URL_CREAR,
            { cuerpoRespuesta -> procesarRespuesta(cuerpoRespuesta) },
            { error ->
                guardando = false
                val crudo = error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
                Log.e("CrearContacto", "codigo=${error.networkResponse?.statusCode} cuerpo=$crudo", error)
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

    private fun enviarConFotografia(
        nombre: String,
        apellido: String,
        telefono: String,
        email: String,
        imagen: ImagenSeleccionada
    ) {
        thread(name = "crear-contacto") {
            val texto = runCatching {
                val limite = "----AgendaUniandes${System.currentTimeMillis()}"
                val cuerpo = construirMultipart(limite, nombre, apellido, telefono, email, imagen)
                val conexion = (URL(URL_CREAR).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 8000
                    readTimeout = 15000
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$limite")
                    setRequestProperty("Connection", "close")
                }
                conexion.outputStream.use { it.write(cuerpo) }
                val codigo = conexion.responseCode
                val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
                flujo?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                texto.fold(
                    onSuccess = { procesarRespuesta(it) },
                    onFailure = {
                        guardando = false
                        mostrar("No se pudo conectar con el servicio.", false)
                    }
                )
            }
        }
    }

    private fun construirMultipart(
        limite: String,
        nombre: String,
        apellido: String,
        telefono: String,
        email: String,
        imagen: ImagenSeleccionada
    ): ByteArray {
        val campos = listOf(
            "nom_contacto" to nombre,
            "ape_contacto" to apellido,
            "telefono_contacto" to telefono,
            "email_contacto" to email
        ).joinToString("") { (clave, valor) ->
            val limpio = valor.replace("\r", " ").replace("\n", " ")
            "--$limite\r\nContent-Disposition: form-data; name=\"$clave\"\r\n\r\n$limpio\r\n"
        }
        val encabezado = "--$limite\r\n" +
            "Content-Disposition: form-data; name=\"foto\"; filename=\"foto.${imagen.extension}\"\r\n" +
            "Content-Type: ${imagen.mime}\r\n\r\n"
        val cierre = "\r\n--$limite--\r\n"
        return campos.toByteArray() + encabezado.toByteArray() + imagen.bytes + cierre.toByteArray()
    }

    private fun procesarRespuesta(cuerpoRespuesta: String) {
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
        private const val TAMANO_MAXIMO = 2 * 1024 * 1024

        private fun interpretarImagen(bytes: ByteArray): ImagenSeleccionada? {
            if (bytes.size > TAMANO_MAXIMO) return null
            val jpeg = bytes.size >= 3 &&
                bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
            val png = bytes.size >= 8 &&
                bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
            val webp = bytes.size >= 12 &&
                bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
                bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
                bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
                bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()
            return when {
                jpeg -> ImagenSeleccionada(bytes, "jpg", "image/jpeg")
                png -> ImagenSeleccionada(bytes, "png", "image/png")
                webp -> ImagenSeleccionada(bytes, "webp", "image/webp")
                else -> null
            }
        }
    }
}

private data class ImagenSeleccionada(
    val bytes: ByteArray,
    val extension: String,
    val mime: String
)
