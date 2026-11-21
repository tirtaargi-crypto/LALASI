package com.danrey.lalasi.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.danrey.lalasi.R
import com.danrey.lalasi.ui.component.SettingsGroup
import com.danrey.lalasi.ui.component.SettingsGroupStyle
import com.danrey.lalasi.ui.component.SettingsHighlight
import com.danrey.lalasi.ui.component.SettingsItem
import com.danrey.lalasi.ui.component.SubSettingsScaffold
import com.danrey.lalasi.ui.component.bleedHorizontally
import com.danrey.lalasi.ui.screens.gesture.LyricsStyleSection

// settings > lyrics: its own page, apart from Customize. The style picker lives here; the lyric switches stay where they work (App behaviour) and are one tap away.
@Composable
fun LyricsSettingsScreen(navController: NavController) {
    val chevron = painterResource(R.drawable.navigate_next)

    @Composable
    fun link(key: String, titleRes: Int, icon: Int) = SettingsItem(
        icon = painterResource(icon),
        title = { Text(stringResource(titleRes)) },
        highlightKey = "lyrics-link-$key",
        searchText = stringResource(titleRes),
        onClick = {
            SettingsHighlight.pending = key
            navController.navigate("general_settings")
        },
        trailingContent = {
            Icon(painter = chevron, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(20.dp))
        },
    )

    SubSettingsScaffold(
        title = stringResource(R.string.lalasi_set_lyrics),
        onBack = { navController.navigateUp() },
    ) {
        LyricsStyleSection(modifier = Modifier.bleedHorizontally(16.dp))
        Spacer(Modifier.height(22.dp))
        SettingsGroup(
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                link("High quality bloom", R.string.appearance_bloom, R.drawable.lyrics),
                link("Translations from YouTube", R.string.settings_youtube_translations, R.drawable.translate),
                link("Sync lyrics to music videos", R.string.settings_sync_lyrics_video, R.drawable.sync),
            ),
        )
        Spacer(Modifier.height(140.dp))
    }
}
