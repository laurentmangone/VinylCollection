package com.example.vinylcollection

import kotlinx.coroutines.flow.Flow

class VinylRepository(private val dao: VinylDao) {
    fun search(query: String): Flow<List<Vinyl>> = dao.search(query)

    suspend fun insert(vinyl: Vinyl): Long = dao.insert(vinyl)

    suspend fun update(vinyl: Vinyl) = dao.update(vinyl)

    suspend fun delete(vinyl: Vinyl) = dao.delete(vinyl)
}

