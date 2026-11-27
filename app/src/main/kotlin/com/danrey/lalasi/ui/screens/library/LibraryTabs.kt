// LibraryTabs.kt
//
// The All / Local / Cached / Imported switch at the top of the Library, and the small pieces the
// tabs share: the two-wide card grid, the caps section label and the "Manage" capsule of a tab.

package com.danrey.lalasi.ui.screens.library

import com.danrey.lalasi.ui.theme.lalasiAdapt

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danrey.lalasi.R
import com.danrey.lalasi.ui.component.LalasiTokens
import com.danrey.lalasi.ui.component.calmGlassSurface
import com.danrey.lalasi.ui.theme.LalasiInk
import androidx.compose.foundation.shape.CircleShape
import com.danrey.lalasi.ui.screens.search.SearchColors
import com.danrey.lalasi.ui.screens.search.SearchHorizontalPadding
import com.danrey.lalasi.viewmodels.LibraryShelfKind

/**
 * What the Library shows: everything, what plays from the phone, what the player has cached, or
 * what was imported. [shelf] is the slice of the library a tab is built from, if it is one.
 */
enum class LibraryTab(val labelRes: Int, val shelf: LibraryShelfKind? = null) {
    All(R.string.library_tab_all),
    Local(R.string.library_tab_local, LibraryShelfKind.Local),
    Cached(R.string.cached, LibraryShelfKind.Cached),
    Imported(R.string.library_tab_imported),
}

private val ChipGap = 8.dp
private val ChipRowGap = 8.dp

/**
 * The tabs as separate capsules, each with its own rim - the same look as the Home chips (Semua / Lagu / Playlist / Album):
 * the picked one frosted silver with dark text, the others calm glass with the silver rim. No track or pill wraps them.
 */
@Composable
fun LibraryTabChips(
    selected: LibraryTab,
    onSelect: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    // with a big font or display size the capsules that don't fit wrap to a row below
    Layout(
        modifier = modifier,
        content = {
            LibraryTab.entries.forEach { tab -> LibraryTabChip(tab, isSelected = tab == selected, onSelect = onSelect) }
        },
    ) { measurables, constraints ->
        val gap = ChipGap.roundToPx()
        val rowGap = ChipRowGap.roundToPx()
        val room = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity
        val chips = measurables.map { it.measure(Constraints(maxWidth = room)) }

        // a capsule stays on the current row while it fits, otherwise it starts the next one
        val rows = mutableListOf(mutableListOf<Placeable>())
        var rowWidth = 0
        chips.forEach { chip ->
            val row = rows.last()
            val widthWithChip = if (row.isEmpty()) chip.width else rowWidth + gap + chip.width
            if (row.isNotEmpty() && widthWithChip > room) {
                rows += mutableListOf(chip)
                rowWidth = chip.width
            } else {
                row += chip
                rowWidth = widthWithChip
            }
        }

        val rowHeights = rows.map { row -> row.maxOf { it.height } }
        val width = rows.maxOf { row -> row.sumOf { it.width } + gap * (row.size - 1) }.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = rowHeights.sum() + rowGap * (rows.size - 1)

        layout(width, height) {
            var y = 0
            rows.forEachIndexed { index, row ->
                var x = 0
                row.forEach { chip ->
                    chip.placeRelative(x, y + (rowHeights[index] - chip.height) / 2)
                    x += chip.width + gap
                }
                y += rowHeights[index] + rowGap
            }
        }
    }
}

@Composable
private fun LibraryTabChip(tab: LibraryTab, isSelected: Boolean, onSelect: (LibraryTab) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 36.dp)
            .then(
                if (isSelected) {
                    Modifier.clip(CircleShape).background(LalasiTokens.FrostedSilver)
                } else {
                    Modifier.calmGlassSurface(CircleShape)
                }
            )
            .clickable(
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelect(tab) },
            )
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        Text(
            text = stringResource(tab.labelRes),
            color = if (isSelected) Color.Black else LalasiInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** "ALL MUSIC", "PLAYLISTS": a small caps label over a group of cards. */
@Composable
fun LibraryCapsLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = SearchColors.Primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        modifier = modifier.padding(horizontal = SearchHorizontalPadding),
    )
}

/** The small centred capsule (silver rim, like the Home chips) at the bottom of a tab that opens its Manage sheet. */
@Composable
fun LibraryManagePill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .calmGlassSurface(CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 34.dp)
                .padding(horizontal = 20.dp, vertical = 6.dp),
        ) {
            Text(
                text = text,
                color = LalasiInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** One card in a [LibraryCardGrid]. */
@Immutable
class LibraryCardEntry(
    val title: String,
    val count: Int,
    val covers: List<String?>,
    val onClick: () -> Unit,
    val roundCovers: Boolean = false,
    val icon: (@Composable () -> Unit)? = null,
)

/** Cards two to a row, the last one keeping its half width when it is alone. */
@Composable
fun LibraryCardGrid(entries: List<LibraryCardEntry>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = SearchHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { entry ->
                    LibraryCategoryCard(
                        title = entry.title,
                        count = entry.count,
                        covers = entry.covers,
                        onClick = entry.onClick,
                        icon = entry.icon,
                        roundCovers = entry.roundCovers,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
