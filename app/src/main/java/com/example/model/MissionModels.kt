package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.StarlightCyan

enum class MissionCategory(val displayName: String, val badgeColor: Color) {
    DAILY("Daily Missions", StarlightCyan),
    WEEKLY("Weekly Missions", AstralGold),
    ACHIEVEMENT("Special Achievements", DreamEmerald)
}

enum class MissionStatus {
    IN_PROGRESS,
    CLAIM_REWARD,
    COMPLETED
}

enum class MissionMetric {
    COINS_RUN,
    COINS_TOTAL,
    METERS_RUN,
    METERS_TOTAL,
    MYSTERY_BOXES_TOTAL,
    SKY_BOOSTERS_USED,
    ANIMAL_TOKENS_COLLECTED,
    ANIMAL_TRANSFORMATIONS,
    HOVERBOARD_ACTIVATED,
    RUNS_COMPLETED,
    HIGH_SCORE_REACHED,
    ANIMALS_UNLOCKED_COUNT,
    HOVERBOARDS_UNLOCKED_COUNT
}

data class MissionDefinition(
    val id: String,
    val category: MissionCategory,
    val title: String,
    val description: String,
    val target: Int,
    val metric: MissionMetric,
    val rewardCoins: Int,
    val rewardAnimalId: String? = null,
    val rewardCosmeticName: String? = null
) {
    companion object {
        val ALL: List<MissionDefinition> = listOf(
            // --- DAILY MISSIONS ---
            MissionDefinition(
                id = "daily_coins_100",
                category = MissionCategory.DAILY,
                title = "Treasure Gathering",
                description = "Collect 100 gold coins during your jungle dashes.",
                target = 100,
                metric = MissionMetric.COINS_TOTAL,
                rewardCoins = 300
            ),
            MissionDefinition(
                id = "daily_distance_1000",
                category = MissionCategory.DAILY,
                title = "Jungle Trekker",
                description = "Dash 1,000 meters across the lush tropical canopy.",
                target = 1000,
                metric = MissionMetric.METERS_TOTAL,
                rewardCoins = 400
            ),
            MissionDefinition(
                id = "daily_boxes_3",
                category = MissionCategory.DAILY,
                title = "Mystery Seeker",
                description = "Find and collect 3 magical Mystery Boxes in the jungle.",
                target = 3,
                metric = MissionMetric.MYSTERY_BOXES_TOTAL,
                rewardCoins = 500
            ),
            MissionDefinition(
                id = "daily_boosters_5",
                category = MissionCategory.DAILY,
                title = "Sky Dancer",
                description = "Leap and collect 5 floating Jungle Sky Boosters.",
                target = 5,
                metric = MissionMetric.SKY_BOOSTERS_USED,
                rewardCoins = 450
            ),
            MissionDefinition(
                id = "daily_tokens_10",
                category = MissionCategory.DAILY,
                title = "Primal Resonance",
                description = "Collect 10 glowing Animal Energy tokens.",
                target = 10,
                metric = MissionMetric.ANIMAL_TOKENS_COLLECTED,
                rewardCoins = 350
            ),
            MissionDefinition(
                id = "daily_transform_1",
                category = MissionCategory.DAILY,
                title = "Beast Unleashed",
                description = "Fill your meter and complete 1 Animal Transformation.",
                target = 1,
                metric = MissionMetric.ANIMAL_TRANSFORMATIONS,
                rewardCoins = 500
            ),
            MissionDefinition(
                id = "daily_hoverboard_1",
                category = MissionCategory.DAILY,
                title = "Glider's Grace",
                description = "Activate your Hoverboard during a run.",
                target = 1,
                metric = MissionMetric.HOVERBOARD_ACTIVATED,
                rewardCoins = 350
            ),

            // --- WEEKLY MISSIONS ---
            MissionDefinition(
                id = "weekly_coins_2000",
                category = MissionCategory.WEEKLY,
                title = "Grand Hoard",
                description = "Collect 2,000 coins across multiple jungle runs.",
                target = 2000,
                metric = MissionMetric.COINS_TOTAL,
                rewardCoins = 1800
            ),
            MissionDefinition(
                id = "weekly_distance_10000",
                category = MissionCategory.WEEKLY,
                title = "Canopy Marathon",
                description = "Run 10,000 total meters through ancient jungle ruins.",
                target = 10000,
                metric = MissionMetric.METERS_TOTAL,
                rewardCoins = 2200
            ),
            MissionDefinition(
                id = "weekly_boxes_15",
                category = MissionCategory.WEEKLY,
                title = "Relic Archaeologist",
                description = "Collect 15 Mystery Boxes in the jungle.",
                target = 15,
                metric = MissionMetric.MYSTERY_BOXES_TOTAL,
                rewardCoins = 2500
            ),
            MissionDefinition(
                id = "weekly_transforms_5",
                category = MissionCategory.WEEKLY,
                title = "Master of Wilds",
                description = "Complete 5 magical animal transformations.",
                target = 5,
                metric = MissionMetric.ANIMAL_TRANSFORMATIONS,
                rewardCoins = 2000,
                rewardAnimalId = "monkey"
            ),
            MissionDefinition(
                id = "weekly_runs_15",
                category = MissionCategory.WEEKLY,
                title = "Relentless Explorer",
                description = "Embark and finish 15 jungle dashes.",
                target = 15,
                metric = MissionMetric.RUNS_COMPLETED,
                rewardCoins = 1600
            ),
            MissionDefinition(
                id = "weekly_boosters_30",
                category = MissionCategory.WEEKLY,
                title = "Aerobatics Master",
                description = "Collect 30 Sky Boosters suspended above the tracks.",
                target = 30,
                metric = MissionMetric.SKY_BOOSTERS_USED,
                rewardCoins = 2200,
                rewardAnimalId = "eagle"
            ),

            // --- SPECIAL ACHIEVEMENTS ---
            MissionDefinition(
                id = "achieve_first_run",
                category = MissionCategory.ACHIEVEMENT,
                title = "First Steps in the Wilds",
                description = "Complete your first ever DreamDash jungle expedition.",
                target = 1,
                metric = MissionMetric.RUNS_COMPLETED,
                rewardCoins = 250
            ),
            MissionDefinition(
                id = "achieve_distance_3000",
                category = MissionCategory.ACHIEVEMENT,
                title = "Into Deep Canopy",
                description = "Reach 3,000 meters in a single jungle run.",
                target = 3000,
                metric = MissionMetric.METERS_RUN,
                rewardCoins = 3000,
                rewardAnimalId = "elephant"
            ),
            MissionDefinition(
                id = "achieve_first_board",
                category = MissionCategory.ACHIEVEMENT,
                title = "Hover Master",
                description = "Unlock a second hoverboard in the Hoverboard Shop.",
                target = 2,
                metric = MissionMetric.HOVERBOARDS_UNLOCKED_COUNT,
                rewardCoins = 1000
            ),
            MissionDefinition(
                id = "achieve_transform_all",
                category = MissionCategory.ACHIEVEMENT,
                title = "Primal Sovereign",
                description = "Unlock 5 or more unique jungle animal forms.",
                target = 5,
                metric = MissionMetric.ANIMALS_UNLOCKED_COUNT,
                rewardCoins = 4000,
                rewardAnimalId = "gorilla"
            ),
            MissionDefinition(
                id = "achieve_boxes_50",
                category = MissionCategory.ACHIEVEMENT,
                title = "Vault Breaker",
                description = "Collect 50 Mystery Boxes across all your adventures.",
                target = 50,
                metric = MissionMetric.MYSTERY_BOXES_TOTAL,
                rewardCoins = 3500,
                rewardAnimalId = "wolf"
            ),
            MissionDefinition(
                id = "achieve_high_score_20k",
                category = MissionCategory.ACHIEVEMENT,
                title = "Jungle Legend",
                description = "Score 20,000 points in a single legendary run.",
                target = 20000,
                metric = MissionMetric.HIGH_SCORE_REACHED,
                rewardCoins = 5000,
                rewardCosmeticName = "Golden Explorer Crest"
            )
        )
    }
}

data class MissionProgress(
    val missionId: String,
    val currentProgress: Int,
    val isClaimed: Boolean
) {
    fun status(definition: MissionDefinition): MissionStatus {
        return when {
            isClaimed -> MissionStatus.COMPLETED
            currentProgress >= definition.target -> MissionStatus.CLAIM_REWARD
            else -> MissionStatus.IN_PROGRESS
        }
    }

    fun toCsv(): String = "$missionId:$currentProgress:$isClaimed"

    companion object {
        fun fromCsv(entry: String): MissionProgress? {
            val parts = entry.split(":")
            if (parts.size < 3) return null
            val id = parts[0]
            val prog = parts[1].toIntOrNull() ?: 0
            val claimed = parts[2].toBooleanStrictOrNull() ?: false
            return MissionProgress(id, prog, claimed)
        }

        fun parseMap(csv: String): Map<String, MissionProgress> {
            if (csv.isBlank()) return emptyMap()
            return csv.split(";")
                .mapNotNull { fromCsv(it.trim()) }
                .associateBy { it.missionId }
        }

        fun serializeMap(map: Map<String, MissionProgress>): String {
            return map.values.joinToString(";") { it.toCsv() }
        }
    }
}
