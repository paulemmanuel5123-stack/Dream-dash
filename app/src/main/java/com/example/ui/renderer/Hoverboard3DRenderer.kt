package com.example.ui.renderer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.HoverboardInfo
import com.example.model.HoverboardType
import com.example.ui.theme.AstralGold
import com.example.ui.theme.StarlightCyan
import com.example.ui.theme.VoidDark
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Isometric & In-Game Renderer for all 7 DreamDash Fantasy Hoverboards:
 * 1. DREAM BOARD (Starter magical glide board)
 * 2. CRYSTAL BOARD (Prismatic crystal shards & sparkling trail)
 * 3. FIRE BOARD (Blazing swept phoenix board with flame exhaust)
 * 4. ICE BOARD (Glacial frost crystal plates with frozen rime)
 * 5. SHADOW BOARD (Obsidian crescent stealth board with twilight runes)
 * 6. SKY BOARD (Celestial wing board with sun crest & cloud feathers)
 * 7. YORUBA LEGEND BOARD (Respectful, original fantasy Nigerian bronze & cowrie gold motifs)
 */
object Hoverboard3DRenderer {

    fun drawHoverboardPreview(
        drawScope: DrawScope,
        board: HoverboardInfo,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        timeSeconds: Float,
        tiltAngle: Float = 0.15f
    ) {
        val hoverBob = sin(timeSeconds * 3.2f) * 6f
        val cy = centerY + hoverBob

        // 1. Soft Shadow / Energy Projection underneath
        drawScope.drawOval(
            color = board.glowColor.copy(alpha = 0.35f),
            topLeft = Offset(centerX - width * 0.44f, cy + height * 0.45f),
            size = Size(width * 0.88f, height * 0.22f)
        )

        drawScope.drawOval(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(centerX - width * 0.35f, cy + height * 0.48f),
            size = Size(width * 0.70f, height * 0.14f)
        )

        // 2. Thruster / Levitator Particle Flares
        val pulse = 1f + sin(timeSeconds * 6f) * 0.2f
        drawScope.drawCircle(
            color = board.trailColor.copy(alpha = 0.6f * pulse),
            radius = width * 0.12f * pulse,
            center = Offset(centerX - width * 0.32f, cy + height * 0.12f)
        )
        drawScope.drawCircle(
            color = board.trailColor.copy(alpha = 0.6f * pulse),
            radius = width * 0.12f * pulse,
            center = Offset(centerX + width * 0.32f, cy + height * 0.12f)
        )

        // 3. Render Board Geometry based on HoverboardType
        when (board.type) {
            HoverboardType.DREAM -> drawDreamBoard(drawScope, board, centerX, cy, width, height, timeSeconds)
            HoverboardType.CRYSTAL -> drawCrystalBoard(drawScope, board, centerX, cy, width, height, timeSeconds)
            HoverboardType.FIRE -> drawFireBoard(drawScope, board, centerX, cy, width, height, timeSeconds)
            HoverboardType.ICE -> drawIceBoard(drawScope, board, centerX, cy, width, height, timeSeconds)
            HoverboardType.SHADOW -> drawShadowBoard(drawScope, board, centerX, cy, width, height, timeSeconds)
            HoverboardType.SKY -> drawSkyBoard(drawScope, board, centerX, cy, width, height, timeSeconds)
            HoverboardType.YORUBA_LEGEND -> drawYorubaLegendBoard(drawScope, board, centerX, cy, width, height, timeSeconds)
        }
    }

    /**
     * Renders the hoverboard directly beneath the player's boots during gameplay.
     */
    fun drawHoverboardUnderPlayer(
        drawScope: DrawScope,
        board: HoverboardInfo,
        px: Float,
        playerFeetY: Float,
        scale: Float,
        timeSeconds: Float,
        laneTilt: Float
    ) {
        val boardW = 68f * scale
        val boardH = 26f * scale
        val hoverY = playerFeetY + 6f * scale + sin(timeSeconds * 6f) * 3f * scale

        // Dynamic Thruster Glow under player
        val pulse = 1f + sin(timeSeconds * 8f) * 0.25f
        drawScope.drawOval(
            color = board.glowColor.copy(alpha = 0.45f * pulse),
            topLeft = Offset(px - boardW * 0.5f, hoverY + boardH * 0.3f),
            size = Size(boardW, boardH * 0.55f)
        )

        // Board Main Body with tilt
        val tiltOffset = laneTilt * 8f * scale
        drawHoverboardPreview(
            drawScope = drawScope,
            board = board,
            centerX = px + tiltOffset,
            centerY = hoverY,
            width = boardW,
            height = boardH,
            timeSeconds = timeSeconds
        )

        // Subtle Foot Bindings / Energy Locks
        val footSpread = boardW * 0.22f
        drawScope.drawRoundRect(
            color = AstralGold.copy(alpha = 0.85f),
            topLeft = Offset(px - footSpread - 8f * scale + tiltOffset, hoverY - 4f * scale),
            size = Size(16f * scale, 5f * scale),
            cornerRadius = CornerRadius(2f * scale, 2f * scale)
        )
        drawScope.drawRoundRect(
            color = AstralGold.copy(alpha = 0.85f),
            topLeft = Offset(px + footSpread - 8f * scale + tiltOffset, hoverY - 4f * scale),
            size = Size(16f * scale, 5f * scale),
            cornerRadius = CornerRadius(2f * scale, 2f * scale)
        )
    }

    // --- 1. DREAM BOARD: Ethereal cyan/purple magical curved deck ---
    private fun drawDreamBoard(
        drawScope: DrawScope,
        board: HoverboardInfo,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        t: Float
    ) {
        val hw = w * 0.5f
        val hh = h * 0.5f

        // Sleek aerodynamic fantasy deck path
        val deckPath = Path().apply {
            moveTo(cx, cy - hh * 0.95f) // Nose tip
            cubicTo(cx + hw * 0.75f, cy - hh * 0.7f, cx + hw, cy + hh * 0.2f, cx + hw * 0.72f, cy + hh)
            lineTo(cx - hw * 0.72f, cy + hh)
            cubicTo(cx - hw, cy + hh * 0.2f, cx - hw * 0.75f, cy - hh * 0.7f, cx, cy - hh * 0.95f)
            close()
        }

        drawScope.drawPath(
            path = deckPath,
            brush = Brush.verticalGradient(
                colors = listOf(board.glowColor, board.primaryColor, board.secondaryColor),
                startY = cy - hh,
                endY = cy + hh
            )
        )

        drawScope.drawPath(
            path = deckPath,
            color = board.glowColor,
            style = Stroke(width = 2.2f)
        )

        // Center magical glowing rune line
        drawScope.drawLine(
            color = Color.White.copy(alpha = 0.85f),
            start = Offset(cx, cy - hh * 0.7f),
            end = Offset(cx, cy + hh * 0.7f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )

        // Twin floating wingtips
        drawScope.drawCircle(
            color = board.glowColor,
            radius = 3.5f,
            center = Offset(cx - hw * 0.75f, cy + hh * 0.3f)
        )
        drawScope.drawCircle(
            color = board.glowColor,
            radius = 3.5f,
            center = Offset(cx + hw * 0.75f, cy + hh * 0.3f)
        )
    }

    // --- 2. CRYSTAL BOARD: Prismatic Hexagonal Crystal Shards ---
    private fun drawCrystalBoard(
        drawScope: DrawScope,
        board: HoverboardInfo,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        t: Float
    ) {
        val hw = w * 0.5f
        val hh = h * 0.5f

        val crystalPath = Path().apply {
            moveTo(cx, cy - hh)
            lineTo(cx + hw * 0.95f, cy - hh * 0.15f)
            lineTo(cx + hw * 0.70f, cy + hh * 0.95f)
            lineTo(cx, cy + hh * 0.65f)
            lineTo(cx - hw * 0.70f, cy + hh * 0.95f)
            lineTo(cx - hw * 0.95f, cy - hh * 0.15f)
            close()
        }

        drawScope.drawPath(
            path = crystalPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFE1F5FE), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF0091EA)),
                start = Offset(cx - hw, cy - hh),
                end = Offset(cx + hw, cy + hh)
            )
        )

        drawScope.drawPath(
            path = crystalPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 2.4f)
        )

        // Internal faceted crystal ribs
        drawScope.drawLine(Color.White, Offset(cx, cy - hh), Offset(cx, cy + hh * 0.65f), strokeWidth = 2f)
        drawScope.drawLine(Color.White.copy(alpha = 0.7f), Offset(cx, cy - hh * 0.2f), Offset(cx + hw * 0.95f, cy - hh * 0.15f), strokeWidth = 1.5f)
        drawScope.drawLine(Color.White.copy(alpha = 0.7f), Offset(cx, cy - hh * 0.2f), Offset(cx - hw * 0.95f, cy - hh * 0.15f), strokeWidth = 1.5f)

        // Orbiting Diamond Shards
        for (i in 0 until 3) {
            val ang = t * 4f + i * 2.09f
            val sx = cx + cos(ang) * (hw * 0.85f)
            val sy = cy + sin(ang) * (hh * 0.7f)
            drawScope.drawCircle(color = Color.White, radius = 2.8f, center = Offset(sx, sy))
        }
    }

    // --- 3. FIRE BOARD: Swept Blazing Phoenix Board ---
    private fun drawFireBoard(
        drawScope: DrawScope,
        board: HoverboardInfo,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        t: Float
    ) {
        val hw = w * 0.5f
        val hh = h * 0.5f

        val firePath = Path().apply {
            moveTo(cx, cy - hh * 1.05f) // Sharp flame nose
            lineTo(cx + hw * 0.65f, cy - hh * 0.5f)
            lineTo(cx + hw, cy + hh * 0.4f) // Swept wing
            lineTo(cx + hw * 0.5f, cy + hh * 0.85f)
            lineTo(cx, cy + hh * 0.45f) // Tail cleft
            lineTo(cx - hw * 0.5f, cy + hh * 0.85f)
            lineTo(cx - hw, cy + hh * 0.4f)
            lineTo(cx - hw * 0.65f, cy - hh * 0.5f)
            close()
        }

        drawScope.drawPath(
            path = firePath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF176), Color(0xFFFF6D00), Color(0xFFD50000)),
                startY = cy - hh,
                endY = cy + hh
            )
        )

        drawScope.drawPath(
            path = firePath,
            color = Color(0xFFFFD54F),
            style = Stroke(width = 2.2f)
        )

        // Magma core slit
        drawScope.drawLine(
            color = Color(0xFFFFF59D),
            start = Offset(cx, cy - hh * 0.7f),
            end = Offset(cx, cy + hh * 0.25f),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )

        // Flame exhaust jets at tail
        val exhaustPulse = 1f + sin(t * 14f) * 0.3f
        drawScope.drawCircle(color = Color(0xFFFFAB00), radius = 5f * exhaustPulse, center = Offset(cx - hw * 0.4f, cy + hh * 0.7f))
        drawScope.drawCircle(color = Color(0xFFFFAB00), radius = 5f * exhaustPulse, center = Offset(cx + hw * 0.4f, cy + hh * 0.7f))
    }

    // --- 4. ICE BOARD: Glacial Frost Plate & Ice Spikes ---
    private fun drawIceBoard(
        drawScope: DrawScope,
        board: HoverboardInfo,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        t: Float
    ) {
        val hw = w * 0.5f
        val hh = h * 0.5f

        val icePath = Path().apply {
            moveTo(cx, cy - hh * 1.05f)
            lineTo(cx + hw * 0.45f, cy - hh * 0.7f)
            lineTo(cx + hw * 0.92f, cy - hh * 0.05f)
            lineTo(cx + hw * 0.78f, cy + hh * 0.75f)
            lineTo(cx + hw * 0.25f, cy + hh * 0.95f)
            lineTo(cx, cy + hh * 0.65f)
            lineTo(cx - hw * 0.25f, cy + hh * 0.95f)
            lineTo(cx - hw * 0.78f, cy + hh * 0.75f)
            lineTo(cx - hw * 0.92f, cy - hh * 0.05f)
            lineTo(cx - hw * 0.45f, cy - hh * 0.7f)
            close()
        }

        drawScope.drawPath(
            path = icePath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE0F7FA), Color(0xFF80DEEA), Color(0xFF0097A7), Color(0xFF006064)),
                startY = cy - hh,
                endY = cy + hh
            )
        )

        drawScope.drawPath(
            path = icePath,
            color = Color.White,
            style = Stroke(width = 2.4f)
        )

        // Glacial Frost Rime Lines
        drawScope.drawLine(Color.White.copy(alpha = 0.9f), Offset(cx, cy - hh * 0.8f), Offset(cx, cy + hh * 0.5f), strokeWidth = 2f)
        drawScope.drawLine(Color.White.copy(alpha = 0.75f), Offset(cx - hw * 0.4f, cy), Offset(cx + hw * 0.4f, cy), strokeWidth = 1.5f)

        // Snowflake crystals
        drawScope.drawCircle(Color.White, radius = 3f, center = Offset(cx, cy))
    }

    // --- 5. SHADOW BOARD: Obsidian Twilight Crescent Stealth Board ---
    private fun drawShadowBoard(
        drawScope: DrawScope,
        board: HoverboardInfo,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        t: Float
    ) {
        val hw = w * 0.5f
        val hh = h * 0.5f

        val shadowPath = Path().apply {
            moveTo(cx, cy - hh * 0.9f)
            cubicTo(cx + hw * 0.85f, cy - hh * 0.8f, cx + hw * 1.05f, cy + hh * 0.2f, cx + hw * 0.65f, cy + hh * 1.05f)
            lineTo(cx, cy + hh * 0.4f) // Deep crescent notch
            lineTo(cx - hw * 0.65f, cy + hh * 1.05f)
            cubicTo(cx - hw * 1.05f, cy + hh * 0.2f, cx - hw * 0.85f, cy - hh * 0.8f, cx, cy - hh * 0.9f)
            close()
        }

        drawScope.drawPath(
            path = shadowPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF26123D), Color(0xFF130924), Color(0xFF4A148C)),
                startY = cy - hh,
                endY = cy + hh
            )
        )

        // Glowing Amethyst Runes along edge
        drawScope.drawPath(
            path = shadowPath,
            color = Color(0xFFE040FB),
            style = Stroke(width = 2.2f)
        )

        // Center Twilight Eye / Glyph
        drawScope.drawCircle(
            color = Color(0xFF7C4DFF),
            radius = 6f,
            center = Offset(cx, cy - hh * 0.1f)
        )
        drawScope.drawCircle(
            color = Color(0xFFE040FB),
            radius = 3f,
            center = Offset(cx, cy - hh * 0.1f)
        )
    }

    // --- 6. SKY BOARD: Celestial Wing Board with Sun Crest ---
    private fun drawSkyBoard(
        drawScope: DrawScope,
        board: HoverboardInfo,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        t: Float
    ) {
        val hw = w * 0.5f
        val hh = h * 0.5f

        val skyPath = Path().apply {
            moveTo(cx, cy - hh * 1.05f)
            cubicTo(cx + hw * 0.6f, cy - hh * 0.7f, cx + hw, cy - hh * 0.1f, cx + hw * 0.85f, cy + hh * 0.7f)
            lineTo(cx + hw * 0.45f, cy + hh * 0.95f)
            lineTo(cx, cy + hh * 0.6f)
            lineTo(cx - hw * 0.45f, cy + hh * 0.95f)
            lineTo(cx - hw * 0.85f, cy + hh * 0.7f)
            cubicTo(cx - hw, cy - hh * 0.1f, cx - hw * 0.6f, cy - hh * 0.7f, cx, cy - hh * 1.05f)
            close()
        }

        drawScope.drawPath(
            path = skyPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFDE7), Color(0xFFFFD54F), Color(0xFF40C4FF)),
                startY = cy - hh,
                endY = cy + hh
            )
        )

        drawScope.drawPath(
            path = skyPath,
            color = AstralGold,
            style = Stroke(width = 2.5f)
        )

        // Wing feather engravings
        drawScope.drawLine(Color.White, Offset(cx - hw * 0.6f, cy + hh * 0.1f), Offset(cx - hw * 0.15f, cy + hh * 0.4f), strokeWidth = 2f)
        drawScope.drawLine(Color.White, Offset(cx + hw * 0.6f, cy + hh * 0.1f), Offset(cx + hw * 0.15f, cy + hh * 0.4f), strokeWidth = 2f)

        // Solar Sun Disk in center
        drawScope.drawCircle(color = AstralGold, radius = 7f, center = Offset(cx, cy - hh * 0.2f))
        drawScope.drawCircle(color = Color.White, radius = 3.5f, center = Offset(cx, cy - hh * 0.2f))
    }

    // --- 7. YORUBA LEGEND BOARD: Respectful, authentic fantasy-inspired Nigerian artistic motifs ---
    // Warm terracotta, polished bronze relief rim, golden cowrie shell inlays, interlaced geometric diamond patterns
    private fun drawYorubaLegendBoard(
        drawScope: DrawScope,
        board: HoverboardInfo,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        t: Float
    ) {
        val hw = w * 0.5f
        val hh = h * 0.5f

        // Elegant curved royal bronze deck inspired by traditional ceremonial boards & shields
        val yorubaPath = Path().apply {
            moveTo(cx, cy - hh * 1.02f)
            cubicTo(cx + hw * 0.82f, cy - hh * 0.72f, cx + hw * 0.98f, cy + hh * 0.15f, cx + hw * 0.75f, cy + hh * 0.98f)
            lineTo(cx, cy + hh * 0.70f)
            lineTo(cx - hw * 0.75f, cy + hh * 0.98f)
            cubicTo(cx - hw * 0.98f, cy + hh * 0.15f, cx - hw * 0.82f, cy - hh * 0.72f, cx, cy - hh * 1.02f)
            close()
        }

        // Warm Terracotta & Ancient Royal Bronze gradient deck
        drawScope.drawPath(
            path = yorubaPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFD700), Color(0xFFC67D2B), Color(0xFF6D3B14), Color(0xFF3E1C0A)),
                startY = cy - hh,
                endY = cy + hh
            )
        )

        // Heavy Sculpted Bronze Trim Border
        drawScope.drawPath(
            path = yorubaPath,
            color = Color(0xFFFFD54F),
            style = Stroke(width = 3.0f)
        )

        // Inner Embossed Royal Diamond Lattice / Interlace Motif
        for (i in -1..1) {
            val dX = cx + i * (hw * 0.42f)
            val dY = cy - hh * 0.05f
            val dSize = 8f

            val diamondPath = Path().apply {
                moveTo(dX, dY - dSize)
                lineTo(dX + dSize * 0.7f, dY)
                lineTo(dX, dY + dSize)
                lineTo(dX - dSize * 0.7f, dY)
                close()
            }
            drawScope.drawPath(diamondPath, color = AstralGold.copy(alpha = 0.9f))
            drawScope.drawPath(diamondPath, color = Color(0xFF3E1C0A), style = Stroke(width = 1.4f))
        }

        // Traditional Cowrie Shell Inlays (Representing fortune, nobility & prosperity)
        // 4 Golden Cowries positioned gracefully along the deck
        val cowrieOffsets = listOf(
            Offset(cx - hw * 0.58f, cy + hh * 0.35f),
            Offset(cx + hw * 0.58f, cy + hh * 0.35f),
            Offset(cx, cy - hh * 0.62f),
            Offset(cx, cy + hh * 0.35f)
        )

        for (cPos in cowrieOffsets) {
            // Shell body (ivory & golden rim)
            drawScope.drawOval(
                color = Color(0xFFFFF8E1),
                topLeft = Offset(cPos.x - 4f, cPos.y - 6f),
                size = Size(8f, 12f)
            )
            drawScope.drawOval(
                color = AstralGold,
                topLeft = Offset(cPos.x - 4f, cPos.y - 6f),
                size = Size(8f, 12f),
                style = Stroke(width = 1.2f)
            )
            // Characteristic central slit line of the cowrie
            drawScope.drawLine(
                color = Color(0xFF5D4037),
                start = Offset(cPos.x, cPos.y - 4f),
                end = Offset(cPos.x, cPos.y + 4f),
                strokeWidth = 1.2f
            )
        }

        // Radiant Sunburst Fortune Core
        val sunPulse = 1f + sin(t * 5f) * 0.15f
        drawScope.drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.5f * sunPulse),
            radius = 11f * sunPulse,
            center = Offset(cx, cy - hh * 0.15f)
        )
        drawScope.drawCircle(
            color = Color(0xFFFFE082),
            radius = 6f,
            center = Offset(cx, cy - hh * 0.15f)
        )
    }
}
