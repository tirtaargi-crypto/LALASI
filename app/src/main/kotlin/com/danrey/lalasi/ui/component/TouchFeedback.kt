// TouchFeedback.kt

package com.danrey.lalasi.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/** the pressed row from the Figma: #1E1E1E on the black pages, and the same lift on any darker colour */
val HighlightColor = Color.White.copy(alpha = 0.12f)

/**
 * a soft rounded box that fades in behind whatever is pressed, in place of material's square
 * ripple. it's drawn under the content so nothing gets clipped, and it can spill past the bounds
 * (or sit inside them, with a negative spill) so a card or a row gets some room around it.
 */
@Immutable
class RoundedHighlight(
    private val cornerRadius: Dp,
    private val spillX: Dp = 0.dp,
    private val spillY: Dp = 0.dp,
    private val color: Color = HighlightColor,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        RoundedHighlightNode(interactionSource, cornerRadius, spillX, spillY, color)

    override fun equals(other: Any?): Boolean =
        other is RoundedHighlight &&
            other.cornerRadius == cornerRadius &&
            other.spillX == spillX &&
            other.spillY == spillY &&
            other.color == color

    override fun hashCode(): Int {
        var result = cornerRadius.hashCode()
        result = 31 * result + spillX.hashCode()
        result = 31 * result + spillY.hashCode()
        result = 31 * result + color.hashCode()
        return result
    }
}

private class RoundedHighlightNode(
    private val interactionSource: InteractionSource,
    private val cornerRadius: Dp,
    private val spillX: Dp,
    private val spillY: Dp,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {
    private val strength = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            val presses = ArrayList<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses.add(interaction)
                    is PressInteraction.Release -> presses.remove(interaction.press)
                    is PressInteraction.Cancel -> presses.remove(interaction.press)
                    else -> return@collect
                }
                if (presses.isNotEmpty()) {
                    launch { strength.animateTo(1f, tween(90)) }
                } else {
                    launch {
                        // a quick tap lets go before the box has shown at all, so flash it first
                        if (strength.value < 1f) strength.animateTo(1f, tween(70))
                        strength.animateTo(0f, tween(340))
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val s = strength.value
        if (s > 0f) {
            val dx = spillX.toPx()
            val dy = spillY.toPx()
            drawRoundRect(
                color = color.copy(alpha = color.alpha * s),
                topLeft = Offset(-dx, -dy),
                size = Size(size.width + dx * 2f, size.height + dy * 2f),
                cornerRadius = CornerRadius(cornerRadius.toPx()),
            )
        }
        drawContent()
    }
}

/**
 * squishes under the finger and springs back with a little overshoot. a quick tap inside a list
 * reports its press and release together, so the release plays the squish first when it never showed.
 */
@Composable
fun Modifier.pressBounce(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.94f,
): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(interactionSource, pressedScale) {
        var pressed = 0
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressed++
                    launch { scale.animateTo(pressedScale, spring(dampingRatio = 1f, stiffness = 1600f)) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    pressed = (pressed - 1).coerceAtLeast(0)
                    if (pressed == 0) {
                        launch {
                            if (scale.value > pressedScale + 0.03f) scale.animateTo(pressedScale, tween(70))
                            scale.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 420f))
                        }
                    }
                }
            }
        }
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}


/**
 * The Home liquid glass press. On press the surface swells a few percent with a soft overshoot, a pale reflection
 * appears under the finger and the lens edge of the nav bar's bubble (see IridescentBubble.drawLens) fades in. On release it settles back and everything fades out: nothing stays lit. Idle cost is zero (no animation,
 * nothing allocated until the first press).
 *
 * Every node owns its own animation state, so two buttons never share a highlight. The press is observed, not
 * consumed: the click is delivered at once, exactly as before.
 *
 * Bounds: the bubble may reach [spillX]/[spillY] past the component, and the swell adds a little more. The parent list
 * must leave that room (Home's rows have vertical content padding for it); the rim itself is always drawn inside the
 * bubble's own outline, so it is never cut by the shape.
 */
@Immutable
class LiquidGlassIndication(
    private val shape: Shape,
    private val spillX: Dp = 0.dp,
    private val spillY: Dp = 0.dp,
    private val inflate: Float = 0.035f,
    private val thin: Float = 1f,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        LiquidGlassNode(interactionSource, shape, spillX, spillY, inflate, thin)

    override fun equals(other: Any?): Boolean =
        other is LiquidGlassIndication &&
            other.shape == shape &&
            other.spillX == spillX &&
            other.spillY == spillY &&
            other.inflate == inflate &&
            other.thin == thin

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + spillX.hashCode()
        result = 31 * result + spillY.hashCode()
        result = 31 * result + inflate.hashCode()
        result = 31 * result + thin.hashCode()
        return result
    }
}

private class LiquidGlassNode(
    private val interactionSource: InteractionSource,
    private val shape: Shape,
    private val spillX: Dp,
    private val spillY: Dp,
    private val inflate: Float,
    private val thin: Float,
) : Modifier.Node(), DrawModifierNode {
    private val glow = Animatable(0f)
    private val swell = Animatable(0f)
    private var lens: IridescentBubble? = null
    private var path: Path? = null
    private var pathSize = Size.Unspecified
    private var sheen: Brush? = null
    private var touch = Offset.Unspecified

    override fun onAttach() {
        coroutineScope.launch {
            val presses = ArrayList<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        presses.add(interaction)
                        touch = interaction.pressPosition
                        sheen = null
                    }
                    is PressInteraction.Release -> presses.remove(interaction.press)
                    is PressInteraction.Cancel -> presses.remove(interaction.press)
                    else -> return@collect
                }
                if (presses.isNotEmpty()) {
                    launch { glow.animateTo(1f, tween(110)) }
                    launch { swell.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 520f)) }
                } else {
                    // a tap in a list reports press and release together: show the swell first so it is seen.
                    // a cancel (the finger became a scroll) just lets go, with no flash.
                    val tap = interaction is PressInteraction.Release
                    launch {
                        if (tap && glow.value < 0.6f) glow.animateTo(1f, tween(60))
                        glow.animateTo(0f, tween(380))
                    }
                    launch {
                        if (tap && swell.value < 0.6f) swell.animateTo(1f, tween(70))
                        swell.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f))
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val g = glow.value
        val sw = swell.value
        if (g <= 0.004f && abs(sw) <= 0.0005f) {
            drawContent()
            return
        }
        val dx = spillX.toPx()
        val dy = spillY.toPx()
        val bw = size.width + dx * 2f
        val bh = size.height + dy * 2f
        val cachedPath = path
        val outline: Path = if (cachedPath != null && pathSize == size) {
            cachedPath
        } else {
            outlineToPath(shape.createOutline(Size(bw, bh), layoutDirection, this), Offset(-dx, -dy)).also {
                path = it
                pathSize = size
                sheen = null
            }
        }
        val bubble = lens ?: IridescentBubble().also { lens = it }
        val cx = size.width / 2f
        val cy = size.height / 2f
        val t = touch
        val at = if (t.isSpecified) {
            Offset(t.x.coerceIn(-dx, size.width + dx), t.y.coerceIn(-dy, size.height + dy))
        } else {
            Offset(cx * 0.6f, cy * 0.5f)
        }
        val reflection: Brush = sheen ?: Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.22f),
            1f to Color.Transparent,
            center = at,
            radius = maxOf(bw, bh) * 0.8f,
        ).also { sheen = it }
        // organic: it swells wider than it grows tall, and the spring overshoot makes it wobble back
        val swellX = 1f + inflate * sw
        val swellY = 1f + inflate * 0.6f * sw
        withTransform({ scale(swellX, swellY, Offset(cx, cy)) }) {
            this@draw.drawContent()
            clipPath(outline) { drawPath(outline, reflection, alpha = g) }
            // the glass over the content: frosted body, lens edge, chrome edge (the navbar bubble's look)
            bubble.drawLens(this, outline, -dx, -dy, size.width + dx, size.height + dy, g, thin, 0.10f)
        }
    }
}
