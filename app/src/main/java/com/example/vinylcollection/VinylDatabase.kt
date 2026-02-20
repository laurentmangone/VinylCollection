package com.example.vinylcollection

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Vinyl::class], version = 3, exportSchema = false)
abstract class VinylDatabase : RoomDatabase() {
    abstract fun vinylDao(): VinylDao

    companion object {
        @Volatile
        private var INSTANCE: VinylDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vinyls ADD COLUMN coverUri TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vinyls ADD COLUMN isOwned INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(context: Context): VinylDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VinylDatabase::class.java,
                    "vinyls.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
