package com.example.ui.renderer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.example.engine.GameEngine
import com.example.model.AnimalForm
import com.example.model.Biome
import com.example.model.CharacterAnimState
import com.example.model.MysteryBoxRarity
import com.example.model.Obstacle
import com.example.model.ObstacleType
import com.example.model.Pickup
import com.example.model.PickupType
import com.example.model.SceneryObject
import com.example.model.SceneryType
import com.example.model.SkyBoosterType
import com.example.model.TrackSectionType
import com.example.ui.theme.AstralGold
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DreamEmerald
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.MagicPurpleLight
import com.example.ui.theme.ShieldAzure
import com.example.ui.theme.StarlightCyan
import com.example.ui.theme.VoidDark
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class GameCanvasRenderer {

    fun render(
        drawScope: DrawScope,
        engine: GameEngine,
        timeSeconds: Float
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        val shakeOffset = if (engine.screenShake > 0f) {
            Offset(
                x = sin(timeSeconds * 48f) * 16f * engine.screenShake,
                y = cos(timeSeconds * 40f) * 12f * engine.screenShake
            )
        } else Offset.Zero

        val vx = (width / 2f) + shakeOffset.x + (engine.laneX * -14f)
        val vy = (height * 0.355f) + shakeOffset.y + (engine.playerY * -6f)
        val focalLength = 310f
        val trackWidthBottom = width * 0.88f
        val laneSpacing = trackWidthBottom / 3f
        val groundDelta = (height * 0.93f) - vy

        // 1. Cinematic 3D Jungle Sky, Ancient Temple Pyramids, Waterfalls & Canopy Atmosphere
        drawEnvironment(drawScope, width, height, vx, vy, engine, timeSeconds)

        // 2. 3D Ancient Temple Stone Pathway, Bridges & 3 Clear Lanes
        drawTrack(drawScope, width, height, vx, vy, groundDelta, focalLength, laneSpacing, engine, timeSeconds)

        // 3. Depth Sorting: Obstacles, Pickups, Scenery, Player
        val entities = mutableListOf<RenderEntity>()

        engine.obstacles.forEach { obs ->
            val relZ = obs.z - engine.distanceMeters
            if (relZ in -1.5f..118f) entities.add(RenderEntity.ObstacleE(obs, relZ))
        }

        engine.pickups.forEach { p ->
            val relZ = p.z - engine.distanceMeters
            if (!p.collected && relZ in -1.5f..118f) entities.add(RenderEntity.PickupE(p, relZ))
        }

        engine.scenery.forEach { s ->
            val relZ = s.z - engine.distanceMeters
            if (relZ in -2f..120f) entities.add(RenderEntity.SceneryE(s, relZ))
        }

        // Player at relative Z = 0
        entities.add(RenderEntity.PlayerE(relZ = 0f))

        // Painter's algorithm: sort far to near
        entities.sortByDescending { it.relZ }

        entities.forEach { entity ->
            when (entity) {
                is RenderEntity.ObstacleE -> drawObstacle(drawScope, entity.obs, entity.relZ, vx, vy, groundDelta, focalLength, laneSpacing, timeSeconds)
                is RenderEntity.PickupE -> drawPickup(drawScope, entity.pickup, entity.relZ, vx, vy, groundDelta, focalLength, laneSpacing, timeSeconds)
                is RenderEntity.SceneryE -> drawSceneryObject(drawScope, entity.scenery, entity.relZ, vx, vy, groundDelta, focalLength, laneSpacing, engine, timeSeconds)
                is RenderEntity.PlayerE -> drawPlayer(drawScope, engine, vx, vy, groundDelta, laneSpacing, timeSeconds)
            }
        }

        // 4. Glowing Particles & High-Speed Wind Lines
        drawParticles(drawScope, width, height, engine, vx, vy, groundDelta, laneSpacing, timeSeconds)
    }

    private fun drawEnvironment(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        vx: Float,
        vy: Float,
        engine: GameEngine,
        timeSeconds: Float
    ) {
        val cur = engine.currentBiome
        val nxt = engine.nextBiome
        val blend = engine.biomeTransitionBlend

        val skyTop = lerp(cur.skyTopColor, nxt.skyTopColor, blend)
        val horizonCol = lerp(cur.horizonColor, nxt.horizonColor, blend)
        val groundCol = lerp(cur.groundColor, nxt.groundColor, blend)
        val mountainCol = lerp(cur.mountainColor, nxt.mountainColor, blend)
        val particleCol = lerp(cur.ambientParticleColor, nxt.ambientParticleColor, blend)

        // Sky dome & surrounding jungle valley floor
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, horizonCol, groundCol),
                startY = 0f,
                endY = height
            ),
            topLeft = Offset.Zero,
            size = Size(width, height)
        )

        // Sunlight Shining Through Canopy (God-Rays & Tropical Sun Glow)
        val sunX = width * 0.72f
        val sunY = vy * 0.24f
        val sunGlowCol = lerp(cur.sunbeamColor, nxt.sunbeamColor, blend)

        // Soft Sunlight Radiance
        drawScope.drawCircle(
            color = sunGlowCol.copy(alpha = 0.28f),
            radius = 72f,
            center = Offset(sunX, sunY)
        )
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFE0), sunGlowCol, Color.Transparent),
                center = Offset(sunX, sunY),
                radius = 48f
            ),
            radius = 44f,
            center = Offset(sunX, sunY)
        )

        // Multiple Volumetric God-Rays / Sunbeams filtering through tree canopy
        for (ray in 0..4) {
            val rayAngle = -0.55f + ray * 0.26f
            val rayEndX = sunX + sin(rayAngle) * 360f
            val rayEndY = vy + 30f
            val beamPath = Path().apply {
                moveTo(sunX - 12f, sunY)
                lineTo(sunX + 12f, sunY)
                lineTo(rayEndX + 45f, rayEndY)
                lineTo(rayEndX - 45f, rayEndY)
                close()
            }
            val rayPulse = 0.08f + 0.04f * sin(timeSeconds * 1.8f + ray)
            drawScope.drawPath(
                path = beamPath,
                color = sunGlowCol.copy(alpha = rayPulse.coerceIn(0.04f, 0.16f))
            )
        }

        // Distant Ancient Stepped Temple Pyramids (Iconic Ancient Jungle Architecture)
        val pyramidScroll = (engine.distanceMeters * 0.032f) % width
        for (pIdx in 0..1) {
            val px = ((pIdx * width * 0.52f + width * 0.20f - pyramidScroll) % width + width) % width
            val pyBase = vy + 4f
            val pyrW = 76f
            // 3 stepped terraces
            for (step in 0..2) {
                val stepW = pyrW * (1f - step * 0.28f)
                val stepH = 12f
                val stepY = pyBase - 12f - step * 12f
                drawScope.drawRect(
                    color = mountainCol.copy(alpha = 0.90f),
                    topLeft = Offset(px - stepW / 2f, stepY),
                    size = Size(stepW, stepH)
                )
            }
            // Top shrine chamber on the pyramid peak
            val peakY = pyBase - 48f
            drawScope.drawRect(
                color = mountainCol.copy(alpha = 0.95f),
                topLeft = Offset(px - 8f, peakY - 8f),
                size = Size(16f, 8f)
            )
            // Glowing ancient beacon relic on the pyramid pinnacle
            drawScope.drawCircle(
                color = particleCol.copy(alpha = 0.75f),
                radius = 3.5f,
                center = Offset(px, peakY - 11f)
            )
        }

        // Distant Jungle Rainforest Mountains & Ridge Peaks (Parallax Layer)
        val mountainScroll = (engine.distanceMeters * 0.05f) % width
        val backMountainPath = Path().apply {
            moveTo(0f, vy + 12f)
            val peaks = 6
            val segW = width / peaks
            for (i in 0..peaks) {
                val px = i * segW
                val peakH = 55f + 32f * abs(sin((i * 1.55f) + mountainScroll * 0.012f))
                lineTo(px - segW * 0.5f, vy - peakH)
                lineTo(px, vy - peakH * 0.32f)
            }
            lineTo(width, vy + 12f)
            close()
        }
        drawScope.drawPath(backMountainPath, color = mountainCol.copy(alpha = 0.94f))

        // Distant Jungle Cliffs with Towering Waterfalls and Mist Clouds
        for (wIdx in 0..2) {
            val cliffX = ((wIdx * width * 0.38f - engine.distanceMeters * 0.07f) % width + width) % width
            val cliffTop = vy - 46f - (wIdx % 2) * 14f
            val cliffHeight = 52f + (wIdx % 2) * 16f
            val cliffW = 28f

            // Cliff Rock face
            drawScope.drawRoundRect(
                color = mountainCol.copy(alpha = 0.85f),
                topLeft = Offset(cliffX - cliffW / 2f, cliffTop),
                size = Size(cliffW, cliffHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Cascading Waterfall Ribbon
            drawScope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.82f), Color(0xFF80D8FF).copy(alpha = 0.65f), Color.Transparent),
                    startY = cliffTop + 4f,
                    endY = cliffTop + cliffHeight
                ),
                topLeft = Offset(cliffX - 3.5f, cliffTop + 4f),
                size = Size(7f, cliffHeight - 4f)
            )

            // Billowing Mist Cloud at Base of Cliff Waterfall
            val mistBob = sin(timeSeconds * 2.2f + wIdx) * 2f
            drawScope.drawOval(
                color = Color.White.copy(alpha = 0.30f),
                topLeft = Offset(cliffX - 22f, cliffTop + cliffHeight - 10f + mistBob),
                size = Size(44f, 16f)
            )
        }

        // Soft Horizontal Jungle Mist / Fog layer over the valley horizon
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent),
                startY = vy - 24f,
                endY = vy + 14f
            ),
            topLeft = Offset(0f, vy - 24f),
            size = Size(width, 38f)
        )

        // Upper Canopy Silhouette Framing the Screen Corners (Dense Jungle Canopy)
        drawTopCanopySilhouettes(drawScope, width, vy, groundCol)

        // Atmospheric Jungle Wildlife: Soaring Tropical Birds & Fluttering Butterflies
        drawJungleAtmosphere(drawScope, width, vy, cur, particleCol, timeSeconds)
    }

    private fun drawTopCanopySilhouettes(
        drawScope: DrawScope,
        width: Float,
        vy: Float,
        canopyColor: Color
    ) {
        val darkGreen = Color(0xFF0A1F0D)
        // Left Canopy Overhang
        val leftCanopy = Path().apply {
            moveTo(0f, 0f)
            lineTo(width * 0.35f, 0f)
            quadraticTo(width * 0.28f, vy * 0.28f, width * 0.16f, vy * 0.38f)
            quadraticTo(width * 0.08f, vy * 0.25f, 0f, vy * 0.34f)
            close()
        }
        drawScope.drawPath(leftCanopy, color = darkGreen.copy(alpha = 0.72f))

        // Right Canopy Overhang with hanging vine strands
        val rightCanopy = Path().apply {
            moveTo(width, 0f)
            lineTo(width * 0.65f, 0f)
            quadraticTo(width * 0.74f, vy * 0.26f, width * 0.86f, vy * 0.36f)
            quadraticTo(width * 0.94f, vy * 0.22f, width, vy * 0.32f)
            close()
        }
        drawScope.drawPath(rightCanopy, color = darkGreen.copy(alpha = 0.68f))

        // Hanging vine strands dangling from the canopy
        for (v in 0..3) {
            val vx = width * (0.08f + v * 0.06f)
            val vineLen = vy * (0.28f + (v % 2) * 0.12f)
            drawScope.drawLine(
                color = Color(0xFF1B5E20).copy(alpha = 0.8f),
                start = Offset(vx, 0f),
                end = Offset(vx + (v % 2) * 5f, vineLen),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
        for (v in 0..3) {
            val vx = width * (0.75f + v * 0.07f)
            val vineLen = vy * (0.24f + (v % 2) * 0.10f)
            drawScope.drawLine(
                color = Color(0xFF1B5E20).copy(alpha = 0.8f),
                start = Offset(vx, 0f),
                end = Offset(vx - (v % 2) * 5f, vineLen),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }

    private fun drawJungleAtmosphere(
        drawScope: DrawScope,
        width: Float,
        vy: Float,
        biome: Biome,
        particleColor: Color,
        timeSeconds: Float
    ) {
        // 1. Soaring Tropical Jungle Birds (Scarlet Macaws & Golden Sun-Birds)
        for (b in 0..2) {
            val birdSpeed = 32f + b * 14f
            val bx = ((timeSeconds * birdSpeed + b * 260f) % (width + 180f)) - 90f
            val by = vy * (0.20f + b * 0.10f) + sin(timeSeconds * 2.8f + b * 1.7f) * 8f
            val wingFlap = sin(timeSeconds * 8.5f + b * 2.4f) * 10f
            val birdScale = 0.95f + b * 0.18f

            val birdBodyCol = if (b % 2 == 0) Color(0xFFFF3D00) else Color(0xFFFFD600)
            val wingCol = if (b % 2 == 0) Color(0xFF00E5FF) else Color(0xFF00C853)

            // Animated Bird Wings
            val birdWings = Path().apply {
                moveTo(bx, by)
                quadraticTo(bx - 14f * birdScale, by - 10f * birdScale + wingFlap, bx - 26f * birdScale, by + wingFlap * 1.1f)
                quadraticTo(bx - 12f * birdScale, by + 3f * birdScale, bx, by + 2f * birdScale)
                quadraticTo(bx + 12f * birdScale, by + 3f * birdScale, bx + 26f * birdScale, by + wingFlap * 1.1f)
                quadraticTo(bx + 14f * birdScale, by - 10f * birdScale + wingFlap, bx, by)
                close()
            }
            drawScope.drawPath(birdWings, color = wingCol.copy(alpha = 0.90f))

            // Bird Body & Streamer Tail Feather
            drawScope.drawOval(
                color = birdBodyCol,
                topLeft = Offset(bx - 3.5f * birdScale, by - 4f * birdScale),
                size = Size(7f * birdScale, 11f * birdScale)
            )
            drawScope.drawLine(
                color = AstralGold,
                start = Offset(bx, by + 6f * birdScale),
                end = Offset(bx - 14f * birdScale, by + 18f * birdScale),
                strokeWidth = 2.2f * birdScale,
                cap = StrokeCap.Round
            )
        }

        // 2. Fluttering Tropical Butterflies (Morpho Blue & Emerald-Gold)
        for (bf in 0..3) {
            val flutterSpeed = 18f + bf * 7f
            val bfx = ((timeSeconds * flutterSpeed + bf * 160f) % (width + 100f)) - 50f
            val bfy = vy * (0.42f + bf * 0.12f) + sin(timeSeconds * 4.2f + bf * 1.9f) * 12f
            val wingSpread = abs(sin(timeSeconds * 12f + bf * 3.1f)) * 8f + 2f
            val bfColor = when (bf % 3) {
                0 -> Color(0xFF00E5FF) // Morpho Blue
                1 -> Color(0xFF76FF03) // Emerald Green
                else -> Color(0xFFFFD54F) // Golden Monarch
            }

            // Left & Right Fluttering Wings
            drawScope.drawOval(
                color = bfColor.copy(alpha = 0.85f),
                topLeft = Offset(bfx - wingSpread, bfy - 5f),
                size = Size(wingSpread, 9f)
            )
            drawScope.drawOval(
                color = bfColor.copy(alpha = 0.85f),
                topLeft = Offset(bfx, bfy - 5f),
                size = Size(wingSpread, 9f)
            )
            drawScope.drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 1.4f,
                center = Offset(bfx, bfy)
            )
        }
    }

    private fun drawTrack(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        vx: Float,
        vy: Float,
        groundDelta: Float,
        focalLength: Float,
        laneSpacing: Float,
        engine: GameEngine,
        timeSeconds: Float
    ) {
        val cur = engine.currentBiome
        val nxt = engine.nextBiome
        val blend = engine.biomeTransitionBlend
        val pathCol = lerp(cur.pathColor, nxt.pathColor, blend)
        val groundCol = lerp(cur.groundColor, nxt.groundColor, blend)
        val glowCol = lerp(cur.ambientParticleColor, nxt.ambientParticleColor, blend)
        val stoneTileCol = lerp(cur.stoneTileColor, nxt.stoneTileColor, blend)
        val wallCol = lerp(cur.wallColor, nxt.wallColor, blend)

        val bottomY = vy + groundDelta
        val horizonScale = focalLength / (115f + focalLength)
        val topY = vy + (groundDelta * horizonScale)

        val leftBottomX = vx - (laneSpacing * 1.5f)
        val rightBottomX = vx + (laneSpacing * 1.5f)
        val leftTopX = vx - (laneSpacing * 1.5f * horizonScale)
        val rightTopX = vx + (laneSpacing * 1.5f * horizonScale)

        val isBridge = (engine.currentTrackSection == TrackSectionType.MAGICAL_BRIDGE || cur == Biome.STONE_BRIDGE_VALLEY || cur == Biome.WATERFALL_JUNGLE)

        // Valley Ground Flanking the Running Path / Gorge Abyss
        if (isBridge) {
            // Rushing canyon river abyss far below the bridge
            drawScope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(groundCol, Color(0xFF0D2530), Color(0xFF041017)),
                    startY = topY,
                    endY = height
                ),
                topLeft = Offset(0f, topY),
                size = Size(width, height - topY)
            )
            // Foaming river rapids deep down in the chasm
            val riverFlow = (timeSeconds * 50f) % 60f
            for (rv in 0..3) {
                val ry = vy + groundDelta * 0.95f + rv * 12f
                drawScope.drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF80D8FF).copy(alpha = 0.35f), Color.White.copy(alpha = 0.45f), Color.Transparent),
                        startX = vx - 120f + riverFlow,
                        endX = vx + 120f + riverFlow
                    ),
                    topLeft = Offset(0f, ry),
                    size = Size(width, 6f)
                )
            }
        } else {
            // Jungle Valley floor with lush vegetation flanking the stone pathway
            drawScope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(groundCol, Color(0xFF0B1F0E), VoidDark),
                    startY = topY,
                    endY = height
                ),
                topLeft = Offset(0f, topY),
                size = Size(width, height - topY)
            )
        }

        // 3D Ancient Stone Temple Pathway Surface
        val trackPath = Path().apply {
            moveTo(leftTopX, topY)
            lineTo(rightTopX, topY)
            lineTo(rightBottomX, bottomY + 40f)
            lineTo(leftBottomX, bottomY + 40f)
            close()
        }
        drawScope.drawPath(
            path = trackPath,
            brush = Brush.verticalGradient(
                colors = if (isBridge) {
                    listOf(Color(0xFF3E2723), Color(0xFF4E342E), Color(0xFF2E1C14))
                } else {
                    listOf(groundCol, stoneTileCol, pathCol)
                },
                startY = topY,
                endY = bottomY
            )
        )

        // Ancient Raised Stone Parapets / Walls flanking the path (3D temple borders)
        val curbWBottom = 22f
        val curbWTop = 22f * horizonScale
        val curbH = 14f

        if (!isBridge) {
            // Left Stone Parapet Wall (raised 3D temple balustrade)
            val leftParapet = Path().apply {
                moveTo(leftTopX - curbWTop, topY - curbH * horizonScale)
                lineTo(leftTopX, topY)
                lineTo(leftBottomX, bottomY + 40f)
                lineTo(leftBottomX - curbWBottom, bottomY + 40f - curbH)
                close()
            }
            drawScope.drawPath(leftParapet, color = wallCol)
            // Left Parapet top coping stone edge
            drawScope.drawLine(
                color = stoneTileCol.copy(alpha = 0.9f),
                start = Offset(leftTopX - curbWTop, topY - curbH * horizonScale),
                end = Offset(leftBottomX - curbWBottom, bottomY + 40f - curbH),
                strokeWidth = 3f
            )

            // Right Stone Parapet Wall
            val rightParapet = Path().apply {
                moveTo(rightTopX, topY)
                lineTo(rightTopX + curbWTop, topY - curbH * horizonScale)
                lineTo(rightBottomX + curbWBottom, bottomY + 40f - curbH)
                lineTo(rightBottomX, bottomY + 40f)
                close()
            }
            drawScope.drawPath(rightParapet, color = wallCol)
            // Right Parapet top coping stone edge
            drawScope.drawLine(
                color = stoneTileCol.copy(alpha = 0.9f),
                start = Offset(rightTopX + curbWTop, topY - curbH * horizonScale),
                end = Offset(rightBottomX + curbWBottom, bottomY + 40f - curbH),
                strokeWidth = 3f
            )
        }

        // Scrolling Ancient Stone Flagstones & Bridge Planks
        val scrollDist = engine.distanceMeters % 4.8f
        for (d in 0..22) {
            val slabZ = (d * 4.8f) - scrollDist + 0.6f
            if (slabZ in 0.5f..112f) {
                val scale = focalLength / (slabZ + focalLength)
                val py = vy + (groundDelta * scale)
                val pLeftX = vx - (laneSpacing * 1.5f * scale)
                val pRightX = vx + (laneSpacing * 1.5f * scale)
                val spanW = pRightX - pLeftX

                // Transverse ancient stone joint / wooden plank seam
                val seamColor = if (isBridge) {
                    Color(0xFF261811)
                } else {
                    Color(0xFF1B221B).copy(alpha = (scale * 0.75f).coerceIn(0.2f, 0.75f))
                }
                drawScope.drawLine(
                    color = seamColor,
                    start = Offset(pLeftX, py),
                    end = Offset(pRightX, py),
                    strokeWidth = (3.5f * scale).coerceAtLeast(1.2f)
                )

                // Staggered vertical stone joints between flags on alternate rows
                if (!isBridge && slabZ in 1.5f..85f) {
                    val jointFrac = if (d % 2 == 0) 0.333f else 0.666f
                    val jointX = pLeftX + spanW * jointFrac
                    val slabH = 4.8f * scale * (groundDelta / focalLength)
                    drawScope.drawLine(
                        color = seamColor,
                        start = Offset(jointX, py),
                        end = Offset(jointX, py - slabH * 0.85f),
                        strokeWidth = (2.2f * scale).coerceAtLeast(0.8f)
                    )

                    // Moss creeping in stone crevices
                    if (d % 3 == 0) {
                        drawScope.drawOval(
                            color = Color(0xFF2E7D32).copy(alpha = (scale * 0.55f).coerceIn(0.15f, 0.5f)),
                            topLeft = Offset(jointX - 4f * scale, py - 3f * scale),
                            size = Size(8f * scale, 3f * scale)
                        )
                    }

                    // Golden Temple golden rune veins
                    if (cur == Biome.MYSTERIOUS_GOLDEN_TEMPLE && d % 4 == 0) {
                        drawScope.drawLine(
                            color = AstralGold.copy(alpha = 0.5f * scale),
                            start = Offset(pLeftX + spanW * 0.2f, py),
                            end = Offset(pLeftX + spanW * 0.8f, py),
                            strokeWidth = 1.8f * scale
                        )
                    }
                }

                // Wooden Bridge Posts & Suspension Ropes along bridge edges
                if (isBridge && d % 2 == 0) {
                    val postH = 34f * scale
                    val postW = (4f * scale).coerceAtLeast(1.5f)
                    // Left timber post
                    drawScope.drawLine(
                        color = Color(0xFF5D4037),
                        start = Offset(pLeftX, py),
                        end = Offset(pLeftX, py - postH),
                        strokeWidth = postW,
                        cap = StrokeCap.Round
                    )
                    // Right timber post
                    drawScope.drawLine(
                        color = Color(0xFF5D4037),
                        start = Offset(pRightX, py),
                        end = Offset(pRightX, py - postH),
                        strokeWidth = postW,
                        cap = StrokeCap.Round
                    )
                    // Rope handrails connecting posts
                    drawScope.drawCircle(
                        color = Color(0xFFBCAAA4),
                        radius = 2.5f * scale,
                        center = Offset(pLeftX, py - postH)
                    )
                    drawScope.drawCircle(
                        color = Color(0xFFBCAAA4),
                        radius = 2.5f * scale,
                        center = Offset(pRightX, py - postH)
                    )
                }
            }
        }

        // Animated Dappled Canopy Leaf Shadows swaying across the stone road
        val shadowScroll = (engine.distanceMeters * 0.35f) % 24f
        for (s in 0..4) {
            val sZ = 12f + s * 16f - shadowScroll
            if (sZ in 4f..75f) {
                val scale = focalLength / (sZ + focalLength)
                val sy = vy + (groundDelta * scale)
                val swayX = sin(timeSeconds * 1.8f + s * 1.2f) * 18f * scale
                val sx = vx + swayX
                val sW = laneSpacing * 1.1f * scale
                val sH = 16f * scale
                drawScope.drawOval(
                    color = Color.Black.copy(alpha = 0.16f),
                    topLeft = Offset(sx - sW / 2f, sy - sH / 2f),
                    size = Size(sW, sH)
                )
            }
        }

        // Three Clear Running Lanes with Ancient Stone Dividers & Outer Borders
        for (i in -1..2) {
            val offsetUnits = i - 0.5f
            val isOuter = (i == -1 || i == 2)
            val edgeColor = if (isOuter) {
                if (cur == Biome.MYSTERIOUS_GOLDEN_TEMPLE) AstralGold else wallCol
            } else {
                glowCol.copy(alpha = 0.45f)
            }
            val edgeWidth = if (isOuter) 3.8f else 2.2f

            val xBottom = vx + (laneSpacing * offsetUnits)
            val xTop = vx + (laneSpacing * offsetUnits * horizonScale)

            drawScope.drawLine(
                color = edgeColor,
                start = Offset(xTop, topY),
                end = Offset(xBottom, bottomY + 20f),
                strokeWidth = edgeWidth
            )
        }

        // Early Obstacle Lane Warning Telegraphs (makes incoming obstacles clearly visible ahead!)
        engine.obstacles.forEach { obs ->
            val relZ = obs.z - engine.distanceMeters
            if (relZ in 4f..68f) {
                val scale = focalLength / (relZ + focalLength)
                val warnY = vy + (groundDelta * scale)
                val warnX = vx + (obs.lane * laneSpacing * scale)
                val warnW = laneSpacing * 0.72f * scale
                val pulse = 0.35f + 0.25f * abs(sin(timeSeconds * 8f + obs.id))
                val indicatorColor = when (obs.type) {
                    ObstacleType.LOW_LOG, ObstacleType.BRIDGE_GAP -> AstralGold
                    ObstacleType.HIGH_ARCH -> StarlightCyan
                    ObstacleType.TALL_CRYSTAL, ObstacleType.MYSTIC_WISP -> DangerCrimson
                }
                drawScope.drawOval(
                    color = indicatorColor.copy(alpha = pulse),
                    topLeft = Offset(warnX - warnW / 2f, warnY - 5f * scale),
                    size = Size(warnW, 10f * scale)
                )
            }
        }
    }

    private fun drawSceneryObject(
        drawScope: DrawScope,
        obj: SceneryObject,
        relZ: Float,
        vx: Float,
        vy: Float,
        groundDelta: Float,
        focalLength: Float,
        laneSpacing: Float,
        engine: GameEngine,
        timeSeconds: Float
    ) {
        val scale = focalLength / (relZ + focalLength)
        val groundY = vy + (groundDelta * scale)
        val sx = vx + (obj.side * obj.lateralOffset * laneSpacing * scale)
        val h = 76f * obj.scale * scale
        val w = 30f * obj.scale * scale
        val glowCol = engine.currentBiome.ambientParticleColor

        when (obj.type) {
            SceneryType.DENSE_TROPICAL_TREE -> {
                // Towering Tropical Rainforest Tree with buttress roots, vines & broad leafy canopy
                val trunkW = w * 0.36f
                val trunkH = h * 0.70f
                // Buttress roots at base
                val rootPath = Path().apply {
                    moveTo(sx - trunkW * 1.3f, groundY)
                    quadraticTo(sx - trunkW * 0.5f, groundY - trunkH * 0.25f, sx - trunkW * 0.4f, groundY - trunkH * 0.4f)
                    lineTo(sx + trunkW * 0.4f, groundY - trunkH * 0.4f)
                    quadraticTo(sx + trunkW * 0.5f, groundY - trunkH * 0.25f, sx + trunkW * 1.3f, groundY)
                    close()
                }
                drawScope.drawPath(rootPath, color = Color(0xFF3E2723))

                // Main trunk with bark shading
                drawScope.drawRoundRect(
                    color = Color(0xFF4E342E),
                    topLeft = Offset(sx - trunkW / 2f, groundY - trunkH),
                    size = Size(trunkW, trunkH),
                    cornerRadius = CornerRadius(4f * scale, 4f * scale)
                )

                // Creeping vine wrapping around trunk
                for (v in 0..2) {
                    val vineY = groundY - trunkH * (0.2f + v * 0.28f)
                    drawScope.drawLine(
                        color = Color(0xFF2E7D32),
                        start = Offset(sx - trunkW * 0.6f, vineY + 6f * scale),
                        end = Offset(sx + trunkW * 0.6f, vineY - 4f * scale),
                        strokeWidth = 2.5f * scale,
                        cap = StrokeCap.Round
                    )
                }

                // Dense multi-tiered tropical palm & monstera canopies
                for (tier in 0..2) {
                    val tierY = groundY - trunkH - (tier * h * 0.18f)
                    val tierR = w * (1.15f - tier * 0.22f)

                    // Deep foliage shadow
                    drawScope.drawCircle(
                        color = Color(0xFF1B5E20).copy(alpha = 0.94f),
                        radius = tierR,
                        center = Offset(sx, tierY)
                    )
                    // Sunlit foliage highlight
                    drawScope.drawCircle(
                        color = Color(0xFF43A047).copy(alpha = 0.85f),
                        radius = tierR * 0.72f,
                        center = Offset(sx - tierR * 0.18f, tierY - tierR * 0.18f)
                    )
                    // Ambient jungle glow speckle
                    drawScope.drawCircle(
                        color = glowCol.copy(alpha = 0.40f),
                        radius = tierR * 0.35f,
                        center = Offset(sx + tierR * 0.12f, tierY - tierR * 0.12f)
                    )
                }
            }

            SceneryType.HANGING_GIANT_VINE -> {
                // Giant sweeping jungle liana vine draping from canopy with leaves & glowing blossoms
                val vineH = h * 0.95f
                val swing = sin(timeSeconds * 2.2f + obj.id) * 6f * scale
                val vinePath = Path().apply {
                    moveTo(sx, groundY - vineH)
                    quadraticTo(sx + obj.side * w * 0.8f + swing, groundY - vineH * 0.5f, sx + swing, groundY - 6f * scale)
                }
                drawScope.drawPath(
                    path = vinePath,
                    color = Color(0xFF2E7D32),
                    style = Stroke(width = 4.5f * scale, cap = StrokeCap.Round)
                )

                // Clustered jungle leaves along the vine
                for (lf in 1..4) {
                    val frac = lf / 5f
                    val lx = sx + obj.side * w * 0.6f * sin(frac * 3.14f) + swing * frac
                    val ly = groundY - vineH * (1f - frac)
                    drawScope.drawOval(
                        color = Color(0xFF66BB6A),
                        topLeft = Offset(lx - 5f * scale, ly - 3f * scale),
                        size = Size(10f * scale, 6f * scale)
                    )
                }

                // Hanging magical jungle blossom at the bottom tip
                val blossomY = groundY - 6f * scale
                drawScope.drawCircle(
                    color = glowCol,
                    radius = 5.5f * scale,
                    center = Offset(sx + swing, blossomY)
                )
                drawScope.drawCircle(
                    color = Color.White,
                    radius = 2.5f * scale,
                    center = Offset(sx + swing, blossomY)
                )
            }

            SceneryType.ANCIENT_RUIN_PILLAR -> {
                // Weathered Ancient Stone Ruins Pillar with carved relief runes & overgrowth
                val pillarW = w * 0.82f
                val pillarH = h * 0.90f
                val pTopY = groundY - pillarH

                // Stepped stone base pedestal
                drawScope.drawRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(sx - pillarW * 0.65f, groundY - 10f * scale),
                    size = Size(pillarW * 1.3f, 10f * scale)
                )
                // Main carved stone shaft
                drawScope.drawRoundRect(
                    color = Color(0xFF455A64),
                    topLeft = Offset(sx - pillarW / 2f, pTopY + 12f * scale),
                    size = Size(pillarW, pillarH - 22f * scale),
                    cornerRadius = CornerRadius(3f * scale, 3f * scale)
                )
                // Weathered stone capital top
                drawScope.drawRect(
                    color = Color(0xFF546E7A),
                    topLeft = Offset(sx - pillarW * 0.6f, pTopY),
                    size = Size(pillarW * 1.2f, 12f * scale)
                )
                // Overgrowing moss patches
                drawScope.drawOval(
                    color = Color(0xFF2E7D32).copy(alpha = 0.88f),
                    topLeft = Offset(sx - pillarW * 0.45f, groundY - pillarH * 0.4f),
                    size = Size(pillarW * 0.6f, 18f * scale)
                )
                // Glowing carved ancient rune symbol
                drawScope.drawCircle(
                    color = glowCol,
                    radius = 5.5f * scale,
                    center = Offset(sx, groundY - pillarH * 0.55f)
                )
                drawScope.drawCircle(
                    color = AstralGold,
                    radius = 2.5f * scale,
                    center = Offset(sx, groundY - pillarH * 0.55f)
                )
            }

            SceneryType.JUNGLE_WATERFALL -> {
                // Cascading jungle waterfall pouring down rocky moss cliff into a foaming stream
                val wfW = w * 1.25f
                val wfH = h * 1.35f
                val cliffTop = groundY - wfH

                // Cliff rock backdrop
                drawScope.drawRoundRect(
                    color = Color(0xFF263238),
                    topLeft = Offset(sx - wfW * 0.65f, cliffTop),
                    size = Size(wfW * 1.3f, wfH),
                    cornerRadius = CornerRadius(6f * scale, 6f * scale)
                )
                // Rushing waterfall torrent
                drawScope.drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.90f),
                            Color(0xFF80D8FF).copy(alpha = 0.85f),
                            Color(0xFF00B0FF).copy(alpha = 0.65f)
                        ),
                        startY = cliffTop,
                        endY = groundY
                    ),
                    topLeft = Offset(sx - wfW / 2f, cliffTop),
                    size = Size(wfW, wfH)
                )

                // Animated rushing foam lines
                val foamOffset = ((timeSeconds * 65f) % 24f) * scale
                drawScope.drawLine(
                    color = Color.White,
                    start = Offset(sx - wfW * 0.2f, cliffTop + foamOffset),
                    end = Offset(sx - wfW * 0.2f, groundY - 6f * scale),
                    strokeWidth = 2.2f * scale,
                    cap = StrokeCap.Round
                )
                drawScope.drawLine(
                    color = Color.White,
                    start = Offset(sx + wfW * 0.2f, cliffTop + (foamOffset * 1.3f) % 24f * scale),
                    end = Offset(sx + wfW * 0.2f, groundY - 6f * scale),
                    strokeWidth = 2.2f * scale,
                    cap = StrokeCap.Round
                )

                // Splash mist cloud at waterfall basin
                drawScope.drawOval(
                    color = Color.White.copy(alpha = 0.48f),
                    topLeft = Offset(sx - wfW * 0.8f, groundY - 12f * scale),
                    size = Size(wfW * 1.6f, 20f * scale)
                )
            }

            SceneryType.GLOWING_ORCHID_PLANT -> {
                // Bioluminescent Jungle Orchid with glowing petals and magical spore particles
                val stemH = h * 0.55f
                val stemSway = sin(timeSeconds * 3f + obj.id) * 4f * scale
                val stemTopX = sx + stemSway
                val stemTopY = groundY - stemH

                // Curved rainforest stem
                val stemPath = Path().apply {
                    moveTo(sx, groundY)
                    quadraticTo(sx + obj.side * 8f * scale, groundY - stemH * 0.5f, stemTopX, stemTopY)
                }
                drawScope.drawPath(
                    path = stemPath,
                    color = Color(0xFF2E7D32),
                    style = Stroke(width = 4.2f * scale, cap = StrokeCap.Round)
                )

                // Broad jungle leaf at base
                drawScope.drawOval(
                    color = Color(0xFF1B5E20),
                    topLeft = Offset(sx - 12f * scale, groundY - 8f * scale),
                    size = Size(24f * scale, 8f * scale)
                )

                // Radiant orchid petals (glowing fantasy flower)
                val petalRadius = 14f * scale
                for (p in 0..4) {
                    val angle = (p * 60f) * (3.14159f / 180f)
                    val px = stemTopX + cos(angle) * petalRadius * 0.7f
                    val py = stemTopY + sin(angle) * petalRadius * 0.7f
                    drawScope.drawCircle(
                        color = glowCol.copy(alpha = 0.88f),
                        radius = petalRadius * 0.45f,
                        center = Offset(px, py)
                    )
                }
                // Bright glowing stamen core
                drawScope.drawCircle(
                    color = Color.White,
                    radius = petalRadius * 0.28f,
                    center = Offset(stemTopX, stemTopY)
                )
            }

            SceneryType.MOSSY_BOULDER_CLIFF -> {
                // Large moss-covered jungle rock boulder with gnarled tree roots & ferns
                val rockW = w * 1.3f
                val rockH = h * 0.68f
                val rockTopY = groundY - rockH

                // Rocky body
                drawScope.drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(sx - rockW / 2f, rockTopY),
                    size = Size(rockW, rockH),
                    cornerRadius = CornerRadius(16f * scale, 16f * scale)
                )
                // Overgrown thick moss cap
                drawScope.drawRoundRect(
                    color = Color(0xFF2E7D32),
                    topLeft = Offset(sx - rockW * 0.46f, rockTopY),
                    size = Size(rockW * 0.92f, rockH * 0.42f),
                    cornerRadius = CornerRadius(10f * scale, 10f * scale)
                )
                // Gnarled tree roots wrapping over the boulder
                for (r in 0..1) {
                    val rootX = sx - rockW * 0.25f + r * rockW * 0.5f
                    drawScope.drawLine(
                        color = Color(0xFF4E342E),
                        start = Offset(rootX, rockTopY + 4f * scale),
                        end = Offset(rootX + (r * 2 - 1) * 8f * scale, groundY),
                        strokeWidth = 3.5f * scale,
                        cap = StrokeCap.Round
                    )
                }
            }

            SceneryType.WOODEN_ROPE_BRIDGE_POST -> {
                // Sturdy timber bridge post wrapped with ropes and a glowing hanging lantern
                val postW = w * 0.42f
                val postH = h * 0.85f
                val pTopY = groundY - postH

                // Main timber upright post
                drawScope.drawRoundRect(
                    color = Color(0xFF4E342E),
                    topLeft = Offset(sx - postW / 2f, pTopY),
                    size = Size(postW, postH),
                    cornerRadius = CornerRadius(3f * scale, 3f * scale)
                )
                // Wrapped hemp rope coils
                for (rc in 0..2) {
                    val ropeY = pTopY + postH * (0.25f + rc * 0.12f)
                    drawScope.drawRect(
                        color = Color(0xFFBCAAA4),
                        topLeft = Offset(sx - postW * 0.6f, ropeY),
                        size = Size(postW * 1.2f, 4f * scale)
                    )
                }
                // Wooden arm holding lantern
                val armLen = 14f * scale
                val armX = sx + obj.side * armLen
                drawScope.drawLine(
                    color = Color(0xFF3E2723),
                    start = Offset(sx, pTopY + 6f * scale),
                    end = Offset(armX, pTopY + 6f * scale),
                    strokeWidth = 3f * scale,
                    cap = StrokeCap.Round
                )
                // Hanging glowing lantern
                val lanternY = pTopY + 18f * scale
                drawScope.drawLine(
                    color = Color(0xFF212121),
                    start = Offset(armX, pTopY + 6f * scale),
                    end = Offset(armX, lanternY),
                    strokeWidth = 1.5f * scale
                )
                drawScope.drawRoundRect(
                    color = AstralGold,
                    topLeft = Offset(armX - 4f * scale, lanternY),
                    size = Size(8f * scale, 12f * scale),
                    cornerRadius = CornerRadius(2f * scale, 2f * scale)
                )
                drawScope.drawCircle(
                    color = Color(0xFFFFF9C4),
                    radius = 2.5f * scale,
                    center = Offset(armX, lanternY + 6f * scale)
                )
            }

            SceneryType.JUNGLE_CREATURE -> {
                // Cute harmless jungle wildlife: animated golden tamarin monkey or colorful toucan!
                val branchY = groundY - h * 0.65f
                val isMonkey = (obj.id % 2L == 0L)

                // Perch branch extending outward
                drawScope.drawLine(
                    color = Color(0xFF4E342E),
                    start = Offset(sx - obj.side * 18f * scale, branchY + 6f * scale),
                    end = Offset(sx + obj.side * 14f * scale, branchY),
                    strokeWidth = 4f * scale,
                    cap = StrokeCap.Round
                )

                if (isMonkey) {
                    // Playful golden tamarin monkey with curling tail
                    val creatureY = branchY - 14f * scale
                    val bounce = sin(timeSeconds * 4.5f + obj.id) * 2f * scale
                    // Body
                    drawScope.drawOval(
                        color = Color(0xFFFFB300),
                        topLeft = Offset(sx - 6f * scale, creatureY + bounce - 6f * scale),
                        size = Size(12f * scale, 14f * scale)
                    )
                    // Head
                    drawScope.drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 5.5f * scale,
                        center = Offset(sx, creatureY + bounce - 9f * scale)
                    )
                    // Cute eye
                    drawScope.drawCircle(
                        color = Color.Black,
                        radius = 1.2f * scale,
                        center = Offset(sx - obj.side * 2f * scale, creatureY + bounce - 9f * scale)
                    )
                    // Curly tail
                    val tailPath = Path().apply {
                        moveTo(sx, creatureY + bounce + 4f * scale)
                        quadraticTo(sx - obj.side * 12f * scale, creatureY + bounce + 10f * scale, sx - obj.side * 8f * scale, creatureY + bounce + 18f * scale)
                    }
                    drawScope.drawPath(
                        path = tailPath,
                        color = Color(0xFFFFB300),
                        style = Stroke(width = 2.5f * scale, cap = StrokeCap.Round)
                    )
                } else {
                    // Brightly colored tropical toucan with oversized yellow beak
                    val birdY = branchY - 12f * scale
                    val headBob = sin(timeSeconds * 3.5f + obj.id) * 1.5f * scale
                    // Sleek black body
                    drawScope.drawOval(
                        color = Color(0xFF212121),
                        topLeft = Offset(sx - 6f * scale, birdY + headBob - 4f * scale),
                        size = Size(12f * scale, 16f * scale)
                    )
                    // White chest patch
                    drawScope.drawCircle(
                        color = Color.White,
                        radius = 4f * scale,
                        center = Offset(sx - obj.side * 2f * scale, birdY + headBob)
                    )
                    // Oversized bright yellow/orange bill
                    val beakPath = Path().apply {
                        moveTo(sx - obj.side * 2f * scale, birdY + headBob - 3f * scale)
                        lineTo(sx - obj.side * 14f * scale, birdY + headBob - 1f * scale)
                        lineTo(sx - obj.side * 2f * scale, birdY + headBob + 3f * scale)
                        close()
                    }
                    drawScope.drawPath(beakPath, color = Color(0xFFFFD600))
                    // Beak tip accent
                    drawScope.drawCircle(
                        color = Color(0xFFFF3D00),
                        radius = 1.5f * scale,
                        center = Offset(sx - obj.side * 13f * scale, birdY + headBob - 1f * scale)
                    )
                }
            }

            SceneryType.ANCIENT_TEMPLE_ARCH -> {
                // Monumental carved ancient stone arch spanning overhead above the track
                val archH = h * 1.55f
                val pillarW = 28f * obj.scale * scale
                val archTopY = groundY - archH
                val stoneCol = engine.currentBiome.stoneTileColor
                val wallCol = engine.currentBiome.wallColor

                // Pillar column on this side
                drawScope.drawRoundRect(
                    color = wallCol,
                    topLeft = Offset(sx - pillarW / 2f, archTopY),
                    size = Size(pillarW, archH),
                    cornerRadius = CornerRadius(4f * scale, 4f * scale)
                )
                // Stepped base plinth
                drawScope.drawRect(
                    color = stoneCol,
                    topLeft = Offset(sx - pillarW * 0.7f, groundY - 14f * scale),
                    size = Size(pillarW * 1.4f, 14f * scale)
                )
                // Carved capital block
                drawScope.drawRect(
                    color = stoneCol,
                    topLeft = Offset(sx - pillarW * 0.65f, archTopY),
                    size = Size(pillarW * 1.3f, 16f * scale)
                )

                // If side is -1 (left side), draw the grand overhead arch beam spanning across to the right side
                if (obj.side == -1) {
                    val rightPillarX = vx + (1.68f * laneSpacing * scale)
                    val beamTopY = archTopY - 8f * scale
                    val beamH = 24f * scale
                    // Overhead lintel beam
                    drawScope.drawRoundRect(
                        color = stoneCol,
                        topLeft = Offset(sx - pillarW * 0.2f, beamTopY),
                        size = Size(rightPillarX - sx + pillarW * 0.4f, beamH),
                        cornerRadius = CornerRadius(4f * scale, 4f * scale)
                    )
                    // Central carved sun medallion / relief crest
                    val midX = (sx + rightPillarX) / 2f
                    drawScope.drawCircle(
                        color = AstralGold,
                        radius = 8f * scale,
                        center = Offset(midX, beamTopY + beamH * 0.5f)
                    )
                    drawScope.drawCircle(
                        color = glowCol,
                        radius = 4f * scale,
                        center = Offset(midX, beamTopY + beamH * 0.5f)
                    )
                    // Dangling creepers and vines hanging from overhead arch
                    for (v in 1..5) {
                        val vineX = sx + (rightPillarX - sx) * (v / 6f)
                        val vineLen = 14f * scale + (v % 3) * 8f * scale
                        drawScope.drawLine(
                            color = Color(0xFF2E7D32),
                            start = Offset(vineX, beamTopY + beamH),
                            end = Offset(vineX + (v % 2) * 3f * scale, beamTopY + beamH + vineLen),
                            strokeWidth = 2.4f * scale,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Moss patches on pillar
                drawScope.drawOval(
                    color = Color(0xFF2E7D32).copy(alpha = 0.85f),
                    topLeft = Offset(sx - pillarW * 0.4f, groundY - archH * 0.4f),
                    size = Size(pillarW * 0.8f, 22f * scale)
                )
                // Glowing carved rune glyph on pillar
                drawScope.drawCircle(
                    color = glowCol.copy(alpha = 0.9f),
                    radius = 4.5f * scale,
                    center = Offset(sx, groundY - archH * 0.5f)
                )
            }

            SceneryType.TEMPLE_WALL_PARAPET -> {
                // Ancient carved temple balustrade / parapet wall alongside the stone track
                val wallW = w * 1.5f
                val wallH = h * 0.55f
                val wTopY = groundY - wallH
                val stoneCol = engine.currentBiome.wallColor

                // Solid ancient ashlar stone wall block
                drawScope.drawRoundRect(
                    color = stoneCol,
                    topLeft = Offset(sx - wallW / 2f, wTopY),
                    size = Size(wallW, wallH),
                    cornerRadius = CornerRadius(3f * scale, 3f * scale)
                )
                // Heavy coping stone cap on top
                drawScope.drawRect(
                    color = engine.currentBiome.stoneTileColor,
                    topLeft = Offset(sx - wallW * 0.55f, wTopY - 4f * scale),
                    size = Size(wallW * 1.1f, 6f * scale)
                )
                // Carved horizontal relief frieze line
                drawScope.drawLine(
                    color = Color.Black.copy(alpha = 0.35f),
                    start = Offset(sx - wallW * 0.45f, wTopY + wallH * 0.45f),
                    end = Offset(sx + wallW * 0.45f, wTopY + wallH * 0.45f),
                    strokeWidth = 2f * scale
                )
                // Hanging lush green ivy strands draping over the wall
                for (ivy in 0..2) {
                    val ivyX = sx - wallW * 0.3f + ivy * wallW * 0.3f
                    drawScope.drawLine(
                        color = Color(0xFF2E7D32),
                        start = Offset(ivyX, wTopY),
                        end = Offset(ivyX + (ivy % 2) * 2f * scale, wTopY + wallH * 0.7f),
                        strokeWidth = 3f * scale,
                        cap = StrokeCap.Round
                    )
                }
            }

            SceneryType.ANCIENT_GOLDEN_IDOL -> {
                // Majestic ancient golden jungle relic / idol on a stepped stone plinth
                val idolH = h * 0.95f
                val idolW = w * 0.90f
                val idolTopY = groundY - idolH

                // Stepped stone sacrificial altar / pedestal
                drawScope.drawRect(
                    color = engine.currentBiome.wallColor,
                    topLeft = Offset(sx - idolW * 0.7f, groundY - 14f * scale),
                    size = Size(idolW * 1.4f, 14f * scale)
                )
                drawScope.drawRect(
                    color = engine.currentBiome.stoneTileColor,
                    topLeft = Offset(sx - idolW * 0.55f, groundY - 24f * scale),
                    size = Size(idolW * 1.1f, 10f * scale)
                )

                // Golden Idol body & carved head
                drawScope.drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFECB3), Color(0xFFFFD700), Color(0xFFFFA000)),
                        startY = idolTopY,
                        endY = groundY - 24f * scale
                    ),
                    topLeft = Offset(sx - idolW * 0.45f, idolTopY),
                    size = Size(idolW * 0.9f, idolH * 0.68f),
                    cornerRadius = CornerRadius(6f * scale, 6f * scale)
                )
                // Radiant golden crown / headdress fan
                val crownPath = Path().apply {
                    moveTo(sx - idolW * 0.55f, idolTopY + 4f * scale)
                    lineTo(sx, idolTopY - 12f * scale)
                    lineTo(sx + idolW * 0.55f, idolTopY + 4f * scale)
                    close()
                }
                drawScope.drawPath(crownPath, color = AstralGold)

                // Glowing gemstone eyes (ruby or emerald)
                val eyeY = idolTopY + 12f * scale
                drawScope.drawCircle(
                    color = DangerCrimson,
                    radius = 2.8f * scale,
                    center = Offset(sx - idolW * 0.18f, eyeY)
                )
                drawScope.drawCircle(
                    color = DangerCrimson,
                    radius = 2.8f * scale,
                    center = Offset(sx + idolW * 0.18f, eyeY)
                )
                // Ambient golden aura glow
                drawScope.drawCircle(
                    color = AstralGold.copy(alpha = 0.35f),
                    radius = idolW * 0.85f,
                    center = Offset(sx, idolTopY + idolH * 0.3f)
                )
            }

            SceneryType.JUNGLE_TORCH_BRAZIER -> {
                // Ancient carved stone brazier with animated dancing fire and glowing ember sparks
                val brazierH = h * 0.65f
                val brazierW = w * 0.55f
                val bTopY = groundY - brazierH

                // Flared stone pedestal
                drawScope.drawRoundRect(
                    color = engine.currentBiome.wallColor,
                    topLeft = Offset(sx - brazierW * 0.25f, bTopY + 12f * scale),
                    size = Size(brazierW * 0.5f, brazierH - 12f * scale),
                    cornerRadius = CornerRadius(2f * scale, 2f * scale)
                )
                // Carved stone bowl urn
                val bowlPath = Path().apply {
                    moveTo(sx - brazierW * 0.55f, bTopY)
                    lineTo(sx + brazierW * 0.55f, bTopY)
                    lineTo(sx + brazierW * 0.35f, bTopY + 14f * scale)
                    lineTo(sx - brazierW * 0.35f, bTopY + 14f * scale)
                    close()
                }
                drawScope.drawPath(bowlPath, color = engine.currentBiome.stoneTileColor)

                // Glowing hot charcoal bed in the bowl
                drawScope.drawOval(
                    color = Color(0xFFFF3D00),
                    topLeft = Offset(sx - brazierW * 0.45f, bTopY - 2f * scale),
                    size = Size(brazierW * 0.9f, 6f * scale)
                )

                // Animated leaping flame tongues
                val flicker1 = sin(timeSeconds * 16f + obj.id) * 3f * scale
                val flicker2 = cos(timeSeconds * 13f + obj.id) * 2.5f * scale
                val flameH = 22f * scale
                val flamePath = Path().apply {
                    moveTo(sx - brazierW * 0.4f, bTopY)
                    quadraticTo(sx - brazierW * 0.2f + flicker1, bTopY - flameH * 0.6f, sx + flicker2, bTopY - flameH)
                    quadraticTo(sx + brazierW * 0.2f + flicker1, bTopY - flameH * 0.6f, sx + brazierW * 0.4f, bTopY)
                    close()
                }
                drawScope.drawPath(
                    path = flamePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFFF3D00)),
                        startY = bTopY - flameH,
                        endY = bTopY
                    )
                )

                // Warm radial torchlight glow casting light onto the stone road
                drawScope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFB300).copy(alpha = 0.38f), Color.Transparent),
                        center = Offset(sx, bTopY - 4f * scale),
                        radius = 32f * scale
                    ),
                    radius = 32f * scale,
                    center = Offset(sx, bTopY - 4f * scale)
                )
            }
        }
    }

    private fun drawObstacle(
        drawScope: DrawScope,
        obs: Obstacle,
        relZ: Float,
        vx: Float,
        vy: Float,
        groundDelta: Float,
        focalLength: Float,
        laneSpacing: Float,
        timeSeconds: Float
    ) {
        val scale = focalLength / (relZ + focalLength)
        val groundY = vy + (groundDelta * scale)
        val ox = vx + (obs.lane * laneSpacing * scale)
        val width = (laneSpacing * obs.type.baseWidth) * scale

        when (obs.type) {
            ObstacleType.LOW_LOG -> {
                // Fallen Jungle Log: mossy bark, mushroom bracket sprigs & upward jump chevron
                val height = 44f * scale
                // Trunk log
                drawScope.drawRoundRect(
                    color = Color(0xFF4E342E),
                    topLeft = Offset(ox - width / 2f, groundY - height),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(8f * scale, 8f * scale)
                )
                // Overgrown green moss layer
                drawScope.drawRoundRect(
                    color = Color(0xFF2E7D32),
                    topLeft = Offset(ox - width * 0.44f, groundY - height),
                    size = Size(width * 0.88f, height * 0.32f),
                    cornerRadius = CornerRadius(5f * scale, 5f * scale)
                )
                // Jump chevron indicator
                drawScope.drawLine(
                    color = AstralGold,
                    start = Offset(ox - width * 0.28f, groundY - height * 0.5f),
                    end = Offset(ox, groundY - height * 0.72f),
                    strokeWidth = 3.5f * scale,
                    cap = StrokeCap.Round
                )
                drawScope.drawLine(
                    color = AstralGold,
                    start = Offset(ox, groundY - height * 0.72f),
                    end = Offset(ox + width * 0.28f, groundY - height * 0.5f),
                    strokeWidth = 3.5f * scale,
                    cap = StrokeCap.Round
                )
            }

            ObstacleType.BRIDGE_GAP -> {
                // Wooden Bridge Chasm: fractured bridge timbers & rushing torrent rapids underneath
                val gapH = 26f * scale
                // Rushing dark river water with froth
                drawScope.drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D47A1), Color(0xFF00B0FF), Color.White.copy(alpha = 0.8f)),
                        startY = groundY - gapH * 0.5f,
                        endY = groundY + gapH * 0.5f
                    ),
                    topLeft = Offset(ox - width * 0.54f, groundY - gapH * 0.5f),
                    size = Size(width * 1.08f, gapH),
                    cornerRadius = CornerRadius(4f * scale, 4f * scale)
                )
                // Broken timber planks at edges
                drawScope.drawRect(
                    color = Color(0xFF4E342E),
                    topLeft = Offset(ox - width * 0.54f, groundY - gapH * 0.5f),
                    size = Size(width * 0.18f, gapH)
                )
                drawScope.drawRect(
                    color = Color(0xFF4E342E),
                    topLeft = Offset(ox + width * 0.36f, groundY - gapH * 0.5f),
                    size = Size(width * 0.18f, gapH)
                )
                // Warning border
                drawScope.drawRoundRect(
                    color = AstralGold,
                    topLeft = Offset(ox - width * 0.54f, groundY - gapH * 0.5f),
                    size = Size(width * 1.08f, gapH),
                    cornerRadius = CornerRadius(4f * scale, 4f * scale),
                    style = Stroke(width = 2.5f * scale)
                )
            }

            ObstacleType.HIGH_ARCH -> {
                // Hanging Giant Vine Arch: twisted woody lianas & jungle leaves (slide underneath!)
                val totalH = 126f * scale
                val clearance = 52f * scale
                val pWidth = 14f * scale
                val archTopY = groundY - totalH

                // Left & Right vine trunks
                drawScope.drawRoundRect(
                    color = Color(0xFF3E2723),
                    topLeft = Offset(ox - width / 2f, archTopY),
                    size = Size(pWidth, totalH),
                    cornerRadius = CornerRadius(4f * scale, 4f * scale)
                )
                drawScope.drawRoundRect(
                    color = Color(0xFF3E2723),
                    topLeft = Offset(ox + width / 2f - pWidth, archTopY),
                    size = Size(pWidth, totalH),
                    cornerRadius = CornerRadius(4f * scale, 4f * scale)
                )

                // Overhead curved thick vine barrier
                drawScope.drawRoundRect(
                    color = Color(0xFF1B5E20),
                    topLeft = Offset(ox - width / 2f, archTopY),
                    size = Size(width, totalH - clearance),
                    cornerRadius = CornerRadius(8f * scale, 8f * scale)
                )

                // Hanging creepers dangling down
                for (v in 1..4) {
                    val vx = ox - width / 2f + v * (width / 5f)
                    drawScope.drawLine(
                        color = Color(0xFF43A047),
                        start = Offset(vx, archTopY + (totalH - clearance)),
                        end = Offset(vx + (v % 2) * 4f * scale, archTopY + (totalH - clearance) + 12f * scale),
                        strokeWidth = 2.5f * scale,
                        cap = StrokeCap.Round
                    )
                }

                // Glowing downward slide chevron
                drawScope.drawLine(
                    color = StarlightCyan,
                    start = Offset(ox - width * 0.25f, archTopY + 22f * scale),
                    end = Offset(ox, archTopY + 34f * scale),
                    strokeWidth = 3.5f * scale,
                    cap = StrokeCap.Round
                )
                drawScope.drawLine(
                    color = StarlightCyan,
                    start = Offset(ox, archTopY + 34f * scale),
                    end = Offset(ox + width * 0.25f, archTopY + 22f * scale),
                    strokeWidth = 3.5f * scale,
                    cap = StrokeCap.Round
                )
            }

            ObstacleType.TALL_CRYSTAL -> {
                // Ancient Jungle Pillar: weathered carved stone monolith with moss & glowing runic reliefs
                val height = 142f * scale
                val pTopY = groundY - height

                // Ancient carved pillar shaft
                val pillarPath = Path().apply {
                    moveTo(ox - width * 0.42f, groundY)
                    lineTo(ox - width * 0.36f, pTopY + 14f * scale)
                    lineTo(ox - width * 0.46f, pTopY)
                    lineTo(ox + width * 0.46f, pTopY)
                    lineTo(ox + width * 0.36f, pTopY + 14f * scale)
                    lineTo(ox + width * 0.42f, groundY)
                    close()
                }
                drawScope.drawPath(
                    path = pillarPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF546E7A), Color(0xFF37474F), Color(0xFF263238)),
                        startY = pTopY,
                        endY = groundY
                    )
                )

                // Overgrown creeping jungle moss
                drawScope.drawOval(
                    color = Color(0xFF2E7D32).copy(alpha = 0.90f),
                    topLeft = Offset(ox - width * 0.32f, groundY - height * 0.42f),
                    size = Size(width * 0.64f, 26f * scale)
                )

                // Glowing carved ancient warning glyph
                val coreY = groundY - height * 0.58f
                drawScope.drawCircle(color = DangerCrimson, radius = 9f * scale, center = Offset(ox, coreY))
                drawScope.drawCircle(color = AstralGold, radius = 4.5f * scale, center = Offset(ox, coreY))
            }

            ObstacleType.MYSTIC_WISP -> {
                // Mossy River Rock & Roots: hefty granite boulder with roots and harmless firefly wisp
                val height = 94f * scale
                val rTopY = groundY - height

                // River rock boulder
                drawScope.drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(ox - width / 2f, rTopY),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(18f * scale, 18f * scale)
                )
                // Moss coating
                drawScope.drawRoundRect(
                    color = Color(0xFF2E7D32),
                    topLeft = Offset(ox - width * 0.45f, rTopY),
                    size = Size(width * 0.90f, height * 0.40f),
                    cornerRadius = CornerRadius(12f * scale, 12f * scale)
                )
                // Tangled roots
                drawScope.drawLine(
                    color = Color(0xFF4E342E),
                    start = Offset(ox - width * 0.3f, rTopY + 16f * scale),
                    end = Offset(ox - width * 0.45f, groundY),
                    strokeWidth = 3.5f * scale,
                    cap = StrokeCap.Round
                )
                drawScope.drawLine(
                    color = Color(0xFF4E342E),
                    start = Offset(ox + width * 0.2f, rTopY + 18f * scale),
                    end = Offset(ox + width * 0.4f, groundY),
                    strokeWidth = 3.5f * scale,
                    cap = StrokeCap.Round
                )

                // Orbiting jungle firefly wisp
                val wispX = ox + cos(timeSeconds * 5f + obs.id) * 16f * scale
                val wispY = rTopY + height * 0.35f + sin(timeSeconds * 5f + obs.id) * 8f * scale
                drawScope.drawCircle(color = DangerCrimson, radius = 10f * scale, center = Offset(wispX, wispY))
                drawScope.drawCircle(color = AstralGold, radius = 5f * scale, center = Offset(wispX, wispY))
            }
        }
    }

    private fun drawPickup(
        drawScope: DrawScope,
        pickup: Pickup,
        relZ: Float,
        vx: Float,
        vy: Float,
        groundDelta: Float,
        focalLength: Float,
        laneSpacing: Float,
        timeSeconds: Float
    ) {
        val scale = focalLength / (relZ + focalLength)
        val groundY = vy + (groundDelta * scale)
        val elevation = (pickup.y * 50f) * scale
        val py = groundY - elevation - (22f * scale)
        val px = vx + (pickup.visualX * laneSpacing * scale)
        val radius = 16f * scale

        if (pickup.type == PickupType.COIN) {
            val spin = abs(cos(timeSeconds * 6f + pickup.id))
            val coinW = (radius * (0.25f + spin * 0.75f)).coerceAtLeast(3f)

            drawScope.drawOval(
                color = Color.Black.copy(alpha = 0.25f * scale),
                topLeft = Offset(px - radius * 0.8f, groundY - 4f),
                size = Size(radius * 1.6f, 8f * scale)
            )

            drawScope.drawOval(
                color = AstralGold,
                topLeft = Offset(px - coinW, py - radius),
                size = Size(coinW * 2f, radius * 2f)
            )
            if (coinW > 4f) {
                drawScope.drawOval(
                    color = GoldCoin,
                    topLeft = Offset(px - coinW * 0.7f, py - radius * 0.7f),
                    size = Size(coinW * 1.4f, radius * 1.4f)
                )
            }
        } else if (pickup.type == PickupType.MYSTERY_BOX) {
            // FEATURE 1: 3D Magical Jungle Mystery Box
            val rarity = pickup.mysteryBoxRarity ?: MysteryBoxRarity.COMMON
            val floatBob = sin(timeSeconds * 4.5f + pickup.id) * 7f * scale
            val boxY = py + floatBob
            val boxSize = 34f * scale

            // Ground shadow
            drawScope.drawOval(
                color = Color.Black.copy(alpha = 0.35f * scale),
                topLeft = Offset(px - boxSize * 0.8f, groundY - 4f * scale),
                size = Size(boxSize * 1.6f, 8f * scale)
            )

            // Radiant rarity aura
            drawScope.drawCircle(
                color = rarity.glowColor.copy(alpha = 0.32f + sin(timeSeconds * 5f + pickup.id) * 0.12f),
                radius = boxSize * 1.1f,
                center = Offset(px, boxY)
            )

            // Wooden chest base (with dark teak wood texture)
            drawScope.drawRoundRect(
                color = Color(0xFF4E342E),
                topLeft = Offset(px - boxSize / 2f, boxY - boxSize * 0.4f),
                size = Size(boxSize, boxSize * 0.8f),
                cornerRadius = CornerRadius(4f * scale, 4f * scale)
            )
            // Golden edge filigree brackets
            drawScope.drawRoundRect(
                color = AstralGold,
                topLeft = Offset(px - boxSize / 2f, boxY - boxSize * 0.4f),
                size = Size(boxSize, boxSize * 0.8f),
                cornerRadius = CornerRadius(4f * scale, 4f * scale),
                style = Stroke(width = 2.8f * scale)
            )
            // Chest Lid
            drawScope.drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(rarity.primaryColor, Color(0xFF3E2723)),
                    startY = boxY - boxSize * 0.55f,
                    endY = boxY - boxSize * 0.25f
                ),
                topLeft = Offset(px - boxSize * 0.54f, boxY - boxSize * 0.55f),
                size = Size(boxSize * 1.08f, boxSize * 0.35f),
                cornerRadius = CornerRadius(5f * scale, 5f * scale)
            )
            // Glowing Top Crystal
            drawScope.drawCircle(
                color = rarity.glowColor,
                radius = 4.5f * scale,
                center = Offset(px, boxY - boxSize * 0.55f)
            )
            drawScope.drawCircle(
                color = Color.White,
                radius = 2.2f * scale,
                center = Offset(px, boxY - boxSize * 0.55f)
            )

            // Wrapping Jungle Vines
            drawScope.drawLine(
                color = Color(0xFF2E7D32),
                start = Offset(px - boxSize * 0.45f, boxY + boxSize * 0.35f),
                end = Offset(px - boxSize * 0.15f, boxY - boxSize * 0.25f),
                strokeWidth = 2.2f * scale,
                cap = StrokeCap.Round
            )
            drawScope.drawLine(
                color = Color(0xFF2E7D32),
                start = Offset(px + boxSize * 0.45f, boxY + boxSize * 0.35f),
                end = Offset(px + boxSize * 0.15f, boxY - boxSize * 0.25f),
                strokeWidth = 2.2f * scale,
                cap = StrokeCap.Round
            )

            // Glowing Golden Question Mark '?' Emblem
            val qy = boxY - 2f * scale
            drawScope.drawCircle(
                color = AstralGold,
                radius = 2f * scale,
                center = Offset(px, qy + 6f * scale)
            )
            val qPath = Path().apply {
                moveTo(px - 4f * scale, qy - 6f * scale)
                cubicTo(
                    px - 4f * scale, qy - 12f * scale,
                    px + 5f * scale, qy - 12f * scale,
                    px + 4f * scale, qy - 5f * scale
                )
                lineTo(px, qy + 1f * scale)
            }
            drawScope.drawPath(
                path = qPath,
                color = AstralGold,
                style = Stroke(width = 2.8f * scale, cap = StrokeCap.Round)
            )
        } else if (pickup.type == PickupType.SKY_BOOSTER) {
            // FEATURE 2: 3D Jungle Sky Booster (Suspended in the air)
            val booster = pickup.skyBoosterType ?: SkyBoosterType.SKY_LAUNCHER
            val floatBob = sin(timeSeconds * 5f + pickup.id) * 8f * scale
            val bY = py + floatBob
            val bRadius = 26f * scale

            // Glowing aura
            drawScope.drawCircle(
                color = booster.glowColor.copy(alpha = 0.35f),
                radius = bRadius * 1.25f,
                center = Offset(px, bY)
            )
            // Sparkling particles around booster
            for (sp in 0..3) {
                val sAngle = timeSeconds * 4f + sp * 1.57f
                val spX = px + cos(sAngle) * (bRadius * 1.15f)
                val spY = bY + sin(sAngle) * (bRadius * 1.15f)
                drawScope.drawCircle(
                    color = Color.White,
                    radius = 2.2f * scale,
                    center = Offset(spX, spY)
                )
            }

            // Draw booster by type:
            when (booster) {
                SkyBoosterType.SKY_LAUNCHER -> {
                    drawScope.drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = bRadius,
                        center = Offset(px, bY),
                        style = Stroke(width = 3.5f * scale)
                    )
                    val wingSpread = 16f * scale
                    drawScope.drawLine(
                        color = Color.White,
                        start = Offset(px - wingSpread, bY + 4f * scale),
                        end = Offset(px, bY - wingSpread),
                        strokeWidth = 3f * scale,
                        cap = StrokeCap.Round
                    )
                    drawScope.drawLine(
                        color = Color.White,
                        start = Offset(px + wingSpread, bY + 4f * scale),
                        end = Offset(px, bY - wingSpread),
                        strokeWidth = 3f * scale,
                        cap = StrokeCap.Round
                    )
                    drawScope.drawCircle(color = Color(0xFF00E5FF), radius = 5f * scale, center = Offset(px, bY))
                }
                SkyBoosterType.COIN_MAGNET -> {
                    drawScope.drawRoundRect(
                        color = Color(0xFF37474F),
                        topLeft = Offset(px - 10f * scale, bY - 10f * scale),
                        size = Size(20f * scale, 20f * scale),
                        cornerRadius = CornerRadius(4f * scale, 4f * scale)
                    )
                    drawScope.drawCircle(color = AstralGold, radius = 5.5f * scale, center = Offset(px - 14f * scale, bY))
                    drawScope.drawCircle(color = AstralGold, radius = 5.5f * scale, center = Offset(px + 14f * scale, bY))
                    drawScope.drawLine(color = Color(0xFF84FFFF), start = Offset(px - 10f * scale, bY - 4f * scale), end = Offset(px + 10f * scale, bY - 4f * scale), strokeWidth = 2.5f * scale)
                }
                SkyBoosterType.SPEED_BOOST -> {
                    drawScope.drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF5252), Color(0xFFD50000))),
                        radius = bRadius * 0.75f,
                        center = Offset(px, bY)
                    )
                    val bolt = Path().apply {
                        moveTo(px + 2f * scale, bY - 12f * scale)
                        lineTo(px - 6f * scale, bY)
                        lineTo(px, bY)
                        lineTo(px - 2f * scale, bY + 12f * scale)
                        lineTo(px + 6f * scale, bY)
                        lineTo(px, bY)
                        close()
                    }
                    drawScope.drawPath(bolt, color = Color.White)
                }
                SkyBoosterType.JUNGLE_SHIELD -> {
                    val sw = bRadius * 0.7f
                    val sh = bRadius * 0.85f
                    val sPath = Path().apply {
                        moveTo(px, bY - sh)
                        lineTo(px + sw, bY - sh * 0.35f)
                        lineTo(px + sw * 0.7f, bY + sh * 0.6f)
                        lineTo(px, bY + sh)
                        lineTo(px - sw * 0.7f, bY + sh * 0.6f)
                        lineTo(px - sw, bY - sh * 0.35f)
                        close()
                    }
                    drawScope.drawPath(sPath, color = Color(0xFF5D4037))
                    drawScope.drawPath(sPath, color = DreamEmerald, style = Stroke(width = 2.5f * scale))
                    drawScope.drawCircle(color = ShieldAzure, radius = 4.5f * scale, center = Offset(px, bY))
                    drawScope.drawCircle(color = Color.White, radius = 2f * scale, center = Offset(px, bY))
                }
                SkyBoosterType.SKY_GLIDER -> {
                    val fPath = Path().apply {
                        moveTo(px, bY - 14f * scale)
                        cubicTo(px + 12f * scale, bY - 6f * scale, px + 8f * scale, bY + 10f * scale, px, bY + 14f * scale)
                        cubicTo(px - 8f * scale, bY + 10f * scale, px - 12f * scale, bY - 6f * scale, px, bY - 14f * scale)
                        close()
                    }
                    drawScope.drawPath(fPath, color = DreamEmerald)
                    drawScope.drawLine(color = Color.White, start = Offset(px, bY - 12f * scale), end = Offset(px, bY + 12f * scale), strokeWidth = 2f * scale)
                }
                SkyBoosterType.SCORE_MULTIPLIER -> {
                    drawScope.drawCircle(color = AstralGold, radius = bRadius * 0.85f, center = Offset(px, bY))
                    drawScope.drawCircle(color = Color(0xFFFF6D00), radius = bRadius * 0.7f, center = Offset(px, bY))
                    drawScope.drawCircle(color = Color.White, radius = 3f * scale, center = Offset(px - 4f * scale, bY - 3f * scale))
                    drawScope.drawCircle(color = Color.White, radius = 3.5f * scale, center = Offset(px - 4f * scale, bY + 3f * scale))
                    drawScope.drawLine(color = Color.White, start = Offset(px + 3f * scale, bY - 5f * scale), end = Offset(px + 9f * scale, bY + 5f * scale), strokeWidth = 2.5f * scale, cap = StrokeCap.Round)
                    drawScope.drawLine(color = Color.White, start = Offset(px + 9f * scale, bY - 5f * scale), end = Offset(px + 3f * scale, bY + 5f * scale), strokeWidth = 2.5f * scale, cap = StrokeCap.Round)
                }
            }
        } else if (pickup.type == PickupType.ANIMAL_TOKEN) {
            // FEATURE 3: Animal Energy Token
            val floatBob = sin(timeSeconds * 6f + pickup.id) * 6f * scale
            val tY = py + floatBob
            val tRadius = 18f * scale

            drawScope.drawCircle(
                color = DreamEmerald.copy(alpha = 0.35f),
                radius = tRadius * 1.35f,
                center = Offset(px, tY)
            )
            drawScope.drawCircle(color = AstralGold, radius = tRadius, center = Offset(px, tY))
            drawScope.drawCircle(color = Color(0xFF1B5E20), radius = tRadius * 0.82f, center = Offset(px, tY))
            val pawCenterY = tY + 2f * scale
            drawScope.drawOval(
                color = DreamEmerald,
                topLeft = Offset(px - 5f * scale, pawCenterY - 3f * scale),
                size = Size(10f * scale, 7f * scale)
            )
            val toeOffsets = listOf(
                Offset(-5f * scale, -6f * scale),
                Offset(-2f * scale, -8f * scale),
                Offset(2f * scale, -8f * scale),
                Offset(5f * scale, -6f * scale)
            )
            toeOffsets.forEach { toe ->
                drawScope.drawCircle(color = Color(0xFF69F0AE), radius = 1.8f * scale, center = Offset(px + toe.x, pawCenterY + toe.y))
            }
        } else {
            val floatBob = sin(timeSeconds * 5f + pickup.id) * 8f * scale
            val orbY = py + floatBob
            val orbRadius = 24f * scale

            // Glowing ground beacon ring beneath power-up
            drawScope.drawOval(
                color = pickup.type.color.copy(alpha = 0.42f * scale),
                topLeft = Offset(px - orbRadius * 1.2f, groundY - 6f * scale),
                size = Size(orbRadius * 2.4f, 12f * scale)
            )

            // Outer pulsing aura
            val pulse = 1.25f + sin(timeSeconds * 7f + pickup.id) * 0.18f
            drawScope.drawCircle(
                color = pickup.type.color.copy(alpha = 0.28f),
                radius = orbRadius * pulse,
                center = Offset(px, orbY)
            )

            // 3D Sphere Core
            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, pickup.type.color, VoidDark),
                    center = Offset(px - orbRadius * 0.3f, orbY - orbRadius * 0.3f),
                    radius = orbRadius * 1.15f
                ),
                radius = orbRadius,
                center = Offset(px, orbY)
            )

            // Rotating golden rim ring
            drawScope.drawCircle(
                color = AstralGold.copy(alpha = 0.85f),
                radius = orbRadius,
                center = Offset(px, orbY),
                style = Stroke(width = 2.4f * scale)
            )

            // Distinctive Emblem Inside Power-Up Relic
            when (pickup.type) {
                PickupType.MAGNET -> {
                    val mw = orbRadius * 0.48f
                    val mh = orbRadius * 0.45f
                    drawScope.drawLine(
                        color = Color.White,
                        start = Offset(px - mw, orbY - mh),
                        end = Offset(px - mw, orbY + mh * 0.6f),
                        strokeWidth = 3.5f * scale,
                        cap = StrokeCap.Round
                    )
                    drawScope.drawLine(
                        color = Color.White,
                        start = Offset(px + mw, orbY - mh),
                        end = Offset(px + mw, orbY + mh * 0.6f),
                        strokeWidth = 3.5f * scale,
                        cap = StrokeCap.Round
                    )
                    drawScope.drawLine(
                        color = Color.White,
                        start = Offset(px - mw, orbY + mh * 0.6f),
                        end = Offset(px + mw, orbY + mh * 0.6f),
                        strokeWidth = 3.5f * scale,
                        cap = StrokeCap.Round
                    )
                    drawScope.drawCircle(color = DangerCrimson, radius = 3f * scale, center = Offset(px - mw, orbY - mh))
                    drawScope.drawCircle(color = AstralGold, radius = 3f * scale, center = Offset(px + mw, orbY - mh))
                }

                PickupType.DOUBLE_COINS -> {
                    for (offsetSign in listOf(-1f, 1f)) {
                        val dx = px + offsetSign * orbRadius * 0.22f
                        val dSize = orbRadius * 0.46f
                        val diamond = Path().apply {
                            moveTo(dx, orbY - dSize)
                            lineTo(dx + dSize * 0.7f, orbY)
                            lineTo(dx, orbY + dSize)
                            lineTo(dx - dSize * 0.7f, orbY)
                            close()
                        }
                        drawScope.drawPath(
                            path = diamond,
                            color = if (offsetSign < 0f) AstralGold else Color.White
                        )
                    }
                }

                PickupType.SHIELD -> {
                    val sw = orbRadius * 0.55f
                    val sh = orbRadius * 0.62f
                    val shieldPath = Path().apply {
                        moveTo(px, orbY - sh)
                        lineTo(px + sw, orbY - sh * 0.4f)
                        lineTo(px + sw * 0.6f, orbY + sh * 0.5f)
                        lineTo(px, orbY + sh)
                        lineTo(px - sw * 0.6f, orbY + sh * 0.5f)
                        lineTo(px - sw, orbY - sh * 0.4f)
                        close()
                    }
                    drawScope.drawPath(shieldPath, color = Color.White.copy(alpha = 0.9f))
                }

                PickupType.STAR_RUSH -> {
                    val sr = orbRadius * 0.68f
                    drawScope.drawLine(Color.White, Offset(px - sr, orbY), Offset(px + sr, orbY), strokeWidth = 3.5f * scale, cap = StrokeCap.Round)
                    drawScope.drawLine(Color.White, Offset(px, orbY - sr), Offset(px, orbY + sr), strokeWidth = 3.5f * scale, cap = StrokeCap.Round)
                }

                else -> {}
            }
        }
    }

    private fun drawPlayer(
        drawScope: DrawScope,
        engine: GameEngine,
        vx: Float,
        vy: Float,
        groundDelta: Float,
        laneSpacing: Float,
        timeSeconds: Float
    ) {
        val groundY = vy + groundDelta
        val px = vx + (engine.laneX * laneSpacing)
        val hoverOffset = if (engine.hoverboardActive) 22f + sin(timeSeconds * 6f) * 4f else 0f
        val elevation = (engine.playerY * 60f) + hoverOffset
        val baseY = groundY - elevation

        if (engine.invincibleTimer > 0f && (timeSeconds * 20f).toInt() % 2 == 0) return

        val laneTilt = (
            (engine.targetLane - engine.laneX) * 1.4f +
                engine.laneChangeDirection * (engine.laneChangeTimer / 0.28f)
            ).coerceIn(-1f, 1f)

        val animState = engine.characterAnimState
        val frontFactor = if (
            animState == CharacterAnimState.IDLE ||
            animState == CharacterAnimState.GAME_OVER
        ) 0.85f else 0.42f

        val scaledTime = if (animState == CharacterAnimState.RUNNING) {
            timeSeconds * (1f + engine.speedProgressFraction * 0.48f)
        } else {
            timeSeconds
        }

        // Draw 3D Hoverboard beneath the player if active
        if (engine.hoverboardActive) {
            Hoverboard3DRenderer.drawHoverboardUnderPlayer(
                drawScope = drawScope,
                board = engine.hoverboard,
                px = px,
                playerFeetY = groundY - elevation,
                scale = 1.08f,
                timeSeconds = timeSeconds,
                laneTilt = laneTilt
            )
        }

        // Draw Player: either transformed 3D Animal or 3D Character
        if (engine.activeAnimalForm != null) {
            Animal3DRenderer.drawAnimal(
                drawScope = drawScope,
                animal = engine.activeAnimalForm!!,
                actionState = engine.actionState,
                timeSeconds = scaledTime,
                centerX = px,
                groundY = groundY,
                scale = 1.12f,
                elevationPx = elevation,
                laneTiltDir = laneTilt
            )
        } else {
            Character3DRenderer.drawCharacter(
                drawScope = drawScope,
                character = engine.character,
                animState = animState,
                timeSeconds = scaledTime,
                centerX = px,
                groundY = groundY,
                scale = 1.08f,
                elevationPx = elevation,
                laneTiltDir = laneTilt,
                frontFacingFactor = frontFactor
            )
        }

        // Transformation Swirl Effect (leaves & golden beams)
        if (engine.transformationAnimTimer > 0f) {
            val transPulse = engine.transformationAnimTimer / 1.2f
            drawScope.drawCircle(
                color = DreamEmerald.copy(alpha = 0.45f * transPulse),
                radius = 72f * transPulse,
                center = Offset(px, baseY - 50f)
            )
            for (lf in 0..5) {
                val leafAngle = timeSeconds * 12f + lf * 1.047f
                val lx = px + cos(leafAngle) * (52f * transPulse)
                val ly = (baseY - 50f) + sin(leafAngle) * (28f * transPulse)
                drawScope.drawOval(
                    color = Color(0xFF69F0AE),
                    topLeft = Offset(lx - 6f, ly - 3f),
                    size = Size(12f, 6f)
                )
            }
        }

        // Sky Launcher & Glider Flight Windstream Wings
        if (engine.skyLauncherTimer > 0f || engine.skyGliderTimer > 0f) {
            val wingColor = if (engine.skyLauncherTimer > 0f) Color(0xFF00E5FF) else DreamEmerald
            for (wSide in listOf(-1, 1)) {
                val wingPath = Path().apply {
                    moveTo(px + wSide * 12f, baseY - 45f)
                    lineTo(px + wSide * 55f, baseY - 65f + sin(timeSeconds * 10f) * 8f)
                    lineTo(px + wSide * 35f, baseY - 30f)
                    close()
                }
                drawScope.drawPath(wingPath, color = wingColor.copy(alpha = 0.65f))
            }
        }

        // Magnetic Shield inward-rippling electromagnetic field rings
        if (engine.magnetTimer > 0f) {
            val magPulse = 1f - ((timeSeconds * 2.4f) % 1f)
            val magRadius = 40f + magPulse * 48f
            drawScope.drawCircle(
                color = StarlightCyan.copy(alpha = 0.45f * (1f - magPulse)),
                radius = magRadius,
                center = Offset(px, baseY - 60f),
                style = Stroke(width = 2.5f)
            )
            drawScope.drawCircle(
                color = AstralGold.copy(alpha = 0.35f),
                radius = 54f,
                center = Offset(px, baseY - 60f),
                style = Stroke(width = 1.8f)
            )
        }

        // Double Score orbiting golden stars
        if (engine.multiplierTimer > 0f) {
            for (i in 0 until 3) {
                val angle = timeSeconds * 4.5f + i * 2.094f
                val ox = px + cos(angle) * 48f
                val oy = (baseY - 64f) + sin(angle) * 18f
                drawScope.drawCircle(
                    color = AstralGold,
                    radius = 5.5f,
                    center = Offset(ox, oy)
                )
            }
        }

        if (engine.shieldActive) {
            drawScope.drawCircle(
                color = ShieldAzure.copy(alpha = 0.22f),
                radius = 62f,
                center = Offset(px, baseY - 62f)
            )
            drawScope.drawCircle(
                color = StarlightCyan,
                radius = 62f,
                center = Offset(px, baseY - 62f),
                style = Stroke(width = 3f)
            )
        }

        if (engine.boostTimer > 0f) {
            for (i in 1..3) {
                drawScope.drawCircle(
                    color = if (i % 2 == 0) MagicPurpleLight.copy(alpha = 0.3f / i) else StarlightCyan.copy(alpha = 0.3f / i),
                    radius = 54f - i * 6f,
                    center = Offset(px, (baseY - 60f) + i * 14f)
                )
            }
        }
    }

    private fun drawParticles(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        engine: GameEngine,
        vx: Float,
        vy: Float,
        groundDelta: Float,
        laneSpacing: Float,
        timeSeconds: Float
    ) {
        val groundY = vy + groundDelta
        engine.particles.forEach { p ->
            val px = vx + (p.x * laneSpacing)
            val py = groundY - (p.y * 50f)
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawScope.drawCircle(
                color = p.color.copy(alpha = alpha * 0.32f),
                radius = p.size * 1.6f,
                center = Offset(px, py)
            )
            drawScope.drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size,
                center = Offset(px, py)
            )
        }

        // High-speed peripheral wind lines when running fast
        val speedFactor = engine.speedProgressFraction
        if (speedFactor > 0.18f) {
            val streakAlpha = ((speedFactor - 0.18f) * 0.45f).coerceIn(0f, 0.38f)
            for (i in 0..5) {
                val side = if (i % 2 == 0) -1 else 1
                val progress = ((timeSeconds * (2.5f + speedFactor * 2f) + i * 0.27f) % 1f)
                val sx = vx + side * (width * 0.16f + progress * width * 0.34f)
                val sy = vy + progress * (height * 0.55f)
                drawScope.drawLine(
                    color = Color.White.copy(alpha = streakAlpha * progress),
                    start = Offset(sx - side * 18f * progress, sy - 14f * progress),
                    end = Offset(sx, sy),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    private sealed class RenderEntity(val relZ: Float) {
        class ObstacleE(val obs: Obstacle, relZ: Float) : RenderEntity(relZ)
        class PickupE(val pickup: Pickup, relZ: Float) : RenderEntity(relZ)
        class SceneryE(val scenery: SceneryObject, relZ: Float) : RenderEntity(relZ)
        class PlayerE(relZ: Float) : RenderEntity(relZ)
    }
}
