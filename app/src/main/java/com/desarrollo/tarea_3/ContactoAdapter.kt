package com.desarrollo.tarea_3

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageRequest
import com.google.android.material.imageview.ShapeableImageView

class ContactoAdapter(
    private val contactos: MutableList<Contacto>,
    private val cola: RequestQueue,
    private val alEliminar: (Contacto) -> Unit
) : RecyclerView.Adapter<ContactoAdapter.Holder>() {

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ShapeableImageView = itemView.findViewById(R.id.imageAvatar)
        val nombre: TextView = itemView.findViewById(R.id.textNombreContacto)
        val telefono: TextView = itemView.findViewById(R.id.textTelefonoContacto)
        val eliminar: Button = itemView.findViewById(R.id.buttonEliminarContacto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_contacto, parent, false)
        return Holder(vista)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val contacto = contactos[position]
        holder.nombre.text = "${contacto.nombre} ${contacto.apellido}"
        holder.telefono.text = contacto.telefono.ifBlank { "Sin teléfono" }
        mostrarFoto(holder, contacto.foto)
        holder.eliminar.setOnClickListener {
            val posicion = holder.bindingAdapterPosition
            if (posicion != RecyclerView.NO_POSITION) alEliminar(contactos[posicion])
        }
    }

    override fun getItemCount(): Int = contactos.size

    fun reemplazar(nuevos: List<Contacto>) {
        contactos.clear()
        contactos.addAll(nuevos)
        notifyDataSetChanged()
    }

    fun quitar(codContacto: Int) {
        val quedaron = contactos.filterNot { it.codContacto == codContacto }
        if (quedaron.size == contactos.size) return
        contactos.clear()
        contactos.addAll(quedaron)
        notifyDataSetChanged()
    }

    fun estaVacio(): Boolean = contactos.isEmpty()

    private fun mostrarFoto(holder: Holder, ruta: String) {
        val url = urlFoto(ruta)
        holder.avatar.tag = url
        holder.avatar.setImageResource(R.drawable.ic_avatar)
        if (url == null) return
        val peticion = ImageRequest(
            url,
            { bitmap ->
                if (holder.avatar.tag == url) holder.avatar.setImageBitmap(bitmap)
            },
            256,
            256,
            ImageView.ScaleType.CENTER_CROP,
            Bitmap.Config.ARGB_8888
        ) {
            if (holder.avatar.tag == url) holder.avatar.setImageResource(R.drawable.ic_avatar)
        }
        peticion.setShouldCache(false)
        cola.add(peticion)
    }

    private fun urlFoto(ruta: String): String? {
        val nombre = ruta.substringAfterLast('/')
        if (!Regex("""^[a-f0-9]{32}\.(jpg|png|webp)$""").matches(nombre)) return null
        return "$URL_FOTOS$nombre"
    }

    companion object {
        private const val URL_FOTOS = "http://192.168.56.56:8088/foto.php?archivo="
    }
}
