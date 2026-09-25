package com.fuzinewatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {

    @Query("SELECT * FROM listings ORDER BY firstSeen DESC LIMIT 200")
    fun recent(): Flow<List<Listing>>

    @Query("SELECT id FROM listings WHERE source = :source")
    suspend fun idsBySource(source: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(listings: List<Listing>)
}
