package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM run_records ORDER BY score DESC LIMIT 10")
    fun getTopRuns(): Flow<List<RunRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(record: RunRecord): Long

    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun getPlayerProfile(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getPlayerProfileDirect(): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlayerProfile(profile: PlayerProfile)
}
