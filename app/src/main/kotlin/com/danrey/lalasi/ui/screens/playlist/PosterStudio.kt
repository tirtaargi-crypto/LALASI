package com.danrey.lalasi.ui.screens.playlist

import androidx.compose.runtime.Composable
import com.danrey.lalasi.db.entities.PlaylistSong
import com.danrey.lalasi.ui.screens.playlist.poster.PosterStudioScreen

/**
 * Playlist -> Edit Playlist -> Poster Studio.
 * The implementation lives in the `poster` package (model, renderer, effects, panels); this stays the single entry point
 * so LocalPlaylistScreen and the playlist menu keep working unchanged.
 */
@Composable
fun PosterStudioDialog(
    songs: List<PlaylistSong>,
    playlistName: String,
    onDismiss: () -> Unit,
) {
    PosterStudioScreen(songs = songs, playlistName = playlistName, onDismiss = onDismiss)
}
