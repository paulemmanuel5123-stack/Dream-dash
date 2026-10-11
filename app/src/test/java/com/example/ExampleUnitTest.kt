package com.example

import com.example.data.GameRepository
import com.example.data.db.GameDao
import com.example.data.db.PlayerProfile
import com.example.data.db.RunRecord
import com.example.engine.GameEngine
import com.example.model.ActionState
import com.example.model.Biome
import com.example.model.CharacterAnimState
import com.example.model.CharacterInfo
import com.example.model.DifficultyTier
import com.example.model.HoverboardInfo
import com.example.model.HoverboardType
import com.example.model.Obstacle
import com.example.model.ObstacleType
import com.example.model.Pickup
import com.example.model.PickupType
import com.example.model.SceneryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testAllSixCharacterShopProfilesAndPrices() {
        assertEquals(6, CharacterInfo.ALL.size)

        // 1. Hunter Boy — FREE starter character
        assertEquals("kael", CharacterInfo.KAEL.id)
        assertEquals("Hunter Boy", CharacterInfo.KAEL.name)
        assertEquals(0, CharacterInfo.KAEL.priceCoins)
        assertTrue(CharacterInfo.KAEL.isFreeStarter)
        assertEquals("FREE", CharacterInfo.KAEL.formattedPrice)

        // 2. Fantasy Adventurer Girl — 1,000 coins
        assertEquals("lyra", CharacterInfo.LYRA.id)
        assertEquals("Fantasy Adventurer Girl", CharacterInfo.LYRA.name)
        assertEquals(1000, CharacterInfo.LYRA.priceCoins)

        // 3. Shadow Runner — 2,500 coins
        assertEquals("shadow_runner", CharacterInfo.SHADOW_RUNNER.id)
        assertEquals("Shadow Runner", CharacterInfo.SHADOW_RUNNER.name)
        assertEquals(2500, CharacterInfo.SHADOW_RUNNER.priceCoins)

        // 4. Crystal Warrior — 5,000 coins
        assertEquals("crystal_warrior", CharacterInfo.CRYSTAL_WARRIOR.id)
        assertEquals("Crystal Warrior", CharacterInfo.CRYSTAL_WARRIOR.name)
        assertEquals(5000, CharacterInfo.CRYSTAL_WARRIOR.priceCoins)

        // 5. Forest Guardian — 8,000 coins
        assertEquals("forest_guardian", CharacterInfo.FOREST_GUARDIAN.id)
        assertEquals("Forest Guardian", CharacterInfo.FOREST_GUARDIAN.name)
        assertEquals(8000, CharacterInfo.FOREST_GUARDIAN.priceCoins)

        // 6. Sky Runner — 12,000 coins
        assertEquals("sky_runner", CharacterInfo.SKY_RUNNER.id)
        assertEquals("Sky Runner", CharacterInfo.SKY_RUNNER.name)
        assertEquals(12000, CharacterInfo.SKY_RUNNER.priceCoins)
    }

    @Test
    fun testCharacterShopPurchaseUnlockAndSelection() = runBlocking {
        val fakeDao = object : GameDao {
            private var storedProfile: PlayerProfile? = PlayerProfile(totalCoins = 3200)
            private val profileFlow = MutableStateFlow(storedProfile)
            private val runsFlow = MutableStateFlow<List<RunRecord>>(emptyList())

            override fun getTopRuns(): Flow<List<RunRecord>> = runsFlow
            override suspend fun insertRun(record: RunRecord): Long = 1L
            override fun getPlayerProfile(): Flow<PlayerProfile?> = profileFlow
            override suspend fun getPlayerProfileDirect(): PlayerProfile? = storedProfile
            override suspend fun savePlayerProfile(profile: PlayerProfile) {
                storedProfile = profile
                profileFlow.value = profile
            }
        }

        val repo = GameRepository(fakeDao)

        // Initially only Hunter Boy (kael) is unlocked
        val initial = repo.getProfile()
        assertTrue(initial.isCharacterUnlocked("kael"))
        assertFalse(initial.isCharacterUnlocked("lyra"))
        assertFalse(initial.isCharacterUnlocked("shadow_runner"))

        // Cannot select a locked character before buying
        assertFalse(repo.setSelectedCharacter("lyra"))
        assertEquals("kael", repo.getProfile().selectedCharacter)

        // Purchase Fantasy Adventurer Girl (1,000 coins): 3200 - 1000 = 2200 coins remaining
        assertTrue(repo.purchaseCharacter(CharacterInfo.LYRA))
        val afterLyra = repo.getProfile()
        assertEquals(2200, afterLyra.totalCoins)
        assertTrue(afterLyra.isCharacterUnlocked("lyra"))

        // Now can select Fantasy Adventurer Girl
        assertTrue(repo.setSelectedCharacter("lyra"))
        assertEquals("lyra", repo.getProfile().selectedCharacter)

        // Attempting to buy Shadow Runner (2,500 coins) with only 2,200 coins fails and keeps balance intact
        assertFalse(repo.purchaseCharacter(CharacterInfo.SHADOW_RUNNER))
        assertEquals(2200, repo.getProfile().totalCoins)
        assertFalse(repo.getProfile().isCharacterUnlocked("shadow_runner"))
        assertTrue(repo.getProfile().totalCoins >= 0)
    }

    @Test
    fun testLiveCoinBalanceDuringRun() {
        val engine = GameEngine(
            character = CharacterInfo.KAEL,
            startingCoinBalance = 500,
            soundEffects = null
        )
        assertEquals(500, engine.totalCoinBalance)

        engine.collectPickup(Pickup(id = 1L, lane = 0, z = 0f, type = PickupType.COIN))
        assertEquals(1, engine.coinsCollected)
        assertEquals(501, engine.totalCoinBalance)
    }

    @Test
    fun testEngineLaneSwitchingAndDodgeAnimation() {
        val engine = GameEngine(
            character = CharacterInfo.KAEL,
            soundEffects = null
        )

        assertEquals(0, engine.targetLane)
        assertEquals(CharacterAnimState.RUNNING, engine.characterAnimState)

        engine.switchLane(1)
        assertEquals(1, engine.targetLane)
        assertEquals(CharacterAnimState.LANE_CHANGE, engine.characterAnimState)

        engine.switchLane(1)
        assertEquals(1, engine.targetLane)

        engine.switchLane(-1)
        assertEquals(0, engine.targetLane)
        engine.switchLane(-1)
        assertEquals(-1, engine.targetLane)

        engine.switchLane(-1)
        assertEquals(-1, engine.targetLane)
    }

    @Test
    fun testAllSevenCharacterAnimationStates() {
        val engine = GameEngine(
            character = CharacterInfo.LYRA,
            soundEffects = null
        )

        // 1. IDLE animation
        engine.setIdle(true)
        assertEquals(CharacterAnimState.IDLE, engine.characterAnimState)

        // 2. RUNNING animation
        engine.setIdle(false)
        assertEquals(ActionState.RUNNING, engine.actionState)
        assertEquals(CharacterAnimState.RUNNING, engine.characterAnimState)

        // 3. JUMPING animation
        engine.jump()
        assertEquals(ActionState.JUMPING, engine.actionState)
        assertEquals(CharacterAnimState.JUMPING, engine.characterAnimState)
        assertTrue(engine.verticalVelocity > 0f)

        // 4. LANDING animation (simulate jump touchdown)
        engine.playerY = 0.02f
        engine.verticalVelocity = -5f
        engine.obstacles.clear()
        engine.update(0.02f)
        assertEquals(ActionState.LANDING, engine.actionState)
        assertEquals(CharacterAnimState.LANDING, engine.characterAnimState)

        // 5. SLIDING animation
        engine.slide()
        assertEquals(ActionState.SLIDING, engine.actionState)
        assertEquals(CharacterAnimState.SLIDING, engine.characterAnimState)
        assertTrue(engine.slideTimer > 0f)

        // 6. LANE_CHANGE animation
        engine.actionState = ActionState.RUNNING
        engine.slideTimer = 0f
        engine.switchLane(1)
        assertEquals(CharacterAnimState.LANE_CHANGE, engine.characterAnimState)

        // 7. GAME_OVER animation
        engine.actionState = ActionState.CRASHED
        engine.isGameOver.value = true
        assertEquals(CharacterAnimState.GAME_OVER, engine.characterAnimState)
    }

    @Test
    fun testProgressiveSpeedCurveAndDifficultyTiers() {
        val engine = GameEngine(
            character = CharacterInfo.KAEL,
            soundEffects = null
        )

        assertEquals(20.0f, engine.currentSpeedKmh, 0.01f)
        assertEquals(DifficultyTier.EASY, engine.difficultyTier)

        // 0-30s: Easy (20-30 km/h)
        assertEquals(20.0f, GameEngine.computeTargetSpeedKmh(0f), 0.01f)
        assertEquals(25.0f, GameEngine.computeTargetSpeedKmh(15f), 0.01f)
        assertEquals(30.0f, GameEngine.computeTargetSpeedKmh(30f), 0.01f)
        assertEquals(DifficultyTier.EASY, DifficultyTier.forSurvivalTime(15f))

        // 30-90s: Medium (30-45 km/h)
        assertEquals(37.5f, GameEngine.computeTargetSpeedKmh(60f), 0.01f)
        assertEquals(45.0f, GameEngine.computeTargetSpeedKmh(90f), 0.01f)
        assertEquals(DifficultyTier.MEDIUM, DifficultyTier.forSurvivalTime(60f))

        // 90-180s: Hard (45-60 km/h)
        assertEquals(52.5f, GameEngine.computeTargetSpeedKmh(135f), 0.01f)
        assertEquals(60.0f, GameEngine.computeTargetSpeedKmh(180f), 0.01f)
        assertEquals(DifficultyTier.HARD, DifficultyTier.forSurvivalTime(120f))

        // 180s+: Extreme (60-70 km/h, capped and maintained at 70 km/h)
        assertTrue(GameEngine.computeTargetSpeedKmh(210f) in 60.0f..70.0f)
        assertEquals(70.0f, GameEngine.computeTargetSpeedKmh(240f), 0.01f)
        assertEquals(70.0f, GameEngine.computeTargetSpeedKmh(600f), 0.01f)
        assertEquals(DifficultyTier.EXTREME, DifficultyTier.forSurvivalTime(200f))
    }

    @Test
    fun testPowerUpMagneticShieldAndDoubleScore() {
        val engine = GameEngine(
            character = CharacterInfo.KAEL,
            soundEffects = null
        )

        assertTrue(engine.pickups.any { it.type != PickupType.COIN })
        assertEquals("Magnetic Shield", PickupType.MAGNET.displayName)
        assertEquals("Double Score", PickupType.DOUBLE_COINS.displayName)

        assertFalse(engine.shieldActive)

        val magneticShieldPickup = Pickup(id = 999L, lane = 0, z = 0f, type = PickupType.MAGNET)
        engine.collectPickup(magneticShieldPickup)
        assertTrue(engine.magnetTimer >= 10f)
        assertTrue(engine.shieldActive)
        assertTrue(engine.activePowerUpBanner.contains("MAGNETIC SHIELD"))

        engine.obstacles.clear()
        engine.pickups.clear()
        val sideCoin = Pickup(id = 1000L, lane = 1, z = engine.distanceMeters + 6f, type = PickupType.COIN)
        engine.pickups.add(sideCoin)
        engine.update(0.04f)
        assertTrue(sideCoin.visualX < 1.0f)

        val scoreBefore = engine.score
        val coinsBefore = engine.coinsCollected
        val doubleScorePickup = Pickup(id = 1001L, lane = 0, z = 0f, type = PickupType.DOUBLE_COINS)
        engine.collectPickup(doubleScorePickup)
        assertTrue(engine.multiplierTimer >= 12f)
        assertTrue(engine.activePowerUpBanner.contains("DOUBLE SCORE"))

        val scoreAfterPowerUp = engine.score
        val coinPickup = Pickup(id = 1002L, lane = 0, z = 0f, type = PickupType.COIN)
        engine.collectPickup(coinPickup)
        assertEquals(coinsBefore + 2, engine.coinsCollected)
        assertEquals(scoreAfterPowerUp + 100, engine.score)
        assertTrue(engine.score > scoreBefore)
    }

    @Test
    fun testSixAdventureJungleSectionsProgressionAndScenery() {
        assertEquals(6, Biome.entries.size)
        assertEquals(Biome.GREEN_JUNGLE_ENTRANCE, Biome.forDistance(0))
        assertEquals("Green Jungle Entrance", Biome.GREEN_JUNGLE_ENTRANCE.displayName)

        assertEquals(Biome.DEEP_JUNGLE, Biome.forDistance(250))
        assertEquals("Deep Jungle", Biome.DEEP_JUNGLE.displayName)

        assertEquals(Biome.ANCIENT_RUINS, Biome.forDistance(500))
        assertEquals("Ancient Ruins", Biome.ANCIENT_RUINS.displayName)

        assertEquals(Biome.WATERFALL_VALLEY, Biome.forDistance(750))
        assertEquals("Waterfall Valley", Biome.WATERFALL_VALLEY.displayName)

        assertEquals(Biome.JUNGLE_CLIFF, Biome.forDistance(1000))
        assertEquals("Jungle Cliff", Biome.JUNGLE_CLIFF.displayName)

        assertEquals(Biome.MYSTIC_JUNGLE, Biome.forDistance(1250))
        assertEquals("Mystic Jungle", Biome.MYSTIC_JUNGLE.displayName)

        assertEquals(Biome.GREEN_JUNGLE_ENTRANCE, Biome.forDistance(1500))

        val engine = GameEngine(character = CharacterInfo.KAEL, soundEffects = null)
        assertTrue(engine.scenery.isNotEmpty())
        assertEquals(8, SceneryType.entries.size)
        assertEquals(5, ObstacleType.entries.size)

        engine.obstacles.clear()
        engine.obstacles.add(Obstacle(id = 500L, lane = 0, z = 0.2f, type = ObstacleType.BRIDGE_GAP))
        engine.jump()
        engine.playerY = 1.0f
        engine.update(0.02f)
        assertFalse(engine.isGameOver.value)
    }

    @Test
    fun testAllSevenHoverboardsProfilesAndPrices() {
        assertEquals(7, HoverboardInfo.ALL.size)

        // 1. Dream Board — FREE starter
        assertEquals("dream_board", HoverboardInfo.DREAM_BOARD.id)
        assertEquals("Dream Board", HoverboardInfo.DREAM_BOARD.name)
        assertEquals(0, HoverboardInfo.DREAM_BOARD.priceCoins)
        assertTrue(HoverboardInfo.DREAM_BOARD.isFreeStarter)
        assertEquals("FREE", HoverboardInfo.DREAM_BOARD.formattedPrice)
        assertEquals(HoverboardType.DREAM, HoverboardInfo.DREAM_BOARD.type)

        // 2. Crystal Board — 2,000 coins
        assertEquals("crystal_board", HoverboardInfo.CRYSTAL_BOARD.id)
        assertEquals("Crystal Board", HoverboardInfo.CRYSTAL_BOARD.name)
        assertEquals(2000, HoverboardInfo.CRYSTAL_BOARD.priceCoins)
        assertEquals(HoverboardType.CRYSTAL, HoverboardInfo.CRYSTAL_BOARD.type)

        // 3. Fire Board — 4,000 coins
        assertEquals("fire_board", HoverboardInfo.FIRE_BOARD.id)
        assertEquals("Fire Board", HoverboardInfo.FIRE_BOARD.name)
        assertEquals(4000, HoverboardInfo.FIRE_BOARD.priceCoins)
        assertEquals(HoverboardType.FIRE, HoverboardInfo.FIRE_BOARD.type)

        // 4. Ice Board — 6,000 coins
        assertEquals("ice_board", HoverboardInfo.ICE_BOARD.id)
        assertEquals("Ice Board", HoverboardInfo.ICE_BOARD.name)
        assertEquals(6000, HoverboardInfo.ICE_BOARD.priceCoins)
        assertEquals(HoverboardType.ICE, HoverboardInfo.ICE_BOARD.type)

        // 5. Shadow Board — 8,000 coins
        assertEquals("shadow_board", HoverboardInfo.SHADOW_BOARD.id)
        assertEquals("Shadow Board", HoverboardInfo.SHADOW_BOARD.name)
        assertEquals(8000, HoverboardInfo.SHADOW_BOARD.priceCoins)
        assertEquals(HoverboardType.SHADOW, HoverboardInfo.SHADOW_BOARD.type)

        // 6. Sky Board — 12,000 coins
        assertEquals("sky_board", HoverboardInfo.SKY_BOARD.id)
        assertEquals("Sky Board", HoverboardInfo.SKY_BOARD.name)
        assertEquals(12000, HoverboardInfo.SKY_BOARD.priceCoins)
        assertEquals(HoverboardType.SKY, HoverboardInfo.SKY_BOARD.type)
        assertEquals(25f, HoverboardInfo.SKY_BOARD.durationSeconds, 0.01f)

        // 7. Yoruba Legend Board — 15,000 coins
        assertEquals("yoruba_legend_board", HoverboardInfo.YORUBA_LEGEND_BOARD.id)
        assertEquals("Yoruba Legend Board", HoverboardInfo.YORUBA_LEGEND_BOARD.name)
        assertEquals(15000, HoverboardInfo.YORUBA_LEGEND_BOARD.priceCoins)
        assertEquals(HoverboardType.YORUBA_LEGEND, HoverboardInfo.YORUBA_LEGEND_BOARD.type)
        assertTrue(HoverboardInfo.YORUBA_LEGEND_BOARD.abilityDescription.contains("Yoruba"))
    }

    @Test
    fun testHoverboardPurchaseUnlockAndSelection() = runBlocking {
        val fakeDao = object : GameDao {
            private var storedProfile: PlayerProfile? = PlayerProfile(totalCoins = 5000)
            private val profileFlow = MutableStateFlow(storedProfile)
            private val runsFlow = MutableStateFlow<List<RunRecord>>(emptyList())

            override fun getTopRuns(): Flow<List<RunRecord>> = runsFlow
            override suspend fun insertRun(record: RunRecord): Long = 1L
            override fun getPlayerProfile(): Flow<PlayerProfile?> = profileFlow
            override suspend fun getPlayerProfileDirect(): PlayerProfile? = storedProfile
            override suspend fun savePlayerProfile(profile: PlayerProfile) {
                storedProfile = profile
                profileFlow.value = profile
            }
        }

        val repo = GameRepository(fakeDao)

        // Initially only Dream Board is unlocked
        val initial = repo.getProfile()
        assertTrue(initial.isHoverboardUnlocked("dream_board"))
        assertFalse(initial.isHoverboardUnlocked("crystal_board"))
        assertFalse(initial.isHoverboardUnlocked("fire_board"))

        // Cannot select a locked board before purchase
        assertFalse(repo.setSelectedHoverboard("crystal_board"))
        assertEquals("dream_board", repo.getProfile().selectedHoverboard)

        // Purchase Crystal Board (2,000 coins): 5000 - 2000 = 3000 coins remaining
        assertTrue(repo.purchaseHoverboard(HoverboardInfo.CRYSTAL_BOARD))
        val afterCrystal = repo.getProfile()
        assertEquals(3000, afterCrystal.totalCoins)
        assertTrue(afterCrystal.isHoverboardUnlocked("crystal_board"))

        // Now can select Crystal Board
        assertTrue(repo.setSelectedHoverboard("crystal_board"))
        assertEquals("crystal_board", repo.getProfile().selectedHoverboard)

        // Attempting to buy Fire Board (4,000 coins) with only 3,000 coins fails safely
        assertFalse(repo.purchaseHoverboard(HoverboardInfo.FIRE_BOARD))
        assertEquals(3000, repo.getProfile().totalCoins)
        assertFalse(repo.getProfile().isHoverboardUnlocked("fire_board"))
        assertTrue(repo.getProfile().totalCoins >= 0)
    }

    @Test
    fun testHoverboardInGameActivationAndCrashAbsorption() {
        val engine = GameEngine(
            character = CharacterInfo.KAEL,
            hoverboard = HoverboardInfo.FIRE_BOARD,
            soundEffects = null
        )

        assertFalse(engine.hoverboardActive)
        assertEquals(0f, engine.hoverboardTimer, 0.01f)

        // Activate hoverboard during run
        assertTrue(engine.activateHoverboard())
        assertTrue(engine.hoverboardActive)
        assertTrue(engine.hoverboardTimer > 0f)
        assertTrue(engine.boostTimer > 0f) // Fire Board grants speed boost upon activation!

        // Obstacle collision while on hoverboard should dispel hoverboard and SAVE the player!
        engine.boostTimer = 0f // Clear speed boost to test crash absorption
        engine.obstacles.clear()
        engine.obstacles.add(Obstacle(id = 888L, lane = 0, z = engine.distanceMeters, type = ObstacleType.TALL_CRYSTAL))
        engine.update(0.01f)

        // Player is ALIVE! Hoverboard absorbed the crash!
        assertFalse(engine.isGameOver.value)
        assertFalse(engine.hoverboardActive) // Board dispelled to save player
        assertTrue(engine.invincibleTimer > 0f) // Invincibility buffer granted
    }
}
