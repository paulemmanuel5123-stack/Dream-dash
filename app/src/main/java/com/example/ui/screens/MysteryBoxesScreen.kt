package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.PlayerProfile
import com.example.model.MysteryBoxRarity
import com.example.model.MysteryBoxReward
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.MagicPurpleLight
import com.example.ui.theme.ShieldAzure
import com.example.ui.theme.StarlightCyan
import com.example.ui.theme.VoidDark
import com.example.ui.theme.VoidSurface
import com.example.ui.theme.VoidSurfaceVariant
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MysteryBoxesScreen(
    profile: PlayerProfile,
    onOpenBox: (MysteryBoxRarity) -> MysteryBoxReward,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var stageTime by remember { mutableFloatStateOf(0f) }
    var selectedRarity by remember { mutableStateOf(MysteryBoxRarity.COMMON) }
    var openedReward by remember { mutableStateOf<MysteryBoxReward?>(null) }
    var isOpeningAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var lastNano = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNano = now
                stageTime += dt
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "boxPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boxPulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(VoidSurfaceVariant, CircleShape)
                        .testTag("mystery_box_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Menu",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MYSTERY BOX VAULT",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Magical Jungle Treasure Boxes & Surprise Rewards",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AstralGold
                    )
                }
                Surface(
                    color = VoidSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🪙", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "%,d".format(profile.totalCoins),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = AstralGold
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive 3D Mystery Box Showcase Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = VoidSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, selectedRarity.glowColor.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = selectedRarity.primaryColor.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, selectedRarity.glowColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "✨ ${selectedRarity.displayName.uppercase()} ✨",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = selectedRarity.glowColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3D Canvas Box Preview
                            Box(
                                modifier = Modifier
                                    .size(width = 220.dp, height = 150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(selectedRarity.glowColor.copy(alpha = 0.28f), VoidDark)
                                        )
                                    )
                                    .border(1.dp, selectedRarity.glowColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cx = size.width * 0.5f
                                    val cy = size.height * 0.54f + sin(stageTime * 4f) * 6f
                                    val boxSize = 70f

                                    // Radiant Aura
                                    drawCircle(
                                        color = selectedRarity.glowColor.copy(alpha = 0.25f + sin(stageTime * 6f) * 0.1f),
                                        radius = boxSize * 1.15f,
                                        center = Offset(cx, cy)
                                    )

                                    // Wooden Chest Body
                                    drawRoundRect(
                                        color = Color(0xFF4E342E),
                                        topLeft = Offset(cx - boxSize / 2f, cy - boxSize * 0.35f),
                                        size = Size(boxSize, boxSize * 0.75f),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                    // Gold Filigree Borders
                                    drawRoundRect(
                                        color = AstralGold,
                                        topLeft = Offset(cx - boxSize / 2f, cy - boxSize * 0.35f),
                                        size = Size(boxSize, boxSize * 0.75f),
                                        cornerRadius = CornerRadius(8f, 8f),
                                        style = Stroke(width = 3.5f)
                                    )
                                    // Chest Lid
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(selectedRarity.primaryColor, Color(0xFF3E2723)),
                                            startY = cy - boxSize * 0.55f,
                                            endY = cy - boxSize * 0.2f
                                        ),
                                        topLeft = Offset(cx - boxSize * 0.54f, cy - boxSize * 0.55f),
                                        size = Size(boxSize * 1.08f, boxSize * 0.38f),
                                        cornerRadius = CornerRadius(10f, 10f)
                                    )
                                    // Glowing Top Crystal
                                    drawCircle(color = selectedRarity.glowColor, radius = 8f, center = Offset(cx, cy - boxSize * 0.55f))
                                    drawCircle(color = Color.White, radius = 4f, center = Offset(cx, cy - boxSize * 0.55f))

                                    // Vines wrapping edges
                                    drawLine(
                                        color = Color(0xFF2E7D32),
                                        start = Offset(cx - boxSize * 0.45f, cy + boxSize * 0.35f),
                                        end = Offset(cx - boxSize * 0.15f, cy - boxSize * 0.2f),
                                        strokeWidth = 3f,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = Color(0xFF2E7D32),
                                        start = Offset(cx + boxSize * 0.45f, cy + boxSize * 0.35f),
                                        end = Offset(cx + boxSize * 0.15f, cy - boxSize * 0.2f),
                                        strokeWidth = 3f,
                                        cap = StrokeCap.Round
                                    )

                                    // Center Question Mark '?'
                                    drawCircle(color = AstralGold, radius = 3.5f, center = Offset(cx, cy + 12f))
                                    val qPath = Path().apply {
                                        moveTo(cx - 7f, cy - 10f)
                                        cubicTo(cx - 7f, cy - 22f, cx + 9f, cy - 22f, cx + 7f, cy - 9f)
                                        lineTo(cx, cy + 3f)
                                    }
                                    drawPath(qPath, color = AstralGold, style = Stroke(width = 4.5f, cap = StrokeCap.Round))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Open Button
                            Button(
                                onClick = {
                                    val rew = onOpenBox(selectedRarity)
                                    openedReward = rew
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .scale(pulseScale)
                                    .testTag("open_mystery_box_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = selectedRarity.primaryColor,
                                    contentColor = VoidDark
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "OPEN ${selectedRarity.displayName.uppercase()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                // Rarity Selector Cards
                item {
                    Text(
                        text = "SELECT RARITY TIER TO PREVIEW & TEST ODDS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(MysteryBoxRarity.entries.toList(), key = { it.name }) { rarity ->
                    val isCur = (selectedRarity == rarity)
                    Surface(
                        color = if (isCur) VoidSurfaceVariant else VoidSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isCur) 1.8.dp else 1.dp,
                            if (isCur) rarity.glowColor else Color.Gray.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRarity = rarity }
                            .testTag("rarity_chip_${rarity.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(rarity.glowColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = rarity.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = when (rarity) {
                                            MysteryBoxRarity.COMMON -> "Spawn: 60% • 100–250 Coins, Magnet, Shield"
                                            MysteryBoxRarity.RARE -> "Spawn: 25% • 250–500 Coins, Multiplier, Speed Boost"
                                            MysteryBoxRarity.EPIC -> "Spawn: 11% • 500–1000 Coins, Current Hoverboard, Ward"
                                            MysteryBoxRarity.LEGENDARY -> "Spawn: 4% • 1,000 Coins, Golden Board, Shaman Relic"
                                        },
                                        fontSize = 10.sp,
                                        color = Color(0xFFCFD8DC)
                                    )
                                }
                            }
                            Surface(
                                color = rarity.primaryColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${rarity.weight}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = rarity.glowColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Opened Reward Dialog Popup
        if (openedReward != null) {
            val reward = openedReward!!
            Dialog(onDismissRequest = { openedReward = null }) {
                Surface(
                    color = VoidDark,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, reward.rarity.glowColor),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "🎁", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = reward.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reward.description,
                            fontSize = 12.sp,
                            color = AstralGold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { openedReward = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("claim_box_reward_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AstralGold,
                                contentColor = VoidDark
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(text = "COLLECT PRIZE!", fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
