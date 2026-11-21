package com.desarrollo.tarea_3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactoAdapter(
    private val contactos: MutableList<Contacto>,
    private val alEliminar: (Contacto) -> Unit
) : RecyclerView.Adapter<ContactoAdapter.Holder>() {

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
        holder.eliminar.setOnClickListener { alEliminar(contacto) }
    }

    override fun getItemCount(): Int = contactos.size

    fun reemplazar(nuevos: List<Contacto>) {
        contactos.clear()
        contactos.addAll(nuevos)
        notifyDataSetChanged()
    }

    fun quitar(codContacto: Int) {
        val indice = contactos.indexOfFirst { it.codContacto == codContacto }
        if (indice >= 0) {
            contactos.removeAt(indice)
            notifyItemRemoved(indice)
        }
    }

    fun estaVacio(): Boolean = contactos.isEmpty()
}
