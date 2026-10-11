package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.MagicPurpleLight
import com.example.ui.theme.ShieldAzure
import com.example.ui.theme.StarlightCyan

enum class SkyBoosterType(
    val displayName: String,
    val iconSymbol: String,
    val badgeLabel: String,
    val effectSummary: String,
    val durationSeconds: Float,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val trailColor: Color
) {
    SKY_LAUNCHER(
        displayName = "Sky Launcher",
        iconSymbol = "🚀",
        badgeLabel = "SKY LAUNCH",
        effectSummary = "Launches character high in the air above jungle obstacles",
        durationSeconds = 8.0f,
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFFE040FB),
        glowColor = Color(0xFF80D8FF),
        trailColor = Color(0xFF00B0FF)
    ),
    COIN_MAGNET(
        displayName = "Coin Magnet",
        iconSymbol = "🧲",
        badgeLabel = "MAGNET",
        effectSummary = "Attracts all coins from all lanes automatically",
        durationSeconds = 12.0f,
        primaryColor = StarlightCyan,
        secondaryColor = AstralGold,
        glowColor = Color(0xFF84FFFF),
        trailColor = StarlightCyan
    ),
    SPEED_BOOST(
        displayName = "Speed Boost",
        iconSymbol = "⚡",
        badgeLabel = "SPEED BURST",
        effectSummary = "Increases running speed with glowing invincibility flame trail",
        durationSeconds = 8.0f,
        primaryColor = Color(0xFFFF5252),
        secondaryColor = AstralGold,
        glowColor = Color(0xFFFF8A80),
        trailColor = Color(0xFFFF3D00)
    ),
    JUNGLE_SHIELD(
        displayName = "Jungle Shield",
        iconSymbol = "🛡️",
        badgeLabel = "JUNGLE WARD",
        effectSummary = "Protects from one collision during active duration",
        durationSeconds = 14.0f,
        primaryColor = ShieldAzure,
        secondaryColor = DreamEmerald,
        glowColor = Color(0xFF82B1FF),
        trailColor = ShieldAzure
    ),
    SKY_GLIDER(
        displayName = "Sky Glider",
        iconSymbol = "🪂",
        badgeLabel = "SKY GLIDER",
        effectSummary = "Glides smoothly above the jungle track collecting aerial coins",
        durationSeconds = 9.0f,
        primaryColor = DreamEmerald,
        secondaryColor = StarlightCyan,
        glowColor = Color(0xFFB9F6CA),
        trailColor = Color(0xFF00E676)
    ),
    SCORE_MULTIPLIER(
        displayName = "Score Multiplier",
        iconSymbol = "⭐",
        badgeLabel = "3X MULTIPLIER",
        effectSummary = "Multiplies all earned score by 3x while active",
        durationSeconds = 12.0f,
        primaryColor = AstralGold,
        secondaryColor = Color(0xFFFF6D00),
        glowColor = Color(0xFFFFE082),
        trailColor = AstralGold
    );

    companion object {
        fun randomBooster(random: kotlin.random.Random = kotlin.random.Random.Default): SkyBoosterType {
            return entries[random.nextInt(entries.size)]
        }
    }
}

data class ActiveBoosterState(
    val type: SkyBoosterType,
    var remainingTime: Float,
    val maxDuration: Float = type.durationSeconds
) {
    val progressFraction: Float
        get() = (remainingTime / maxDuration).coerceIn(0f, 1f)

    val isExpiringSoon: Boolean
        get() = remainingTime < 2.5f
}
