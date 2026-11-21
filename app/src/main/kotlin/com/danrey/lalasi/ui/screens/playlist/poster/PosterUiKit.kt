package com.danrey.lalasi.ui.screens.playlist.poster

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Poster Studio controls: flat and white. No gloss, no gradients, no drop shadows. One solid white fill
 * for the thing that is on or primary, a faint flat tint with a hairline for the rest, and a small spring
 * press with a haptic tick.
 */
internal val StudioAccent = Color.White
private val Ink = Color(0xFF000000)
private val Fill = Color(0x14FFFFFF)
private val Line = Color(0x26FFFFFF)
private val Quiet = Color(0xFF8E8E93)
private val Danger = Color(0xFFFF6B6B)

/** press feedback shared by every control: 0 = resting, 1 = fully pressed */
@Composable
private fun pressState(src: MutableInteractionSource): Float {
    val pressed by src.collectIsPressedAsState()
    val p by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = .7f, stiffness = Spring.StiffnessMedium), label = "press")
    return p
}

/** Primary action: solid white, black label. */
@Composable
internal fun PillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val src = remember { MutableInteractionSource() }
    val press = pressState(src)
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .graphicsLayer { val s = 1f - .04f * press; scaleX = s; scaleY = s }
            .clip(shape)
            .background(Color.White.copy(alpha = if (enabled) 1f else .3f))
            .clickable(interactionSource = src, indication = null, enabled = enabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
            .heightIn(min = 44.dp)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            CompositionLocalProvider(LocalContentColor provides Ink) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)) { content() }
            }
        }
    }
}

/** Secondary action: a flat tint with a hairline, white label. */
@Composable
internal fun GhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val src = remember { MutableInteractionSource() }
    val press = pressState(src)
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .graphicsLayer { val s = 1f - .04f * press; scaleX = s; scaleY = s }
            .clip(shape)
            .background(if (enabled) Fill else Color(0x0AFFFFFF))
            .border(0.5.dp, Line, shape)
            .clickable(interactionSource = src, indication = null, enabled = enabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
            .heightIn(min = 44.dp)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            CompositionLocalProvider(LocalContentColor provides if (enabled) Color.White else Color(0x66FFFFFF)) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)) { content() }
            }
        }
    }
}

/** Option for every choice row: the selected one is solid white, the rest are a flat tint. */
@Composable
internal fun StudioChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val src = remember { MutableInteractionSource() }
    val press = pressState(src)
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier.graphicsLayer { val s = 1f - .05f * press; scaleX = s; scaleY = s }
            .clip(shape)
            .background(if (selected) Color.White else Fill)
            .heightIn(min = 38.dp)
            .clickable(interactionSource = src, indication = null) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Ink else Color(0xE6FFFFFF), style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

/** Icon-first action with its label underneath. `active` fills it white; `danger` tints the icon red. */
@Composable
internal fun ActionTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, active: Boolean = false, danger: Boolean = false, enabled: Boolean = true) {
    val shape = RoundedCornerShape(14.dp)
    val src = remember { MutableInteractionSource() }
    val press = pressState(src)
    val haptic = LocalHapticFeedback.current
    Column(
        modifier.width(58.dp).graphicsLayer { val s = 1f - .06f * press; scaleX = s; scaleY = s }
            .clickable(interactionSource = src, indication = null, enabled = enabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(44.dp).clip(shape).background(if (active) Color.White else Fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = when { !enabled -> Color(0x55FFFFFF); active -> Ink; danger -> Danger; else -> Color.White }, modifier = Modifier.size(22.dp))
        }
        Text(label, color = if (enabled) Color(0xCCFFFFFF) else Color(0x55FFFFFF), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.padding(top = 3.dp))
    }
}

/** One slot of the bottom dock: the selected tool sits on a white tile, the others stay quiet grey. */
@Composable
internal fun DockItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    val press = pressState(src)
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier.clickable(interactionSource = src, indication = null) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
            .padding(vertical = 5.dp).graphicsLayer { val s = 1f - .06f * press; scaleX = s; scaleY = s },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(40.dp).clip(shape).background(if (selected) Color.White else Color.Transparent), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = if (selected) Ink else Quiet, modifier = Modifier.size(23.dp))
        }
        Text(label, color = if (selected) Color.White else Quiet, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

/** the dock's own surface: a flat dark capsule with a hairline */
internal fun Modifier.dockSurface(): Modifier {
    val shape = RoundedCornerShape(28.dp)
    return this.clip(shape).background(Color(0xFF1C1C1E)).border(0.5.dp, Line, shape)
}
