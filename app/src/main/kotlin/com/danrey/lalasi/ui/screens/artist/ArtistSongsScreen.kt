// ArtistSongsScreen.kt

package com.danrey.lalasi.ui.screens.artist

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.danrey.lalasi.LocalPlayerAwareWindowInsets
import com.danrey.lalasi.LocalPlayerConnection
import com.danrey.lalasi.R
import com.danrey.lalasi.constants.ArtistSongSortDescendingKey
import com.danrey.lalasi.constants.ArtistSongSortType
import com.danrey.lalasi.constants.ArtistSongSortTypeKey
import com.danrey.lalasi.extensions.toMediaItem
import com.danrey.lalasi.playback.queues.ListQueue
import com.danrey.lalasi.ui.component.LocalMenuState
import com.danrey.lalasi.ui.component.SongSwipeBox
import com.danrey.lalasi.ui.component.detail.CreatorUi
import com.danrey.lalasi.ui.component.detail.PlaylistDetailScaffold
import com.danrey.lalasi.ui.component.detail.PlaylistEndOfLine
import com.danrey.lalasi.ui.component.detail.PlaylistHeader
import com.danrey.lalasi.ui.component.detail.PlaylistSortControl
import com.danrey.lalasi.ui.component.detail.PlaylistTrackRow
import com.danrey.lalasi.ui.component.librarySwipeAction
import com.danrey.lalasi.ui.component.queueSwipeAction
import com.danrey.lalasi.ui.component.rememberRevealSeenState
import com.danrey.lalasi.ui.component.revealOnAppear
import com.danrey.lalasi.ui.menu.SongMenu
import com.danrey.lalasi.ui.utils.backToMain
import com.danrey.lalasi.utils.makeTimeString
import com.danrey.lalasi.utils.rememberEnumPreference
import com.danrey.lalasi.utils.rememberPreference
import com.danrey.lalasi.viewmodels.ArtistSongsViewModel

/** the artist's songs in your library, as a playlist on the artist's banner */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSongsScreen(
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistSongsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(ArtistSongSortTypeKey, ArtistSongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(ArtistSongSortDescendingKey, true)

    val artist by viewModel.artist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val lazyListState = rememberLazyListState()
    val revealSeen = rememberRevealSeenState()

    val artistId = artist?.artist?.id
    val (cachedName, bannerUrl) = rememberArtistBanner(artistId)
    val name = artist?.artist?.name ?: cachedName.orEmpty()
    val title = if (name.isNotBlank()) "$name in your library" else stringResource(R.string.songs)
    val totalSeconds = remember(songs) { songs.sumOf { it.song.duration } }
    val isThisQueue = queueTitle == title

    fun play(startIndex: Int = 0, shuffled: Boolean = false) {
        if (songs.isEmpty()) return
        val list = if (shuffled) songs.shuffled() else songs
        playerConnection.playQueue(ListQueue(title = title, items = list.map { it.toMediaItem() }, startIndex = startIndex))
    }

    PlaylistDetailScaffold(
        sharedElementKey = null,
        coverUrl = bannerUrl ?: songs.firstOrNull()?.song?.thumbnailUrl,
        lazyListState = lazyListState,
        onBackClick = { navController.navigateUp() },
        onBackLongClick = { navController.backToMain() },
        contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).asPaddingValues(),
    ) {
        item(key = "header", contentType = "header") {
            PlaylistHeader(
                title = title,
                creators = listOfNotNull(
                    name.takeIf { it.isNotBlank() }?.let { CreatorUi(name = it, avatarUrl = artist?.artist?.thumbnailUrl, artistId = artistId) }
                ),
                stats = com.danrey.lalasi.ui.screens.playlist.generatedStats(songs.size, totalSeconds),
                isPlaying = isThisQueue && isPlaying,
                onPlayClick = { if (isThisQueue) playerConnection.togglePlayPause() else play() },
                onShuffleClick = { play(shuffled = true) },
                onCreatorClick = { creator -> creator.artistId?.let { navController.navigate("artist/$it") } },
                sortControl = {
                    PlaylistSortControl(
                        current = sortType,
                        descending = sortDescending,
                        options = ArtistSongSortType.entries,
                        label = { type ->
                            stringResource(
                                when (type) {
                                    ArtistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                    ArtistSongSortType.NAME -> R.string.sort_by_name
                                    ArtistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                }
                            )
                        },
                        onSelect = onSortTypeChange,
                        onToggleDirection = { onSortDescendingChange(!sortDescending) },
                    )
                },
            )
        }

        itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> "track" }) { index, song ->
            SongSwipeBox(
                modifier = Modifier.animateItem(),
                library = { librarySwipeAction(song.song) },
                queue = { queueSwipeAction { song.toMediaItem() } },
            ) {
                PlaylistTrackRow(
                    thumbnailUrl = song.song.thumbnailUrl,
                    songId = song.id,
                    title = song.song.title,
                    subtitle = listOfNotNull(song.album?.title, makeTimeString(song.song.duration * 1000L)).joinToString(" • "),
                    isActive = song.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    modifier = Modifier.revealOnAppear(key = "song_${song.id}", seenState = revealSeen, delayMillis = minOf(index, 8) * 28),
                    onClick = {
                        if (song.id == mediaMetadata?.id) playerConnection.togglePlayPause() else play(startIndex = index)
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss)
                        }
                    },
                    onMenuClick = {
                        menuState.show {
                            SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss)
                        }
                    },
                )
            }
        }

        if (songs.isNotEmpty()) {
            item(key = "end_of_line") {
                PlaylistEndOfLine(songCount = songs.size, totalSeconds = totalSeconds)
            }
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(50.dp))
        }
    }
}
