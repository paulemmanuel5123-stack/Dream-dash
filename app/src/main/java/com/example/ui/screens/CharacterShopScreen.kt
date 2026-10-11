package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlayerProfile
import com.example.model.CharacterAnimState
import com.example.model.CharacterInfo
import com.example.model.HoverboardInfo
import com.example.model.HoverboardType
import com.example.ui.renderer.Character3DRenderer
import com.example.ui.renderer.Hoverboard3DRenderer
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.FantasyDarkBg
import com.example.ui.theme.MagicPurpleLight
import com.example.ui.theme.ShieldAzure
import com.example.ui.theme.StarlightCyan
import com.example.ui.theme.VoidDark
import com.example.ui.theme.VoidSurface
import com.example.ui.theme.VoidSurfaceVariant

@Composable
fun CharacterShopScreen(
    profile: PlayerProfile,
    onBuyCharacter: (CharacterInfo) -> Unit,
    onSelectCharacter: (String) -> Unit,
    onBuyHoverboard: (HoverboardInfo) -> Unit,
    onSelectHoverboard: (String) -> Unit,
    onUpgradeRelic: (String) -> Unit,
    onBack: () -> Unit,
    onStartRunWithSelected: () -> Unit
) {
    BackHandler { onBack() }

    val selectedHero = CharacterInfo.find(profile.selectedCharacter)
    var previewHeroId by remember(profile.selectedCharacter) { mutableStateOf(profile.selectedCharacter) }
    val previewHero = CharacterInfo.find(previewHeroId)
    var previewAnimState by remember { mutableStateOf(CharacterAnimState.IDLE) }

    val selectedBoard = HoverboardInfo.find(profile.selectedHoverboard)
    var previewBoardId by remember(profile.selectedHoverboard) { mutableStateOf(profile.selectedHoverboard) }
    val previewBoard = HoverboardInfo.find(previewBoardId)

    // 0 = Characters, 1 = Hoverboards, 2 = Relic Upgrades
    var selectedTab by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "shop_3d_anim")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 62.83f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 62830, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shop_time"
    )

    val unlockedHeroesCount = CharacterInfo.ALL.count { profile.isCharacterUnlocked(it.id) }
    val unlockedBoardsCount = HoverboardInfo.ALL.count { profile.isHoverboardUnlocked(it.id) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0E0826), FantasyDarkBg, VoidDark)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Top Bar: Back Button, Title & Persistent Coin Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .background(VoidSurfaceVariant, CircleShape)
                            .testTag("shop_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Menu",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DREAMDASH VAULT",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (selectedTab == 0) {
                                "$unlockedHeroesCount / ${CharacterInfo.ALL.size} Heroes Unlocked"
                            } else if (selectedTab == 1) {
                                "$unlockedBoardsCount / ${HoverboardInfo.ALL.size} Hoverboards Owned"
                            } else {
                                "Power-Up Relic Mastery"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StarlightCyan
                        )
                    }
                }

                // Persistent Coin Balance Pill
                Surface(
                    color = VoidSurfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, AstralGold.copy(alpha = 0.85f)),
                    modifier = Modifier.testTag("shop_coin_balance")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .background(AstralGold, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%,d".format(profile.totalCoins),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = AstralGold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "COINS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Category Tabs: HEROES (6) vs HOVERBOARDS (7) vs RELICS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VoidSurfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShopTabPill(
                    title = "HEROES (${CharacterInfo.ALL.size})",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_characters")
                )
                ShopTabPill(
                    title = "HOVERBOARDS (${HoverboardInfo.ALL.size})",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("tab_hoverboards")
                )
                ShopTabPill(
                    title = "RELICS",
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("tab_relics")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TAB 0: CHARACTERS
            if (selectedTab == 0) {
                // Interactive 3D Character Showcase Stage
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = VoidSurfaceVariant.copy(alpha = 0.92f)),
                    border = BorderStroke(1.5.dp, previewHero.primaryColor.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live 3D Full-Body Character Canvas
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 130.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                previewHero.primaryColor.copy(alpha = 0.30f),
                                                VoidDark
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    Character3DRenderer.drawCharacter(
                                        drawScope = this,
                                        character = previewHero,
                                        animState = previewAnimState,
                                        timeSeconds = animTime,
                                        centerX = size.width * 0.5f,
                                        groundY = size.height * 0.88f,
                                        scale = 0.82f,
                                        frontFacingFactor = 0.85f
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Previewed Hero Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isPreviewUnlocked = profile.isCharacterUnlocked(previewHero.id)
                                    Surface(
                                        color = if (isPreviewUnlocked) DreamEmerald.copy(alpha = 0.2f) else DangerCrimson.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isPreviewUnlocked) DreamEmerald else DangerCrimson
                                        )
                                    ) {
                                        Text(
                                            text = if (isPreviewUnlocked) "UNLOCKED" else "LOCKED • ${previewHero.formattedPrice}",
                                            color = if (isPreviewUnlocked) DreamEmerald else AstralGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (profile.selectedCharacter == previewHero.id) {
                                        Surface(
                                            color = StarlightCyan.copy(alpha = 0.22f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, StarlightCyan)
                                        ) {
                                            Text(
                                                text = "ACTIVE HERO",
                                                color = StarlightCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = previewHero.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = previewHero.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = previewHero.primaryColor
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = previewHero.description,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    color = Color.LightGray,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "★ ${previewHero.perkName}: ${previewHero.perkDescription}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AstralGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Animation Preview Selector Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CharacterAnimState.entries.forEach { state ->
                                val active = (previewAnimState == state)
                                Surface(
                                    color = if (active) previewHero.primaryColor else VoidDark,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (active) Color.White else previewHero.primaryColor.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.clickable { previewAnimState = state }
                                ) {
                                    Text(
                                        text = state.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) VoidDark else Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable List of All 6 Original Fantasy Character Cards
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("character_shop_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(CharacterInfo.ALL, key = { it.id }) { character ->
                        val isUnlocked = profile.isCharacterUnlocked(character.id)
                        val isSelected = (profile.selectedCharacter == character.id)
                        val canAfford = profile.totalCoins >= character.priceCoins
                        val coinsNeeded = (character.priceCoins - profile.totalCoins).coerceAtLeast(0)

                        CharacterShopCard(
                            character = character,
                            isUnlocked = isUnlocked,
                            isSelected = isSelected,
                            isPreviewing = (previewHero.id == character.id),
                            canAfford = canAfford,
                            coinsNeeded = coinsNeeded,
                            animTime = animTime,
                            onPreview = { previewHeroId = character.id },
                            onBuy = {
                                previewHeroId = character.id
                                onBuyCharacter(character)
                            },
                            onSelect = {
                                previewHeroId = character.id
                                onSelectCharacter(character.id)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onStartRunWithSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("shop_start_run_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedHero.primaryColor,
                        contentColor = VoidDark
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DASH AS ${selectedHero.name.uppercase()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }

            } else if (selectedTab == 1) {
                // TAB 1: DEDICATED HOVERBOARD SHOP
                // 3D Hoverboard Showcase Stage for Previewed Board
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = VoidSurfaceVariant.copy(alpha = 0.92f)),
                    border = BorderStroke(1.5.dp, previewBoard.primaryColor.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live 3D Hoverboard Showcase Canvas
                            Box(
                                modifier = Modifier
                                    .size(width = 118.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                previewBoard.glowColor.copy(alpha = 0.28f),
                                                VoidDark
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    Hoverboard3DRenderer.drawHoverboardPreview(
                                        drawScope = this,
                                        board = previewBoard,
                                        centerX = size.width * 0.5f,
                                        centerY = size.height * 0.5f,
                                        width = size.width * 0.88f,
                                        height = size.height * 0.55f,
                                        timeSeconds = animTime
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details of Previewed Board
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isBoardUnlocked = profile.isHoverboardUnlocked(previewBoard.id)
                                    Surface(
                                        color = if (isBoardUnlocked) DreamEmerald.copy(alpha = 0.2f) else DangerCrimson.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isBoardUnlocked) DreamEmerald else DangerCrimson
                                        )
                                    ) {
                                        Text(
                                            text = if (isBoardUnlocked) "OWNED" else "LOCKED • ${previewBoard.formattedPrice}",
                                            color = if (isBoardUnlocked) DreamEmerald else AstralGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (profile.selectedHoverboard == previewBoard.id) {
                                        Surface(
                                            color = StarlightCyan.copy(alpha = 0.22f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, StarlightCyan)
                                        ) {
                                            Text(
                                                text = "ACTIVE BOARD",
                                                color = StarlightCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = previewBoard.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = previewBoard.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = previewBoard.glowColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = previewBoard.primaryColor.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, previewBoard.glowColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "★ ${previewBoard.specialAbility} (${previewBoard.durationSeconds.toInt()}s)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AstralGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = previewBoard.abilityDescription,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    color = Color.LightGray,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable List of All 7 Hoverboard Cards
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("hoverboard_shop_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(HoverboardInfo.ALL, key = { it.id }) { board ->
                        val isUnlocked = profile.isHoverboardUnlocked(board.id)
                        val isSelected = (profile.selectedHoverboard == board.id)
                        val canAfford = profile.totalCoins >= board.priceCoins
                        val coinsNeeded = (board.priceCoins - profile.totalCoins).coerceAtLeast(0)

                        HoverboardShopCard(
                            board = board,
                            isUnlocked = isUnlocked,
                            isSelected = isSelected,
                            isPreviewing = (previewBoard.id == board.id),
                            canAfford = canAfford,
                            coinsNeeded = coinsNeeded,
                            animTime = animTime,
                            onPreview = { previewBoardId = board.id },
                            onBuy = {
                                previewBoardId = board.id
                                onBuyHoverboard(board)
                            },
                            onSelect = {
                                previewBoardId = board.id
                                onSelectHoverboard(board.id)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onStartRunWithSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("shop_start_run_with_board_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedBoard.glowColor,
                        contentColor = VoidDark
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Surfing, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RIDE ${selectedBoard.name.uppercase()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }

            } else {
                // TAB 2: POWER-UP RELICS
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    item {
                        RelicUpgradeCard(
                            title = "Magnetic Shield Relic",
                            desc = "Draws dream coins from all 3 lanes with expanding electromagnetic radius.",
                            icon = Icons.Default.VolunteerActivism,
                            themeColor = StarlightCyan,
                            level = profile.magnetLevel,
                            cost = profile.magnetLevel * 50,
                            canAfford = profile.totalCoins >= profile.magnetLevel * 50,
                            onUpgrade = { onUpgradeRelic("magnet") },
                            tag = "upgrade_magnet_button"
                        )
                    }
                    item {
                        RelicUpgradeCard(
                            title = "Aegis Ward Relic",
                            desc = "Provides impenetrable starlight armor that shatters obstacles on impact.",
                            icon = Icons.Default.Security,
                            themeColor = ShieldAzure,
                            level = profile.shieldLevel,
                            cost = profile.shieldLevel * 60,
                            canAfford = profile.totalCoins >= profile.shieldLevel * 60,
                            onUpgrade = { onUpgradeRelic("shield") },
                            tag = "upgrade_shield_button"
                        )
                    }
                    item {
                        RelicUpgradeCard(
                            title = "Dream Rush Relic",
                            desc = "Increases hyper-speed surge duration to blast through obstacles for bonus points.",
                            icon = Icons.Default.ElectricBolt,
                            themeColor = MagicPurpleLight,
                            level = profile.boostLevel,
                            cost = profile.boostLevel * 75,
                            canAfford = profile.totalCoins >= profile.boostLevel * 75,
                            onUpgrade = { onUpgradeRelic("boost") },
                            tag = "upgrade_boost_button"
                        )
                    }
                    item {
                        RelicUpgradeCard(
                            title = "Double Score Multiplier Relic",
                            desc = "Doubles distance score points and coin yield throughout your run.",
                            icon = Icons.Default.Star,
                            themeColor = AstralGold,
                            level = profile.multiplierLevel,
                            cost = profile.multiplierLevel * 80,
                            canAfford = profile.totalCoins >= profile.multiplierLevel * 80,
                            onUpgrade = { onUpgradeRelic("multiplier") },
                            tag = "upgrade_multiplier_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopTabPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) StarlightCyan else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (selected) VoidDark else Color.LightGray,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun CharacterShopCard(
    character: CharacterInfo,
    isUnlocked: Boolean,
    isSelected: Boolean,
    isPreviewing: Boolean,
    canAfford: Boolean,
    coinsNeeded: Int,
    animTime: Float,
    onPreview: () -> Unit,
    onBuy: () -> Unit,
    onSelect: () -> Unit
) {
    val cardBorder = when {
        isSelected -> BorderStroke(2.dp, character.primaryColor)
        isPreviewing -> BorderStroke(1.5.dp, StarlightCyan.copy(alpha = 0.8f))
        isUnlocked -> BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPreviewing) VoidSurfaceVariant else VoidSurfaceVariant.copy(alpha = 0.85f)
        ),
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview() }
            .testTag("character_card_${character.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini 3D Preview
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(character.primaryColor.copy(alpha = 0.28f), VoidDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    Character3DRenderer.drawCharacter(
                        drawScope = this,
                        character = character,
                        animState = CharacterAnimState.IDLE,
                        timeSeconds = animTime,
                        centerX = size.width * 0.5f,
                        groundY = size.height * 0.88f,
                        scale = 0.52f,
                        frontFacingFactor = 0.85f
                    )
                }

                if (!isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = AstralGold,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Character Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = character.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (character.isFreeStarter) {
                        Surface(
                            color = DreamEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "STARTER",
                                color = DreamEmerald,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = character.title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = character.primaryColor
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "★ ${character.perkName}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AstralGold
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isUnlocked) "UNLOCKED" else character.formattedPrice,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isUnlocked) DreamEmerald else AstralGold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button: BUY / SELECT / SELECTED
            when {
                !isUnlocked -> {
                    Button(
                        onClick = onBuy,
                        enabled = canAfford,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("buy_button_${character.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AstralGold,
                            contentColor = VoidDark,
                            disabledContainerColor = VoidSurface,
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (canAfford) "BUY" else "NOT ENOUGH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (canAfford) "${character.priceCoins}" else "Need %,d".format(coinsNeeded),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                isSelected -> {
                    Button(
                        onClick = {},
                        enabled = true,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("selected_button_${character.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = character.primaryColor,
                            contentColor = VoidDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                else -> {
                    Button(
                        onClick = onSelect,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("select_button_${character.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StarlightCyan,
                            contentColor = VoidDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SELECT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoverboardShopCard(
    board: HoverboardInfo,
    isUnlocked: Boolean,
    isSelected: Boolean,
    isPreviewing: Boolean,
    canAfford: Boolean,
    coinsNeeded: Int,
    animTime: Float,
    onPreview: () -> Unit,
    onBuy: () -> Unit,
    onSelect: () -> Unit
) {
    val cardBorder = when {
        isSelected -> BorderStroke(2.dp, board.glowColor)
        isPreviewing -> BorderStroke(1.5.dp, StarlightCyan.copy(alpha = 0.8f))
        isUnlocked -> BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPreviewing) VoidSurfaceVariant else VoidSurfaceVariant.copy(alpha = 0.85f)
        ),
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview() }
            .testTag("hoverboard_card_${board.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live 3D Board Preview Canvas
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(board.glowColor.copy(alpha = 0.25f), VoidDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    Hoverboard3DRenderer.drawHoverboardPreview(
                        drawScope = this,
                        board = board,
                        centerX = size.width * 0.5f,
                        centerY = size.height * 0.5f,
                        width = size.width * 0.88f,
                        height = size.height * 0.55f,
                        timeSeconds = animTime
                    )
                }

                if (!isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = AstralGold,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Hoverboard Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = board.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (board.isFreeStarter) {
                        Surface(
                            color = DreamEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "STARTER",
                                color = DreamEmerald,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = board.title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = board.glowColor
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "★ ${board.specialAbility}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AstralGold
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isUnlocked) "OWNED" else board.formattedPrice,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isUnlocked) DreamEmerald else AstralGold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button: BUY / SELECT / SELECTED
            when {
                !isUnlocked -> {
                    Button(
                        onClick = onBuy,
                        enabled = canAfford,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("buy_hoverboard_${board.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AstralGold,
                            contentColor = VoidDark,
                            disabledContainerColor = VoidSurface,
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (canAfford) "BUY" else "NOT ENOUGH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (canAfford) "${board.priceCoins}" else "Need %,d".format(coinsNeeded),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                isSelected -> {
                    Button(
                        onClick = {},
                        enabled = true,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("selected_hoverboard_${board.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = board.glowColor,
                            contentColor = VoidDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                else -> {
                    Button(
                        onClick = onSelect,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("select_hoverboard_${board.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StarlightCyan,
                            contentColor = VoidDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SELECT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelicUpgradeCard(
    title: String,
    desc: String,
    icon: ImageVector,
    themeColor: Color,
    level: Int,
    cost: Int,
    canAfford: Boolean,
    onUpgrade: () -> Unit,
    tag: String
) {
    val isMax = level >= 5
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VoidSurfaceVariant.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = themeColor.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = themeColor, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Row(modifier = Modifier.padding(top = 2.dp)) {
                            for (i in 1..5) {
                                Text(
                                    text = "★",
                                    color = if (i <= level) themeColor else Color.DarkGray,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMax) "MAX" else "LVL $level",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMax) themeColor else Color.Gray
                            )
                        }
                    }
                }

                if (!isMax) {
                    Button(
                        onClick = onUpgrade,
                        enabled = canAfford,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag(tag),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor,
                            contentColor = VoidDark,
                            disabledContainerColor = VoidSurface,
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("$cost Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("MAXED", color = themeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = desc, fontSize = 11.sp, color = Color.LightGray)
        }
    }
}
