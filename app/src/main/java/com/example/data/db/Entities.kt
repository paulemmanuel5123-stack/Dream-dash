package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_records")
data class RunRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val characterId: String,
    val score: Int,
    val distanceMeters: Int,
    val coinsCollected: Int,
    val causeOfDeath: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey
    val id: Int = 1,
    val totalCoins: Int = 0,
    val highScore: Int = 0,
    val maxDistance: Int = 0,
    val runsCompleted: Int = 0,
    val magnetLevel: Int = 1,
    val shieldLevel: Int = 1,
    val boostLevel: Int = 1,
    val multiplierLevel: Int = 1,
    val selectedCharacter: String = "kael",
    val unlockedCharactersCsv: String = "kael",
    val selectedHoverboard: String = "dream_board",
    val unlockedHoverboardsCsv: String = "dream_board",
    val selectedAnimal: String = "cheetah",
    val unlockedAnimalsCsv: String = "cheetah",
    val collectedMysteryBoxesTotal: Int = 0,
    val unopenedMysteryBoxesCount: Int = 0,
    val unlockedCosmeticsCsv: String = "",
    val skyBoostersUsedTotal: Int = 0,
    val animalTokensCollectedTotal: Int = 0,
    val animalTransformationsTotal: Int = 0,
    val missionsProgressCsv: String = "",
    val dailyMissionsDate: Long = 0L,
    val weeklyMissionsDate: Long = 0L
) {
    val unlockedCharacterIds: Set<String>
        get() = unlockedCharactersCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet() + "kael"

    fun isCharacterUnlocked(characterId: String): Boolean {
        return characterId == "kael" || unlockedCharacterIds.contains(characterId)
    }

    val unlockedHoverboardIds: Set<String>
        get() = unlockedHoverboardsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet() + "dream_board"

    fun isHoverboardUnlocked(hoverboardId: String): Boolean {
        return hoverboardId == "dream_board" || unlockedHoverboardIds.contains(hoverboardId)
    }

    val unlockedAnimalIds: Set<String>
        get() = unlockedAnimalsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet() + "cheetah"

    fun isAnimalUnlocked(animalId: String): Boolean {
        return animalId == "cheetah" || unlockedAnimalIds.contains(animalId)
    }

    val unlockedCosmetics: Set<String>
        get() = unlockedCosmeticsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
}
