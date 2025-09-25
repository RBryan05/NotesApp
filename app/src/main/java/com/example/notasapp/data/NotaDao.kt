package com.example.notasapp.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NotaDao {
    @Query("SELECT * FROM notas ORDER BY fechaModificacion DESC")
    fun getAllNotas(): LiveData<List<Nota>>

    @Query("SELECT * FROM notas WHERE titulo LIKE '%' || :query || '%' OR contenido LIKE '%' || :query || '%' ORDER BY CASE WHEN titulo LIKE '%' || :query || '%' THEN 1 ELSE 2 END, fechaModificacion DESC")
    fun buscarNotas(query: String): LiveData<List<Nota>>

    @Query("SELECT * FROM notas WHERE id = :id")
    suspend fun getNotaById(id: Long): Nota?

    @Insert
    suspend fun insertNota(nota: Nota): Long

    @Update
    suspend fun updateNota(nota: Nota)

    @Delete
    suspend fun deleteNota(nota: Nota)
}