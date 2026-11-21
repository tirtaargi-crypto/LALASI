// HomeCards.kt

package com.danrey.lalasi.ui.component

import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.danrey.lalasi.LocalGridItemSize
import com.danrey.lalasi.R
import com.danrey.lalasi.constants.GridItemSize
import com.danrey.lalasi.ui.theme.InterFontFamily
import coil3.compose.AsyncImage

// spacing and type straight from the Figma home frames (13:11 / 14:212)
val HomeContentInset = 36.dp
val HomeCardSpacing = 15.dp

private val HomeTracking = (-0.06).em

val HomeLabelStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 19.sp,
    letterSpacing = HomeTracking,
)

val HomeTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    letterSpacing = HomeTracking,
)

private val HomeCardTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 17.sp,
    letterSpacing = (-0.03).em,
)

private val HomeCardSubtitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 15.sp,
    letterSpacing = (-0.02).em,
)

private val HomeLiquidSectionStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.05).em,
)

private val HomeRankStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    letterSpacing = HomeTracking,
)

private val SectionLabelColor = Color.White.copy(alpha = 0.69f)

/**
 * True inside HomeScreen only. Home's cards, rows and headers are shared with the Library, artist, album and playlist
 * screens, which keep their previous look; they read this flag and switch to the liquid glass design only on Home.
 */
val LocalHomeLiquidGlass = staticCompositionLocalOf { false }

/** the artwork proportions Home's shelves mix, so every shelf is not the same square */
enum class HomeCoverProportion(val widthScale: Float, val heightScale: Float) {
    Square(1f, 1f),
    Tall(0.92f, 1.24f),
    Wide(1.38f, 1f),
}

private val HomeLiquidTitle = LiquidGlassIndication(RoundedCornerShape(100.dp), inflate = 0.012f, thin = 0.5f)
private val HomeLiquidRow = LiquidGlassIndication(RoundedCornerShape(24.dp), spillX = 6.dp, spillY = 3.dp, inflate = 0.02f, thin = 0.7f)
private val HomeLiquidRound = LiquidGlassIndication(CircleShape, spillX = 4.dp, spillY = 4.dp, inflate = 0.10f, thin = 0.55f)

/** chips and quick-access pills */
val HomeLiquidChip = LiquidGlassIndication(RoundedCornerShape(100.dp), spillX = 4.dp, spillY = 5.dp, inflate = 0.04f, thin = 0.65f)

/** the cover cards: the bubble reaches 8dp round the card, cornered to match the artwork (the radius is a user setting) */
@Composable
fun homeLiquidCardIndication(): LiquidGlassIndication {
    val radius = HomeCardCornerRadius
    return remember(radius) {
        LiquidGlassIndication(RoundedCornerShape(radius + 8.dp), spillX = 8.dp, spillY = 8.dp, inflate = 0.04f, thin = 0.9f)
    }
}

@Composable
private fun homeLiquidHopInIndication(radius: Dp): LiquidGlassIndication =
    remember(radius) {
        LiquidGlassIndication(RoundedCornerShape(radius + 4.dp), spillX = 4.dp, spillY = 4.dp, inflate = 0.03f, thin = 0.8f)
    }
private val HomeRowShape = RoundedCornerShape(14.dp)
val HomeRowHeight = 54.dp

/**
 * A song row's height: [HomeRowHeight], or taller when a big system font size makes the title and
 * artist lines need more room. A fixed 54dp cut both lines off at the top and bottom.
 */
@Composable
fun homeRowHeight(): Dp {
    val linesHeight = with(LocalDensity.current) { (HomeRowTitleLineHeight.value + HomeCardSubtitleStyle.lineHeight.value).sp.toDp() }
    return maxOf(HomeRowHeight, linesHeight + 14.dp)
}

private val HomeRowTitleLineHeight = 17.sp

// pressing anything on Home shows a soft rounded box behind it instead of a square ripple
private val SectionTapMargin = 12.dp
private val SectionTitleHighlight = RoundedHighlight(cornerRadius = 100.dp)
private val TrackRowHighlight = RoundedHighlight(cornerRadius = 22.dp, spillX = 8.dp)

/** for the cover cards: a little bigger than the card all round, cornered to match */
val HomeCardHighlight = RoundedHighlight(cornerRadius = HomeCardCornerRadius + 6.dp, spillX = 7.dp, spillY = 7.dp)

/** square cover size on Home: the Figma 137dp card, or the compact size if the user picked small grids */
@Composable
fun homeCardSize(): Dp =
    if (LocalGridItemSize.current == GridItemSize.BIG) 137.dp else 104.dp

/**
 * "Recently Played >" style header. the whole line is the tap target, so pressing it lights a
 * pill from one side of the screen to the other; the dots play the whole section on their own.
 */
@Composable
fun HomeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val liquid = LocalHomeLiquidGlass.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = SectionTapMargin, end = SectionTapMargin, bottom = if (liquid) 4.dp else 6.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = null,
                        indication = if (liquid) HomeLiquidTitle else SectionTitleHighlight,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            // the text still starts on the 36dp inset, inside the wider tap area
            .padding(
                start = HomeContentInset - SectionTapMargin,
                end = HomeContentInset - SectionTapMargin,
                top = 4.dp,
                bottom = 4.dp,
            ),
    ) {
        if (thumbnail != null) {
            thumbnail()
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f, fill = false)) {
            if (label != null) {
                Text(
                    text = label,
                    style = HomeCardSubtitleStyle,
                    color = Color.White.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                style = if (liquid) HomeLiquidSectionStyle else HomeLabelStyle,
                color = if (liquid) Color.White.copy(alpha = 0.96f) else SectionLabelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) {
            Spacer(Modifier.width(if (liquid) 10.dp else 6.dp))
            if (liquid) {
                // the arrow sits in its own small glass disc
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(28.dp).calmGlassSurface(CircleShape, rimAlpha = 0.5f, rimWidth = 1.dp),
                ) { SectionChevron(size = 16.dp) }
            } else {
                SectionChevron()
            }
        }
    }
}

@Composable
private fun SectionChevron(size: Dp = 18.dp) {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.55f),
        modifier = Modifier.size(size),
    )
}

/** square (or round, for artists) cover with two lines under it. no outline, on purpose. */
@Composable
fun HomeCoverCard(
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    sharedElementKey: String? = null,
    badges: @Composable RowScope.() -> Unit = {},
    cover: (@Composable (Dp) -> Unit)? = null,
    proportion: HomeCoverProportion = HomeCoverProportion.Square,
) = HomeCoverCard(
    title, subtitle, thumbnailUrl, modifier, circular, isActive, { isPlaying }, sharedElementKey, badges, cover, proportion,
)

// isPlaying is read only inside the cover's indicator, so a play or pause doesn't recompose every card
@Composable
fun HomeCoverCard(
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
    isActive: Boolean = false,
    isPlaying: () -> Boolean,
    sharedElementKey: String? = null,
    badges: @Composable RowScope.() -> Unit = {},
    cover: (@Composable (Dp) -> Unit)? = null,
    proportion: HomeCoverProportion = HomeCoverProportion.Square,
) {
    val liquid = LocalHomeLiquidGlass.current
    // Home's artwork is 10% larger; the library, artist and album screens keep the Figma size
    val size = if (liquid) homeCardSize() * 1.1f else homeCardSize()
    val shape = if (circular) CircleShape else RoundedCornerShape(HomeCardCornerRadius)
    // circles and custom covers (liked songs, playlist collages) are drawn square; photos can take another proportion
    val mixed = liquid && !circular && cover == null
    val coverWidth = if (mixed) size * proportion.widthScale else size
    val coverHeight = if (mixed) size * proportion.heightScale else size

    Column(modifier = modifier.width(coverWidth)) {
        Box(
            modifier = Modifier
                .size(coverWidth, coverHeight)
                .homeSharedElement(sharedElementKey, coverUrl = thumbnailUrl)
                .clip(shape)
                .background(BoneColor)
                .then(if (liquid) Modifier.glassHairline(shape) else Modifier),
        ) {
            if (cover != null) {
                cover(size)
            } else {
                ItemThumbnail(
                    thumbnailUrl = thumbnailUrl,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = shape,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(if (liquid) 10.dp else 8.dp))
        Text(
            text = title,
            style = if (liquid) HomeCardTitleStyle.copy(fontSize = 15.sp, lineHeight = 18.sp) else HomeCardTitleStyle,
            color = Color.White.copy(alpha = if (liquid) 0.96f else 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (circular) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!subtitle.isNullOrEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                badges()
                Text(
                    text = subtitle,
                    style = HomeCardSubtitleStyle,
                    color = Color.White.copy(alpha = if (liquid) 0.56f else 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// the Figma "Concert" card (20:195) is 314 x 247 on a 402 wide frame
const val HomeVideoCardAspect = 314f / 247f

private val HomeVideoTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 18.sp,
    letterSpacing = (-0.03).em,
)

private val HomeVideoSubtitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 16.sp,
    letterSpacing = (-0.02).em,
)

// dark at the top where the text sits, clear by the middle so the frame itself shows
private val HomeVideoScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.62f),
    0.24f to Color.Black.copy(alpha = 0.34f),
    0.52f to Color.Transparent,
)


@Composable
fun HomeVideoCard(
    title: String,
    subtitle: String?,
    videoId: String,
    fallbackThumbnailUrl: String?,
    width: Dp,
    isActive: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) = HomeVideoCard(title, subtitle, videoId, fallbackThumbnailUrl, width, isActive, { isPlaying }, modifier)

// isPlaying is read only inside the indicator, same as the cover card
@Composable
fun HomeVideoCard(
    title: String,
    subtitle: String?,
    videoId: String,
    fallbackThumbnailUrl: String?,
    width: Dp,
    isActive: Boolean,
    isPlaying: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(HomeCardCornerRadius)
    var useFallback by remember(videoId) { mutableStateOf(false) }
    val liquid = LocalHomeLiquidGlass.current

    Box(
        modifier = modifier
            .width(width)
            .height(width / HomeVideoCardAspect)
            .clip(shape)
            .background(BoneColor)
            .then(if (liquid) Modifier.glassHairline(shape, width = 1.dp) else Modifier),
    ) {
        AsyncImage(
            model = if (useFallback) fallbackThumbnailUrl else "https://i.ytimg.com/vi/$videoId/hq720.jpg",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onError = { if (!useFallback) useFallback = true },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (liquid) HomeVideoScrimDeep else HomeVideoScrim),
        )
        PlayingIndicatorBox(
            isActive = isActive,
            playWhenReady = isPlaying,
            shape = shape,
            modifier = Modifier.fillMaxSize(),
        )
        Column(modifier = Modifier.padding(start = 18.dp, top = 16.dp, end = 18.dp)) {
            Text(
                text = title,
                style = HomeVideoTitleStyle,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = subtitle,
                    style = HomeVideoSubtitleStyle,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// the liquid version: darker at the top for the title, and darker again along the bottom edge so the frame sits in smoke
private val HomeVideoScrimDeep = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.70f),
    0.30f to Color.Black.copy(alpha = 0.30f),
    0.58f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.34f),
)

/**
 * the "list" and "Rank" rows: 54dp cover, two lines, a small more button. [rank] adds the big number.
 * No [onMoreClick], no button: the Library's lists use these rows and open the menu on long press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTrackRow(
    thumbnailUrl: String?,
    title: String,
    subtitle: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    /** The cover's outline: Home's rounded square, or a circle for an artist. */
    coverShape: Shape = RoundedCornerShape((HomeCardCornerRadius * 0.56f).coerceIn(0.dp, 20.dp)),
    /** Applied to the cover itself, e.g. to open a playlist growing out of it. */
    coverModifier: Modifier = Modifier,
    /** A song's id: marks the row when the song is downloaded or an imported file. */
    songId: String? = null,
) = HomeTrackRow(
    thumbnailUrl, title, subtitle, isActive, { isPlaying }, onClick, onLongClick, onMoreClick, modifier,
    rank, coverShape, coverModifier, songId,
)

// isPlaying is read only inside the cover's indicator, so a play or pause doesn't recompose every row
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTrackRow(
    thumbnailUrl: String?,
    title: String,
    subtitle: String?,
    isActive: Boolean,
    isPlaying: () -> Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    coverShape: Shape = RoundedCornerShape((HomeCardCornerRadius * 0.56f).coerceIn(0.dp, 20.dp)),
    coverModifier: Modifier = Modifier,
    songId: String? = null,
) {
    val liquid = LocalHomeLiquidGlass.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (liquid) {
            // the bubble is the outer layer: the playing row's calm glass is part of what swells, the rim is drawn over it
            modifier
                .height(homeRowHeight())
                .combinedClickable(
                    interactionSource = null,
                    indication = HomeLiquidRow,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .then(if (isActive) Modifier.padding(horizontal = 4.dp, vertical = 2.dp).calmGlassSurface(HomeRowShape) else Modifier)
        } else {
            modifier
                .height(homeRowHeight())
                .then(if (isActive) Modifier.padding(horizontal = 4.dp, vertical = 2.dp).lalasiGlassSurface(HomeRowShape, cornerRadiusFraction = 0.42f) else Modifier)
                .combinedClickable(
                    interactionSource = null,
                    indication = TrackRowHighlight,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
        },
    ) {
        if (rank != null) {
            Text(
                text = rank.toString(),
                style = HomeRankStyle,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.width(37.dp),
            )
        }
        Box(
            modifier = coverModifier
                .size(HomeRowHeight)
                .clip(coverShape)
                .background(BoneColor)
                .then(if (liquid) Modifier.glassHairline(coverShape) else Modifier),
        ) {
            ItemThumbnail(
                thumbnailUrl = thumbnailUrl,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = coverShape,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 13.dp),
        ) {
            Text(
                text = title,
                style = HomeCardTitleStyle.copy(fontSize = 15.sp),
                color = if (isActive) Color.White else Color.White.copy(alpha = if (liquid) 0.96f else 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = HomeCardSubtitleStyle,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        OnDeviceMarks(songId, modifier = Modifier.padding(end = 2.dp))
        if (onMoreClick != null) Box(
            contentAlignment = Alignment.Center,
            modifier = if (liquid) {
                // 44dp touch target; the small round button inflates the most, and its rim is the same closed ring
                Modifier
                    .size(44.dp)
                    .clickable(interactionSource = null, indication = HomeLiquidRound, onClick = onMoreClick)
                    .calmGlassSurface(CircleShape, rimAlpha = 0.5f, rimWidth = 1.dp)
            } else {
                Modifier
                    .size(40.dp)
                    .lalasiGlassSurface(CircleShape, cornerRadiusFraction = 0.5f)
                    .clickable(onClick = onMoreClick)
            },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_horiz),
                    contentDescription = "More",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

/** a section that hasn't arrived yet: header bar plus a row of cards, all bones */
@Composable
fun HomeSectionBones(modifier: Modifier = Modifier) {
    val size = homeCardSize()
    Column(modifier = modifier.fillMaxWidth()) {
        Bone(
            width = 128.dp,
            height = 15.dp,
            modifier = Modifier.padding(start = HomeContentInset),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(HomeCardSpacing),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .padding(start = HomeContentInset),
        ) {
            repeat(3) {
                Column {
                    Bone(width = size, height = size, shape = RoundedCornerShape(HomeCardCornerRadius))
                    Spacer(Modifier.height(8.dp))
                    Bone(width = size * 0.62f, height = 15.dp)
                    Spacer(Modifier.height(3.dp))
                    Bone(width = size * 0.28f, height = 11.dp)
                }
            }
        }
    }
}

private val HopInTileBg = Brush.verticalGradient(
    0f to Color(0xFF191B20),
    1f to Color(0xFF0D0E11),
)
val HopInCardHeight = 66.dp

// compact horizontal pill card used in the hop in 2x2 speed-dial grid at the top of home
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeHopInCard(
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: () -> Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedElementKey: String? = null,
    cover: (@Composable (Dp, Shape) -> Unit)? = null,
) {
    val radius = (HomeCardCornerRadius * 0.65f).coerceIn(0.dp, 18.dp)
    val shape = RoundedCornerShape(radius)
    // the artwork sits inside the tile with a small margin, cornered concentrically
    val inset = 6.dp
    val coverSize = HopInCardHeight - inset * 2
    val coverShape = RoundedCornerShape((radius - inset).coerceAtLeast(6.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(HopInCardHeight)
            // the press bubble wraps the tile, so it is applied outside the tile's own clip
            .combinedClickable(
                interactionSource = null,
                indication = homeLiquidHopInIndication(radius),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .clip(shape)
            .background(HopInTileBg)
            .glassHairline(shape),
    ) {
        Spacer(Modifier.width(inset))
        Box(
            modifier = Modifier
                .size(coverSize)
                .homeSharedElement(sharedElementKey, coverUrl = thumbnailUrl)
                .clip(coverShape)
                .background(BoneColor),
        ) {
            if (cover != null) {
                cover(coverSize, coverShape)
            } else {
                ItemThumbnail(
                    thumbnailUrl = thumbnailUrl,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = coverShape,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 11.dp),
        ) {
            Text(
                text = title,
                style = HomeCardTitleStyle.copy(fontSize = 14.sp, lineHeight = 17.sp),
                color = Color.White.copy(alpha = 0.96f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = HomeCardSubtitleStyle.copy(fontSize = 11.sp, lineHeight = 13.sp),
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// skeleton bones for the compact listen-now or hop-in top section while home loads
@Composable
fun HomeCompactTopBones(
    isHopIn: Boolean,
    modifier: Modifier = Modifier,
) {
    val radius = (HomeCardCornerRadius * 0.65f).coerceIn(0.dp, 18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HomeContentInset),
    ) {
        Bone(width = 148.dp, height = 16.dp)
        Spacer(Modifier.height(14.dp))
        if (isHopIn) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        repeat(2) {
                            Bone(
                                width = 154.dp,
                                height = HopInCardHeight,
                                shape = RoundedCornerShape(radius),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(HomeRowHeight),
                    ) {
                        Bone(
                            width = HomeRowHeight,
                            height = HomeRowHeight,
                            shape = RoundedCornerShape(radius),
                        )
                        Spacer(Modifier.width(13.dp))
                        Column {
                            Bone(width = 140.dp, height = 14.dp)
                            Spacer(Modifier.height(6.dp))
                            Bone(width = 88.dp, height = 11.dp)
                        }
                    }
                }
            }
        }
    }
}
