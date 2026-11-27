// AppSwitch.kt

package com.danrey.lalasi.ui.component

import com.danrey.lalasi.ui.theme.LalasiInk

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val TrackWidth = 51.dp
private val TrackHeight = 31.dp
private val ThumbSize = 27.dp
private val ThumbInset = (TrackHeight - ThumbSize) / 2
private val IosGreen = Color(0xFF34C759)
private val IosOff: Color get() = if (com.danrey.lalasi.ui.theme.LalasiPalette.light) Color(0xFFD9DAE0) else Color(0xFF39393D)

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbContent: (@Composable () -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else 0.38f
    val animSpec = tween<Color>(150)

    val trackColor by animateColorAsState(
        targetValue = (if (checked) IosGreen else IosOff).copy(alpha = alpha),
        animationSpec = animSpec,
        label = "trackColor"
    )
    // the thumb is white in every theme (iOS), with a hair of edge on Terang so it stays visible on the light track
    val thumbColor = Color.White.copy(alpha = alpha)
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - ThumbSize - ThumbInset else ThumbInset,
        animationSpec = tween(150),
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .size(TrackWidth, TrackHeight)
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                onValueChange = { onCheckedChange?.invoke(it) },
            )
            .clip(RoundedCornerShape(50))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(ThumbSize)
                .clip(CircleShape)
                .background(thumbColor)
                .then(if (com.danrey.lalasi.ui.theme.LalasiPalette.light) Modifier.border(0.6.dp, LalasiInk.copy(alpha = 0.18f), CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            thumbContent?.invoke()
        }
    }
}
