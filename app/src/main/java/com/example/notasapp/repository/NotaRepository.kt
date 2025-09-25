package com.example.notasapp.repository

import androidx.lifecycle.LiveData
import com.example.notasapp.data.Nota
import com.example.notasapp.data.NotaDao

class NotaRepository(private val notaDao: NotaDao) {

    fun getAllNotas(): LiveData<List<Nota>> = notaDao.getAllNotas()

    fun buscarNotas(query: String): LiveData<List<Nota>> = notaDao.buscarNotas(query)

    suspend fun getNotaById(id: Long): Nota? = notaDao.getNotaById(id)

    suspend fun insertNota(nota: Nota): Long = notaDao.insertNota(nota)

    suspend fun updateNota(nota: Nota) = notaDao.updateNota(nota)

    suspend fun deleteNota(nota: Nota) = notaDao.deleteNota(nota)
}