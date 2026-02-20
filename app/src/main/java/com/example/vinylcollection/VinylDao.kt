package com.example.vinylcollection

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface VinylDao {
    @Query(
        """
        SELECT * FROM vinyls
        WHERE (title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%')
        AND isOwned = :isOwned
        ORDER BY artist COLLATE NOCASE, title COLLATE NOCASE
        """
    )
    fun searchByStatus(query: String, isOwned: Boolean): Flow<List<Vinyl>>

    @Query(
        """
        SELECT * FROM vinyls
        WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%'
        ORDER BY artist COLLATE NOCASE, title COLLATE NOCASE
        """
    )
    fun search(query: String): Flow<List<Vinyl>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vinyl: Vinyl): Long

    @Update
    suspend fun update(vinyl: Vinyl): Int

    @Delete
    suspend fun delete(vinyl: Vinyl): Int

    @Query("SELECT COUNT(*) FROM vinyls WHERE isOwned = 1")
    fun countOwned(): Flow<Int>
}
