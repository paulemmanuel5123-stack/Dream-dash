package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.audio.SoundEffects
import com.example.model.ActionState
import com.example.model.ActiveBoosterState
import com.example.model.AnimalForm
import com.example.model.Biome
import com.example.model.CharacterAnimState
import com.example.model.CharacterInfo
import com.example.model.DifficultyTier
import com.example.model.HoverboardInfo
import com.example.model.HoverboardType
import com.example.model.MissionMetric
import com.example.model.MysteryBoxRarity
import com.example.model.MysteryBoxReward
import com.example.model.Obstacle
import com.example.model.ObstacleType
import com.example.model.Pickup
import com.example.model.PickupType
import com.example.model.SceneryObject
import com.example.model.SceneryType
import com.example.model.SkyBoosterType
import com.example.model.TrackSectionType
import com.example.model.VisualParticle
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.MagicPurpleLight
import com.example.ui.theme.ShieldAzure
import com.example.ui.theme.StarlightCyan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs
import kotlin.random.Random

class GameEngine(
    val character: CharacterInfo,
    val hoverboard: HoverboardInfo = HoverboardInfo.DREAM_BOARD,
    val startingCoinBalance: Int = 0,
    val magnetLevel: Int = 1,
    val shieldLevel: Int = 1,
    val boostLevel: Int = 1,
    val multiplierLevel: Int = 1,
    val unlockedAnimals: Set<String> = setOf("cheetah"),
    val preferredAnimal: String = "cheetah",
    val unlockedCosmetics: Set<String> = emptySet(),
    private val soundEffects: SoundEffects? = null,
    val onMissionEvent: ((MissionMetric, Int) -> Unit)? = null
) {
    companion object {
        const val START_SPEED_KMH = 20.0f
        const val EARLY_MAX_KMH = 30.0f
        const val MID_MAX_KMH = 45.0f
        const val LATE_MAX_KMH = 60.0f
        const val MAX_TARGET_SPEED_KMH = 70.0f

        private const val MIN_TRACK_SPEED = 14.5f
        private const val MAX_TRACK_SPEED = 34.0f

        /**
         * Computes the smooth target running speed in km/h based on survival time (and distance):
         * - 0–30s (Easy): 20–30 km/h
         * - 30–90s (Medium): 30–45 km/h
         * - 90–180s (Hard): 45–60 km/h
         * - 180s+ (Extreme): 60–70 km/h (smoothly caps and maintains at 70 km/h)
         */
        fun computeTargetSpeedKmh(survivalSeconds: Float, distanceMeters: Float = 0f): Float {
            val effectiveSeconds = maxOf(survivalSeconds, distanceMeters / 22f)
            return when {
                effectiveSeconds <= 0f -> START_SPEED_KMH
                effectiveSeconds < 30f -> {
                    val u = effectiveSeconds / 30f
                    START_SPEED_KMH + u * (EARLY_MAX_KMH - START_SPEED_KMH)
                }
                effectiveSeconds < 90f -> {
                    val u = (effectiveSeconds - 30f) / 60f
                    EARLY_MAX_KMH + u * (MID_MAX_KMH - EARLY_MAX_KMH)
                }
                effectiveSeconds < 180f -> {
                    val u = (effectiveSeconds - 90f) / 90f
                    MID_MAX_KMH + u * (LATE_MAX_KMH - MID_MAX_KMH)
                }
                effectiveSeconds < 240f -> {
                    val u = (effectiveSeconds - 180f) / 60f
                    val smoothOut = 1f - (1f - u) * (1f - u)
                    LATE_MAX_KMH + smoothOut * (MAX_TARGET_SPEED_KMH - LATE_MAX_KMH)
                }
                else -> MAX_TARGET_SPEED_KMH
            }.coerceIn(START_SPEED_KMH, MAX_TARGET_SPEED_KMH)
        }

        fun kmhToTrackSpeed(speedKmh: Float): Float {
            val norm = ((speedKmh - START_SPEED_KMH) / (MAX_TARGET_SPEED_KMH - START_SPEED_KMH)).coerceIn(0f, 1f)
            return MIN_TRACK_SPEED + norm * (MAX_TRACK_SPEED - MIN_TRACK_SPEED)
        }
    }

    // Flow states
    val isGameOver = MutableStateFlow(false)
    val isPaused = MutableStateFlow(false)

    // Player position & animation timers
    var targetLane: Int = 0 // -1 (Left), 0 (Center), 1 (Right)
    var laneX: Float = 0f // Smooth interpolation towards targetLane
    var playerY: Float = 0f // Vertical height in meters
    var verticalVelocity: Float = 0f
    var actionState: ActionState = ActionState.RUNNING
    var slideTimer: Float = 0f
    var landingTimer: Float = 0f
    var laneChangeTimer: Float = 0f
    var laneChangeDirection: Int = 0 // -1 (Left), 1 (Right)
    var crashAnimTimer: Float = 0f

    val characterAnimState: CharacterAnimState
        get() = when {
            isGameOver.value || actionState == ActionState.CRASHED -> CharacterAnimState.GAME_OVER
            actionState == ActionState.IDLE -> CharacterAnimState.IDLE
            actionState == ActionState.JUMPING -> CharacterAnimState.JUMPING
            actionState == ActionState.SLIDING -> CharacterAnimState.SLIDING
            actionState == ActionState.LANDING || landingTimer > 0f -> CharacterAnimState.LANDING
            actionState == ActionState.LANE_CHANGE || laneChangeTimer > 0f -> CharacterAnimState.LANE_CHANGE
            else -> CharacterAnimState.RUNNING
        }

    // Progressive Speed & Survival Metrics
    var survivalTimeSeconds: Float = 0f
    var baseSpeedKmh: Float = START_SPEED_KMH
    var currentSpeedKmh: Float = START_SPEED_KMH
    var peakSpeedKmh: Float = START_SPEED_KMH
    var currentSpeed: Float = kmhToTrackSpeed(START_SPEED_KMH)

    val difficultyTier: DifficultyTier
        get() = DifficultyTier.forSurvivalTime(survivalTimeSeconds)

    val speedProgressFraction: Float
        get() = ((currentSpeedKmh - START_SPEED_KMH) / (MAX_TARGET_SPEED_KMH - START_SPEED_KMH)).coerceIn(0f, 1f)

    // Run metrics
    var distanceMeters: Float = 0f
    var score: Int = 0
    var coinsCollected: Int = 0
    var causeOfDeath: String = ""

    val totalCoinBalance: Int
        get() = (startingCoinBalance + coinsCollected).coerceAtLeast(0)

    // Active power-up timers & max durations for HUD progress bars
    var magnetTimer: Float = when (character.id) {
        "forest_guardian" -> 6f
        "sky_runner" -> 8f
        else -> 0f
    }
    var magnetMaxDuration: Float = 10f
    var shieldActive: Boolean = character.id in setOf("lyra", "crystal_warrior", "forest_guardian", "sky_runner")
    var boostTimer: Float = 0f
    var boostMaxDuration: Float = 7f
    var multiplierTimer: Float = if (character.id == "sky_runner") 8f else 0f
    var multiplierMaxDuration: Float = 12f
    var invincibleTimer: Float = 0f
    var powerUpsCollected: Int = 0

    // Hoverboard State & Activation
    var hoverboardActive: Boolean = false
    var hoverboardTimer: Float = 0f
    var hoverboardMaxDuration: Float = hoverboard.durationSeconds
    var hoverboardProtectionUsed: Boolean = false
    var hoverboardActivatedThisRun: Boolean = false

    // Feature 1: Mystery Box System
    var mysteryBoxesCollectedThisRun: Int = 0
    var activeMysteryBoxReward: MysteryBoxReward? = null
    var mysteryBoxRewardTimer: Float = 0f

    // Feature 2: Jungle Sky Boosters
    var skyBoostersCollectedThisRun: Int = 0
    var skyLauncherTimer: Float = 0f
    var skyGliderTimer: Float = 0f
    val activeBoosters = mutableMapOf<SkyBoosterType, ActiveBoosterState>()

    // Feature 3: Animal Transformation Zone
    var animalEnergy: Float = 0.0f // 0f..1f (each token is +0.20f)
    var activeAnimalForm: AnimalForm? = null
    var animalDurationTimer: Float = 0.0f
    var animalMaxDuration: Float = 14.0f
    var transformationAnimTimer: Float = 0.0f
    var animalTokensCollectedThisRun: Int = 0
    var animalTransformationsThisRun: Int = 0

    fun activateHoverboard(): Boolean {
        if (isGameOver.value || isPaused.value) return false
        if (hoverboardActive) return false

        hoverboardActive = true
        hoverboardTimer = hoverboard.durationSeconds
        hoverboardMaxDuration = hoverboard.durationSeconds
        hoverboardProtectionUsed = false
        hoverboardActivatedThisRun = true
        onMissionEvent?.invoke(MissionMetric.HOVERBOARD_ACTIVATED, 1)

        soundEffects?.playPowerUp()
        activePowerUpBanner = "HOVERBOARD: ${hoverboard.name.uppercase()}"
        activePowerUpColor = hoverboard.glowColor
        powerUpBannerTimer = 2.4f

        when (hoverboard.type) {
            HoverboardType.FIRE -> {
                boostTimer = maxOf(boostTimer, 4.5f)
                boostMaxDuration = 4.5f
            }
            HoverboardType.SHADOW -> {
                invincibleTimer = maxOf(invincibleTimer, 3.5f)
            }
            HoverboardType.CRYSTAL -> {
                magnetTimer = maxOf(magnetTimer, hoverboard.durationSeconds)
                magnetMaxDuration = hoverboard.durationSeconds
            }
            HoverboardType.ICE -> {
                // Frost protection active
            }
            HoverboardType.SKY -> {
                // Duration is 25s
            }
            HoverboardType.YORUBA_LEGEND -> {
                magnetTimer = maxOf(magnetTimer, hoverboard.durationSeconds)
                magnetMaxDuration = hoverboard.durationSeconds
            }
            HoverboardType.DREAM -> {
                // Smooth fantasy glide
            }
        }
        return true
    }

    // Live HUD notification banner when collecting a power-up or entering a new fantasy realm
    var activePowerUpBanner: String = ""
    var activePowerUpColor: Color = StarlightCyan
    var powerUpBannerTimer: Float = 0f

    // World & Track Section Progression
    var currentTrackSection: TrackSectionType = TrackSectionType.SAFE_HAVEN
    private var lastAnnouncedBiome: Biome = Biome.ANCIENT_JUNGLE_ENTRANCE
    private var sectionSpawnsRemaining: Int = 2

    val currentBiome: Biome
        get() = Biome.forDistance(distanceMeters.toInt())

    val nextBiome: Biome
        get() = Biome.nextBiome(currentBiome)

    /**
     * Smooth 0..1 blend factor during the final 40 meters of a zone before transitioning to the next.
     */
    val biomeTransitionBlend: Float
        get() {
            val nextThreshold = when (currentBiome) {
                Biome.ANCIENT_JUNGLE_ENTRANCE -> 450f
                Biome.DEEP_TROPICAL_FOREST -> 1000f
                Biome.FORGOTTEN_TEMPLE_RUINS -> 1700f
                Biome.STONE_BRIDGE_VALLEY -> 2500f
                Biome.WATERFALL_JUNGLE -> 3400f
                Biome.OVERGROWN_TEMPLE_COURTYARD -> 4400f
                Biome.MYSTERIOUS_GOLDEN_TEMPLE -> Float.MAX_VALUE
            }
            val distToNext = nextThreshold - distanceMeters
            return if (distToNext in 0f..40f) {
                (1f - (distToNext / 40f)).coerceIn(0f, 1f)
            } else 0f
        }

    // Screen effects
    var screenShake: Float = 0f

    // Track objects
    val obstacles = mutableListOf<Obstacle>()
    val pickups = mutableListOf<Pickup>()
    val scenery = mutableListOf<SceneryObject>()
    val particles = mutableListOf<VisualParticle>()

    // Internal trackers
    private var lastSpawnZ: Float = 28f
    private var lastSceneryZ: Float = 6f
    private var ambientParticleCooldown: Float = 0f
    private var nextObstacleId = 1L
    private var nextPickupId = 1L
    private var nextSceneryId = 1L
    private val random = Random(System.currentTimeMillis())

    private val gravity = 36f
    private val jumpImpulse = 13.5f

    init {
        generateInitialTrack()
    }

    fun setIdle(idle: Boolean) {
        if (isGameOver.value) return
        actionState = if (idle) ActionState.IDLE else ActionState.RUNNING
    }

    fun switchLane(direction: Int) {
        if (isGameOver.value || isPaused.value) return
        val newLane = (targetLane + direction).coerceIn(-1, 1)
        if (newLane != targetLane) {
            targetLane = newLane
            laneChangeDirection = direction.coerceIn(-1, 1)
            laneChangeTimer = 0.28f - speedProgressFraction * 0.09f
            if (actionState == ActionState.RUNNING || actionState == ActionState.LANDING) {
                actionState = ActionState.LANE_CHANGE
            }
            soundEffects?.playLaneSwitch()
            spawnLaneDust()
        }
    }

    fun jump() {
        if (isGameOver.value || isPaused.value) return
        if (actionState != ActionState.JUMPING) {
            actionState = ActionState.JUMPING
            slideTimer = 0f
            landingTimer = 0f
            val isMonkey = (activeAnimalForm == AnimalForm.MONKEY)
            val monkeyBonus = if (isMonkey) 1.55f else 1.0f
            val jumpScale = (1f + speedProgressFraction * 0.42f) * monkeyBonus
            verticalVelocity = jumpImpulse * jumpScale
            soundEffects?.playJump()
            spawnJumpParticles()
        }
    }

    fun slide() {
        if (isGameOver.value || isPaused.value) return
        val jumpScale = 1f + speedProgressFraction * 0.42f
        val responsiveSlideDuration = 0.70f - speedProgressFraction * 0.18f
        if (actionState == ActionState.JUMPING && playerY > 0.35f) {
            verticalVelocity = -jumpImpulse * 1.8f * jumpScale
            actionState = ActionState.SLIDING
            slideTimer = responsiveSlideDuration
            landingTimer = 0f
            soundEffects?.playSlide()
        } else if (actionState != ActionState.SLIDING) {
            actionState = ActionState.SLIDING
            slideTimer = responsiveSlideDuration
            landingTimer = 0f
            soundEffects?.playSlide()
            spawnSlideParticles()
        }
    }

    fun updatePostCrash(dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0.001f, 0.05f)
        crashAnimTimer += dt
        if (screenShake > 0f) screenShake = (screenShake - dt * 2.8f).coerceAtLeast(0f)
        updateParticles(dt)
    }

    fun update(dtSeconds: Float) {
        if (isPaused.value) return
        if (isGameOver.value) {
            updatePostCrash(dtSeconds)
            return
        }
        val dt = dtSeconds.coerceIn(0.001f, 0.05f)

        if (actionState == ActionState.IDLE) {
            updateParticles(dt)
            return
        }

        // 1. Progressive Survival Time & Smooth Imperceptible Speed Acceleration
        survivalTimeSeconds += dt
        val targetKmh = computeTargetSpeedKmh(survivalTimeSeconds, distanceMeters)
        val smoothRate = (dt * 3.2f).coerceIn(0.001f, 1f)
        baseSpeedKmh += (targetKmh - baseSpeedKmh) * smoothRate
        baseSpeedKmh = baseSpeedKmh.coerceIn(START_SPEED_KMH, MAX_TARGET_SPEED_KMH)

        var speedSurge = 0f
        if (boostTimer > 0f) speedSurge += 10.0f
        if (activeAnimalForm == AnimalForm.CHEETAH) speedSurge += 22.0f
        currentSpeedKmh = (baseSpeedKmh + speedSurge).coerceAtMost(MAX_TARGET_SPEED_KMH + 16f)

        if (currentSpeedKmh > peakSpeedKmh) {
            peakSpeedKmh = currentSpeedKmh
        }

        currentSpeed = kmhToTrackSpeed(currentSpeedKmh)

        val step = currentSpeed * dt
        distanceMeters += step
        onMissionEvent?.invoke(MissionMetric.METERS_TOTAL, step.toInt())

        // Check for Fantasy World Realm Progression Announcement
        val nowBiome = currentBiome
        if (nowBiome != lastAnnouncedBiome) {
            lastAnnouncedBiome = nowBiome
            soundEffects?.playPowerUp()
            activePowerUpBanner = "JUNGLE SECTION ${nowBiome.stageNumber}: ${nowBiome.displayName.uppercase()}"
            activePowerUpColor = nowBiome.ambientParticleColor
            powerUpBannerTimer = 2.6f
        }

        // Ambient magical particles drifting through the world
        ambientParticleCooldown -= dt
        if (ambientParticleCooldown <= 0f) {
            ambientParticleCooldown = 0.14f
            spawnAmbientWorldParticle()
        }

        // Score progression: distance + speed bonus + multipliers
        val speedScoreMultiplier = 1f + speedProgressFraction * 0.75f
        val multiBonus = (if (multiplierTimer > 0f) 3f else 1f) * (1f + (multiplierLevel - 1) * 0.1f)
        score += (step * 1.5f * speedScoreMultiplier * multiBonus).toInt()

        // 2. Responsive Lane Interpolation (scales up with speed so dodging stays razor-sharp)
        val laneLerpRate = 14.5f + speedProgressFraction * 12.5f
        val lerpFactor = (laneLerpRate * dt).coerceIn(0f, 1f)
        laneX += (targetLane - laneX) * lerpFactor

        // Lane change animation timer
        if (laneChangeTimer > 0f) {
            laneChangeTimer = (laneChangeTimer - dt).coerceAtLeast(0f)
            if (laneChangeTimer <= 0f && actionState == ActionState.LANE_CHANGE) {
                actionState = ActionState.RUNNING
            }
        }

        // Sky Launcher & Sky Glider Flight Control
        if (skyLauncherTimer > 0f) {
            skyLauncherTimer = (skyLauncherTimer - dt).coerceAtLeast(0f)
            playerY = 3.2f
            verticalVelocity = 0f
            spawnSkyLauncherParticles()
        } else if (skyGliderTimer > 0f) {
            skyGliderTimer = (skyGliderTimer - dt).coerceAtLeast(0f)
            playerY = 2.0f
            verticalVelocity = 0f
            spawnSkyGliderParticles()
        } else {
            // 3. Responsive Vertical Physics (Jumping & Landing)
            val isMonkey = (activeAnimalForm == AnimalForm.MONKEY)
            val monkeyBonus = if (isMonkey) 1.55f else 1.0f
            val jumpScale = (1f + speedProgressFraction * 0.42f) * monkeyBonus
            val effectiveGravity = gravity * jumpScale * jumpScale

            if (actionState == ActionState.JUMPING) {
                playerY += verticalVelocity * dt
                verticalVelocity -= effectiveGravity * dt
                if (playerY <= 0f) {
                    playerY = 0f
                    verticalVelocity = 0f
                    landingTimer = (0.26f - speedProgressFraction * 0.07f).coerceAtLeast(0.16f)
                    actionState = ActionState.LANDING
                    spawnLandingDust()
                }
            } else if (actionState == ActionState.LANDING || landingTimer > 0f) {
                landingTimer = (landingTimer - dt).coerceAtLeast(0f)
                if (landingTimer <= 0f && actionState == ActionState.LANDING) {
                    actionState = if (laneChangeTimer > 0f) ActionState.LANE_CHANGE else ActionState.RUNNING
                }
            }

            // Slide timing
            if (actionState == ActionState.SLIDING) {
                if (playerY > 0f) {
                    playerY = (playerY + verticalVelocity * dt).coerceAtLeast(0f)
                    if (playerY <= 0f) verticalVelocity = 0f
                }
                slideTimer -= dt
                if (slideTimer <= 0f) {
                    actionState = ActionState.RUNNING
                    slideTimer = 0f
                }
            }
        }

        // Animal Transformation Timers & Particles
        if (transformationAnimTimer > 0f) {
            transformationAnimTimer = (transformationAnimTimer - dt).coerceAtLeast(0f)
            spawnTransformationParticles()
        }

        if (activeAnimalForm != null) {
            animalDurationTimer = (animalDurationTimer - dt).coerceAtLeast(0f)
            if (activeAnimalForm == AnimalForm.EAGLE && playerY < 1.8f) {
                playerY = 1.8f
                verticalVelocity = 0f
            }
            if (activeAnimalForm == AnimalForm.CHEETAH) {
                spawnCheetahSparks()
            }
            if (animalDurationTimer <= 0f) {
                activeAnimalForm = null
                transformationAnimTimer = 0.8f
                soundEffects?.playPowerUp()
                activePowerUpBanner = "RETURNED TO ${character.name.uppercase()}"
                activePowerUpColor = character.primaryColor
                powerUpBannerTimer = 2.2f
            }
        }

        // Active Boosters Countdown
        val boosterKeys = activeBoosters.keys.toList()
        for (b in boosterKeys) {
            val st = activeBoosters[b] ?: continue
            st.remainingTime = (st.remainingTime - dt).coerceAtLeast(0f)
            if (st.remainingTime <= 0f) {
                activeBoosters.remove(b)
            }
        }

        // Power-up countdowns
        if (magnetTimer > 0f) magnetTimer = (magnetTimer - dt).coerceAtLeast(0f)
        if (boostTimer > 0f) {
            boostTimer = (boostTimer - dt).coerceAtLeast(0f)
            spawnBoostParticles()
        }
        if (multiplierTimer > 0f) multiplierTimer = (multiplierTimer - dt).coerceAtLeast(0f)
        if (invincibleTimer > 0f) invincibleTimer = (invincibleTimer - dt).coerceAtLeast(0f)
        if (powerUpBannerTimer > 0f) powerUpBannerTimer = (powerUpBannerTimer - dt).coerceAtLeast(0f)
        if (mysteryBoxRewardTimer > 0f) mysteryBoxRewardTimer = (mysteryBoxRewardTimer - dt).coerceAtLeast(0f)
        if (screenShake > 0f) screenShake = (screenShake - dt * 3.5f).coerceAtLeast(0f)

        // Hoverboard countdown & visual trail emission
        if (hoverboardActive) {
            hoverboardTimer -= dt
            spawnHoverboardTrail()
            if (hoverboardTimer <= 0f) {
                hoverboardActive = false
                hoverboardTimer = 0f
            }
        }

        // Magnetic attraction logic (Wolf has +80% magnet pull range for tokens and coins!)
        val isWolf = (activeAnimalForm == AnimalForm.WOLF)
        val magnetActive = (magnetTimer > 0f) || isWolf
        val wolfMultiplier = if (isWolf) 1.8f else 1.0f
        val magnetRadius = if (magnetActive) {
            ((13f + (magnetLevel - 1) * 3f) * (if (character.id == "kael") 1.4f else 1.0f) * wolfMultiplier).coerceAtLeast(16f)
        } else 0f

        // Process Pickups
        val pickupIter = pickups.iterator()
        while (pickupIter.hasNext()) {
            val p = pickupIter.next()
            if (p.collected) {
                pickupIter.remove()
                continue
            }

            val zDist = p.z - distanceMeters

            // Magnetic Shield & Wolf Spirit visibly pulling coins & animal tokens across lanes
            if ((p.type == PickupType.COIN || p.type == PickupType.ANIMAL_TOKEN) && magnetActive && zDist in -1.5f..magnetRadius) {
                val pullStrength = ((1f - (zDist / magnetRadius).coerceIn(0f, 1f)) * 14f * dt).coerceIn(0.08f, 1f)
                p.visualX += (laneX - p.visualX) * pullStrength
                p.y += ((playerY + 0.45f) - p.y) * pullStrength
                if (zDist < 2.8f) {
                    collectPickup(p)
                    pickupIter.remove()
                    continue
                }
            }

            // Standard collection check
            if (abs(zDist) < 1.1f && abs(laneX - p.visualX) < 0.68f) {
                val yDiff = abs(playerY - p.y)
                if (yDiff < 1.4f) {
                    collectPickup(p)
                    pickupIter.remove()
                    continue
                }
            }

            // Cleanup passed pickups
            if (zDist < -10f) {
                pickupIter.remove()
            }
        }

        // Process Obstacles & Collisions
        val obsIter = obstacles.iterator()
        while (obsIter.hasNext()) {
            val obs = obsIter.next()
            val zDist = obs.z - distanceMeters

            if (abs(zDist) < 0.75f && abs(laneX - obs.lane) < 0.62f) {
                // Airborne Sky Launcher flies high above everything
                if (skyLauncherTimer > 0f && playerY >= 2.5f) {
                    continue
                }

                // Speed boost or invincible rush smashes obstacles
                if (boostTimer > 0f) {
                    obs.destroyed = true
                    obsIter.remove()
                    val smashBonus = 150 * (if (multiplierTimer > 0f) 2 else 1)
                    score += smashBonus
                    soundEffects?.playCoin()
                    screenShake = 0.35f
                    spawnSmashParticles(obs.lane, obs.type)
                    continue
                }

                // GORILLA ABILITY: Primal smash destroys obstacles on impact!
                if (activeAnimalForm == AnimalForm.GORILLA) {
                    obs.destroyed = true
                    obsIter.remove()
                    val smashBonus = 200 * (if (multiplierTimer > 0f) 2 else 1)
                    score += smashBonus
                    soundEffects?.playCrash()
                    screenShake = 0.45f
                    spawnSmashParticles(obs.lane, obs.type)
                    continue
                }

                // ELEPHANT ABILITY: Trample charge passes through lightweight obstacles
                if (activeAnimalForm == AnimalForm.ELEPHANT &&
                    (obs.type == ObstacleType.LOW_LOG || obs.type == ObstacleType.BRIDGE_GAP || obs.type == ObstacleType.HIGH_ARCH)
                ) {
                    obs.destroyed = true
                    obsIter.remove()
                    score += 150 * (if (multiplierTimer > 0f) 2 else 1)
                    soundEffects?.playCrash()
                    screenShake = 0.38f
                    spawnSmashParticles(obs.lane, obs.type)
                    continue
                }

                // EAGLE ABILITY: Soars above all ground obstacles
                if (activeAnimalForm == AnimalForm.EAGLE &&
                    (obs.type == ObstacleType.LOW_LOG || obs.type == ObstacleType.BRIDGE_GAP || obs.type == ObstacleType.MYSTIC_WISP)
                ) {
                    continue
                }

                // SKY GLIDER: Glides over logs, chasms, and rocks
                if (skyGliderTimer > 0f &&
                    (obs.type == ObstacleType.LOW_LOG || obs.type == ObstacleType.BRIDGE_GAP || obs.type == ObstacleType.MYSTIC_WISP)
                ) {
                    continue
                }

                if (invincibleTimer > 0f) {
                    continue
                }

                var hit = false
                when (obs.type) {
                    ObstacleType.LOW_LOG -> {
                        if (playerY < 0.85f) hit = true
                    }
                    ObstacleType.BRIDGE_GAP -> {
                        if (playerY < 0.45f) hit = true
                    }
                    ObstacleType.HIGH_ARCH -> {
                        if (actionState != ActionState.SLIDING || playerY > 0.4f) hit = true
                    }
                    ObstacleType.TALL_CRYSTAL, ObstacleType.MYSTIC_WISP -> {
                        hit = true
                    }
                }

                if (hit) {
                    if (hoverboardActive) {
                        if (hoverboard.type == HoverboardType.ICE && !hoverboardProtectionUsed) {
                            hoverboardProtectionUsed = true
                            obsIter.remove()
                            invincibleTimer = 1.6f
                            screenShake = 0.45f
                            soundEffects?.playShieldBreak()
                            spawnHoverboardCrashBurst()
                            activePowerUpBanner = "ICE ARMOR ABSORBED IMPACT!"
                            activePowerUpColor = Color(0xFF84FFFF)
                            powerUpBannerTimer = 2.0f
                            continue
                        } else {
                            hoverboardActive = false
                            hoverboardTimer = 0f
                            obsIter.remove()
                            invincibleTimer = 2.0f
                            screenShake = 0.55f
                            soundEffects?.playCrash()
                            spawnHoverboardCrashBurst()
                            activePowerUpBanner = "${hoverboard.name.uppercase()} SAVED YOU!"
                            activePowerUpColor = hoverboard.glowColor
                            powerUpBannerTimer = 2.5f
                            continue
                        }
                    }

                    if (shieldActive) {
                        shieldActive = false
                        obsIter.remove()
                        invincibleTimer = 1.3f + (shieldLevel - 1) * 0.25f
                        score += 100 * (if (multiplierTimer > 0f) 2 else 1)
                        screenShake = 0.5f
                        soundEffects?.playShieldBreak()
                        spawnShieldBurst()
                    } else {
                        causeOfDeath = "Hit ${obs.type.displayName}"
                        actionState = ActionState.CRASHED
                        crashAnimTimer = 0f
                        soundEffects?.playCrash()
                        screenShake = 0.8f
                        spawnCrashParticles(obs.lane)
                        isGameOver.value = true
                        return
                    }
                }
            }

            if (zDist < -15f) {
                obsIter.remove()
            }
        }

        // Update scenery objects
        scenery.removeAll { (it.z - distanceMeters) < -25f }

        // Update active particles
        updateParticles(dt)

        // Spawn track & scenery ahead with extended lookahead for high speeds
        generateTrackUpTo(distanceMeters + 140f)
    }

    private fun updateParticles(dt: Float) {
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val pt = pIter.next()
            pt.life -= dt
            if (pt.life <= 0f) {
                pIter.remove()
            } else {
                pt.x += pt.vx * dt
                pt.y += pt.vy * dt
            }
        }
    }

    fun collectPickup(pickup: Pickup) {
        pickup.collected = true
        val scoreMulti = if (multiplierTimer > 0f) 2 else 1
        when (pickup.type) {
            PickupType.COIN -> {
                val isYorubaFortune = hoverboardActive && hoverboard.type == HoverboardType.YORUBA_LEGEND
                val coinGain = (1 * scoreMulti) + (if (isYorubaFortune) 1 else 0)
                coinsCollected += coinGain
                score += (50 * scoreMulti) + (if (isYorubaFortune) 30 else 0)
                soundEffects?.playCoin()
                spawnCoinSparkle(pickup.lane)
                onMissionEvent?.invoke(MissionMetric.COINS_TOTAL, coinGain)
            }
            PickupType.MYSTERY_BOX -> {
                mysteryBoxesCollectedThisRun++
                soundEffects?.playMysteryBoxCollect()
                val rarity = pickup.mysteryBoxRarity ?: MysteryBoxRarity.rollRarity(random)
                val reward = MysteryBoxReward.generateReward(rarity, unlockedCosmetics, random)
                activeMysteryBoxReward = reward
                mysteryBoxRewardTimer = 3.6f

                // Apply reward immediately without deducting coins!
                when (reward.type) {
                    com.example.model.MysteryBoxRewardType.COINS_100,
                    com.example.model.MysteryBoxRewardType.COINS_250,
                    com.example.model.MysteryBoxRewardType.COINS_500,
                    com.example.model.MysteryBoxRewardType.COINS_1000 -> {
                        coinsCollected += reward.coins
                        score += reward.coins * 2
                        onMissionEvent?.invoke(MissionMetric.COINS_TOTAL, reward.coins)
                    }
                    com.example.model.MysteryBoxRewardType.TEMPORARY_MAGNET -> {
                        magnetTimer = maxOf(magnetTimer, 12f)
                        magnetMaxDuration = 12f
                        shieldActive = true
                    }
                    com.example.model.MysteryBoxRewardType.TEMPORARY_SHIELD -> {
                        shieldActive = true
                        invincibleTimer = maxOf(invincibleTimer, 1.2f)
                    }
                    com.example.model.MysteryBoxRewardType.TEMPORARY_MULTIPLIER -> {
                        multiplierTimer = maxOf(multiplierTimer, 15f)
                        multiplierMaxDuration = 15f
                    }
                    com.example.model.MysteryBoxRewardType.TEMPORARY_SPEED_BOOST -> {
                        boostTimer = maxOf(boostTimer, 8f)
                        boostMaxDuration = 8f
                    }
                    com.example.model.MysteryBoxRewardType.HOVERBOARD_RUN -> {
                        activateHoverboard()
                    }
                    com.example.model.MysteryBoxRewardType.RARE_COSMETIC -> {
                        coinsCollected += 500
                    }
                }

                activePowerUpBanner = "MYSTERY BOX: ${reward.title.uppercase()}!"
                activePowerUpColor = reward.rarity.glowColor
                powerUpBannerTimer = 3.2f
                spawnMysteryBoxBurst(pickup.lane, reward.rarity.glowColor)
                onMissionEvent?.invoke(MissionMetric.MYSTERY_BOXES_TOTAL, 1)
            }
            PickupType.SKY_BOOSTER -> {
                skyBoostersCollectedThisRun++
                soundEffects?.playSkyBoosterCollect()
                val booster = pickup.skyBoosterType ?: SkyBoosterType.randomBooster(random)

                when (booster) {
                    SkyBoosterType.SKY_LAUNCHER -> {
                        skyLauncherTimer = 8.0f
                        playerY = 3.2f
                        verticalVelocity = 0f
                    }
                    SkyBoosterType.COIN_MAGNET -> {
                        magnetTimer = maxOf(magnetTimer, booster.durationSeconds)
                        magnetMaxDuration = booster.durationSeconds
                    }
                    SkyBoosterType.SPEED_BOOST -> {
                        boostTimer = maxOf(boostTimer, booster.durationSeconds)
                        boostMaxDuration = booster.durationSeconds
                    }
                    SkyBoosterType.JUNGLE_SHIELD -> {
                        shieldActive = true
                        invincibleTimer = maxOf(invincibleTimer, 1.5f)
                    }
                    SkyBoosterType.SKY_GLIDER -> {
                        skyGliderTimer = 9.0f
                        playerY = 2.0f
                        verticalVelocity = 0f
                    }
                    SkyBoosterType.SCORE_MULTIPLIER -> {
                        multiplierTimer = maxOf(multiplierTimer, booster.durationSeconds)
                        multiplierMaxDuration = booster.durationSeconds
                    }
                }
                activeBoosters[booster] = ActiveBoosterState(booster, booster.durationSeconds)

                activePowerUpBanner = "SKY BOOSTER: ${booster.displayName.uppercase()}!"
                activePowerUpColor = booster.glowColor
                powerUpBannerTimer = 2.8f
                spawnSkyBoosterBurst(pickup.lane, booster.glowColor)
                onMissionEvent?.invoke(MissionMetric.SKY_BOOSTERS_USED, 1)
            }
            PickupType.ANIMAL_TOKEN -> {
                animalTokensCollectedThisRun++
                soundEffects?.playAnimalToken()
                animalEnergy = (animalEnergy + 0.20f).coerceAtMost(1.0f)
                score += 75 * scoreMulti
                spawnAnimalTokenSparkle(pickup.lane)
                onMissionEvent?.invoke(MissionMetric.ANIMAL_TOKENS_COLLECTED, 1)

                if (animalEnergy >= 1.0f && activeAnimalForm == null) {
                    animalTransformationsThisRun++
                    soundEffects?.playTransformation()
                    val chosen = if (preferredAnimal in unlockedAnimals) {
                        AnimalForm.find(preferredAnimal)
                    } else {
                        AnimalForm.pickRandomUnlocked(unlockedAnimals, random)
                    }
                    activeAnimalForm = chosen
                    animalDurationTimer = chosen.durationSeconds
                    animalMaxDuration = chosen.durationSeconds
                    transformationAnimTimer = 1.2f
                    animalEnergy = 0.0f
                    screenShake = 0.4f

                    activePowerUpBanner = "TRANSFORMED: ${chosen.displayName.uppercase()}!"
                    activePowerUpColor = chosen.primaryColor
                    powerUpBannerTimer = 3.2f
                    spawnTransformationParticles()
                    onMissionEvent?.invoke(MissionMetric.ANIMAL_TRANSFORMATIONS, 1)
                }
            }
            PickupType.MAGNET -> {
                soundEffects?.playPowerUp()
                powerUpsCollected++
                val dur = 10f + (magnetLevel - 1) * 3f
                magnetMaxDuration = dur
                magnetTimer = maxOf(magnetTimer, dur)
                shieldActive = true
                score += 200 * scoreMulti
                activePowerUpBanner = "MAGNETIC SHIELD! Coin Pull + Barrier"
                activePowerUpColor = StarlightCyan
                powerUpBannerTimer = 2.2f
                spawnPowerUpBurst(pickup.lane, StarlightCyan)
            }
            PickupType.SHIELD -> {
                soundEffects?.playPowerUp()
                powerUpsCollected++
                shieldActive = true
                invincibleTimer = maxOf(invincibleTimer, 1.0f)
                score += 200 * scoreMulti
                activePowerUpBanner = "AEGIS WARD! Protective Barrier Ready"
                activePowerUpColor = ShieldAzure
                powerUpBannerTimer = 2.2f
                spawnPowerUpBurst(pickup.lane, ShieldAzure)
            }
            PickupType.STAR_RUSH -> {
                soundEffects?.playPowerUp()
                powerUpsCollected++
                val dur = 7f + (boostLevel - 1) * 2f
                boostMaxDuration = dur
                boostTimer = maxOf(boostTimer, dur)
                screenShake = 0.4f
                score += 250 * scoreMulti
                activePowerUpBanner = "DREAM RUSH! Smash Through Obstacles"
                activePowerUpColor = MagicPurpleLight
                powerUpBannerTimer = 2.2f
                spawnPowerUpBurst(pickup.lane, MagicPurpleLight)
            }
            PickupType.DOUBLE_COINS -> {
                soundEffects?.playPowerUp()
                powerUpsCollected++
                val dur = 12f + (multiplierLevel - 1) * 3f
                multiplierMaxDuration = dur
                multiplierTimer = maxOf(multiplierTimer, dur)
                score += 200 * scoreMulti
                activePowerUpBanner = "DOUBLE SCORE! 2X Points & Coins"
                activePowerUpColor = AstralGold
                powerUpBannerTimer = 2.2f
                spawnPowerUpBurst(pickup.lane, AstralGold)
            }
        }
    }

    private fun generateInitialTrack() {
        // Safe Haven start with early guaranteed power-up at 48m and welcoming coin trails
        val starterPowerUp = if (random.nextBoolean()) PickupType.MAGNET else PickupType.DOUBLE_COINS
        val starterLane = random.nextInt(-1, 2)
        pickups.add(Pickup(id = nextPickupId++, lane = starterLane, z = 48f, y = 0.25f, type = starterPowerUp))
        for (l in -1..1) {
            spawnCoinArc(startZ = 53f, length = 4, lane = l, isArc = false)
        }
        lastSpawnZ = 64f
        currentTrackSection = TrackSectionType.STANDARD_RUN
        sectionSpawnsRemaining = 3
        generateTrackUpTo(145f)
    }

    private fun advanceTrackSection(tier: DifficultyTier) {
        if (sectionSpawnsRemaining > 0) {
            sectionSpawnsRemaining--
            return
        }
        // Randomly combine safe sections, magical bridges, standard trails, and challenging gauntlets
        val r = random.nextFloat()
        currentTrackSection = when {
            currentTrackSection == TrackSectionType.CHALLENGE_GAUNTLET && r < 0.55f -> {
                // Reward players after a challenge gauntlet with a Safe Clearing or Magical Bridge!
                TrackSectionType.SAFE_HAVEN
            }
            r < 0.18f -> TrackSectionType.SAFE_HAVEN
            r < 0.42f -> TrackSectionType.MAGICAL_BRIDGE
            r < 0.74f -> TrackSectionType.STANDARD_RUN
            else -> TrackSectionType.CHALLENGE_GAUNTLET
        }
        sectionSpawnsRemaining = when (currentTrackSection) {
            TrackSectionType.SAFE_HAVEN -> 2
            TrackSectionType.MAGICAL_BRIDGE -> 3
            TrackSectionType.STANDARD_RUN -> 4
            TrackSectionType.CHALLENGE_GAUNTLET -> if (tier == DifficultyTier.EASY) 2 else 4
        }
    }

    private fun generateTrackUpTo(maxZ: Float) {
        val tier = difficultyTier
        while (lastSpawnZ < maxZ) {
            advanceTrackSection(tier)

            val minReactionSec = when (tier) {
                DifficultyTier.EASY -> 1.45f
                DifficultyTier.MEDIUM -> 1.25f
                DifficultyTier.HARD -> 1.10f
                DifficultyTier.EXTREME -> 0.98f
            }
            val extraWindowSec = when (tier) {
                DifficultyTier.EASY -> 0.40f
                DifficultyTier.MEDIUM -> 0.33f
                DifficultyTier.HARD -> 0.28f
                DifficultyTier.EXTREME -> 0.24f
            }
            val reactionSeconds = minReactionSec + random.nextFloat() * extraWindowSec
            val gap = (currentSpeed * reactionSeconds).coerceIn(20f, 44f)
            lastSpawnZ += gap

            when (currentTrackSection) {
                TrackSectionType.SAFE_HAVEN -> {
                    // Safe section: open paths with coin ribbons and a strategic Power-Up Shrine
                    val powerLane = random.nextInt(-1, 2)
                    if (random.nextFloat() < 0.55f) {
                        pickups.add(
                            Pickup(
                                id = nextPickupId++,
                                lane = powerLane,
                                z = lastSpawnZ,
                                y = 0.25f,
                                type = selectRandomPowerUp()
                            )
                        )
                    }
                    for (l in -1..1) {
                        spawnCoinArc(lastSpawnZ + 3f, length = 4, lane = l, isArc = (l == 0))
                    }
                }

                TrackSectionType.MAGICAL_BRIDGE -> {
                    // Magical bridge section: features jumpable bridge chasms, crystal monoliths, and coin arcs
                    spawnMagicalBridgeSection(lastSpawnZ, tier)
                }

                TrackSectionType.STANDARD_RUN,
                TrackSectionType.CHALLENGE_GAUNTLET -> {
                    val isGauntlet = (currentTrackSection == TrackSectionType.CHALLENGE_GAUNTLET)
                    val obstacleChance = if (isGauntlet) 0.85f else when (tier) {
                        DifficultyTier.EASY -> 0.56f
                        DifficultyTier.MEDIUM -> 0.65f
                        DifficultyTier.HARD -> 0.72f
                        DifficultyTier.EXTREME -> 0.78f
                    }
                    val coinChance = obstacleChance + 0.12f

                    val roll = random.nextFloat()
                    if (roll < obstacleChance) {
                        spawnObstacleGroup(lastSpawnZ, tier, isGauntlet)
                    } else if (roll < coinChance) {
                        spawnCoinArc(lastSpawnZ, length = 6, lane = random.nextInt(-1, 2), isArc = random.nextBoolean())
                    } else if (roll < coinChance + 0.10f) {
                        // Spawn exciting Jungle Sky Booster suspended in the air
                        val boosterLane = random.nextInt(-1, 2)
                        val boosterType = SkyBoosterType.randomBooster(random)
                        pickups.add(
                            Pickup(
                                id = nextPickupId++,
                                lane = boosterLane,
                                z = lastSpawnZ,
                                y = 1.75f,
                                type = PickupType.SKY_BOOSTER,
                                skyBoosterType = boosterType
                            )
                        )
                        spawnCoinArc(lastSpawnZ + 3f, length = 3, lane = boosterLane, isArc = false)
                    } else if (roll < coinChance + 0.18f) {
                        // Spawn magical Jungle Mystery Box with question mark
                        val boxLane = random.nextInt(-1, 2)
                        val boxRarity = MysteryBoxRarity.rollRarity(random)
                        pickups.add(
                            Pickup(
                                id = nextPickupId++,
                                lane = boxLane,
                                z = lastSpawnZ,
                                y = 0.35f,
                                type = PickupType.MYSTERY_BOX,
                                mysteryBoxRarity = boxRarity
                            )
                        )
                    } else if (roll < coinChance + 0.28f) {
                        // Spawn glowing Animal Energy Tokens
                        val tokenLane = random.nextInt(-1, 2)
                        for (i in 0 until 4) {
                            pickups.add(
                                Pickup(
                                    id = nextPickupId++,
                                    lane = tokenLane,
                                    z = lastSpawnZ + i * 2.2f,
                                    y = 0.3f,
                                    type = PickupType.ANIMAL_TOKEN
                                )
                            )
                        }
                    } else {
                        val type = selectRandomPowerUp()
                        val lane = random.nextInt(-1, 2)
                        pickups.add(Pickup(id = nextPickupId++, lane = lane, z = lastSpawnZ, y = 0.25f, type = type))
                        for (l in -1..1) {
                            spawnCoinArc(lastSpawnZ + 4f, length = 3, lane = l, isArc = false)
                        }
                    }
                }
            }
        }

        // Continuously populate both sides of the fantasy world with biome-themed 3D scenery
        while (lastSceneryZ < maxZ) {
            lastSceneryZ += 14f
            val biomeAtZ = Biome.forDistance(lastSceneryZ.toInt())
            for (side in listOf(-1, 1)) {
                if (random.nextFloat() < 0.82f) {
                    val sType = selectBiomeScenery(biomeAtZ)
                    val offset = when (sType) {
                        SceneryType.JUNGLE_WATERFALL, SceneryType.MOSSY_BOULDER_CLIFF -> 2.85f + random.nextFloat() * 0.9f
                        SceneryType.ANCIENT_TEMPLE_ARCH -> 1.68f
                        SceneryType.TEMPLE_WALL_PARAPET -> 1.62f + random.nextFloat() * 0.2f
                        SceneryType.JUNGLE_TORCH_BRAZIER -> 1.72f + random.nextFloat() * 0.25f
                        SceneryType.ANCIENT_GOLDEN_IDOL -> 2.30f + random.nextFloat() * 0.4f
                        else -> 2.15f + random.nextFloat() * 0.75f
                    }
                    scenery.add(
                        SceneryObject(
                            id = nextSceneryId++,
                            side = side,
                            lateralOffset = offset,
                            z = lastSceneryZ + random.nextFloat() * 4f,
                            type = sType,
                            scale = 0.88f + random.nextFloat() * 0.45f
                        )
                    )
                }
            }
        }
    }

    private fun selectBiomeScenery(biome: Biome): SceneryType {
        val r = random.nextFloat()
        return when (biome) {
            Biome.ANCIENT_JUNGLE_ENTRANCE -> when {
                r < 0.32f -> SceneryType.DENSE_TROPICAL_TREE
                r < 0.50f -> SceneryType.ANCIENT_TEMPLE_ARCH
                r < 0.68f -> SceneryType.HANGING_GIANT_VINE
                r < 0.80f -> SceneryType.GLOWING_ORCHID_PLANT
                r < 0.90f -> SceneryType.JUNGLE_TORCH_BRAZIER
                else -> SceneryType.JUNGLE_CREATURE
            }
            Biome.DEEP_TROPICAL_FOREST -> when {
                r < 0.35f -> SceneryType.DENSE_TROPICAL_TREE
                r < 0.58f -> SceneryType.HANGING_GIANT_VINE
                r < 0.75f -> SceneryType.GLOWING_ORCHID_PLANT
                r < 0.88f -> SceneryType.MOSSY_BOULDER_CLIFF
                else -> SceneryType.JUNGLE_CREATURE
            }
            Biome.FORGOTTEN_TEMPLE_RUINS -> when {
                r < 0.30f -> SceneryType.ANCIENT_RUIN_PILLAR
                r < 0.50f -> SceneryType.TEMPLE_WALL_PARAPET
                r < 0.68f -> SceneryType.ANCIENT_TEMPLE_ARCH
                r < 0.80f -> SceneryType.JUNGLE_TORCH_BRAZIER
                r < 0.90f -> SceneryType.MOSSY_BOULDER_CLIFF
                else -> SceneryType.HANGING_GIANT_VINE
            }
            Biome.STONE_BRIDGE_VALLEY -> when {
                r < 0.32f -> SceneryType.WOODEN_ROPE_BRIDGE_POST
                r < 0.52f -> SceneryType.JUNGLE_WATERFALL
                r < 0.70f -> SceneryType.TEMPLE_WALL_PARAPET
                r < 0.84f -> SceneryType.MOSSY_BOULDER_CLIFF
                else -> SceneryType.DENSE_TROPICAL_TREE
            }
            Biome.WATERFALL_JUNGLE -> when {
                r < 0.36f -> SceneryType.JUNGLE_WATERFALL
                r < 0.56f -> SceneryType.DENSE_TROPICAL_TREE
                r < 0.72f -> SceneryType.WOODEN_ROPE_BRIDGE_POST
                r < 0.86f -> SceneryType.HANGING_GIANT_VINE
                else -> SceneryType.GLOWING_ORCHID_PLANT
            }
            Biome.OVERGROWN_TEMPLE_COURTYARD -> when {
                r < 0.28f -> SceneryType.TEMPLE_WALL_PARAPET
                r < 0.50f -> SceneryType.ANCIENT_RUIN_PILLAR
                r < 0.68f -> SceneryType.ANCIENT_TEMPLE_ARCH
                r < 0.82f -> SceneryType.JUNGLE_TORCH_BRAZIER
                r < 0.92f -> SceneryType.HANGING_GIANT_VINE
                else -> SceneryType.DENSE_TROPICAL_TREE
            }
            Biome.MYSTERIOUS_GOLDEN_TEMPLE -> when {
                r < 0.28f -> SceneryType.ANCIENT_GOLDEN_IDOL
                r < 0.48f -> SceneryType.ANCIENT_TEMPLE_ARCH
                r < 0.68f -> SceneryType.ANCIENT_RUIN_PILLAR
                r < 0.82f -> SceneryType.JUNGLE_TORCH_BRAZIER
                r < 0.92f -> SceneryType.TEMPLE_WALL_PARAPET
                else -> SceneryType.GLOWING_ORCHID_PLANT
            }
        }
    }

    private fun spawnMagicalBridgeSection(z: Float, tier: DifficultyTier) {
        val r = random.nextFloat()
        if (r < 0.55f) {
            // Bridge Chasm across 2 or 3 lanes with guiding coin arcs (and always jumpable!)
            val openLane = if (tier == DifficultyTier.EASY) random.nextInt(-1, 2) else -99
            for (l in -1..1) {
                if (l != openLane) {
                    obstacles.add(Obstacle(id = nextObstacleId++, lane = l, z = z, type = ObstacleType.BRIDGE_GAP))
                    spawnCoinArc(startZ = z - 3f, length = 4, lane = l, isArc = true)
                } else {
                    spawnCoinArc(startZ = z - 3f, length = 4, lane = l, isArc = false)
                }
            }
        } else {
            // Bridge guarded by ancient rune arch or crystal monoliths leaving a clear path
            val openLane = random.nextInt(-1, 2)
            for (l in -1..1) {
                if (l != openLane) {
                    val obsType = if (random.nextBoolean()) ObstacleType.TALL_CRYSTAL else ObstacleType.HIGH_ARCH
                    obstacles.add(Obstacle(id = nextObstacleId++, lane = l, z = z, type = obsType))
                } else {
                    spawnCoinArc(startZ = z - 4f, length = 4, lane = l, isArc = false)
                }
            }
        }
    }

    private fun selectRandomPowerUp(): PickupType {
        val r = random.nextFloat()
        return when {
            r < 0.34f -> PickupType.MAGNET // Magnetic Shield
            r < 0.66f -> PickupType.DOUBLE_COINS // Double Score
            r < 0.84f -> PickupType.STAR_RUSH // Dream Rush
            else -> PickupType.SHIELD // Aegis Ward
        }
    }

    private fun spawnObstacleGroup(z: Float, tier: DifficultyTier, isGauntlet: Boolean = false) {
        val roll = random.nextFloat()
        val singleThreshold = if (isGauntlet) 0.20f else when (tier) {
            DifficultyTier.EASY -> 0.58f
            DifficultyTier.MEDIUM -> 0.38f
            DifficultyTier.HARD -> 0.25f
            DifficultyTier.EXTREME -> 0.15f
        }
        val dualThreshold = when (tier) {
            DifficultyTier.EASY -> 0.80f
            DifficultyTier.MEDIUM -> 0.74f
            DifficultyTier.HARD -> 0.68f
            DifficultyTier.EXTREME -> 0.64f
        }

        if (roll < singleThreshold) {
            val blocked = random.nextInt(-1, 2)
            val type = ObstacleType.entries[random.nextInt(ObstacleType.entries.size)]
            obstacles.add(Obstacle(id = nextObstacleId++, lane = blocked, z = z, type = type))

            if ((type == ObstacleType.LOW_LOG || type == ObstacleType.BRIDGE_GAP || type == ObstacleType.HIGH_ARCH) &&
                random.nextFloat() < 0.32f
            ) {
                val puY = if (type == ObstacleType.HIGH_ARCH) 0.15f else 1.55f
                pickups.add(
                    Pickup(
                        id = nextPickupId++,
                        lane = blocked,
                        z = z,
                        y = puY,
                        type = selectRandomPowerUp()
                    )
                )
            }

            for (l in -1..1) {
                if (l != blocked) {
                    spawnCoinArc(z - 4f, length = 3, lane = l, isArc = false)
                }
            }
        } else if (roll < dualThreshold) {
            // Two lanes blocked, ONE guaranteed clear or jump/slide playable lane
            val open = random.nextInt(-1, 2)
            val sideObstacles = listOf(
                ObstacleType.LOW_LOG,
                ObstacleType.TALL_CRYSTAL,
                ObstacleType.MYSTIC_WISP,
                ObstacleType.HIGH_ARCH
            )
            for (l in -1..1) {
                if (l != open) {
                    val type = sideObstacles[random.nextInt(sideObstacles.size)]
                    obstacles.add(Obstacle(id = nextObstacleId++, lane = l, z = z, type = type))
                } else if ((tier == DifficultyTier.EXTREME || isGauntlet) && random.nextFloat() < 0.38f) {
                    val actionObs = if (random.nextBoolean()) ObstacleType.LOW_LOG else ObstacleType.HIGH_ARCH
                    obstacles.add(Obstacle(id = nextObstacleId++, lane = l, z = z, type = actionObs))
                }
            }
            if (random.nextFloat() < 0.26f) {
                pickups.add(
                    Pickup(
                        id = nextPickupId++,
                        lane = open,
                        z = z,
                        y = 0.25f,
                        type = selectRandomPowerUp()
                    )
                )
            } else {
                spawnCoinArc(z - 5f, length = 5, lane = open, isArc = (tier == DifficultyTier.EXTREME))
            }
        } else {
            // Full 3-lane Jump (Fallen Tree / Bridge Chasm) or Slide (Ancient Rune Barrier)
            val patternRoll = random.nextFloat()
            val type = when {
                patternRoll < 0.38f -> ObstacleType.LOW_LOG
                patternRoll < 0.68f -> ObstacleType.HIGH_ARCH
                else -> ObstacleType.BRIDGE_GAP
            }
            val isJumpAction = (type == ObstacleType.LOW_LOG || type == ObstacleType.BRIDGE_GAP)
            val powerUpLane = if (random.nextFloat() < 0.30f) random.nextInt(-1, 2) else -99
            val boosterLane = if (isJumpAction && random.nextFloat() < 0.45f && powerUpLane == -99) random.nextInt(-1, 2) else -99
            for (l in -1..1) {
                obstacles.add(Obstacle(id = nextObstacleId++, lane = l, z = z, type = type))
                val pType = when {
                    l == boosterLane -> PickupType.SKY_BOOSTER
                    l == powerUpLane -> selectRandomPowerUp()
                    else -> PickupType.COIN
                }
                pickups.add(
                    Pickup(
                        id = nextPickupId++,
                        lane = l,
                        z = z,
                        y = if (isJumpAction) 1.70f else 0.1f,
                        type = pType,
                        skyBoosterType = if (l == boosterLane) SkyBoosterType.randomBooster(random) else null
                    )
                )
            }
        }
    }

    private fun spawnCoinArc(startZ: Float, length: Int, lane: Int, isArc: Boolean) {
        for (i in 0 until length) {
            val z = startZ + i * 2.2f
            val y = if (isArc) {
                val mid = length / 2f
                (1.8f - abs(i - mid) * 0.45f).coerceAtLeast(0f)
            } else 0f
            pickups.add(Pickup(id = nextPickupId++, lane = lane, z = z, y = y, type = PickupType.COIN))
        }
    }

    // Particle Spawners
    private fun spawnAmbientWorldParticle() {
        if (particles.size > 48) return
        particles.add(
            VisualParticle(
                x = (random.nextFloat() - 0.5f) * 3.4f,
                y = 0.3f + random.nextFloat() * 3.2f,
                z = 0f,
                vx = (random.nextFloat() - 0.5f) * 0.45f,
                vy = 0.25f + random.nextFloat() * 0.55f,
                vz = 0f,
                color = currentBiome.ambientParticleColor.copy(alpha = 0.75f),
                life = 1.2f,
                maxLife = 1.2f,
                size = 3.5f + random.nextFloat() * 2.5f
            )
        )
    }

    private fun spawnLaneDust() {
        for (i in 0 until 7) {
            particles.add(
                VisualParticle(
                    x = laneX, y = 0.2f, z = 0f,
                    vx = -laneChangeDirection * (1.2f + random.nextFloat() * 2.2f),
                    vy = random.nextFloat() * 1.8f,
                    vz = 0f,
                    color = character.primaryColor,
                    life = 0.35f, maxLife = 0.35f, size = 5.5f
                )
            )
        }
    }

    private fun spawnJumpParticles() {
        for (i in 0 until 8) {
            particles.add(
                VisualParticle(
                    x = laneX, y = 0f, z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 2.5f,
                    vy = random.nextFloat() * 3f,
                    vz = 0f,
                    color = currentBiome.ambientParticleColor,
                    life = 0.4f, maxLife = 0.4f, size = 6f
                )
            )
        }
    }

    private fun spawnSlideParticles() {
        for (i in 0 until 6) {
            particles.add(
                VisualParticle(
                    x = laneX, y = 0f, z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 3f,
                    vy = random.nextFloat() * 1.2f,
                    vz = 0f,
                    color = AstralGold,
                    life = 0.35f, maxLife = 0.35f, size = 5f
                )
            )
        }
    }

    private fun spawnLandingDust() {
        for (i in 0 until 8) {
            particles.add(
                VisualParticle(
                    x = laneX + (random.nextFloat() - 0.5f) * 0.5f, y = 0f, z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 3.2f,
                    vy = random.nextFloat() * 1.6f,
                    vz = 0f,
                    color = Color.White.copy(alpha = 0.8f),
                    life = 0.32f, maxLife = 0.32f, size = 5f
                )
            )
        }
    }

    private fun spawnCoinSparkle(lane: Int) {
        for (i in 0 until 8) {
            particles.add(
                VisualParticle(
                    x = lane.toFloat(), y = playerY + 0.8f, z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 4f,
                    vy = (random.nextFloat() - 0.2f) * 4f,
                    vz = 0f,
                    color = AstralGold,
                    life = 0.45f, maxLife = 0.45f, size = 7f
                )
            )
        }
    }

    private fun spawnPowerUpBurst(lane: Int, color: Color) {
        for (i in 0 until 14) {
            particles.add(
                VisualParticle(
                    x = lane.toFloat(), y = playerY + 0.9f, z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 6f,
                    vy = (random.nextFloat() - 0.1f) * 6f,
                    vz = 0f,
                    color = if (i % 2 == 0) color else AstralGold,
                    life = 0.55f, maxLife = 0.55f, size = 8f
                )
            )
        }
    }

    private fun spawnBoostParticles() {
        particles.add(
            VisualParticle(
                x = laneX + (random.nextFloat() - 0.5f) * 0.8f,
                y = playerY + random.nextFloat() * 1.5f,
                z = 0f,
                vx = (random.nextFloat() - 0.5f) * 2f,
                vy = (random.nextFloat() - 0.5f) * 2f,
                vz = 0f,
                color = if (random.nextBoolean()) MagicPurpleLight else StarlightCyan,
                life = 0.4f, maxLife = 0.4f, size = 7f
            )
        )
    }

    private fun spawnShieldBurst() {
        for (i in 0 until 20) {
            particles.add(
                VisualParticle(
                    x = laneX, y = playerY + 1f, z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 7f,
                    vy = (random.nextFloat() - 0.5f) * 7f,
                    vz = 0f,
                    color = ShieldAzure,
                    life = 0.65f, maxLife = 0.65f, size = 8f
                )
            )
        }
    }

    private fun spawnSmashParticles(lane: Int, type: ObstacleType) {
        for (i in 0 until 18) {
            particles.add(
                VisualParticle(
                    x = lane.toFloat(), y = 1f, z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 6f,
                    vy = random.nextFloat() * 7f,
                    vz = 0f,
                    color = if (type == ObstacleType.LOW_LOG) DreamEmerald else StarlightCyan,
                    life = 0.55f, maxLife = 0.55f, size = 8f
                )
            )
        }
    }

    private fun spawnCrashParticles(lane: Int) {
        for (i in 0 until 24) {
            particles.add(
                VisualParticle(
                    x = lane.toFloat(), y = 1f, z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 8f,
                    vy = random.nextFloat() * 8f,
                    vz = 0f,
                    color = DangerCrimson,
                    life = 0.7f, maxLife = 0.7f, size = 9f
                )
            )
        }
    }

    private fun spawnHoverboardTrail() {
        particles.add(
            VisualParticle(
                x = laneX + (random.nextFloat() - 0.5f) * 0.45f,
                y = playerY + 0.15f + (random.nextFloat() - 0.5f) * 0.2f,
                z = 0f,
                vx = (random.nextFloat() - 0.5f) * 2.2f,
                vy = (random.nextFloat() - 0.2f) * 1.5f,
                vz = -0.5f,
                color = if (random.nextBoolean()) hoverboard.trailColor else hoverboard.glowColor,
                life = 0.38f,
                maxLife = 0.38f,
                size = 6.5f
            )
        )
    }

    private fun spawnHoverboardCrashBurst() {
        for (i in 0 until 24) {
            particles.add(
                VisualParticle(
                    x = laneX,
                    y = playerY + 0.5f,
                    z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 8f,
                    vy = (random.nextFloat() - 0.2f) * 7f,
                    vz = 0f,
                    color = if (i % 2 == 0) hoverboard.glowColor else hoverboard.trailColor,
                    life = 0.65f,
                    maxLife = 0.65f,
                    size = 9f
                )
            )
        }
    }

    private fun spawnMysteryBoxBurst(lane: Int, glowColor: Color) {
        for (i in 0 until 28) {
            particles.add(
                VisualParticle(
                    x = lane.toFloat(),
                    y = playerY + 1.1f,
                    z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 9f,
                    vy = (random.nextFloat() - 0.1f) * 8f,
                    vz = 0f,
                    color = if (i % 3 == 0) AstralGold else if (i % 3 == 1) glowColor else Color.White,
                    life = 0.75f,
                    maxLife = 0.75f,
                    size = 9.5f
                )
            )
        }
    }

    private fun spawnSkyBoosterBurst(lane: Int, glowColor: Color) {
        for (i in 0 until 20) {
            particles.add(
                VisualParticle(
                    x = lane.toFloat(),
                    y = playerY + 1.2f,
                    z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 7f,
                    vy = (random.nextFloat() - 0.5f) * 7f,
                    vz = 0f,
                    color = if (i % 2 == 0) glowColor else Color.White,
                    life = 0.6f,
                    maxLife = 0.6f,
                    size = 8.5f
                )
            )
        }
    }

    private fun spawnAnimalTokenSparkle(lane: Int) {
        for (i in 0 until 12) {
            particles.add(
                VisualParticle(
                    x = lane.toFloat(),
                    y = playerY + 0.8f,
                    z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 5f,
                    vy = (random.nextFloat() - 0.2f) * 5f,
                    vz = 0f,
                    color = if (i % 2 == 0) DreamEmerald else AstralGold,
                    life = 0.5f,
                    maxLife = 0.5f,
                    size = 7f
                )
            )
        }
    }

    private fun spawnTransformationParticles() {
        for (i in 0 until 32) {
            particles.add(
                VisualParticle(
                    x = laneX + (random.nextFloat() - 0.5f) * 1.6f,
                    y = playerY + 0.3f + random.nextFloat() * 1.8f,
                    z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 6f,
                    vy = 1.5f + random.nextFloat() * 4f,
                    vz = 0f,
                    color = if (i % 3 == 0) DreamEmerald else if (i % 3 == 1) AstralGold else Color(0xFF69F0AE),
                    life = 0.8f,
                    maxLife = 0.8f,
                    size = 8.5f
                )
            )
        }
    }

    private fun spawnCheetahSparks() {
        particles.add(
            VisualParticle(
                x = laneX + (random.nextFloat() - 0.5f) * 0.6f,
                y = playerY + 0.2f + random.nextFloat() * 0.4f,
                z = 0f,
                vx = (random.nextFloat() - 0.5f) * 4f,
                vy = random.nextFloat() * 2f,
                vz = -1f,
                color = if (random.nextBoolean()) AstralGold else Color(0xFFFFD54F),
                life = 0.25f,
                maxLife = 0.25f,
                size = 6f
            )
        )
    }

    private fun spawnSkyLauncherParticles() {
        for (i in 0 until 3) {
            particles.add(
                VisualParticle(
                    x = laneX + (random.nextFloat() - 0.5f) * 0.8f,
                    y = playerY - 0.5f - random.nextFloat() * 0.6f,
                    z = 0f,
                    vx = (random.nextFloat() - 0.5f) * 2f,
                    vy = -3f - random.nextFloat() * 2f,
                    vz = 0f,
                    color = if (i % 2 == 0) Color(0xFF00E5FF) else Color.White,
                    life = 0.35f,
                    maxLife = 0.35f,
                    size = 6.5f
                )
            )
        }
    }

    private fun spawnSkyGliderParticles() {
        particles.add(
            VisualParticle(
                x = laneX + (random.nextFloat() - 0.5f) * 0.6f,
                y = playerY - 0.3f,
                z = 0f,
                vx = (random.nextFloat() - 0.5f) * 1.5f,
                vy = -1.2f,
                vz = 0f,
                color = DreamEmerald.copy(alpha = 0.8f),
                life = 0.4f,
                maxLife = 0.4f,
                size = 6f
            )
        )
    }
}
