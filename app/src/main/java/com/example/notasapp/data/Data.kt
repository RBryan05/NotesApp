package com.example.notasapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notas")
data class Nota(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String,
    val contenido: String,
    val fechaCreacion: Date,
    val fechaModificacion: Date
) {
    fun getTituloMostrar(): String {
        return if (titulo.isBlank()) {
            val contenidoLimpio = contenido.replace(Regex("<[^>]*>"), "").trim()
            if (contenidoLimpio.length > 30) {
                "${contenidoLimpio.substring(0, 30)}..."
            } else if (contenidoLimpio.isNotEmpty()) {
                contenidoLimpio
            } else {
                "Nota sin título"
            }
        } else {
            titulo
        }
    }
}