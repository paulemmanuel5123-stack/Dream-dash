package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.MagicPurpleLight
import com.example.ui.theme.ShieldAzure
import com.example.ui.theme.StarlightCyan

enum class GamePhase {
    INTRO,
    MENU,
    CHARACTER_SHOP,
    PLAYING,
    PAUSED,
    GAME_OVER,
    MYSTERY_BOXES,
    ANIMAL_GALLERY,
    MISSIONS
}

enum class ActionState {
    IDLE,
    RUNNING,
    JUMPING,
    LANDING,
    SLIDING,
    LANE_CHANGE,
    CRASHED
}

enum class CharacterAnimState(val displayName: String) {
    IDLE("Idle"),
    RUNNING("Run"),
    JUMPING("Jump"),
    LANDING("Land"),
    SLIDING("Slide"),
    LANE_CHANGE("Dodge"),
    GAME_OVER("Game Over")
}

enum class DifficultyTier(
    val displayName: String,
    val speedRangeLabel: String,
    val badgeColor: Color
) {
    EASY("Easy", "20–30 km/h", DreamEmerald),
    MEDIUM("Medium", "30–45 km/h", StarlightCyan),
    HARD("Hard", "45–60 km/h", AstralGold),
    EXTREME("Extreme", "60–70 km/h", Color(0xFFFF5252));

    companion object {
        fun forSurvivalTime(seconds: Float): DifficultyTier = when {
            seconds < 30f -> EASY
            seconds < 90f -> MEDIUM
            seconds < 180f -> HARD
            else -> EXTREME
        }
    }
}

data class CharacterInfo(
    val id: String,
    val name: String,
    val title: String,
    val roleLabel: String,
    val priceCoins: Int,
    val description: String,
    val perkName: String,
    val perkDescription: String,
    val gearLoadout: List<String>,
    @DrawableRes val avatarRes: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
) {
    val isFreeStarter: Boolean
        get() = priceCoins == 0

    val formattedPrice: String
        get() = if (priceCoins <= 0) "FREE" else "%,d Coins".format(priceCoins)

    companion object {
        val KAEL = CharacterInfo(
            id = "kael",
            name = "Hunter Boy",
            title = "Kael — The Swift Hunter",
            roleLabel = "Fantasy Hunter",
            priceCoins = 0,
            description = "An adventurous, heroic young fantasy hunter in lightweight leather adventure gear and emerald tunic. Carries a compact explorer backpack, ash-wood bow & quiver of arrows, and rugged fantasy boots.",
            perkName = "Hunter's Magnetism",
            perkDescription = "+40% coin attraction radius & agile hunter instincts",
            gearLoadout = listOf(
                "Hunter Tunic & Leather Gear",
                "Bow & Quiver on Back",
                "Small Adventure Backpack",
                "Rugged Fantasy Boots",
                "Brass Compass & Pouches"
            ),
            avatarRes = R.drawable.img_kael_hunter_1790888828151,
            primaryColor = DreamEmerald,
            secondaryColor = AstralGold,
            accentColor = Color(0xFF8D6E63)
        )

        val LYRA = CharacterInfo(
            id = "lyra",
            name = "Fantasy Adventurer Girl",
            title = "Lyra — Starlight Adventurer",
            roleLabel = "Fantasy Adventurer",
            priceCoins = 1000,
            description = "A friendly, confident young fantasy adventurer with a signature braided starlight ponytail. Wears an elegant cyan-indigo adventure outfit, fitted light gloves, adventure boots, and a floating Magical Astrolabe.",
            perkName = "Starlight Ward",
            perkDescription = "Starts every run with a protective Starlight Shield",
            gearLoadout = listOf(
                "Elegant Adventure Outfit",
                "Braided Starlight Ponytail",
                "Floating Magical Astrolabe",
                "Adventure Boots & Light Gloves",
                "Starlight Amulet Brooch"
            ),
            avatarRes = R.drawable.img_lyra_ranger_1790888839544,
            primaryColor = StarlightCyan,
            secondaryColor = ShieldAzure,
            accentColor = Color(0xFFD1C4E9)
        )

        val SHADOW_RUNNER = CharacterInfo(
            id = "shadow_runner",
            name = "Shadow Runner",
            title = "Vex — The Twilight Phantom",
            roleLabel = "Shadow Runner",
            priceCoins = 2500,
            description = "A swift twilight-realm scout cloaked in obsidian-violet stealth leather and a flowing dusk scarf. Carries twin glowing amethyst shadow-blades and moves between lanes like a phantom.",
            perkName = "Shadow Step",
            perkDescription = "+25% faster lane-dodge & extended post-hit shadow phase",
            gearLoadout = listOf(
                "Obsidian-Violet Stealth Tunic",
                "Flowing Twilight Scarf",
                "Twin Amethyst Shadow-Blades",
                "Silent Phantom Strider Boots",
                "Glowing Night-Rune Mask"
            ),
            avatarRes = R.drawable.img_kael_hunter_1790888828151,
            primaryColor = MagicPurpleLight,
            secondaryColor = Color(0xFFE040FB),
            accentColor = Color(0xFF311B92)
        )

        val CRYSTAL_WARRIOR = CharacterInfo(
            id = "crystal_warrior",
            name = "Crystal Warrior",
            title = "Seraphina — Prism Vanguard",
            roleLabel = "Crystal Warrior",
            priceCoins = 5000,
            description = "A radiant champion of Crystal Valley clad in lightweight prism-forged armor and sapphire silk. Wields a glowing Starlight Greatsword on her back and an orbiting Prism Halo.",
            perkName = "Prism Aegis",
            perkDescription = "Starts with Aegis Shield & +20% bonus score from crystals",
            gearLoadout = listOf(
                "Faceted Prism Pauldrons",
                "Starlight Crystal Greatsword",
                "Orbiting Diamond Halo",
                "Sapphire Vanguard Greaves",
                "Radiant Core Breastplate"
            ),
            avatarRes = R.drawable.img_lyra_ranger_1790888839544,
            primaryColor = Color(0xFF40C4FF),
            secondaryColor = Color(0xFFE1F5FE),
            accentColor = Color(0xFF0288D1)
        )

        val FOREST_GUARDIAN = CharacterInfo(
            id = "forest_guardian",
            name = "Forest Guardian",
            title = "Thorne — Ancient Grove Warden",
            roleLabel = "Forest Guardian",
            priceCoins = 8000,
            description = "A mystical protector of the Enchanted Forest wearing living emerald-bark armor, a golden antler circlet, and a blossoming Heartwood Staff that attracts nature's treasures.",
            perkName = "Grove's Bounty",
            perkDescription = "+60% coin magnet range & starts with Magnetic Shield",
            gearLoadout = listOf(
                "Living Emerald-Bark Armor",
                "Golden Antler Crown Circlet",
                "Blossoming Heartwood Staff",
                "Root-Bound Strider Boots",
                "Orbiting Amber Leaf Sprites"
            ),
            avatarRes = R.drawable.img_kael_hunter_1790888828151,
            primaryColor = Color(0xFF69F0AE),
            secondaryColor = AstralGold,
            accentColor = Color(0xFF2E7D32)
        )

        val SKY_RUNNER = CharacterInfo(
            id = "sky_runner",
            name = "Sky Runner",
            title = "Zephyr — Celestial Wind-Dancer",
            roleLabel = "Sky Runner",
            priceCoins = 12000,
            description = "A legendary hero of the Floating Islands adorned in solar-gold and cloud-white regalia, bearing luminous Aether Wings and a floating Celestial Wind-Crown.",
            perkName = "Celestial Tailwind",
            perkDescription = "Starts with Magnetic Shield + Double Score boost active!",
            gearLoadout = listOf(
                "Luminous Aether Feather Wings",
                "Solar-Gold & Cloud Regalia",
                "Floating Celestial Wind-Crown",
                "Winged Valkyrie Strider Boots",
                "Astral Gale Gauntlets"
            ),
            avatarRes = R.drawable.img_lyra_ranger_1790888839544,
            primaryColor = AstralGold,
            secondaryColor = StarlightCyan,
            accentColor = Color(0xFFFFF8E1)
        )

        val ALL = listOf(
            KAEL,
            LYRA,
            SHADOW_RUNNER,
            CRYSTAL_WARRIOR,
            FOREST_GUARDIAN,
            SKY_RUNNER
        )

        fun find(id: String): CharacterInfo = ALL.find { it.id == id } ?: KAEL
    }
}

enum class HoverboardType {
    DREAM,
    CRYSTAL,
    FIRE,
    ICE,
    SHADOW,
    SKY,
    YORUBA_LEGEND
}

data class HoverboardInfo(
    val id: String,
    val name: String,
    val title: String,
    val priceCoins: Int,
    val specialAbility: String,
    val abilityDescription: String,
    val durationSeconds: Float,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val trailColor: Color,
    val type: HoverboardType
) {
    val isFreeStarter: Boolean get() = priceCoins == 0
    val formattedPrice: String get() = if (priceCoins <= 0) "FREE" else "%,d Coins".format(priceCoins)

    companion object {
        val DREAM_BOARD = HoverboardInfo(
            id = "dream_board",
            name = "Dream Board",
            title = "Starter Fantasy Board",
            priceCoins = 0,
            specialAbility = "Standard Glide",
            abilityDescription = "Simple magical fantasy design that glides smoothly over obstacles.",
            durationSeconds = 15f,
            primaryColor = StarlightCyan,
            secondaryColor = MagicPurpleLight,
            glowColor = Color(0xFF80D8FF),
            trailColor = StarlightCyan,
            type = HoverboardType.DREAM
        )

        val CRYSTAL_BOARD = HoverboardInfo(
            id = "crystal_board",
            name = "Crystal Board",
            title = "Prismatic Shard Board",
            priceCoins = 2000,
            specialAbility = "Sparkling Magnetic Draw",
            abilityDescription = "Made with glowing fantasy crystals. Leaves a sparkling trail and improves coin collection.",
            durationSeconds = 16f,
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFFB388FF),
            glowColor = Color(0xFFE1F5FE),
            trailColor = Color(0xFF40C4FF),
            type = HoverboardType.CRYSTAL
        )

        val FIRE_BOARD = HoverboardInfo(
            id = "fire_board",
            name = "Fire Board",
            title = "Blazing Phoenix Board",
            priceCoins = 4000,
            specialAbility = "Flame Burst Speed Surge",
            abilityDescription = "Fiery fantasy design with blazing flame trail. Gives a short speed boost when activated.",
            durationSeconds = 15f,
            primaryColor = Color(0xFFFF3D00),
            secondaryColor = AstralGold,
            glowColor = Color(0xFFFFAB00),
            trailColor = Color(0xFFFF6E40),
            type = HoverboardType.FIRE
        )

        val ICE_BOARD = HoverboardInfo(
            id = "ice_board",
            name = "Ice Board",
            title = "Glacial Frost Board",
            priceCoins = 6000,
            specialAbility = "Glacial Armor Barrier",
            abilityDescription = "Frozen magical design leaving an icy trail. Provides improved obstacle protection.",
            durationSeconds = 16f,
            primaryColor = Color(0xFF84FFFF),
            secondaryColor = Color(0xFF0091EA),
            glowColor = Color(0xFFE0F7FA),
            trailColor = Color(0xFF00B0FF),
            type = HoverboardType.ICE
        )

        val SHADOW_BOARD = HoverboardInfo(
            id = "shadow_board",
            name = "Shadow Board",
            title = "Twilight Phantom Board",
            priceCoins = 8000,
            specialAbility = "Shadow Phase Invincibility",
            abilityDescription = "Dark magical design with shadow trail. Gives a short temporary invincibility effect.",
            durationSeconds = 16f,
            primaryColor = Color(0xFF7C4DFF),
            secondaryColor = Color(0xFF212121),
            glowColor = Color(0xFFE040FB),
            trailColor = Color(0xFF651FFF),
            type = HoverboardType.SHADOW
        )

        val SKY_BOARD = HoverboardInfo(
            id = "sky_board",
            name = "Sky Board",
            title = "Aether Wind Board",
            priceCoins = 12000,
            specialAbility = "Extended Cloud Flight",
            abilityDescription = "Floating magical design leaving a cloud/light trail. Gives a longer 25s hoverboard duration.",
            durationSeconds = 25f,
            primaryColor = Color(0xFFFFF9C4),
            secondaryColor = AstralGold,
            glowColor = Color(0xFF80D8FF),
            trailColor = Color(0xFFFFF176),
            type = HoverboardType.SKY
        )

        val YORUBA_LEGEND_BOARD = HoverboardInfo(
            id = "yoruba_legend_board",
            name = "Yoruba Legend Board",
            title = "Golden Ile-Ife Relic Board",
            priceCoins = 15000,
            specialAbility = "Royal Golden Fortune",
            abilityDescription = "Inspired by Yoruba bronze patterns, cowrie-gold inlays and warm terracotta motifs. Increases coin collection while active.",
            durationSeconds = 20f,
            primaryColor = Color(0xFFFFD700),
            secondaryColor = Color(0xFF8D5B28),
            glowColor = Color(0xFFFFECB3),
            trailColor = Color(0xFFFFC107),
            type = HoverboardType.YORUBA_LEGEND
        )

        val ALL = listOf(
            DREAM_BOARD,
            CRYSTAL_BOARD,
            FIRE_BOARD,
            ICE_BOARD,
            SHADOW_BOARD,
            SKY_BOARD,
            YORUBA_LEGEND_BOARD
        )

        fun find(id: String): HoverboardInfo = ALL.find { it.id == id } ?: DREAM_BOARD
    }
}

enum class ObstacleType(
    val displayName: String,
    val avoidance: String,
    val baseWidth: Float,
    val baseHeight: Float
) {
    LOW_LOG("Fallen Jungle Log", "Jump to clear", 0.80f, 0.45f),
    HIGH_ARCH("Hanging Vine Arch", "Slide underneath", 0.86f, 0.72f),
    TALL_CRYSTAL("Ancient Jungle Pillar", "Change lane", 0.68f, 1.25f),
    MYSTIC_WISP("Mossy River Rock & Roots", "Change lane", 0.70f, 0.92f),
    BRIDGE_GAP("Wooden Bridge Chasm", "Jump across", 0.88f, 0.25f)
}

enum class TrackSectionType(val displayName: String) {
    SAFE_HAVEN("Sunlit Jungle Clearing"),
    MAGICAL_BRIDGE("Wooden Rope Bridge"),
    STANDARD_RUN("Overgrown Jungle Trail"),
    CHALLENGE_GAUNTLET("Ancient Ruins Gauntlet")
}

data class Obstacle(
    val id: Long,
    val lane: Int, // -1 (Left), 0 (Center), 1 (Right)
    var z: Float,
    val type: ObstacleType,
    var destroyed: Boolean = false
)

enum class PickupType(
    val displayName: String,
    val shortBadge: String,
    val effectSummary: String,
    val durationSeconds: Float,
    val color: Color
) {
    COIN("Dream Coin", "COIN", "+1 Coin & +50 Score", 0f, AstralGold),
    MAGNET("Magnetic Shield", "MAG SHIELD", "Pulls coins across all lanes & grants magnetic barrier", 10f, StarlightCyan),
    SHIELD("Aegis Ward", "AEGIS WARD", "Protective starlight ward that shatters 1 obstacle", 12f, ShieldAzure),
    STAR_RUSH("Dream Rush", "DREAM RUSH", "Invincible speed surge that smashes obstacles (+150 pts)", 7f, Color(0xFFFF5252)),
    DOUBLE_COINS("Double Score", "2X SCORE", "2x Score & 2x Coins from all sources", 12f, Color(0xFFFFB74D)),
    MYSTERY_BOX("Jungle Mystery Box", "MYSTERY BOX", "Magical treasure box with randomized surprise rewards", 0f, AstralGold),
    SKY_BOOSTER("Jungle Sky Booster", "SKY BOOSTER", "Ancient suspended relic granting a powerful airborne ability", 10f, Color(0xFF00E5FF)),
    ANIMAL_TOKEN("Animal Energy Token", "ANIMAL ENERGY", "Charges the Primal Transformation meter by +20%", 0f, DreamEmerald);

    companion object {
        val POWER_UPS = listOf(MAGNET, DOUBLE_COINS, SHIELD, STAR_RUSH)
    }
}

data class Pickup(
    val id: Long,
    val lane: Int,
    var z: Float,
    var y: Float = 0f,
    val type: PickupType,
    var collected: Boolean = false,
    var visualX: Float = lane.toFloat(),
    val mysteryBoxRarity: MysteryBoxRarity? = null,
    val skyBoosterType: SkyBoosterType? = null
)

enum class Biome(
    val stageNumber: Int,
    val displayName: String,
    val groundColor: Color,
    val pathColor: Color,
    val horizonColor: Color,
    val skyTopColor: Color,
    val ambientParticleColor: Color,
    val mountainColor: Color,
    val stoneTileColor: Color = Color(0xFF4A554A),
    val wallColor: Color = Color(0xFF37474F),
    val sunbeamColor: Color = Color(0xFFFFF9C4)
) {
    ANCIENT_JUNGLE_ENTRANCE(
        stageNumber = 1,
        displayName = "Ancient Jungle Entrance",
        groundColor = Color(0xFF1B3820),
        pathColor = Color(0xFF3A3022),
        horizonColor = Color(0xFF26582B),
        skyTopColor = Color(0xFF123B52),
        ambientParticleColor = Color(0xFF76FF03),
        mountainColor = Color(0xFF1B4323),
        stoneTileColor = Color(0xFF4E5948),
        wallColor = Color(0xFF3A4738),
        sunbeamColor = Color(0xFFFFF59D)
    ),
    DEEP_TROPICAL_FOREST(
        stageNumber = 2,
        displayName = "Deep Tropical Forest",
        groundColor = Color(0xFF0F2618),
        pathColor = Color(0xFF2A281E),
        horizonColor = Color(0xFF134A29),
        skyTopColor = Color(0xFF09291B),
        ambientParticleColor = Color(0xFF00E676),
        mountainColor = Color(0xFF0E381E),
        stoneTileColor = Color(0xFF3E4E38),
        wallColor = Color(0xFF2E3D2A),
        sunbeamColor = Color(0xFFE8F5E9)
    ),
    FORGOTTEN_TEMPLE_RUINS(
        stageNumber = 3,
        displayName = "Forgotten Temple Ruins",
        groundColor = Color(0xFF222923),
        pathColor = Color(0xFF424A40),
        horizonColor = Color(0xFF374E3C),
        skyTopColor = Color(0xFF192922),
        ambientParticleColor = Color(0xFFFFD54F),
        mountainColor = Color(0xFF2B3830),
        stoneTileColor = Color(0xFF5A6258),
        wallColor = Color(0xFF455A64),
        sunbeamColor = Color(0xFFFFE082)
    ),
    STONE_BRIDGE_VALLEY(
        stageNumber = 4,
        displayName = "Stone Bridge Valley",
        groundColor = Color(0xFF102830),
        pathColor = Color(0xFF254855),
        horizonColor = Color(0xFF1D5D72),
        skyTopColor = Color(0xFF0B3040),
        ambientParticleColor = Color(0xFF40C4FF),
        mountainColor = Color(0xFF163E4D),
        stoneTileColor = Color(0xFF37474F),
        wallColor = Color(0xFF263238),
        sunbeamColor = Color(0xFFB3E5FC)
    ),
    WATERFALL_JUNGLE(
        stageNumber = 5,
        displayName = "Waterfall Jungle",
        groundColor = Color(0xFF0D2D2A),
        pathColor = Color(0xFF1B4E48),
        horizonColor = Color(0xFF136860),
        skyTopColor = Color(0xFF0A3330),
        ambientParticleColor = Color(0xFF00E5FF),
        mountainColor = Color(0xFF10443E),
        stoneTileColor = Color(0xFF2D5954),
        wallColor = Color(0xFF1E3F3B),
        sunbeamColor = Color(0xFF80DEEA)
    ),
    OVERGROWN_TEMPLE_COURTYARD(
        stageNumber = 6,
        displayName = "Overgrown Temple Courtyard",
        groundColor = Color(0xFF1E261B),
        pathColor = Color(0xFF384332),
        horizonColor = Color(0xFF3B562F),
        skyTopColor = Color(0xFF162E19),
        ambientParticleColor = Color(0xFFAEEA00),
        mountainColor = Color(0xFF293F23),
        stoneTileColor = Color(0xFF54624B),
        wallColor = Color(0xFF3E4F36),
        sunbeamColor = Color(0xFFDCEDC8)
    ),
    MYSTERIOUS_GOLDEN_TEMPLE(
        stageNumber = 7,
        displayName = "Mysterious Golden Temple",
        groundColor = Color(0xFF2A1C0A),
        pathColor = Color(0xFF4A3416),
        horizonColor = Color(0xFF6B4512),
        skyTopColor = Color(0xFF2B1604),
        ambientParticleColor = Color(0xFFFFD700),
        mountainColor = Color(0xFF42280A),
        stoneTileColor = Color(0xFF755524),
        wallColor = Color(0xFF5C3E14),
        sunbeamColor = Color(0xFFFFD54F)
    );

    companion object {
        fun forDistance(meters: Int): Biome {
            return when {
                meters < 450 -> ANCIENT_JUNGLE_ENTRANCE
                meters < 1000 -> DEEP_TROPICAL_FOREST
                meters < 1700 -> FORGOTTEN_TEMPLE_RUINS
                meters < 2500 -> STONE_BRIDGE_VALLEY
                meters < 3400 -> WATERFALL_JUNGLE
                meters < 4400 -> OVERGROWN_TEMPLE_COURTYARD
                else -> MYSTERIOUS_GOLDEN_TEMPLE
            }
        }

        fun nextBiome(current: Biome): Biome {
            val nextIdx = (current.ordinal + 1).coerceAtMost(entries.size - 1)
            return entries[nextIdx]
        }
    }
}

data class VisualParticle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    val color: Color,
    var life: Float,
    val maxLife: Float,
    val size: Float
)

data class SceneryObject(
    val id: Long,
    val side: Int, // -1: Left of track, +1: Right of track
    val lateralOffset: Float,
    var z: Float,
    val type: SceneryType,
    val scale: Float
)

enum class SceneryType {
    DENSE_TROPICAL_TREE,
    HANGING_GIANT_VINE,
    ANCIENT_RUIN_PILLAR,
    JUNGLE_WATERFALL,
    GLOWING_ORCHID_PLANT,
    MOSSY_BOULDER_CLIFF,
    WOODEN_ROPE_BRIDGE_POST,
    JUNGLE_CREATURE,
    ANCIENT_TEMPLE_ARCH,
    TEMPLE_WALL_PARAPET,
    ANCIENT_GOLDEN_IDOL,
    JUNGLE_TORCH_BRAZIER
}
