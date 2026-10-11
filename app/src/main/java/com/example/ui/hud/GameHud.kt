package com.example.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameEngine
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.MagicPurpleLight
import com.example.ui.theme.ShieldAzure
import com.example.ui.theme.StarlightCyan
import com.example.ui.renderer.Hoverboard3DRenderer
import com.example.ui.theme.VoidDark
import com.example.ui.theme.VoidSurfaceVariant
import kotlin.math.roundToInt

@Composable
fun GameHud(
    engine: GameEngine,
    showTactileButtons: Boolean,
    onPause: () -> Unit
) {
    val tier = engine.difficultyTier
    val speedKmhInt = engine.currentSpeedKmh.roundToInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top status HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            // Primary Row: Distance, Speed, Coins, Score, Pause
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Distance & Biome badge
                Surface(
                    color = VoidSurfaceVariant.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        engine.currentBiome.ambientParticleColor.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("hud_distance")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(
                            text = "${engine.distanceMeters.toInt()}m",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = engine.currentBiome.displayName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = engine.currentBiome.ambientParticleColor
                        )
                    }
                }

                // 2. Clean Speedometer & Difficulty Tier HUD Pill
                Surface(
                    color = VoidSurfaceVariant.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        tier.badgeColor.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("hud_speed")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Running Speed",
                                tint = tier.badgeColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "$speedKmhInt km/h",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = tier.displayName.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = tier.badgeColor,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(VoidDark)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(engine.speedProgressFraction.coerceIn(0.08f, 1f))
                                        .height(3.5.dp)
                                        .background(tier.badgeColor)
                                )
                            }
                        }
                    }
                }

                // 3. Collected Coins & Live Total Coin Balance
                Surface(
                    color = VoidSurfaceVariant.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AstralGold.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("hud_coins")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .background(AstralGold, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "%,d".format(engine.totalCoinBalance),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = AstralGold
                            )
                            Text(
                                text = "+${engine.coinsCollected} run",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // 4. Score Counter
                Surface(
                    color = VoidSurfaceVariant.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StarlightCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("hud_score")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "SCORE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = StarlightCyan
                        )
                        Text(
                            text = "${engine.score}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // 5. Pause Button
                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .size(48.dp)
                        .background(VoidSurfaceVariant.copy(alpha = 0.86f), CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause Game",
                        tint = Color.White
                    )
                }
            }

            // Active Power-Up Timers Bar
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (engine.hoverboardActive) {
                    HudPowerUpBadge(
                        label = "BOARD: ${engine.hoverboard.name.uppercase()} ${engine.hoverboardTimer.toInt()}s",
                        progress = (engine.hoverboardTimer / engine.hoverboardMaxDuration).coerceIn(0f, 1f),
                        color = engine.hoverboard.glowColor
                    )
                }
                if (engine.magnetTimer > 0f) {
                    HudPowerUpBadge(
                        label = "MAGNETIC SHIELD ${engine.magnetTimer.toInt()}s",
                        progress = (engine.magnetTimer / engine.magnetMaxDuration).coerceIn(0f, 1f),
                        color = StarlightCyan
                    )
                }
                if (engine.multiplierTimer > 0f) {
                    HudPowerUpBadge(
                        label = "DOUBLE SCORE ${engine.multiplierTimer.toInt()}s",
                        progress = (engine.multiplierTimer / engine.multiplierMaxDuration).coerceIn(0f, 1f),
                        color = AstralGold
                    )
                }
                if (engine.shieldActive && engine.magnetTimer <= 0f) {
                    HudPowerUpBadge(
                        label = "AEGIS WARD",
                        progress = 1f,
                        color = ShieldAzure
                    )
                }
                if (engine.boostTimer > 0f) {
                    HudPowerUpBadge(
                        label = "DREAM RUSH ${engine.boostTimer.toInt()}s",
                        progress = (engine.boostTimer / engine.boostMaxDuration).coerceIn(0f, 1f),
                        color = MagicPurpleLight
                    )
                }
            }

            // FEATURE 2: Active Sky Boosters Row
            if (engine.activeBoosters.isNotEmpty() || engine.skyLauncherTimer > 0f || engine.skyGliderTimer > 0f) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (engine.skyLauncherTimer > 0f) {
                        HudPowerUpBadge(
                            label = "🚀 SKY LAUNCHER ${engine.skyLauncherTimer.toInt()}s",
                            progress = (engine.skyLauncherTimer / 8f).coerceIn(0f, 1f),
                            color = Color(0xFF00E5FF)
                        )
                    }
                    if (engine.skyGliderTimer > 0f) {
                        HudPowerUpBadge(
                            label = "🪂 SKY GLIDER ${engine.skyGliderTimer.toInt()}s",
                            progress = (engine.skyGliderTimer / 9f).coerceIn(0f, 1f),
                            color = DreamEmerald
                        )
                    }
                    engine.activeBoosters.values.forEach { bState ->
                        if (bState.type != com.example.model.SkyBoosterType.SKY_LAUNCHER && bState.type != com.example.model.SkyBoosterType.SKY_GLIDER) {
                            HudPowerUpBadge(
                                label = "${bState.type.iconSymbol} ${bState.type.badgeLabel} ${bState.remainingTime.toInt()}s",
                                progress = bState.progressFraction,
                                color = bState.type.glowColor
                            )
                        }
                    }
                }
            }

            // FEATURE 3: Animal Energy Meter & Transformation Status Bar
            Spacer(modifier = Modifier.height(6.dp))
            if (engine.activeAnimalForm != null) {
                // Transformed Animal Active Banner
                val animal = engine.activeAnimalForm!!
                val remSeconds = engine.animalDurationTimer.toInt()
                val remFraction = (engine.animalDurationTimer / engine.animalMaxDuration).coerceIn(0f, 1f)
                Surface(
                    color = VoidDark.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, animal.primaryColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hud_animal_transformed")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🐾 ${animal.displayName.uppercase()} — ${animal.abilityName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = animal.primaryColor
                                )
                            }
                            Text(
                                text = "${remSeconds}s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AstralGold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(VoidSurfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(remFraction)
                                    .height(5.dp)
                                    .background(animal.primaryColor)
                            )
                        }
                    }
                }
            } else {
                // Animal Energy Meter (Fills to 100%)
                val energyPct = (engine.animalEnergy * 100).toInt()
                Surface(
                    color = VoidSurfaceVariant.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DreamEmerald.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hud_animal_energy_meter")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🐾", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ANIMAL ENERGY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DreamEmerald
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(VoidDark)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(engine.animalEnergy.coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .background(DreamEmerald)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$energyPct%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Live Power-Up & Mystery Box Pickup Toast Banner
            if (engine.powerUpBannerTimer > 0f && engine.activePowerUpBanner.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = VoidDark.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, engine.activePowerUpColor),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("hud_powerup_banner")
                ) {
                    Text(
                        text = "⚡ ${engine.activePowerUpBanner}",
                        color = engine.activePowerUpColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // FEATURE 1: Mystery Box Reward Celebration Popup
        if (engine.mysteryBoxRewardTimer > 0f && engine.activeMysteryBoxReward != null) {
            val reward = engine.activeMysteryBoxReward!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = VoidDark.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, reward.rarity.glowColor),
                    shadowElevation = 12.dp,
                    modifier = Modifier.testTag("mystery_box_popup")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = reward.rarity.primaryColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, reward.rarity.glowColor)
                        ) {
                            Text(
                                text = "✨ ${reward.rarity.displayName.uppercase()} ✨",
                                color = reward.rarity.glowColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "🎁", fontSize = 38.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = reward.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reward.description,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AstralGold
                        )
                    }
                }
            }
        }

        // Optional Tactile D-Pad Controls
        if (showTactileButtons) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 24.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Lane switch buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledIconButton(
                        onClick = { engine.switchLane(-1) },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("button_left"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = VoidSurfaceVariant.copy(alpha = 0.85f)
                        )
                    ) {
                        Text("◀", color = StarlightCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledIconButton(
                        onClick = { engine.switchLane(1) },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("button_right"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = VoidSurfaceVariant.copy(alpha = 0.85f)
                        )
                    ) {
                        Text("▶", color = StarlightCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Jump & Slide buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledIconButton(
                        onClick = { engine.jump() },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("button_jump"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = StarlightCyan.copy(alpha = 0.9f)
                        )
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Jump", tint = VoidDark)
                    }

                    FilledIconButton(
                        onClick = { engine.slide() },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("button_slide"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AstralGold.copy(alpha = 0.9f)
                        )
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Slide", tint = VoidDark)
                    }
                }
            }
        }

        // Clear Hoverboard Activation Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    bottom = if (showTactileButtons) 92.dp else 24.dp,
                    end = 20.dp
                )
        ) {
            HoverboardActivationHudButton(
                engine = engine,
                onActivate = { engine.activateHoverboard() }
            )
        }
    }
}

@Composable
private fun HudPowerUpBadge(
    label: String,
    progress: Float,
    color: Color
) {
    Surface(
        color = VoidSurfaceVariant.copy(alpha = 0.9f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VoidDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                        .height(3.dp)
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun HoverboardActivationHudButton(
    engine: GameEngine,
    onActivate: () -> Unit
) {
    val board = engine.hoverboard
    val isActive = engine.hoverboardActive
    val timeLeft = engine.hoverboardTimer.coerceAtLeast(0f)

    Surface(
        onClick = onActivate,
        color = if (isActive) board.primaryColor.copy(alpha = 0.35f) else VoidSurfaceVariant.copy(alpha = 0.92f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isActive) 2.dp else 1.5.dp,
            if (isActive) board.glowColor else board.primaryColor.copy(alpha = 0.85f)
        ),
        modifier = Modifier
            .size(width = 84.dp, height = 74.dp)
            .testTag("hoverboard_activate_button")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    Hoverboard3DRenderer.drawHoverboardPreview(
                        drawScope = this,
                        board = board,
                        centerX = size.width * 0.5f,
                        centerY = size.height * 0.5f,
                        width = size.width * 0.95f,
                        height = size.height * 0.9f,
                        timeSeconds = if (isActive) engine.survivalTimeSeconds else 0f
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isActive) "${timeLeft.toInt()}s" else "RIDE",
                    color = if (isActive) AstralGold else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = board.name.uppercase().take(8),
                    color = board.glowColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
