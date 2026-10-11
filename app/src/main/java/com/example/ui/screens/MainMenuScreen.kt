package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.db.PlayerProfile
import com.example.model.CharacterAnimState
import com.example.model.CharacterInfo
import com.example.model.HoverboardInfo
import com.example.ui.renderer.Character3DRenderer
import com.example.ui.renderer.Hoverboard3DRenderer
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.StarlightCyan
import com.example.ui.theme.VoidDark
import com.example.ui.theme.VoidSurface
import com.example.ui.theme.VoidSurfaceVariant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainMenuScreen(
    profile: PlayerProfile,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onStartGame: () -> Unit,
    onSelectCharacter: (String) -> Unit,
    onOpenShop: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenMysteryBoxes: () -> Unit = {},
    onOpenAnimalGallery: () -> Unit = {},
    onOpenMissions: () -> Unit = {},
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit
) {
    var showHowToPlay by remember { mutableStateOf(false) }
    var previewAnimState by remember { mutableStateOf(CharacterAnimState.IDLE) }
    var stageTime by remember { mutableFloatStateOf(0f) }

    // 60FPS animation loop for the 3D Hero Stage
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

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val selectedChar = CharacterInfo.find(profile.selectedCharacter)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = VoidSurfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(16.dp).background(AstralGold, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${profile.totalCoins}",
                                color = AstralGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { onToggleSound(!soundEnabled) },
                            modifier = Modifier.testTag("sound_toggle")
                        ) {
                            Icon(
                                if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = "Toggle Sound",
                                tint = if (soundEnabled) StarlightCyan else Color.Gray
                            )
                        }
                        IconButton(
                            onClick = { onToggleHaptics(!hapticsEnabled) },
                            modifier = Modifier.testTag("haptics_toggle")
                        ) {
                            Icon(
                                Icons.Default.Vibration,
                                contentDescription = "Toggle Vibrations",
                                tint = if (hapticsEnabled) AstralGold else Color.Gray
                            )
                        }
                        IconButton(
                            onClick = { showHowToPlay = true },
                            modifier = Modifier.testTag("help_button")
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "How to play", tint = Color.White)
                        }
                    }
                }
            }

            // 2. Hero Game Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_dreamdash_banner),
                            contentDescription = "DreamDash Title Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, VoidDark.copy(alpha = 0.88f)),
                                        startY = 50f
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "DreamDash",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.1.sp
                                )
                                Text(
                                    text = "Fantasy Endless Runner",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StarlightCyan
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("BEST SCORE", fontSize = 9.sp, color = AstralGold, fontWeight = FontWeight.Bold)
                                    Text("${profile.highScore}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("BEST DIST", fontSize = 9.sp, color = StarlightCyan, fontWeight = FontWeight.Bold)
                                    Text("${profile.maxDistance}m", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Character Selection Carousel (All 6 Fantasy Heroes + Quick Shop Access)
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT YOUR 3D HERO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarlightCyan,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        color = AstralGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AstralGold.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .clickable { onOpenShop() }
                            .testTag("open_character_shop_pill")
                    ) {
                        Text(
                            text = "CHARACTER SHOP (${profile.unlockedCharacterIds.size}/${CharacterInfo.ALL.size}) →",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = AstralGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CharacterInfo.ALL.forEach { char ->
                        val isUnlocked = profile.isCharacterUnlocked(char.id)
                        val isSelected = (char.id == selectedChar.id)
                        val borderColor = when {
                            isSelected -> char.primaryColor
                            isUnlocked -> char.primaryColor.copy(alpha = 0.38f)
                            else -> Color.White.copy(alpha = 0.14f)
                        }

                        Card(
                            modifier = Modifier
                                .width(136.dp)
                                .clickable {
                                    if (isUnlocked) {
                                        onSelectCharacter(char.id)
                                        previewAnimState = CharacterAnimState.IDLE
                                    } else {
                                        onOpenShop()
                                    }
                                }
                                .border(if (isSelected) 2.2.dp else 1.2.dp, borderColor, RoundedCornerShape(16.dp))
                                .testTag("select_char_${char.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) VoidSurfaceVariant else VoidSurface
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(106.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    char.primaryColor.copy(alpha = 0.25f),
                                                    VoidDark
                                                )
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) char.primaryColor else Color.Gray.copy(alpha = 0.3f),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        Character3DRenderer.drawCharacter(
                                            drawScope = this,
                                            character = char,
                                            animState = if (isSelected) CharacterAnimState.RUNNING else CharacterAnimState.IDLE,
                                            timeSeconds = stageTime,
                                            centerX = size.width * 0.5f,
                                            groundY = size.height * 0.88f,
                                            scale = 0.66f,
                                            frontFacingFactor = 0.85f
                                        )
                                    }
                                    Surface(
                                        color = VoidDark.copy(alpha = 0.82f),
                                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = if (isSelected) "SELECTED" else if (isUnlocked) "UNLOCKED" else "LOCKED",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) StarlightCyan else if (isUnlocked) char.primaryColor else AstralGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = char.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (isUnlocked) char.perkName else char.formattedPrice,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) char.primaryColor else AstralGold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 4. Live 3D Character Stage & 7-Animation Previewer + Gear Loadout
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VoidSurface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, selectedChar.primaryColor.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Live 60FPS Full-Body 3D Character Stage
                            Box(
                                modifier = Modifier
                                    .width(126.dp)
                                    .height(166.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                VoidDark,
                                                selectedChar.primaryColor.copy(alpha = 0.22f),
                                                VoidSurfaceVariant
                                            )
                                        )
                                    )
                                    .border(1.dp, selectedChar.primaryColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .testTag("hero_3d_stage")
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cx = size.width * 0.5f
                                    val gy = size.height * 0.86f

                                    // Magical stage rune circle
                                    drawCircle(
                                        color = selectedChar.primaryColor.copy(alpha = 0.16f),
                                        radius = size.width * 0.36f,
                                        center = Offset(cx, gy - 45f)
                                    )

                                    Character3DRenderer.drawCharacter(
                                        drawScope = this,
                                        character = selectedChar,
                                        animState = previewAnimState,
                                        timeSeconds = stageTime,
                                        centerX = cx,
                                        groundY = gy,
                                        scale = 0.92f,
                                        elevationPx = if (previewAnimState == CharacterAnimState.JUMPING) 16f else 0f,
                                        laneTiltDir = 0f,
                                        frontFacingFactor = 0.9f
                                    )
                                }

                                Surface(
                                    color = VoidDark.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 6.dp)
                                ) {
                                    Text(
                                        text = "3D: ${previewAnimState.displayName.uppercase()}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = selectedChar.secondaryColor,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Character Details & Equipment Loadout
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${selectedChar.name} — ${selectedChar.roleLabel}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = selectedChar.perkDescription,
                                    fontSize = 11.sp,
                                    color = AstralGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = selectedChar.description,
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    selectedChar.gearLoadout.forEach { gear ->
                                        Surface(
                                            color = VoidSurfaceVariant,
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                0.8.dp,
                                                selectedChar.primaryColor.copy(alpha = 0.45f)
                                            )
                                        ) {
                                            Text(
                                                text = gear,
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive 7-Animation Preview Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Animations:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StarlightCyan,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                            CharacterAnimState.entries.forEach { anim ->
                                val active = (previewAnimState == anim)
                                Surface(
                                    modifier = Modifier
                                        .clickable { previewAnimState = anim }
                                        .testTag("anim_preview_${anim.name.lowercase()}"),
                                    color = if (active) selectedChar.primaryColor else VoidSurfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (active) AstralGold else selectedChar.primaryColor.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        text = anim.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) VoidDark else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4b. Active Hoverboard Showcase Card
            item {
                Spacer(modifier = Modifier.height(10.dp))
                val activeBoard = HoverboardInfo.find(profile.selectedHoverboard)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenShop() }
                        .testTag("menu_hoverboard_card"),
                    colors = CardDefaults.cardColors(containerColor = VoidSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, activeBoard.glowColor.copy(alpha = 0.5f))
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
                                    .size(width = 62.dp, height = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(activeBoard.glowColor.copy(alpha = 0.28f), VoidDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    Hoverboard3DRenderer.drawHoverboardPreview(
                                        drawScope = this,
                                        board = activeBoard,
                                        centerX = size.width * 0.5f,
                                        centerY = size.height * 0.5f,
                                        width = size.width * 0.88f,
                                        height = size.height * 0.55f,
                                        timeSeconds = stageTime
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeBoard.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = DreamEmerald.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "EQUIPPED",
                                            color = DreamEmerald,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "★ ${activeBoard.specialAbility}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = activeBoard.glowColor
                                )
                            }
                        }

                        Surface(
                            color = AstralGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AstralGold.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "HOVERBOARDS (${profile.unlockedHoverboardIds.size}/7) →",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = AstralGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 5. Giant "START DASH!" Button
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onStartGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .scale(pulseScale)
                        .testTag("play_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = StarlightCyan, contentColor = VoidDark),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "START DASH!", fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }

            // 6. Feature Action Cards: Mystery Boxes, Animal Gallery, Missions
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenMysteryBoxes,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("mystery_boxes_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = VoidSurfaceVariant,
                            contentColor = AstralGold
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, AstralGold),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Mystery Boxes", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenAnimalGallery,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("animal_gallery_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = VoidSurfaceVariant,
                            contentColor = DreamEmerald
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, DreamEmerald),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Pets, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Animal Gallery", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenMissions,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("missions_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = VoidSurfaceVariant,
                            contentColor = StarlightCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, StarlightCyan),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Missions", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // 7. Quick Navigation Cards
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenShop,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("shop_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = VoidSurfaceVariant,
                            contentColor = AstralGold
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AstralGold.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Characters & Boards", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenLeaderboard,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("leaderboard_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = VoidSurfaceVariant,
                            contentColor = StarlightCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StarlightCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hall of Fame", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // How to play dialog
        if (showHowToPlay) {
            Dialog(onDismissRequest = { showHowToPlay = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VoidSurfaceVariant),
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "How to Dash!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AstralGold)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("• Swipe Left / Right: Dodge between the 3 lanes", color = Color.White, fontSize = 12.sp)
                        Text("• Swipe UP: Jump over fallen jungle logs & wooden bridge chasms", color = Color.White, fontSize = 12.sp)
                        Text("• Swipe DOWN: Slide under hanging giant vines & ancient stone lintels", color = Color.White, fontSize = 12.sp)
                        Text("• Tap Hoverboard: Ride your fantasy board with crash absorption & magical trails", color = AstralGold, fontSize = 12.sp)
                        Text("• Dodge Obstacles: Weave past ancient ruin pillars & mossy river rocks", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "6 Adventure Jungle Sections: 1. Green Jungle Entrance → 2. Deep Jungle → 3. Ancient Ruins → 4. Waterfall Valley → 5. Jungle Cliff → 6. Mystic Jungle",
                            color = DreamEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Power-Ups: Magnetic Shield, Double Score, Aegis Ward & Dream Rush!", color = StarlightCyan, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { showHowToPlay = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = StarlightCyan, contentColor = VoidDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ready to Dash!", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
