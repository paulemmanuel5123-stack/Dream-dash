package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.MagicPurpleLight
import com.example.ui.theme.ShieldAzure
import com.example.ui.theme.StarlightCyan

enum class AnimalForm(
    val id: String,
    val displayName: String,
    val title: String,
    val abilityName: String,
    val abilityDescription: String,
    val durationSeconds: Float,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val auraColor: Color,
    val unlockRequirement: String,
    val unlockMissionKey: String
) {
    CHEETAH(
        id = "cheetah",
        displayName = "Cheetah",
        title = "The Golden Phantom",
        abilityName = "Supersonic Sprint",
        abilityDescription = "Runs at lightning speed (+24 km/h) leaving golden sparks, clearing distance rapidly with agile reflexes.",
        durationSeconds = 14f,
        primaryColor = Color(0xFFFFB300),
        secondaryColor = Color(0xFF6D4C41),
        accentColor = Color(0xFFFFE082),
        auraColor = Color(0xFFFFD54F),
        unlockRequirement = "Unlocked by default as your Starter Jungle Beast!",
        unlockMissionKey = "starter"
    ),
    MONKEY(
        id = "monkey",
        displayName = "Monkey",
        title = "The Canopy Acrobat",
        abilityName = "Acrobatic Sky Leap",
        abilityDescription = "High agile vertical leaps that soar cleanly over tall obstacles and gather elevated coin arcs.",
        durationSeconds = 15f,
        primaryColor = Color(0xFF8D6E63),
        secondaryColor = Color(0xFFD7CCC8),
        accentColor = DreamEmerald,
        auraColor = Color(0xFFA5D6A7),
        unlockRequirement = "Complete 3 Daily Missions or run 1,500 meters in a single run.",
        unlockMissionKey = "monkey_unlocked"
    ),
    EAGLE(
        id = "eagle",
        displayName = "Eagle",
        title = "The Sovereign Glider",
        abilityName = "Aerial Sanctuary Flight",
        abilityDescription = "Soars gracefully above the jungle floor, completely bypassing ground logs, rocks, and bridge chasms.",
        durationSeconds = 13f,
        primaryColor = Color(0xFF42A5F5),
        secondaryColor = AstralGold,
        accentColor = Color(0xFFE3F2FD),
        auraColor = Color(0xFF80D8FF),
        unlockRequirement = "Collect 10 Jungle Sky Boosters across your runs.",
        unlockMissionKey = "eagle_unlocked"
    ),
    GORILLA(
        id = "gorilla",
        displayName = "Gorilla",
        title = "The Mountain Colossus",
        abilityName = "Primal Obstacle Smash",
        abilityDescription = "Shatters and breaks fallen logs, rock wisps, and jungle pillars into harmless rubble with a shockwave (+200 bonus pts)!",
        durationSeconds = 14f,
        primaryColor = Color(0xFF37474F),
        secondaryColor = Color(0xFF90A4AE),
        accentColor = Color(0xFFFF7043),
        auraColor = Color(0xFFFF8A65),
        unlockRequirement = "Collect & open 5 Mystery Boxes.",
        unlockMissionKey = "gorilla_unlocked"
    ),
    WOLF(
        id = "wolf",
        displayName = "Wolf",
        title = "The Spirit Stalker",
        abilityName = "Spirit Pack Magnetism",
        abilityDescription = "Runs with feral agility while automatically attracting all nearby animal energy tokens and gold coins (+80% pull range).",
        durationSeconds = 15f,
        primaryColor = Color(0xFF78909C),
        secondaryColor = StarlightCyan,
        accentColor = Color(0xFFCFD8DC),
        auraColor = Color(0xFF84FFFF),
        unlockRequirement = "Collect 30 Animal Energy Tokens across your runs.",
        unlockMissionKey = "wolf_unlocked"
    ),
    ELEPHANT(
        id = "elephant",
        displayName = "Elephant",
        title = "The Ancient Behemoth",
        abilityName = "Trunk Rampage Charge",
        abilityDescription = "Passes right through logs and bridge gaps with an unstoppable mighty stride, sending obstacles flying safely.",
        durationSeconds = 14f,
        primaryColor = Color(0xFF546E7A),
        secondaryColor = Color(0xFFB0BEC5),
        accentColor = Color(0xFFFFE082),
        auraColor = Color(0xFFFFF59D),
        unlockRequirement = "Reach 3,000 meters in a single jungle run.",
        unlockMissionKey = "elephant_unlocked"
    );

    companion object {
        val ALL = entries

        fun find(id: String): AnimalForm = entries.find { it.id == id } ?: CHEETAH

        fun pickRandomUnlocked(unlockedIds: Set<String>, random: kotlin.random.Random = kotlin.random.Random.Default): AnimalForm {
            val available = entries.filter { it.id == "cheetah" || it.id in unlockedIds }
            return if (available.isNotEmpty()) available[random.nextInt(available.size)] else CHEETAH
        }
    }
}
