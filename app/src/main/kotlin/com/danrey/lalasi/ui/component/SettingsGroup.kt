// SettingsGroup.kt

package com.danrey.lalasi.ui.component

import com.danrey.lalasi.ui.theme.LalasiInk
import com.danrey.lalasi.ui.theme.lalasiAdapt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.danrey.lalasi.R
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.layout.onPlaced
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.heightIn

enum class SettingsGroupStyle {

    Classic,

    Grouped,
}

@Composable
fun SettingsGroup(
    title: String? = null,
    items: List<SettingsItem>,
    style: SettingsGroupStyle = SettingsGroupStyle.Classic,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        title?.let {
            // iOS section header: small, grey, upper-case, set in from the card edge
            Text(
                text = it.uppercase(),
                fontFamily = com.danrey.lalasi.ui.theme.InterFontFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = IosSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)
            )
        }

        IosItems(items, style)
    }
}

private val IosCard get() = lalasiAdapt(0xFF1C1C1E)
private val IosSeparator get() = LalasiInk.copy(alpha = 0.12f)
private val IosSecondary = Color(0xFF8E8E93)

// the system tints Settings icons come in; a row takes one by its place in the list so a page isn't a wall of one colour
private val IosIconColors = listOf(
    Color(0xFF0A84FF), Color(0xFF30D158), Color(0xFFFF9F0A), Color(0xFFFF453A),
    Color(0xFFBF5AF2), Color(0xFF64D2FF), Color(0xFFFF375F), Color(0xFF5E5CE6),
)

/** One inset-grouped list: a single rounded card, rows divided by hairlines that start under the text, like iOS Settings. */
@Composable
private fun IosItems(items: List<SettingsItem>, style: SettingsGroupStyle) {
    val firstVisible = items.indexOfFirst { it.isVisible }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(IosCard)
    ) {
        items.forEachIndexed { index, item ->
            AnimatedVisibility(
                visible = item.isVisible,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
            ) {
                Column {
                    if (index != firstVisible) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                // starts exactly under the text: row inset + 30 dp tile + 14 dp gap (the old 58 dp sat 2 dp short)
                                .padding(start = 16.dp + (if (item.icon != null) 44.dp else 0.dp))
                                .height(0.5.dp)
                                .background(IosSeparator)
                        )
                    }
                    SettingsItemRow(
                        item = item,
                        style = style,
                        iconColor = item.iconTint ?: IosIconColors[index % IosIconColors.size],
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    style: SettingsGroupStyle,
    iconColor: Color = Color(0xFF0A84FF),
) {
    // app behaviour can hide the line under each title, holding the row still shows it
    val (hideDescriptions) = com.danrey.lalasi.utils.rememberPreference(
        com.danrey.lalasi.constants.HideSettingsDescriptionsKey,
        defaultValue = true,
    )
    // a row arrived at from search or a related link claims the request once, scrolls into view and flashes
    val key = item.highlightKey
    val requester = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val settingsScroll = LocalSettingsScroll.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val placement = remember { SettingsPlacement() }
    androidx.compose.runtime.LaunchedEffect(key) {
        if (key == null) return@LaunchedEffect
        // watched, not keyed on: clearing the request restarted the effect, so the row never scrolled and stayed lit
        androidx.compose.runtime.snapshotFlow { SettingsHighlight.pending }.collect { pending ->
            if (pending != key) return@collect
            SettingsHighlight.pending = null
            SettingsHighlight.active = key
            // let the page finish opening first, or it scrolls while still sliding in
            kotlinx.coroutines.delay(320)
            val target = placement.coords
            if (settingsScroll != null && target != null) settingsScroll.reveal(target, density) else requester.bringIntoView()
            kotlinx.coroutines.delay(2400)
            if (SettingsHighlight.active == key) SettingsHighlight.active = null
        }
    }

    // a row answers a finger like a button: a grey fill while pressed (no link-style ripple)
    val rowSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val rowPressed by rowSource.collectIsPressedAsState()
    val isFlashing = key != null && SettingsHighlight.active == key
    val highlightAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFlashing) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 450),
        label = "settingsHighlight",
    )
    val highlightColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .iosMinRow()
            .bringIntoViewRequester(requester)
            .onPlaced { placement.coords = it }
            .drawBehind {
                if (rowPressed) drawRect(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.10f))
                if (highlightAlpha > 0.01f) {
                    drawRect(color = highlightColor.copy(alpha = 0.22f * highlightAlpha))
                }
            }
            .combinedClickable(
                interactionSource = rowSource,
                indication = null,
                enabled = item.enabled && (item.onClick != null || item.descriptionText != null),
                onClick = { item.onClick?.invoke() },
                // holding a row shows its whole description, since the line under it is cut to one
                onLongClick = (item.detail ?: item.descriptionText)?.let { full ->
                    { TopToaster.show(full, iconRes = R.drawable.info) }
                },
            )
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        item.icon?.let { icon ->
            val tile = RoundedCornerShape(7.dp)
            val dim = if (item.enabled) 1f else 0.45f
            if (item.tintIcon) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(tile)
                        .background(iconColor.copy(alpha = dim)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.showBadge) {
                        BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) }) {
                            Icon(painter = icon, contentDescription = null, tint = LalasiInk, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Icon(painter = icon, contentDescription = null, tint = LalasiInk, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp).clip(tile),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            val titleColor = if (item.enabled) LalasiInk else LalasiInk.copy(alpha = 0.38f)
            // one title style for every row of every page: same family, size and line height, so long and short titles line up
            val titleStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = com.danrey.lalasi.ui.theme.InterFontFamily,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
            )
            ProvideTextStyle(titleStyle.copy(color = titleColor)) {
                item.title()
            }

            val descColor = if (item.enabled) IosSecondary else IosSecondary.copy(alpha = 0.6f)

            if (item.descriptionText != null && !hideDescriptions) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.descriptionText,
                    fontFamily = com.danrey.lalasi.ui.theme.InterFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = descColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (item.descriptionText == null) {
                item.description?.let { desc ->
                    Spacer(modifier = Modifier.height(2.dp))
                    ProvideTextStyle(
                        MaterialTheme.typography.labelMedium.copy(
                            color = descColor,
                            fontFamily = com.danrey.lalasi.ui.theme.InterFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    ) {
                        desc()
                    }
                }
            }
        }

        item.trailingContent?.let { trailing ->
            Spacer(modifier = Modifier.width(10.dp))
            trailing()
        }
    }
}

private fun Modifier.iosMinRow(): Modifier = this.heightIn(min = 44.dp)

data class SettingsItem(
    val icon: Painter? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,

    val descriptionText: String? = null,
    /** The whole description, shown when the row is held; [descriptionText] is the short line. */
    val detail: String? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val tintIcon: Boolean = true,
    val iconShape: Shape? = null,
    /** a fixed tile colour for the icon; without one the row takes the next system tint in the list */
    val iconTint: Color? = null,
    val enabled: Boolean = true,
    val isVisible: Boolean = true,
    val isSubOption: Boolean = false,
    /** Identifier used by settings search to flash this row when the user arrives from a result. */
    val highlightKey: String? = null,
    // the title as shown in the app's language, so a page's own search finds it in any language
    val searchText: String? = null,
    val onClick: (() -> Unit)? = null
)
