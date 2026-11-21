package com.desarrollo.tarea_3

import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.google.android.material.card.MaterialCardView

class VerificarCedulaActivity : AppCompatActivity() {
    private lateinit var cedulaInput: EditText
    private lateinit var verificarButton: Button
    private lateinit var resultado: TextView
    private lateinit var resultadoCard: MaterialCardView
    private lateinit var requestQueue: RequestQueue
    private var consultando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verificar_cedula)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pantallaVerificarCedula)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        cedulaInput = findViewById(R.id.editTextCedula)
        verificarButton = findViewById(R.id.buttonVerificar)
        resultado = findViewById(R.id.textViewResultado)
        resultadoCard = findViewById(R.id.cardResultado)
        requestQueue = Volley.newRequestQueue(applicationContext)
        mostrarInicial()
        cedulaInput.doAfterTextChanged {
            cedulaInput.error = null
            mostrarInicial()
        }
        verificarButton.setOnClickListener { verificarCedula() }
        cedulaInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verificarCedula()
                true
            } else false
        }
    }

    private fun verificarCedula() {
        if (consultando) return
        val cedula = cedulaInput.text.toString()
        // Se conserva como texto para no perder ceros iniciales.
        if (!Regex("^[0-9]{10}$").matches(cedula)) {
            cedulaInput.error = getString(R.string.cedula_invalida)
            mostrarResultado(R.string.resultado_titulo_validacion, getString(R.string.cedula_invalida), Estado.ERROR)
            return
        }
        cambiarCarga(true)
        mostrarResultado(R.string.verificando, getString(R.string.resultado_cargando))
        val url = getString(R.string.api_base_url).trimEnd('/') + "/cedulas/verificar"
        val request = JsonObjectRequest(Request.Method.POST, url,
            JSONObject().put("cedula", cedula),
            { response ->
                cambiarCarga(false)
                // Solo se acepta un booleano real: true significa que YA existe.
                val estado = response.opt("estado")
                val cedulaRespuesta = response.opt("cedula")
                val nombre = response.opt("nombre")
                if (estado !is Boolean || cedulaRespuesta != cedula ||
                    (estado && (nombre !is String || nombre.isBlank()))) {
                    mostrarResultado(R.string.resultado_titulo_error, getString(R.string.error_respuesta), Estado.ERROR)
                } else if (estado) {
                    mostrarResultado(R.string.resultado_titulo_registrada,
                        getString(R.string.resultado_datos, cedulaRespuesta, nombre), Estado.REGISTRADA)
                } else {
                    mostrarResultado(R.string.resultado_titulo_disponible,
                        getString(R.string.resultado_libre, cedulaRespuesta), Estado.DISPONIBLE)
                }
            },
            { error ->
                cambiarCarga(false)
                mostrarResultado(R.string.resultado_titulo_error,
                    getString(if (error.networkResponse == null) R.string.error_red else R.string.error_servidor), Estado.ERROR)
            }
        ).apply {
            tag = REQUEST_TAG
            setShouldCache(false)
            retryPolicy = DefaultRetryPolicy(8000, 0, 1f)
        }
        requestQueue.add(request)
    }

    private fun cambiarCarga(cargando: Boolean) {
        consultando = cargando
        verificarButton.isEnabled = !cargando
        cedulaInput.isEnabled = !cargando
        verificarButton.setText(if (cargando) R.string.verificando else R.string.verificar_boton)
    }

    private enum class Estado { NEUTRO, REGISTRADA, DISPONIBLE, ERROR }

    private fun mostrarInicial() {
        mostrarResultado(R.string.resultado_titulo_inicial, getString(R.string.resultado_inicial))
    }

    private fun mostrarResultado(tituloId: Int, detalle: String, estado: Estado = Estado.NEUTRO) {
        // Paletas con texto oscuro y fondo claro, también legibles en modo nocturno.
        val (fondo, tinta) = when (estado) {
            Estado.REGISTRADA -> "#EFF6FF" to "#1E40AF"
            Estado.DISPONIBLE -> "#FFF1F2" to "#9F1239"
            Estado.ERROR -> "#FFF1F2" to "#9F1239"
            Estado.NEUTRO -> "#F1F5F9" to "#334155"
        }
        resultadoCard.setCardBackgroundColor(Color.parseColor(fondo))
        resultadoCard.strokeColor = Color.parseColor(tinta)
        resultado.setTextColor(Color.parseColor(tinta))
        val titulo = getString(tituloId)
        resultado.text = SpannableString("$titulo\n\n$detalle").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, titulo.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(RelativeSizeSpan(1.25f), 0, titulo.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    override fun onStop() {
        super.onStop()
        requestQueue.cancelAll(REQUEST_TAG)
        if (consultando) {
            cambiarCarga(false)
            mostrarInicial()
        }
    }

    override fun onDestroy() {
        requestQueue.stop()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_TAG = "verificarCedula"
    }
}
