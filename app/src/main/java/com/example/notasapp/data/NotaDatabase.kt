package com.example.notasapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Nota::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NotaDatabase : RoomDatabase() {
    abstract fun notaDao(): NotaDao

    companion object {
        @Volatile
        private var INSTANCE: NotaDatabase? = null

        fun getDatabase(context: Context): NotaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotaDatabase::class.java,
                    "nota_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}