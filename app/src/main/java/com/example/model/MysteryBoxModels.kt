package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.MagicPurpleLight
import com.example.ui.theme.ShieldAzure
import com.example.ui.theme.StarlightCyan
import kotlin.random.Random

enum class MysteryBoxRarity(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val weight: Int
) {
    COMMON(
        displayName = "Common Mystery Box",
        primaryColor = Color(0xFF8D6E63),
        secondaryColor = StarlightCyan,
        glowColor = Color(0xFF80DEEA),
        weight = 60
    ),
    RARE(
        displayName = "Rare Mystery Box",
        primaryColor = DreamEmerald,
        secondaryColor = Color(0xFFA7FFEB),
        glowColor = Color(0xFF69F0AE),
        weight = 25
    ),
    EPIC(
        displayName = "Epic Mystery Box",
        primaryColor = MagicPurpleLight,
        secondaryColor = Color(0xFFEA80FC),
        glowColor = Color(0xFFE040FB),
        weight = 11
    ),
    LEGENDARY(
        displayName = "Legendary Mystery Box",
        primaryColor = AstralGold,
        secondaryColor = Color(0xFFFFD54F),
        glowColor = Color(0xFFFFE57F),
        weight = 4
    );

    companion object {
        fun rollRarity(random: Random = Random.Default): MysteryBoxRarity {
            val total = entries.sumOf { it.weight }
            var roll = random.nextInt(total)
            for (rarity in entries) {
                if (roll < rarity.weight) return rarity
                roll -= rarity.weight
            }
            return COMMON
        }
    }
}

enum class MysteryBoxRewardType {
    COINS_100,
    COINS_250,
    COINS_500,
    COINS_1000,
    TEMPORARY_MAGNET,
    TEMPORARY_SHIELD,
    TEMPORARY_MULTIPLIER,
    TEMPORARY_SPEED_BOOST,
    HOVERBOARD_RUN,
    RARE_COSMETIC
}

data class MysteryBoxReward(
    val type: MysteryBoxRewardType,
    val title: String,
    val description: String,
    val coins: Int = 0,
    val rarity: MysteryBoxRarity,
    val cosmeticId: String? = null,
    val highlightColor: Color = AstralGold
) {
    companion object {
        fun generateReward(
            rarity: MysteryBoxRarity,
            unlockedCosmetics: Set<String> = emptySet(),
            random: Random = Random.Default
        ): MysteryBoxReward {
            return when (rarity) {
                MysteryBoxRarity.COMMON -> {
                    when (random.nextInt(100)) {
                        in 0..45 -> MysteryBoxReward(
                            type = MysteryBoxRewardType.COINS_100,
                            title = "100 Gold Coins",
                            description = "Jungle treasure stash added to your pouch!",
                            coins = 100,
                            rarity = rarity,
                            highlightColor = AstralGold
                        )
                        in 46..75 -> MysteryBoxReward(
                            type = MysteryBoxRewardType.COINS_250,
                            title = "250 Gold Coins",
                            description = "Ancient coin satchel discovered!",
                            coins = 250,
                            rarity = rarity,
                            highlightColor = AstralGold
                        )
                        in 76..88 -> MysteryBoxReward(
                            type = MysteryBoxRewardType.TEMPORARY_MAGNET,
                            title = "Temporary Coin Magnet",
                            description = "Attracts all nearby jungle coins for 12 seconds!",
                            rarity = rarity,
                            highlightColor = StarlightCyan
                        )
                        else -> MysteryBoxReward(
                            type = MysteryBoxRewardType.TEMPORARY_SHIELD,
                            title = "Temporary Jungle Shield",
                            description = "Ancient wooden ward absorbs one obstacle collision!",
                            rarity = rarity,
                            highlightColor = ShieldAzure
                        )
                    }
                }
                MysteryBoxRarity.RARE -> {
                    when (random.nextInt(100)) {
                        in 0..35 -> MysteryBoxReward(
                            type = MysteryBoxRewardType.COINS_250,
                            title = "250 Gold Coins",
                            description = "Ancient coin satchel discovered!",
                            coins = 250,
                            rarity = rarity,
                            highlightColor = AstralGold
                        )
                        in 36..65 -> MysteryBoxReward(
                            type = MysteryBoxRewardType.COINS_500,
                            title = "500 Gold Coins",
                            description = "Hefty chest of glittering Aztec coins!",
                            coins = 500,
                            rarity = rarity,
                            highlightColor = AstralGold
                        )
                        in 66..82 -> MysteryBoxReward(
                            type = MysteryBoxRewardType.TEMPORARY_MULTIPLIER,
                            title = "Temporary Score Multiplier",
                            description = "Double score points for 15 seconds!",
                            rarity = rarity,
                            highlightColor = Color(0xFFFFB74D)
                        )
                        else -> MysteryBoxReward(
                            type = MysteryBoxRewardType.TEMPORARY_SPEED_BOOST,
                            title = "Temporary Speed Boost",
                            description = "Rush forward with an ethereal glowing trail!",
                            rarity = rarity,
                            highlightColor = Color(0xFFFF5252)
                        )
                    }
                }
                MysteryBoxRarity.EPIC -> {
                    when (random.nextInt(100)) {
                        in 0..30 -> MysteryBoxReward(
                            type = MysteryBoxRewardType.COINS_500,
                            title = "500 Gold Coins",
                            description = "Hefty chest of glittering Aztec coins!",
                            coins = 500,
                            rarity = rarity,
                            highlightColor = AstralGold
                        )
                        in 31..60 -> MysteryBoxReward(
                            type = MysteryBoxRewardType.COINS_1000,
                            title = "1,000 Gold Coins!",
                            description = "Grand treasure jackpot from the lost ruins!",
                            coins = 1000,
                            rarity = rarity,
                            highlightColor = AstralGold
                        )
                        in 61..80 -> MysteryBoxReward(
                            type = MysteryBoxRewardType.HOVERBOARD_RUN,
                            title = "Hoverboard for Current Run",
                            description = "Deploys a magical floating board instantly!",
                            rarity = rarity,
                            highlightColor = Color(0xFF80D8FF)
                        )
                        else -> MysteryBoxReward(
                            type = MysteryBoxRewardType.TEMPORARY_SHIELD,
                            title = "Grand Aegis Ward",
                            description = "Powerful starlight barrier protects you!",
                            rarity = rarity,
                            highlightColor = ShieldAzure
                        )
                    }
                }
                MysteryBoxRarity.LEGENDARY -> {
                    when (random.nextInt(100)) {
                        in 0..40 -> MysteryBoxReward(
                            type = MysteryBoxRewardType.COINS_1000,
                            title = "1,000 Gold Coins!",
                            description = "Radiant hoard of pure legendary gold!",
                            coins = 1000,
                            rarity = rarity,
                            highlightColor = AstralGold
                        )
                        in 41..70 -> MysteryBoxReward(
                            type = MysteryBoxRewardType.HOVERBOARD_RUN,
                            title = "Golden Relic Hoverboard",
                            description = "Summons the legendary Yoruba golden board for the run!",
                            rarity = rarity,
                            highlightColor = AstralGold
                        )
                        else -> {
                            val cosmetic = "sunfire_shaman_crown"
                            if (cosmetic in unlockedCosmetics) {
                                MysteryBoxReward(
                                    type = MysteryBoxRewardType.COINS_1000,
                                    title = "2,500 Bonus Gold Coins!",
                                    description = "Duplicate legendary cosmetic converted into massive gold fortune!",
                                    coins = 2500,
                                    rarity = rarity,
                                    highlightColor = AstralGold
                                )
                            } else {
                                MysteryBoxReward(
                                    type = MysteryBoxRewardType.RARE_COSMETIC,
                                    title = "Sunfire Shaman Crown",
                                    description = "Rare cosmetic relic! Golden feather headdress that radiates sun rays.",
                                    rarity = rarity,
                                    cosmeticId = cosmetic,
                                    highlightColor = Color(0xFFFFD700)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
