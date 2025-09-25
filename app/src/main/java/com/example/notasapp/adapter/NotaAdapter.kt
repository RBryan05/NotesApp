package com.example.notasapp.adapter

import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notasapp.data.Nota
import com.example.notasapp.databinding.ItemNotaBinding
import java.text.SimpleDateFormat
import java.util.*

class NotaAdapter(
    private val onNotaClick: (Nota) -> Unit,
    private val onNotaLongClick: (Nota) -> Unit
) : ListAdapter<Nota, NotaAdapter.NotaViewHolder>(NotaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val binding = ItemNotaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotaViewHolder(private val binding: ItemNotaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(nota: Nota) {
            binding.apply {
                tituloTextView.text = nota.getTituloMostrar()

                // Convertir HTML a texto plano para preview
                val contenidoTexto = Html.fromHtml(nota.contenido, Html.FROM_HTML_MODE_COMPACT)
                    .toString()
                    .trim()
                    .replace("\n", " ")

                contenidoTextView.text = if (contenidoTexto.length > 100) {
                    "${contenidoTexto.substring(0, 100)}..."
                } else {
                    contenidoTexto
                }

                // Formatear fecha
                val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                fechaTextView.text = "Modificado: ${formato.format(nota.fechaModificacion)}"

                // Click listeners
                root.setOnClickListener { onNotaClick(nota) }
                root.setOnLongClickListener {
                    onNotaLongClick(nota)
                    true
                }
            }
        }
    }

    class NotaDiffCallback : DiffUtil.ItemCallback<Nota>() {
        override fun areItemsTheSame(oldItem: Nota, newItem: Nota): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Nota, newItem: Nota): Boolean {
            return oldItem == newItem
        }
    }
}