package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffects
import com.example.data.GameRepository
import com.example.data.db.AppDatabase
import com.example.data.db.PlayerProfile
import com.example.data.db.RunRecord
import com.example.engine.GameEngine
import com.example.model.CharacterInfo
import com.example.model.GamePhase
import com.example.model.HoverboardInfo
import com.example.model.MissionDefinition
import com.example.model.MissionMetric
import com.example.model.MissionProgress
import com.example.model.MysteryBoxRarity
import com.example.model.MysteryBoxReward
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = GameRepository(db.gameDao())
    val soundEffects = SoundEffects(application)

    val profile: StateFlow<PlayerProfile> = repository.playerProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerProfile()
    )

    val topRuns: StateFlow<List<RunRecord>> = repository.topRuns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _gamePhase = MutableStateFlow(GamePhase.INTRO)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _activeEngine = MutableStateFlow<GameEngine?>(null)
    val activeEngine: StateFlow<GameEngine?> = _activeEngine.asStateFlow()

    private val _isNewHighScore = MutableStateFlow(false)
    val isNewHighScore: StateFlow<Boolean> = _isNewHighScore.asStateFlow()

    // Dialog sheets
    var showShopDialog = MutableStateFlow(false)
    var showLeaderboardDialog = MutableStateFlow(false)
    var showHelpDialog = MutableStateFlow(false)
    var showTactileControls = MutableStateFlow(false)

    fun startGame() {
        val prof = profile.value
        val activeId = if (prof.isCharacterUnlocked(prof.selectedCharacter)) {
            prof.selectedCharacter
        } else {
            CharacterInfo.KAEL.id
        }
        val char = CharacterInfo.find(activeId)

        val boardId = if (prof.isHoverboardUnlocked(prof.selectedHoverboard)) {
            prof.selectedHoverboard
        } else {
            HoverboardInfo.DREAM_BOARD.id
        }
        val board = HoverboardInfo.find(boardId)

        val engine = GameEngine(
            character = char,
            hoverboard = board,
            startingCoinBalance = prof.totalCoins,
            magnetLevel = prof.magnetLevel,
            shieldLevel = prof.shieldLevel,
            boostLevel = prof.boostLevel,
            multiplierLevel = prof.multiplierLevel,
            unlockedAnimals = prof.unlockedAnimalIds,
            preferredAnimal = prof.selectedAnimal,
            unlockedCosmetics = prof.unlockedCosmetics,
            soundEffects = soundEffects,
            onMissionEvent = { metric, amount ->
                viewModelScope.launch {
                    repository.recordMissionMetric(metric, amount)
                }
            }
        )
        _activeEngine.value = engine
        _isNewHighScore.value = false
        _gamePhase.value = GamePhase.PLAYING
    }

    fun openCharacterShop() {
        _gamePhase.value = GamePhase.CHARACTER_SHOP
    }

    fun openMysteryBoxes() {
        _gamePhase.value = GamePhase.MYSTERY_BOXES
    }

    fun openAnimalGallery() {
        _gamePhase.value = GamePhase.ANIMAL_GALLERY
    }

    fun openMissions() {
        refreshMissions()
        _gamePhase.value = GamePhase.MISSIONS
    }

    private val _missionsProgress = MutableStateFlow<Map<String, MissionProgress>>(emptyMap())
    val missionsProgress: StateFlow<Map<String, MissionProgress>> = _missionsProgress.asStateFlow()

    fun refreshMissions() {
        viewModelScope.launch {
            _missionsProgress.value = repository.getMissionsProgressMap()
        }
    }

    fun openMysteryBox(rarity: MysteryBoxRarity): MysteryBoxReward {
        val reward = MysteryBoxReward.generateReward(
            rarity = rarity,
            unlockedCosmetics = profile.value.unlockedCosmetics
        )
        viewModelScope.launch {
            repository.claimMysteryBoxReward(reward)
            repository.recordMissionMetric(MissionMetric.MYSTERY_BOXES_TOTAL, 1)
            soundEffects.playMysteryBoxOpen()
        }
        return reward
    }

    fun selectPreferredAnimal(animalId: String) {
        viewModelScope.launch {
            val selected = repository.setSelectedAnimal(animalId)
            if (selected) {
                soundEffects.playLaneSwitch()
            }
        }
    }

    fun claimMissionReward(mission: MissionDefinition) {
        viewModelScope.launch {
            val success = repository.claimMissionReward(mission)
            if (success) {
                soundEffects.playMissionComplete()
                refreshMissions()
            }
        }
    }

    fun pauseGame() {
        val engine = _activeEngine.value ?: return
        engine.isPaused.value = true
        _gamePhase.value = GamePhase.PAUSED
    }

    fun resumeGame() {
        val engine = _activeEngine.value ?: return
        engine.isPaused.value = false
        _gamePhase.value = GamePhase.PLAYING
    }

    fun finishRun(engine: GameEngine) {
        viewModelScope.launch {
            val isNewBest = repository.recordRun(
                characterId = engine.character.id,
                score = engine.score,
                distanceMeters = engine.distanceMeters.toInt(),
                coinsCollected = engine.coinsCollected,
                causeOfDeath = engine.causeOfDeath.ifEmpty { "Tripped in the Wilds" }
            )
            _isNewHighScore.value = isNewBest
            _gamePhase.value = GamePhase.GAME_OVER
        }
    }

    fun finishIntro() {
        _gamePhase.value = GamePhase.MENU
    }

    fun returnToMenu() {
        if (_gamePhase.value == GamePhase.PAUSED) {
            val unbankedCoins = _activeEngine.value?.coinsCollected ?: 0
            if (unbankedCoins > 0) {
                viewModelScope.launch {
                    repository.bankRunCoins(unbankedCoins)
                }
            }
        }
        _gamePhase.value = GamePhase.MENU
    }

    fun selectCharacter(characterId: String) {
        viewModelScope.launch {
            val selected = repository.setSelectedCharacter(characterId)
            if (selected) {
                soundEffects.playLaneSwitch()
            }
        }
    }

    fun buyCharacter(character: CharacterInfo) {
        viewModelScope.launch {
            val purchased = repository.purchaseCharacter(character)
            if (purchased) {
                soundEffects.playPowerUp()
            }
        }
    }

    fun selectHoverboard(hoverboardId: String) {
        viewModelScope.launch {
            val selected = repository.setSelectedHoverboard(hoverboardId)
            if (selected) {
                soundEffects.playLaneSwitch()
            }
        }
    }

    fun buyHoverboard(hoverboard: HoverboardInfo) {
        viewModelScope.launch {
            val purchased = repository.purchaseHoverboard(hoverboard)
            if (purchased) {
                soundEffects.playPowerUp()
            }
        }
    }

    fun buyUpgrade(powerUpType: String) {
        viewModelScope.launch {
            val success = repository.upgradePowerUp(powerUpType)
            if (success) {
                soundEffects.playCoin()
            }
        }
    }

    fun toggleSound(enabled: Boolean) {
        soundEffects.soundEnabled = enabled
    }

    fun toggleHaptics(enabled: Boolean) {
        soundEffects.hapticsEnabled = enabled
    }

    fun toggleTactileButtons(enabled: Boolean) {
        showTactileControls.value = enabled
    }
}
