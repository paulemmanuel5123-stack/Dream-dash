package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlayerProfile
import com.example.model.MissionCategory
import com.example.model.MissionDefinition
import com.example.model.MissionProgress
import com.example.model.MissionStatus
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.StarlightCyan
import com.example.ui.theme.VoidDark
import com.example.ui.theme.VoidSurface
import com.example.ui.theme.VoidSurfaceVariant

@Composable
fun MissionsScreen(
    profile: PlayerProfile,
    missionsProgress: Map<String, MissionProgress>,
    onClaimReward: (MissionDefinition) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val categories = listOf(
        MissionCategory.DAILY,
        MissionCategory.WEEKLY,
        MissionCategory.ACHIEVEMENT
    )
    val activeCategory = categories[selectedTabIndex]
    val activeMissions = MissionDefinition.ALL.filter { it.category == activeCategory }

    // Pulse animation for ready to claim buttons
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "claimPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header Bar
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
                        .testTag("missions_back_button")
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
                        text = "MISSIONS & REWARDS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Complete challenges to earn coins & unlock wild beasts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AstralGold
                    )
                }

                // Coin Balance Indicator
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

            // Category Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = VoidSurface,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = activeCategory.badgeColor,
                        height = 3.dp
                    )
                }
            ) {
                categories.forEachIndexed { index, cat ->
                    val readyCount = MissionDefinition.ALL.filter { it.category == cat }.count { def ->
                        val prog = missionsProgress[def.id] ?: MissionProgress(def.id, 0, false)
                        prog.status(def) == MissionStatus.CLAIM_REWARD
                    }
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cat.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Black else FontWeight.Bold,
                                    color = if (selectedTabIndex == index) cat.badgeColor else Color.Gray
                                )
                                if (readyCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = AstralGold,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "$readyCount",
                                            color = VoidDark,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Missions List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Header Info
                item {
                    val infoText = when (activeCategory) {
                        MissionCategory.DAILY -> "⏰ Resets Daily at Midnight UTC. Great for consistent coin progression!"
                        MissionCategory.WEEKLY -> "📅 Resets Weekly. Complete bigger challenges for major rewards & animal unlocks!"
                        MissionCategory.ACHIEVEMENT -> "🏆 Permanent Career Milestones. Push your endurance and skill to the limit!"
                    }
                    Surface(
                        color = VoidSurfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = infoText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB0BEC5),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                items(activeMissions, key = { it.id }) { missionDef ->
                    val progress = missionsProgress[missionDef.id] ?: MissionProgress(missionDef.id, 0, false)
                    val status = progress.status(missionDef)
                    val currentClamped = progress.currentProgress.coerceAtMost(missionDef.target)
                    val progressFraction = (currentClamped.toFloat() / missionDef.target.toFloat()).coerceIn(0f, 1f)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mission_card_${missionDef.id}"),
                        colors = CardDefaults.cardColors(containerColor = VoidSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (status) {
                                MissionStatus.CLAIM_REWARD -> AstralGold
                                MissionStatus.COMPLETED -> DreamEmerald.copy(alpha = 0.4f)
                                else -> Color.Gray.copy(alpha = 0.25f)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Mission Title & Category
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = missionDef.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                // Reward Badge
                                Surface(
                                    color = AstralGold.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🪙 +${missionDef.rewardCoins}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AstralGold)
                                        if (missionDef.rewardAnimalId != null) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "🐾 UNLOCK", fontSize = 9.sp, fontWeight = FontWeight.Black, color = DreamEmerald)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = missionDef.description,
                                fontSize = 12.sp,
                                color = Color(0xFFCFD8DC),
                                fontWeight = FontWeight.Normal
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Progress Bar & Counter
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Progress: $currentClamped / ${missionDef.target}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (status == MissionStatus.COMPLETED) DreamEmerald else StarlightCyan
                                )
                                Text(
                                    text = "${(progressFraction * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(3.5.dp))
                                    .background(VoidSurfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .height(7.dp)
                                        .background(
                                            if (status == MissionStatus.COMPLETED) DreamEmerald else activeCategory.badgeColor
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Status / Action Button
                            when (status) {
                                MissionStatus.CLAIM_REWARD -> {
                                    Button(
                                        onClick = { onClaimReward(missionDef) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .scale(pulseScale)
                                            .testTag("claim_reward_button_${missionDef.id}"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AstralGold,
                                            contentColor = VoidDark
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CLAIM REWARD (+${missionDef.rewardCoins} COINS)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                MissionStatus.COMPLETED -> {
                                    Surface(
                                        color = DreamEmerald.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DreamEmerald.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DreamEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "COMPLETED & CLAIMED",
                                                color = DreamEmerald,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }

                                MissionStatus.IN_PROGRESS -> {
                                    Surface(
                                        color = VoidSurfaceVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "IN PROGRESS",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
