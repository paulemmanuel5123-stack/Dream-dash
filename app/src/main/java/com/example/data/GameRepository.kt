package com.example.data

import com.example.data.db.GameDao
import com.example.data.db.PlayerProfile
import com.example.data.db.RunRecord
import com.example.model.CharacterInfo
import com.example.model.HoverboardInfo
import com.example.model.MissionCategory
import com.example.model.MissionDefinition
import com.example.model.MissionMetric
import com.example.model.MissionProgress
import com.example.model.MysteryBoxReward
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val gameDao: GameDao) {

    val topRuns: Flow<List<RunRecord>> = gameDao.getTopRuns()

    val playerProfile: Flow<PlayerProfile> = gameDao.getPlayerProfile().map { profile ->
        profile ?: PlayerProfile()
    }

    suspend fun getProfile(): PlayerProfile {
        return gameDao.getPlayerProfileDirect() ?: PlayerProfile().also {
            gameDao.savePlayerProfile(it)
        }
    }

    suspend fun recordRun(
        characterId: String,
        score: Int,
        distanceMeters: Int,
        coinsCollected: Int,
        causeOfDeath: String
    ): Boolean {
        val current = getProfile()
        val isNewHighScore = score > current.highScore

        val record = RunRecord(
            characterId = characterId,
            score = score,
            distanceMeters = distanceMeters,
            coinsCollected = coinsCollected,
            causeOfDeath = causeOfDeath
        )
        gameDao.insertRun(record)

        val safeCoinsToAdd = coinsCollected.coerceAtLeast(0)
        val updated = current.copy(
            totalCoins = (current.totalCoins + safeCoinsToAdd).coerceAtLeast(0),
            highScore = maxOf(current.highScore, score),
            maxDistance = maxOf(current.maxDistance, distanceMeters),
            runsCompleted = current.runsCompleted + 1
        )
        gameDao.savePlayerProfile(updated)
        return isNewHighScore
    }

    suspend fun bankRunCoins(coinsCollected: Int) {
        if (coinsCollected <= 0) return
        val current = getProfile()
        val updated = current.copy(
            totalCoins = (current.totalCoins + coinsCollected).coerceAtLeast(0)
        )
        gameDao.savePlayerProfile(updated)
    }

    suspend fun setSelectedCharacter(characterId: String): Boolean {
        val current = getProfile()
        if (!current.isCharacterUnlocked(characterId)) {
            return false
        }
        gameDao.savePlayerProfile(current.copy(selectedCharacter = characterId))
        return true
    }

    suspend fun setSelectedHoverboard(hoverboardId: String): Boolean {
        val current = getProfile()
        if (!current.isHoverboardUnlocked(hoverboardId)) {
            return false
        }
        gameDao.savePlayerProfile(current.copy(selectedHoverboard = hoverboardId))
        return true
    }

    suspend fun purchaseCharacter(character: CharacterInfo): Boolean {
        val current = getProfile()
        if (current.isCharacterUnlocked(character.id)) {
            return false
        }
        val price = character.priceCoins.coerceAtLeast(0)
        if (current.totalCoins < price) {
            return false
        }

        val newCoinBalance = (current.totalCoins - price).coerceAtLeast(0)
        val newUnlockedSet = current.unlockedCharacterIds + character.id
        val updated = current.copy(
            totalCoins = newCoinBalance,
            unlockedCharactersCsv = newUnlockedSet.joinToString(",")
        )
        gameDao.savePlayerProfile(updated)
        return true
    }

    suspend fun purchaseHoverboard(hoverboard: HoverboardInfo): Boolean {
        val current = getProfile()
        if (current.isHoverboardUnlocked(hoverboard.id)) {
            return false
        }
        val price = hoverboard.priceCoins.coerceAtLeast(0)
        if (current.totalCoins < price) {
            return false
        }

        val newCoinBalance = (current.totalCoins - price).coerceAtLeast(0)
        val newUnlockedSet = current.unlockedHoverboardIds + hoverboard.id
        val updated = current.copy(
            totalCoins = newCoinBalance,
            unlockedHoverboardsCsv = newUnlockedSet.joinToString(",")
        )
        gameDao.savePlayerProfile(updated)
        return true
    }

    suspend fun upgradePowerUp(powerUpType: String): Boolean {
        val current = getProfile()
        val (_, upgradedProfile) = when (powerUpType) {
            "magnet" -> {
                val cost = current.magnetLevel * 50
                if (current.totalCoins >= cost && current.magnetLevel < 5) {
                    cost to current.copy(
                        totalCoins = (current.totalCoins - cost).coerceAtLeast(0),
                        magnetLevel = current.magnetLevel + 1
                    )
                } else null
            }
            "shield" -> {
                val cost = current.shieldLevel * 60
                if (current.totalCoins >= cost && current.shieldLevel < 5) {
                    cost to current.copy(
                        totalCoins = (current.totalCoins - cost).coerceAtLeast(0),
                        shieldLevel = current.shieldLevel + 1
                    )
                } else null
            }
            "boost" -> {
                val cost = current.boostLevel * 75
                if (current.totalCoins >= cost && current.boostLevel < 5) {
                    cost to current.copy(
                        totalCoins = (current.totalCoins - cost).coerceAtLeast(0),
                        boostLevel = current.boostLevel + 1
                    )
                } else null
            }
            "multiplier" -> {
                val cost = current.multiplierLevel * 80
                if (current.totalCoins >= cost && current.multiplierLevel < 5) {
                    cost to current.copy(
                        totalCoins = (current.totalCoins - cost).coerceAtLeast(0),
                        multiplierLevel = current.multiplierLevel + 1
                    )
                } else null
            }
            else -> null
        } ?: return false

        gameDao.savePlayerProfile(upgradedProfile)
        return true
    }

    suspend fun setSelectedAnimal(animalId: String): Boolean {
        val current = getProfile()
        if (!current.isAnimalUnlocked(animalId)) return false
        gameDao.savePlayerProfile(current.copy(selectedAnimal = animalId))
        return true
    }

    suspend fun recordMysteryBoxCollected() {
        val current = getProfile()
        val updated = current.copy(
            collectedMysteryBoxesTotal = current.collectedMysteryBoxesTotal + 1,
            unopenedMysteryBoxesCount = current.unopenedMysteryBoxesCount + 1
        )
        gameDao.savePlayerProfile(updated)
        recordMissionMetric(MissionMetric.MYSTERY_BOXES_TOTAL, 1)
    }

    suspend fun claimMysteryBoxReward(reward: MysteryBoxReward) {
        val current = getProfile()
        var coinsToAdd = reward.coins
        var cosmetics = current.unlockedCosmetics
        reward.cosmeticId?.let {
            cosmetics = cosmetics + it
        }
        val updated = current.copy(
            totalCoins = (current.totalCoins + coinsToAdd).coerceAtLeast(0),
            unopenedMysteryBoxesCount = (current.unopenedMysteryBoxesCount - 1).coerceAtLeast(0),
            unlockedCosmeticsCsv = cosmetics.joinToString(",")
        )
        gameDao.savePlayerProfile(updated)
    }

    suspend fun recordMissionMetric(metric: MissionMetric, amount: Int = 1) {
        val current = getProfile()
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        val weekMillis = 7 * dayMillis

        val isNewDay = (now - current.dailyMissionsDate) > dayMillis
        val isNewWeek = (now - current.weeklyMissionsDate) > weekMillis

        val existingMap = MissionProgress.parseMap(current.missionsProgressCsv).toMutableMap()

        // Reset if interval passed
        if (isNewDay) {
            MissionDefinition.ALL.filter { it.category == MissionCategory.DAILY }.forEach {
                existingMap[it.id] = MissionProgress(it.id, 0, false)
            }
        }
        if (isNewWeek) {
            MissionDefinition.ALL.filter { it.category == MissionCategory.WEEKLY }.forEach {
                existingMap[it.id] = MissionProgress(it.id, 0, false)
            }
        }

        // Apply metric increment to matching missions
        for (mission in MissionDefinition.ALL) {
            if (mission.metric == metric) {
                val currentProg = existingMap[mission.id] ?: MissionProgress(mission.id, 0, false)
                if (!currentProg.isClaimed) {
                    val updatedVal = when (metric) {
                        MissionMetric.HIGH_SCORE_REACHED -> maxOf(currentProg.currentProgress, amount)
                        MissionMetric.ANIMALS_UNLOCKED_COUNT -> maxOf(currentProg.currentProgress, amount)
                        MissionMetric.HOVERBOARDS_UNLOCKED_COUNT -> maxOf(currentProg.currentProgress, amount)
                        else -> currentProg.currentProgress + amount
                    }
                    existingMap[mission.id] = currentProg.copy(currentProgress = updatedVal)
                }
            }
        }

        val updated = current.copy(
            missionsProgressCsv = MissionProgress.serializeMap(existingMap),
            dailyMissionsDate = if (isNewDay || current.dailyMissionsDate == 0L) now else current.dailyMissionsDate,
            weeklyMissionsDate = if (isNewWeek || current.weeklyMissionsDate == 0L) now else current.weeklyMissionsDate
        )
        gameDao.savePlayerProfile(updated)
    }

    suspend fun claimMissionReward(mission: MissionDefinition): Boolean {
        val current = getProfile()
        val map = MissionProgress.parseMap(current.missionsProgressCsv).toMutableMap()
        val prog = map[mission.id] ?: MissionProgress(mission.id, 0, false)

        if (prog.isClaimed || prog.currentProgress < mission.target) {
            return false
        }

        map[mission.id] = prog.copy(isClaimed = true)

        var newCoins = (current.totalCoins + mission.rewardCoins).coerceAtLeast(0)
        var animals = current.unlockedAnimalIds
        mission.rewardAnimalId?.let {
            animals = animals + it
        }
        var cosmetics = current.unlockedCosmetics
        mission.rewardCosmeticName?.let {
            cosmetics = cosmetics + it
        }

        val updated = current.copy(
            totalCoins = newCoins,
            unlockedAnimalsCsv = animals.joinToString(","),
            unlockedCosmeticsCsv = cosmetics.joinToString(","),
            missionsProgressCsv = MissionProgress.serializeMap(map)
        )
        gameDao.savePlayerProfile(updated)
        return true
    }

    suspend fun getMissionsProgressMap(): Map<String, MissionProgress> {
        val profile = getProfile()
        val map = MissionProgress.parseMap(profile.missionsProgressCsv).toMutableMap()
        // Ensure every known mission has an entry
        for (m in MissionDefinition.ALL) {
            if (!map.containsKey(m.id)) {
                // Initialize default progress for metric-linked achievements
                val initialVal = when (m.metric) {
                    MissionMetric.ANIMALS_UNLOCKED_COUNT -> profile.unlockedAnimalIds.size
                    MissionMetric.HOVERBOARDS_UNLOCKED_COUNT -> profile.unlockedHoverboardIds.size
                    MissionMetric.HIGH_SCORE_REACHED -> profile.highScore
                    MissionMetric.METERS_TOTAL -> profile.maxDistance
                    MissionMetric.RUNS_COMPLETED -> profile.runsCompleted
                    MissionMetric.MYSTERY_BOXES_TOTAL -> profile.collectedMysteryBoxesTotal
                    else -> 0
                }
                map[m.id] = MissionProgress(m.id, initialVal, false)
            }
        }
        return map
    }
}
