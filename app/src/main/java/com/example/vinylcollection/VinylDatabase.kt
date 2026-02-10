package com.example.vinylcollection

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Vinyl::class], version = 1, exportSchema = false)
abstract class VinylDatabase : RoomDatabase() {
    abstract fun vinylDao(): VinylDao

    companion object {
        @Volatile
        private var INSTANCE: VinylDatabase? = null

        fun getInstance(context: Context): VinylDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VinylDatabase::class.java,
                    "vinyls.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

