package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlayerProfile
import com.example.model.AnimalForm
import com.example.ui.renderer.Animal3DRenderer
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.StarlightCyan
import com.example.ui.theme.VoidDark
import com.example.ui.theme.VoidSurface
import com.example.ui.theme.VoidSurfaceVariant

@Composable
fun AnimalGalleryScreen(
    profile: PlayerProfile,
    onSelectPreferredAnimal: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var stageTime by remember { mutableFloatStateOf(0f) }

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
                        .testTag("gallery_back_button")
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
                        text = "ANIMAL GALLERY",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Unlock 6 Jungle Animal Forms & Master Primal Powers",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DreamEmerald
                    )
                }
                Surface(
                    color = DreamEmerald.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DreamEmerald.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${profile.unlockedAnimalIds.size}/6 UNLOCKED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = DreamEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Info Banner
            Surface(
                color = VoidSurfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🐾", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Collect Animal Tokens during runs to fill your energy meter. When full (100%), transform into your preferred beast!",
                        fontSize = 11.sp,
                        color = Color(0xFFCFD8DC),
                        lineHeight = 16.sp
                    )
                }
            }

            // Animal Forms Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(AnimalForm.ALL, key = { it.id }) { animal ->
                    val isUnlocked = profile.isAnimalUnlocked(animal.id)
                    val isSelected = profile.selectedAnimal == animal.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("animal_card_${animal.id}"),
                        colors = CardDefaults.cardColors(containerColor = VoidSurface),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) animal.primaryColor else if (isUnlocked) animal.primaryColor.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 3D Live Animal Animated Stage
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    VoidDark,
                                                    animal.primaryColor.copy(alpha = 0.2f),
                                                    VoidSurfaceVariant
                                                )
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            if (isUnlocked) animal.primaryColor.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                                            RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        Animal3DRenderer.drawAnimalPreview(
                                            drawScope = this,
                                            animal = animal,
                                            centerX = size.width * 0.5f,
                                            groundY = size.height * 0.82f,
                                            scale = 0.95f,
                                            timeSeconds = stageTime
                                        )
                                    }

                                    Surface(
                                        color = VoidDark.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = if (isSelected) "EQUIPPED" else if (isUnlocked) "UNLOCKED" else "LOCKED",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) StarlightCyan else if (isUnlocked) DreamEmerald else AstralGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Details & Ability Info
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = animal.displayName.uppercase(),
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Surface(
                                            color = animal.primaryColor.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${animal.durationSeconds.toInt()}s DURATION",
                                                color = animal.primaryColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = animal.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AstralGold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚡ Ability: ${animal.abilityName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = animal.primaryColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = animal.abilityDescription,
                                        fontSize = 11.sp,
                                        color = Color(0xFFCFD8DC),
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Unlock requirement or selection button
                            if (isUnlocked) {
                                Button(
                                    onClick = { onSelectPreferredAnimal(animal.id) },
                                    enabled = !isSelected,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .testTag("select_animal_button_${animal.id}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) DreamEmerald else StarlightCyan,
                                        contentColor = VoidDark,
                                        disabledContainerColor = VoidSurfaceVariant,
                                        disabledContentColor = StarlightCyan
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Stars,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSelected) "PREFERRED TRANSFORMATION BEAST" else "SET AS PREFERRED BEAST",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            } else {
                                Surface(
                                    color = VoidDark.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = AstralGold, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "UNLOCK REQUIREMENT:",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AstralGold
                                            )
                                            Text(
                                                text = animal.unlockRequirement,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFE0E0E0)
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
}
