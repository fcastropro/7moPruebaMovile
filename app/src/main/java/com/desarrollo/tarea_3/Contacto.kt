package com.desarrollo.tarea_3

data class Contacto(
    val codContacto: Int,
    val nombre: String,
    val apellido: String,
    val telefono: String,
    val foto: String = ""
)
