// the page every audio lab screen sits on, set the way iOS sets a settings page: pure black, a big left-aligned title that folds into a small
// centred one in the top bar as the page scrolls, a round translucent back button, grouped rows on dark grey. The hero card and the coloured
// icon tiles on the home page are what make it LALASI's own.

package com.danrey.lalasi.ui.screens.audiolab

import com.danrey.lalasi.ui.theme.LalasiBlack

import com.danrey.lalasi.ui.theme.LalasiInk
import com.danrey.lalasi.ui.theme.lalasiAdapt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.danrey.lalasi.LocalPlayerAwareWindowInsets
import com.danrey.lalasi.R
import com.danrey.lalasi.ui.theme.InterFontFamily
import com.danrey.lalasi.ui.utils.stableStatusBars

private val GroupColor get() = lalasiAdapt(0xFF1C1C1E)
private val SecondaryLabel = Color(0xFF8E8E93)

/** past this the page's content stops widening, so a tablet reads like a wide phone */
private val ContentMaxWidth = 680.dp
private val SideMargin = 20.dp
private val BackSize = 36.dp
private val TopBarHeight = 52.dp
private val HeroHeight = 168.dp

/** how the page's title is set: [first] and [second] run together on one line, either can be greyed; the sizes are kept for callers but iOS sets one size */
class LabTitle(
    val first: String,
    val second: String,
    val firstSize: TextUnit = 34.sp,
    val secondSize: TextUnit = 34.sp,
    /** the first word greyed, as on Audio FX */
    val firstDim: Boolean = false,
    /** the second word greyed */
    val secondDim: Boolean = false,
    /** a small outlined tag after the title, like beta */
    val secondBadge: String? = null,
)

/** art, when given, sits in a rounded card under the title and is told when it has scrolled away so it can stop animating; subtitle is the line under the title */
@Composable
fun AudioLabPage(
    title: LabTitle,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    art: (@Composable BoxScope.(hidden: State<Boolean>) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    val statusTop = WindowInsets.stableStatusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current
    val foldStart = with(density) { 34.dp.toPx() }
    val foldLength = with(density) { 28.dp.toPx() }
    val heroGone = with(density) { (HeroHeight + 90.dp).toPx() }
    val barAlpha = remember { derivedStateOf { ((scroll.value - foldStart) / foldLength).coerceIn(0f, 1f) } }
    val hidden = remember { derivedStateOf { scroll.value >= heroGone } }

    BoxWithConstraints(modifier.fillMaxSize().background(LalasiBlack)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(statusTop + TopBarHeight))
            LargeTitle(title, subtitle, Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().padding(horizontal = SideMargin))
            if (art != null) {
                Box(
                    Modifier
                        .padding(top = 16.dp)
                        .widthIn(max = ContentMaxWidth)
                        .fillMaxWidth()
                        .padding(horizontal = SideMargin)
                        .height(HeroHeight)
                        .clip(RoundedCornerShape(26.dp))
                        .background(GroupColor),
                ) {
                    Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.62f).fillMaxHeight().padding(top = 18.dp)) { art(hidden) }
                }
            }
            Column(
                modifier = Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = SideMargin)
                    .padding(top = 22.dp, bottom = bottomInset + 32.dp),
                content = content,
            )
        }

        // the top bar: the small title fades in over a dark strip once the big one has scrolled off
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(statusTop + TopBarHeight)
                .graphicsLayer { alpha = barAlpha.value }
                .background(lalasiAdapt(0xEB000000)),
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusTop)
                .height(TopBarHeight)
                .graphicsLayer { alpha = barAlpha.value },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (title.first + " " + title.second).trim(),
                fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = 0.em,
                color = LalasiInk, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = SideMargin + BackSize + 8.dp),
            )
        }
        LabBackButton(onBack, Modifier.padding(start = SideMargin, top = statusTop + (TopBarHeight - BackSize) / 2))
    }
}

@Composable
private fun LargeTitle(title: LabTitle, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = if (title.firstDim) SecondaryLabel else LalasiInk)) { append(title.first) }
                    if (title.second.isNotEmpty()) {
                        append(" ")
                        withStyle(SpanStyle(color = if (title.secondDim) SecondaryLabel else LalasiInk)) { append(title.second) }
                    }
                },
                fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp,
                letterSpacing = (-0.01).em, maxLines = 2, modifier = Modifier.weight(1f, fill = false),
            )
            title.secondBadge?.let { badge ->
                Spacer(Modifier.width(10.dp))
                LabBadge(badge)
            }
        }
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.em, color = SecondaryLabel, maxLines = 3)
        }
    }
}

/** a section's name over its group, small grey capitals as in iOS settings */
@Composable
fun LabSheetTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.02.em,
        color = SecondaryLabel,
        modifier = modifier.padding(start = 16.dp, top = 10.dp, bottom = 8.dp),
    )
}

/** the rounded group a section's rows sit in */
@Composable
fun LabGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(GroupColor)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** the pill that marks a page as still being worked on */
@Composable
private fun LabBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(lalasiAdapt(0xFF2C2C2E))
            .border(1.dp, Color(0xFFFFD866), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 2.dp),
    ) {
        Text(text, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.em, color = Color(0xFFFFD866), maxLines = 1)
    }
}

// the round translucent back button, shared with the customize page
@Composable
internal fun LabBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BackSize)
            .clip(CircleShape)
            .background(LalasiInk.copy(alpha = 0.16f))
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.arrow_back_ios),
            contentDescription = "Back",
            tint = LalasiInk,
            modifier = Modifier.size(16.dp).offset(x = 2.dp),
        )
    }
}
