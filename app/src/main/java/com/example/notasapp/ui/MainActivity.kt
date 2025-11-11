package com.example.notasapp.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notasapp.adapter.NotaAdapter
import com.example.notasapp.databinding.ActivityMainBinding
import com.example.notasapp.viewmodel.NotaViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NotaAdapter
    private val viewModel: NotaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ya no necesitamos setupToolbar() porque usamos un TextView simple
        setupRecyclerView()
        setupFAB()
        setupSearch()
        observeNotas()
    }

    private fun setupRecyclerView() {
        adapter = NotaAdapter(
            onNotaClick = { nota ->
                val intent = Intent(this, EditorActivity::class.java).apply {
                    putExtra("NOTA_ID", nota.id)
                }
                startActivity(intent)
            },
            onNotaLongClick = { nota ->
                // Implementar menú contextual para eliminar
                showDeleteDialog(nota)
            }
        )

        binding.recyclerView.apply {
            // Cambiado a LinearLayoutManager (lista vertical simple)
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupFAB() {
        binding.fab.setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                viewModel.buscar(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeNotas() {
        viewModel.notas.observe(this, Observer { notas ->
            adapter.submitList(notas)

            // Mostrar mensaje si no hay notas
            if (notas.isEmpty()) {
                binding.recyclerView.alpha = 0.5f
            } else {
                binding.recyclerView.alpha = 1.0f
            }
        })
    }

    private fun showDeleteDialog(nota: com.example.notasapp.data.Nota) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar nota")
            .setMessage("¿Estás seguro de que quieres eliminar \"${nota.getTituloMostrar()}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteNota(nota)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar la lista cuando regresemos del editor
        viewModel.buscar(binding.searchEditText.text.toString())
    }
}