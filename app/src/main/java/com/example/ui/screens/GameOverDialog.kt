package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.GameEngine
import com.example.model.CharacterAnimState
import com.example.ui.renderer.Character3DRenderer
import com.example.ui.theme.AstralGold
import com.example.ui.theme.MysticPurpleDark
import com.example.ui.theme.MysticPurpleMid
import com.example.ui.theme.StarlightCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameOverDialog(
    engine: GameEngine,
    isNewHighScore: Boolean,
    onRestart: () -> Unit,
    onHome: () -> Unit
) {
    var animTime by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastNano = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNano = now
                animTime += dt
            }
        }
    }

    Dialog(
        onDismissRequest = { /* Modal */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("game_over_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MysticPurpleMid),
            border = BorderStroke(2.dp, if (isNewHighScore) AstralGold else Color(0xFFFF5252).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isNewHighScore) AstralGold.copy(alpha = 0.2f) else Color(0xFFFF5252).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isNewHighScore) "★ NEW HIGH SCORE! ★" else "RUN CONCLUDED",
                        color = if (isNewHighScore) AstralGold else Color(0xFFFF8A80),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Game Over",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )

                if (engine.causeOfDeath.isNotBlank()) {
                    Text(
                        text = engine.causeOfDeath,
                        color = Color(0xFFFFAB91),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3D Character Game-Over Animation Showcase
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(132.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MysticPurpleDark,
                                    engine.character.primaryColor.copy(alpha = 0.18f),
                                    MysticPurpleDark
                                )
                            )
                        )
                        .border(1.dp, engine.character.primaryColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .testTag("game_over_character_stage")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        Character3DRenderer.drawCharacter(
                            drawScope = this,
                            character = engine.character,
                            animState = CharacterAnimState.GAME_OVER,
                            timeSeconds = animTime,
                            centerX = size.width * 0.5f,
                            groundY = size.height * 0.88f,
                            scale = 0.78f,
                            elevationPx = 0f,
                            laneTiltDir = 0f,
                            frontFacingFactor = 0.9f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MysticPurpleDark.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, StarlightCyan.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Final Score", color = TextSecondary, fontSize = 14.sp)
                            Text(
                                text = "${engine.score}",
                                color = AstralGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Distance Traveled", color = TextSecondary, fontSize = 14.sp)
                            Text(
                                text = "${engine.distanceMeters.toInt()} meters",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Jungle Realm", color = TextSecondary, fontSize = 14.sp)
                            Text(
                                text = engine.currentBiome.displayName,
                                color = engine.currentBiome.ambientParticleColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Top Speed & Tier", color = TextSecondary, fontSize = 14.sp)
                            Text(
                                text = "${engine.peakSpeedKmh.toInt()} km/h (${engine.difficultyTier.displayName})",
                                color = engine.difficultyTier.badgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dream Coins Gathered", color = TextSecondary, fontSize = 14.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(AstralGold)
                                )
                                Text(
                                    text = "+${engine.coinsCollected}",
                                    color = AstralGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("restart_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AstralGold,
                        contentColor = MysticPurpleDark
                    )
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("TRY AGAIN", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("home_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, StarlightCyan.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StarlightCyan)
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("RETURN TO HAVEN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
