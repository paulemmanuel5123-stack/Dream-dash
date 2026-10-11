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
import com.example.model.ActionState
import com.example.model.AnimalForm
import com.example.ui.theme.AstralGold
import com.example.ui.theme.VoidDark
import kotlin.math.cos
import kotlin.math.sin

object Animal3DRenderer {

    fun drawAnimal(
        drawScope: DrawScope,
        animal: AnimalForm,
        actionState: ActionState,
        timeSeconds: Float,
        centerX: Float,
        groundY: Float,
        scale: Float = 1.0f,
        elevationPx: Float = 0f,
        laneTiltDir: Float = 0f
    ) {
        val baseY = groundY - elevationPx
        val cx = centerX + (laneTiltDir * 12f * scale)

        // Drop shadow on ground
        drawScope.drawOval(
            color = Color.Black.copy(alpha = (0.35f - (elevationPx / 300f).coerceIn(0f, 0.25f))),
            topLeft = Offset(centerX - 36f * scale, groundY - 8f * scale),
            size = Size(72f * scale, 16f * scale)
        )

        when (animal) {
            AnimalForm.CHEETAH -> drawCheetah(drawScope, cx, baseY, actionState, timeSeconds, scale)
            AnimalForm.MONKEY -> drawMonkey(drawScope, cx, baseY, actionState, timeSeconds, scale)
            AnimalForm.EAGLE -> drawEagle(drawScope, cx, baseY, actionState, timeSeconds, scale)
            AnimalForm.GORILLA -> drawGorilla(drawScope, cx, baseY, actionState, timeSeconds, scale)
            AnimalForm.WOLF -> drawWolf(drawScope, cx, baseY, actionState, timeSeconds, scale)
            AnimalForm.ELEPHANT -> drawElephant(drawScope, cx, baseY, actionState, timeSeconds, scale)
        }
    }

    fun drawAnimalPreview(
        drawScope: DrawScope,
        animal: AnimalForm,
        centerX: Float,
        groundY: Float,
        scale: Float = 1.0f,
        timeSeconds: Float
    ) {
        drawAnimal(
            drawScope = drawScope,
            animal = animal,
            actionState = ActionState.RUNNING,
            timeSeconds = timeSeconds,
            centerX = centerX,
            groundY = groundY,
            scale = scale,
            elevationPx = 0f,
            laneTiltDir = 0f
        )
    }

    private fun drawCheetah(
        drawScope: DrawScope,
        cx: Float,
        baseY: Float,
        state: ActionState,
        time: Float,
        s: Float
    ) {
        val runCycle = time * 12f
        val legSwing = sin(runCycle) * 16f * s
        val bodyBob = sin(runCycle * 2f) * 4f * s
        val cy = baseY - 32f * s + bodyBob

        // Glowing predatory aura
        drawScope.drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.22f),
            radius = 38f * s,
            center = Offset(cx, cy)
        )

        // Tail
        val tailPath = Path().apply {
            moveTo(cx - 18f * s, cy + 4f * s)
            quadraticTo(
                cx - 38f * s,
                cy - 12f * s + sin(runCycle) * 6f * s,
                cx - 46f * s,
                cy - 4f * s
            )
        }
        drawScope.drawPath(
            path = tailPath,
            color = Color(0xFFFFB300),
            style = Stroke(width = 5f * s, cap = StrokeCap.Round)
        )
        // Black tail tip
        drawScope.drawCircle(
            color = Color(0xFF212121),
            radius = 3.5f * s,
            center = Offset(cx - 46f * s, cy - 4f * s)
        )

        // Back Legs
        drawScope.drawLine(
            color = Color(0xFFFFA000),
            start = Offset(cx - 14f * s, cy + 6f * s),
            end = Offset(cx - 16f * s - legSwing, baseY),
            strokeWidth = 6f * s,
            cap = StrokeCap.Round
        )

        // Athletic Sleek Body (Gold with rosettes)
        drawScope.drawRoundRect(
            color = Color(0xFFFFB300),
            topLeft = Offset(cx - 24f * s, cy - 14f * s),
            size = Size(48f * s, 26f * s),
            cornerRadius = CornerRadius(12f * s, 12f * s)
        )
        // Cheetah Rosettes / Spots
        val spots = listOf(
            Offset(cx - 12f * s, cy - 6f * s),
            Offset(cx - 4f * s, cy + 2f * s),
            Offset(cx + 6f * s, cy - 5f * s),
            Offset(cx + 14f * s, cy + 3f * s),
            Offset(cx - 8f * s, cy + 5f * s)
        )
        spots.forEach { pt ->
            drawScope.drawCircle(color = Color(0xFF3E2723), radius = 2.4f * s, center = pt)
        }

        // Front Legs
        drawScope.drawLine(
            color = Color(0xFFFFB300),
            start = Offset(cx + 14f * s, cy + 6f * s),
            end = Offset(cx + 16f * s + legSwing, baseY),
            strokeWidth = 6f * s,
            cap = StrokeCap.Round
        )

        // Head & Ears
        val headX = cx + 22f * s
        val headY = cy - 12f * s
        drawScope.drawCircle(color = Color(0xFFFFB300), radius = 13f * s, center = Offset(headX, headY))
        // Ears
        drawScope.drawCircle(color = Color(0xFF3E2723), radius = 4f * s, center = Offset(headX - 6f * s, headY - 11f * s))
        drawScope.drawCircle(color = Color(0xFFFFA000), radius = 4f * s, center = Offset(headX + 4f * s, headY - 11f * s))
        // White Muzzle
        drawScope.drawCircle(color = Color(0xFFFFF8E1), radius = 5.5f * s, center = Offset(headX + 8f * s, headY + 3f * s))
        // Glowing Amber Eye
        drawScope.drawCircle(color = Color(0xFFFFD600), radius = 2.8f * s, center = Offset(headX + 4f * s, headY - 2f * s))
        drawScope.drawCircle(color = Color.Black, radius = 1.4f * s, center = Offset(headX + 5f * s, headY - 2f * s))
        // Tear Stripe
        drawScope.drawLine(
            color = Color(0xFF212121),
            start = Offset(headX + 4f * s, headY),
            end = Offset(headX + 7f * s, headY + 6f * s),
            strokeWidth = 1.8f * s,
            cap = StrokeCap.Round
        )
    }

    private fun drawMonkey(
        drawScope: DrawScope,
        cx: Float,
        baseY: Float,
        state: ActionState,
        time: Float,
        s: Float
    ) {
        val runCycle = time * 10f
        val legSwing = sin(runCycle) * 14f * s
        val bodyBob = sin(runCycle * 2f) * 3f * s
        val cy = baseY - 28f * s + bodyBob

        // Curly Prehensile Tail
        val tailPath = Path().apply {
            moveTo(cx - 10f * s, cy + 4f * s)
            cubicTo(
                cx - 24f * s, cy - 8f * s,
                cx - 32f * s, cy - 24f * s,
                cx - 22f * s, cy - 30f * s
            )
        }
        drawScope.drawPath(
            path = tailPath,
            color = Color(0xFF8D6E63),
            style = Stroke(width = 4.5f * s, cap = StrokeCap.Round)
        )

        // Agile Brown Body
        drawScope.drawOval(
            color = Color(0xFF795548),
            topLeft = Offset(cx - 14f * s, cy - 14f * s),
            size = Size(28f * s, 30f * s)
        )
        // Cream Belly
        drawScope.drawOval(
            color = Color(0xFFD7CCC8),
            topLeft = Offset(cx - 8f * s, cy - 6f * s),
            size = Size(16f * s, 18f * s)
        )

        // Arms & Legs
        drawScope.drawLine(
            color = Color(0xFF6D4C41),
            start = Offset(cx - 8f * s, cy + 10f * s),
            end = Offset(cx - 10f * s - legSwing, baseY),
            strokeWidth = 5f * s,
            cap = StrokeCap.Round
        )
        drawScope.drawLine(
            color = Color(0xFF6D4C41),
            start = Offset(cx + 8f * s, cy + 10f * s),
            end = Offset(cx + 10f * s + legSwing, baseY),
            strokeWidth = 5f * s,
            cap = StrokeCap.Round
        )

        // Head & Big Round Ears
        val headY = cy - 22f * s
        // Ears
        drawScope.drawCircle(color = Color(0xFFD7CCC8), radius = 6f * s, center = Offset(cx - 13f * s, headY))
        drawScope.drawCircle(color = Color(0xFFD7CCC8), radius = 6f * s, center = Offset(cx + 13f * s, headY))
        // Face
        drawScope.drawCircle(color = Color(0xFF795548), radius = 12f * s, center = Offset(cx, headY))
        drawScope.drawCircle(color = Color(0xFFD7CCC8), radius = 7.5f * s, center = Offset(cx, headY + 3f * s))
        // Expressive Eyes
        drawScope.drawCircle(color = Color.Black, radius = 2.2f * s, center = Offset(cx - 4f * s, headY - 1f * s))
        drawScope.drawCircle(color = Color.Black, radius = 2.2f * s, center = Offset(cx + 4f * s, headY - 1f * s))
        drawScope.drawCircle(color = Color.White, radius = 0.8f * s, center = Offset(cx - 4f * s, headY - 2f * s))
        drawScope.drawCircle(color = Color.White, radius = 0.8f * s, center = Offset(cx + 4f * s, headY - 2f * s))
    }

    private fun drawEagle(
        drawScope: DrawScope,
        cx: Float,
        baseY: Float,
        state: ActionState,
        time: Float,
        s: Float
    ) {
        val wingFlap = sin(time * 9f) * 16f * s
        val soarY = baseY - 44f * s + sin(time * 3f) * 6f * s

        // Soaring Wind Aura
        drawScope.drawCircle(
            color = Color(0xFF80D8FF).copy(alpha = 0.28f),
            radius = 48f * s,
            center = Offset(cx, soarY)
        )

        // Outstretched Giant Wings
        val leftWing = Path().apply {
            moveTo(cx - 8f * s, soarY)
            lineTo(cx - 46f * s, soarY - 12f * s + wingFlap)
            lineTo(cx - 54f * s, soarY + 4f * s + wingFlap)
            lineTo(cx - 28f * s, soarY + 12f * s)
            close()
        }
        val rightWing = Path().apply {
            moveTo(cx + 8f * s, soarY)
            lineTo(cx + 46f * s, soarY - 12f * s + wingFlap)
            lineTo(cx + 54f * s, soarY + 4f * s + wingFlap)
            lineTo(cx + 28f * s, soarY + 12f * s)
            close()
        }
        drawScope.drawPath(leftWing, color = Color(0xFF3E2723))
        drawScope.drawPath(rightWing, color = Color(0xFF4E342E))

        // Golden Flight Feathers Tips
        drawScope.drawCircle(color = AstralGold, radius = 4f * s, center = Offset(cx - 52f * s, soarY + wingFlap))
        drawScope.drawCircle(color = AstralGold, radius = 4f * s, center = Offset(cx + 52f * s, soarY + wingFlap))

        // Torso & Tail Feathers
        val tailPath = Path().apply {
            moveTo(cx - 8f * s, soarY + 8f * s)
            lineTo(cx, soarY + 28f * s)
            lineTo(cx + 8f * s, soarY + 8f * s)
            close()
        }
        drawScope.drawPath(tailPath, color = Color.White)
        drawScope.drawOval(
            color = Color(0xFF4E342E),
            topLeft = Offset(cx - 12f * s, soarY - 14f * s),
            size = Size(24f * s, 26f * s)
        )

        // Noble White Eagle Hood & Curved Golden Beak
        drawScope.drawCircle(color = Color.White, radius = 10f * s, center = Offset(cx, soarY - 18f * s))
        // Curved Beak
        val beak = Path().apply {
            moveTo(cx + 4f * s, soarY - 21f * s)
            lineTo(cx + 18f * s, soarY - 17f * s)
            lineTo(cx + 8f * s, soarY - 13f * s)
            close()
        }
        drawScope.drawPath(beak, color = AstralGold)
        // Piercing Amber Eye
        drawScope.drawCircle(color = Color(0xFFFFD600), radius = 2.4f * s, center = Offset(cx + 4f * s, soarY - 19f * s))
        drawScope.drawCircle(color = Color.Black, radius = 1.2f * s, center = Offset(cx + 5f * s, soarY - 19f * s))
    }

    private fun drawGorilla(
        drawScope: DrawScope,
        cx: Float,
        baseY: Float,
        state: ActionState,
        time: Float,
        s: Float
    ) {
        val runCycle = time * 7f
        val stride = sin(runCycle) * 10f * s
        val cy = baseY - 36f * s + sin(runCycle * 2f) * 3f * s

        // Heavy Primal Shockwave Aura
        drawScope.drawCircle(
            color = Color(0xFFFF7043).copy(alpha = 0.22f),
            radius = 42f * s,
            center = Offset(cx, cy)
        )

        // Muscular Torso & Silverback Fur
        drawScope.drawRoundRect(
            color = Color(0xFF263238),
            topLeft = Offset(cx - 24f * s, cy - 18f * s),
            size = Size(48f * s, 36f * s),
            cornerRadius = CornerRadius(14f * s, 14f * s)
        )
        // Silverback patch
        drawScope.drawRoundRect(
            color = Color(0xFF78909C),
            topLeft = Offset(cx - 16f * s, cy - 14f * s),
            size = Size(32f * s, 18f * s),
            cornerRadius = CornerRadius(8f * s, 8f * s)
        )

        // Massive Heavy Arm Strides (Smashing Fists)
        drawScope.drawRoundRect(
            color = Color(0xFF37474F),
            topLeft = Offset(cx - 30f * s, cy - 6f * s),
            size = Size(14f * s, 34f * s),
            cornerRadius = CornerRadius(7f * s, 7f * s)
        )
        drawScope.drawRoundRect(
            color = Color(0xFF37474F),
            topLeft = Offset(cx + 16f * s, cy - 6f * s),
            size = Size(14f * s, 34f * s),
            cornerRadius = CornerRadius(7f * s, 7f * s)
        )
        // Knuckle fists resting on ground
        drawScope.drawCircle(color = Color(0xFF212121), radius = 7f * s, center = Offset(cx - 23f * s, baseY))
        drawScope.drawCircle(color = Color(0xFF212121), radius = 7f * s, center = Offset(cx + 23f * s, baseY))

        // Broad Head & Heavy Brow Ridge
        val headY = cy - 24f * s
        drawScope.drawCircle(color = Color(0xFF212121), radius = 13f * s, center = Offset(cx, headY))
        drawScope.drawRect(
            color = Color(0xFF37474F),
            topLeft = Offset(cx - 10f * s, headY - 4f * s),
            size = Size(20f * s, 10f * s)
        )
        // Glowing Heroic Amber Eyes
        drawScope.drawCircle(color = Color(0xFFFFAB00), radius = 2.2f * s, center = Offset(cx - 5f * s, headY - 1f * s))
        drawScope.drawCircle(color = Color(0xFFFFAB00), radius = 2.2f * s, center = Offset(cx + 5f * s, headY - 1f * s))
    }

    private fun drawWolf(
        drawScope: DrawScope,
        cx: Float,
        baseY: Float,
        state: ActionState,
        time: Float,
        s: Float
    ) {
        val runCycle = time * 11f
        val legSwing = sin(runCycle) * 14f * s
        val cy = baseY - 30f * s + sin(runCycle * 2f) * 3f * s

        // Mystical Spirit Glow
        drawScope.drawCircle(
            color = Color(0xFF84FFFF).copy(alpha = 0.28f),
            radius = 36f * s,
            center = Offset(cx, cy)
        )

        // Bushy Wolf Tail
        val tailPath = Path().apply {
            moveTo(cx - 16f * s, cy + 2f * s)
            quadraticTo(cx - 32f * s, cy - 6f * s, cx - 40f * s, cy + 6f * s)
            lineTo(cx - 30f * s, cy + 12f * s)
            close()
        }
        drawScope.drawPath(tailPath, color = Color(0xFF546E7A))

        // Running Legs
        drawScope.drawLine(
            color = Color(0xFF455A64),
            start = Offset(cx - 12f * s, cy + 4f * s),
            end = Offset(cx - 14f * s - legSwing, baseY),
            strokeWidth = 5f * s,
            cap = StrokeCap.Round
        )
        drawScope.drawLine(
            color = Color(0xFF455A64),
            start = Offset(cx + 12f * s, cy + 4f * s),
            end = Offset(cx + 14f * s + legSwing, baseY),
            strokeWidth = 5f * s,
            cap = StrokeCap.Round
        )

        // Silver-Grey Torso
        drawScope.drawOval(
            color = Color(0xFF607D8B),
            topLeft = Offset(cx - 20f * s, cy - 13f * s),
            size = Size(40f * s, 26f * s)
        )
        // White Fur Chest Ruff
        drawScope.drawCircle(color = Color(0xFFECEFF1), radius = 8f * s, center = Offset(cx + 10f * s, cy - 2f * s))

        // Alert Head & Pointed Ears
        val headX = cx + 18f * s
        val headY = cy - 10f * s
        drawScope.drawCircle(color = Color(0xFF546E7A), radius = 11f * s, center = Offset(headX, headY))
        // Pointed Ears
        val leftEar = Path().apply {
            moveTo(headX - 6f * s, headY - 8f * s)
            lineTo(headX - 2f * s, headY - 20f * s)
            lineTo(headX + 2f * s, headY - 8f * s)
            close()
        }
        val rightEar = Path().apply {
            moveTo(headX + 2f * s, headY - 8f * s)
            lineTo(headX + 6f * s, headY - 20f * s)
            lineTo(headX + 10f * s, headY - 8f * s)
            close()
        }
        drawScope.drawPath(leftEar, color = Color(0xFF37474F))
        drawScope.drawPath(rightEar, color = Color(0xFF37474F))
        // Snout
        drawScope.drawCircle(color = Color(0xFFECEFF1), radius = 5f * s, center = Offset(headX + 9f * s, headY + 3f * s))
        drawScope.drawCircle(color = Color.Black, radius = 2f * s, center = Offset(headX + 13f * s, headY + 2f * s))
        // Glowing Spirit Blue Eyes
        drawScope.drawCircle(color = Color(0xFF00E5FF), radius = 2.4f * s, center = Offset(headX + 4f * s, headY - 2f * s))
    }

    private fun drawElephant(
        drawScope: DrawScope,
        cx: Float,
        baseY: Float,
        state: ActionState,
        time: Float,
        s: Float
    ) {
        val runCycle = time * 6.5f
        val legSwing = sin(runCycle) * 8f * s
        val cy = baseY - 38f * s

        // Giant Solid Torso
        drawScope.drawRoundRect(
            color = Color(0xFF455A64),
            topLeft = Offset(cx - 28f * s, cy - 22f * s),
            size = Size(56f * s, 44f * s),
            cornerRadius = CornerRadius(16f * s, 16f * s)
        )

        // Heavy Pillar Legs
        drawScope.drawRoundRect(
            color = Color(0xFF37474F),
            topLeft = Offset(cx - 24f * s, cy + 12f * s),
            size = Size(12f * s, 26f * s),
            cornerRadius = CornerRadius(4f * s, 4f * s)
        )
        drawScope.drawRoundRect(
            color = Color(0xFF37474F),
            topLeft = Offset(cx + 12f * s, cy + 12f * s),
            size = Size(12f * s, 26f * s),
            cornerRadius = CornerRadius(4f * s, 4f * s)
        )

        // Large Elephant Head & Ears
        val headX = cx + 24f * s
        val headY = cy - 8f * s
        // Flapped Ears
        drawScope.drawOval(
            color = Color(0xFF546E7A),
            topLeft = Offset(headX - 18f * s, headY - 14f * s),
            size = Size(16f * s, 28f * s)
        )
        // Head
        drawScope.drawCircle(color = Color(0xFF455A64), radius = 16f * s, center = Offset(headX, headY))

        // Curved Ivory Tusks
        drawScope.drawLine(
            color = Color(0xFFFFF9C4),
            start = Offset(headX + 6f * s, headY + 8f * s),
            end = Offset(headX + 22f * s, headY + 4f * s),
            strokeWidth = 3.5f * s,
            cap = StrokeCap.Round
        )

        // Curling Trunk
        val trunkBob = sin(time * 5f) * 8f * s
        val trunkPath = Path().apply {
            moveTo(headX + 12f * s, headY + 6f * s)
            quadraticTo(
                headX + 22f * s, headY + 22f * s,
                headX + 26f * s + trunkBob, headY + 16f * s + trunkBob
            )
        }
        drawScope.drawPath(
            path = trunkPath,
            color = Color(0xFF37474F),
            style = Stroke(width = 6f * s, cap = StrokeCap.Round)
        )

        // Kind Intelligent Eye
        drawScope.drawCircle(color = AstralGold, radius = 2.4f * s, center = Offset(headX + 6f * s, headY - 4f * s))
        drawScope.drawCircle(color = Color.Black, radius = 1.2f * s, center = Offset(headX + 6f * s, headY - 4f * s))
    }
}
