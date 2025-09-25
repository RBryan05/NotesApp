package com.example.notasapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.notasapp.data.Nota
import com.example.notasapp.data.NotaDatabase
import com.example.notasapp.repository.NotaRepository
import kotlinx.coroutines.launch
import java.util.Date

class NotaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotaRepository
    private val _searchQuery = MutableLiveData<String>()

    val notas: LiveData<List<Nota>>

    init {
        val notaDao = NotaDatabase.getDatabase(application).notaDao()
        repository = NotaRepository(notaDao)

        notas = _searchQuery.switchMap { query ->
            if (query.isNullOrBlank()) {
                repository.getAllNotas()
            } else {
                repository.buscarNotas(query)
            }
        }

        _searchQuery.value = ""
    }

    fun buscar(query: String) {
        _searchQuery.value = query
    }

    fun insertNota(titulo: String, contenido: String, callback: (Long) -> Unit) {
        viewModelScope.launch {
            val fechaActual = Date()
            val nota = Nota(
                titulo = titulo,
                contenido = contenido,
                fechaCreacion = fechaActual,
                fechaModificacion = fechaActual
            )
            val id = repository.insertNota(nota)
            callback(id)
        }
    }

    fun updateNota(id: Long, titulo: String, contenido: String) {
        viewModelScope.launch {
            val notaExistente = repository.getNotaById(id)
            notaExistente?.let {
                val notaActualizada = it.copy(
                    titulo = titulo,
                    contenido = contenido,
                    fechaModificacion = Date()
                )
                repository.updateNota(notaActualizada)
            }
        }
    }

    fun deleteNota(nota: Nota) {
        viewModelScope.launch {
            repository.deleteNota(nota)
        }
    }

    suspend fun getNotaById(id: Long): Nota? {
        return repository.getNotaById(id)
    }
}