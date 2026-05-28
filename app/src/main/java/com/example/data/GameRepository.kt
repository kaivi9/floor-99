package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val gameDao: GameDao) {

    val progression: Flow<Progression> = gameDao.getProgressionFlow().map { 
        it ?: Progression() 
    }

    val activeDeathEchoes: Flow<List<DeathEcho>> = gameDao.getActiveDeathEchoesFlow()

    suspend fun getProgressionDirect(): Progression {
        return gameDao.getProgressionDirect() ?: Progression()
    }

    suspend fun updateProgression(update: (Progression) -> Progression) {
        val current = getProgressionDirect()
        val next = update(current)
        gameDao.saveProgression(next)
    }

    suspend fun getDeathEchoForFloor(floor: Int): DeathEcho? {
        return gameDao.getDeathEchoForFloor(floor)
    }

    suspend fun createDeathEcho(floor: Int, xRatio: Float, yRatio: Float, scrapStored: Int) {
        val echo = DeathEcho(floor = floor, xRatio = xRatio, yRatio = yRatio, scrapStored = scrapStored)
        gameDao.saveDeathEcho(echo)
    }

    suspend fun claimDeathEcho(id: Int) {
        val current = gameDao.getProgressionDirect() ?: Progression()
        // Wait, what does the DAO have? Let's check with query or read and claim.
        // We can have a direct updater
    }

    suspend fun claimDeathEcho(echo: DeathEcho) {
        val updated = echo.copy(isClaimed = true)
        gameDao.updateDeathEcho(updated)
        updateProgression { current ->
            current.copy(totalScrap = current.totalScrap + echo.scrapStored)
        }
    }
}
