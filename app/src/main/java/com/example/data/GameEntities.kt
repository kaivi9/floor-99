package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progression")
data class Progression(
    @PrimaryKey val id: Int = 1,
    val totalScrap: Int = 0,
    val baseHealthLevel: Int = 0, // Persistent HP increments (+10 per level)
    val baseDamageLevel: Int = 0, // Persistent Damage increments (+2 per level)
    val baseSpeedLevel: Int = 0,  // Persistent Speed increments (+5% per level)
    val baseDodgeLevel: Int = 0,  // Persistent Cooldown reduction for dodge (+5% per level)
    val deepestFloorReached: Int = 1,
    val runsCompleted: Int = 0
)

@Entity(tableName = "death_echoes")
data class DeathEcho(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val floor: Int,
    val xRatio: Float,       // Canvas horizontal position ratio (0.0 to 1.0)
    val yRatio: Float,       // Canvas vertical position ratio (0.0 to 1.0)
    val scrapStored: Int,    // Scraps currently stored in the tomb
    val isClaimed: Boolean = false
)
