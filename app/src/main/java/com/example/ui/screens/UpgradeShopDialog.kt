package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.PlayerProfile
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.MagicPurpleLight
import com.example.ui.theme.ShieldAzure
import com.example.ui.theme.StarlightCyan
import com.example.ui.theme.VoidDark
import com.example.ui.theme.VoidSurface
import com.example.ui.theme.VoidSurfaceVariant

@Composable
fun UpgradeShopDialog(
    profile: PlayerProfile,
    onUpgrade: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = VoidSurfaceVariant),
            modifier = Modifier.fillMaxWidth(0.96f)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Relics & Upgrades",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(14.dp).background(AstralGold, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${profile.totalCoins} Dream Coins",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AstralGold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("shop_close_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        ShopRelicItem(
                            title = "Magnetic Shield Core",
                            desc = "Extends Magnetic Shield coin-pull duration (+3s/lvl), range & barrier.",
                            icon = Icons.Default.VolunteerActivism,
                            themeColor = StarlightCyan,
                            level = profile.magnetLevel,
                            cost = profile.magnetLevel * 50,
                            canAfford = profile.totalCoins >= (profile.magnetLevel * 50),
                            onUpgrade = { onUpgrade("magnet") },
                            tag = "upgrade_magnet"
                        )
                    }
                    item {
                        ShopRelicItem(
                            title = "Double Score Crest",
                            desc = "Extends Double Score (2X Points & Coins) duration (+3s/lvl) & base multiplier.",
                            icon = Icons.Default.ElectricBolt,
                            themeColor = AstralGold,
                            level = profile.multiplierLevel,
                            cost = profile.multiplierLevel * 80,
                            canAfford = profile.totalCoins >= (profile.multiplierLevel * 80),
                            onUpgrade = { onUpgrade("multiplier") },
                            tag = "upgrade_multiplier"
                        )
                    }
                    item {
                        ShopRelicItem(
                            title = "Starlight Aegis Ward",
                            desc = "Strengthens protective barrier & extends post-shatter invulnerability.",
                            icon = Icons.Default.Security,
                            themeColor = ShieldAzure,
                            level = profile.shieldLevel,
                            cost = profile.shieldLevel * 60,
                            canAfford = profile.totalCoins >= (profile.shieldLevel * 60),
                            onUpgrade = { onUpgrade("shield") },
                            tag = "upgrade_shield"
                        )
                    }
                    item {
                        ShopRelicItem(
                            title = "Dream Rush Nova",
                            desc = "Extends invincible obstacle-smashing speed surge (+2s per level).",
                            icon = Icons.Default.Star,
                            themeColor = MagicPurpleLight,
                            level = profile.boostLevel,
                            cost = profile.boostLevel * 75,
                            canAfford = profile.totalCoins >= (profile.boostLevel * 75),
                            onUpgrade = { onUpgrade("boost") },
                            tag = "upgrade_boost"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopRelicItem(
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
        colors = CardDefaults.cardColors(containerColor = VoidDark.copy(alpha = 0.7f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = themeColor.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
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
                        modifier = Modifier.testTag(tag),
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
