package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM progression WHERE id = 1 LIMIT 1")
    fun getProgressionFlow(): Flow<Progression?>

    @Query("SELECT * FROM progression WHERE id = 1 LIMIT 1")
    suspend fun getProgressionDirect(): Progression?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgression(progression: Progression)

    @Query("SELECT * FROM death_echoes WHERE isClaimed = 0")
    fun getActiveDeathEchoesFlow(): Flow<List<DeathEcho>>

    @Query("SELECT * FROM death_echoes WHERE floor = :floor AND isClaimed = 0 LIMIT 1")
    suspend fun getDeathEchoForFloor(floor: Int): DeathEcho?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDeathEcho(deathEcho: DeathEcho)

    @Update
    suspend fun updateDeathEcho(deathEcho: DeathEcho)

    @Query("DELETE FROM death_echoes")
    suspend fun clearAllDeathEchoes()
}

@Database(entities = [Progression::class, DeathEcho::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
