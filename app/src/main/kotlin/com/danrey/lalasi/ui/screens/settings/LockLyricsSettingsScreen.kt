package com.danrey.lalasi.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.danrey.lalasi.R
import com.danrey.lalasi.constants.LockLyricsNextLineKey
import com.danrey.lalasi.constants.LockLyricsPositionKey
import com.danrey.lalasi.constants.LockLyricsSizeKey
import com.danrey.lalasi.constants.LockLyricsTitleKey
import com.danrey.lalasi.constants.LockScreenLyricsKey
import com.danrey.lalasi.i18n.activityOrNull
import com.danrey.lalasi.playback.LockLyricsBus
import com.danrey.lalasi.ui.component.AppSwitch
import com.danrey.lalasi.ui.component.SettingsGroup
import com.danrey.lalasi.ui.component.SettingsGroupStyle
import com.danrey.lalasi.ui.component.SettingsItem
import com.danrey.lalasi.ui.component.SubSettingsScaffold
import com.danrey.lalasi.ui.theme.InterFontFamily
import com.danrey.lalasi.utils.rememberPreference

const val LockLyricsSettingsRoute = "lock_lyrics_settings"

// settings > customise > Lock Screen Lyrics
@Composable
fun LockLyricsSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val (enabled, setEnabled) = rememberPreference(LockScreenLyricsKey, defaultValue = false)
    val (position, setPosition) = rememberPreference(LockLyricsPositionKey, defaultValue = 0)
    val (nextLine, setNextLine) = rememberPreference(LockLyricsNextLineKey, defaultValue = true)
    val (songInfo, setSongInfo) = rememberPreference(LockLyricsTitleKey, defaultValue = true)
    val (size, setSize) = rememberPreference(LockLyricsSizeKey, defaultValue = 1)

    // accessibility access is granted on a system page, so it is read again each time this page is resumed
    var resumeTick by remember { mutableIntStateOf(0) }
    val owner = remember(context) { context.activityOrNull() as? LifecycleOwner }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumeTick++ }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    val accessGranted = resumeTick >= 0 && LockLyricsBus.isAccessGranted(context)
    val openAccess = { runCatching { context.startActivity(LockLyricsBus.accessSettingsIntent()) }; Unit }

    val chevron = painterResource(R.drawable.navigate_next)
    val check = painterResource(R.drawable.check)
    val positionNames = listOf(R.string.lock_lyrics_pos_top, R.string.lock_lyrics_pos_middle, R.string.lock_lyrics_pos_bottom)
    val sizeNames = listOf(R.string.lock_lyrics_size_small, R.string.lock_lyrics_size_medium, R.string.lock_lyrics_size_large)

    SubSettingsScaffold(
        title = stringResource(R.string.lock_lyrics_title),
        onBack = { navController.navigateUp() },
    ) {
        SettingsGroup(
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                lockLyricsToggle(
                    R.drawable.lyrics, stringResource(R.string.lock_lyrics_title), stringResource(R.string.lock_lyrics_enable_desc),
                    enabled, { on -> setEnabled(on); if (on && !accessGranted) openAccess() },
                ),
            ),
        )
        Spacer(Modifier.height(18.dp))

        SettingsGroup(
            title = stringResource(R.string.lock_lyrics_access_section),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                SettingsItem(
                    icon = painterResource(R.drawable.lock),
                    title = { Text(stringResource(R.string.lock_lyrics_access)) },
                    descriptionText = stringResource(if (accessGranted) R.string.lock_lyrics_access_on else R.string.lock_lyrics_access_off),
                    iconShape = CircleShape,
                    onClick = { openAccess() },
                    trailingContent = { Icon(chevron, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(20.dp)) },
                ),
            ),
        )
        Spacer(Modifier.height(18.dp))

        SettingsGroup(
            title = stringResource(R.string.lock_lyrics_position),
            style = SettingsGroupStyle.Grouped,
            items = positionNames.mapIndexed { index, res ->
                SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(res)) },
                    iconShape = CircleShape,
                    enabled = enabled,
                    onClick = { setPosition(index) },
                    trailingContent = {
                        if (position == index) Icon(check, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
                    },
                )
            },
        )
        Spacer(Modifier.height(18.dp))

        SettingsGroup(
            title = stringResource(R.string.lock_lyrics_size),
            style = SettingsGroupStyle.Grouped,
            items = sizeNames.mapIndexed { index, res ->
                SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(res)) },
                    iconShape = CircleShape,
                    enabled = enabled,
                    onClick = { setSize(index) },
                    trailingContent = {
                        if (size == index) Icon(check, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
                    },
                )
            },
        )
        Spacer(Modifier.height(18.dp))

        SettingsGroup(
            title = stringResource(R.string.lock_lyrics_display),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                lockLyricsToggle(R.drawable.lyrics, stringResource(R.string.lock_lyrics_next), stringResource(R.string.lock_lyrics_next_desc), nextLine, setNextLine, enabled),
                lockLyricsToggle(R.drawable.album, stringResource(R.string.lock_lyrics_song_info), stringResource(R.string.lock_lyrics_song_info_desc), songInfo, setSongInfo, enabled),
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.lock_lyrics_footer),
            fontFamily = InterFontFamily,
            fontSize = 13.sp,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun lockLyricsToggle(
    icon: Int,
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) = SettingsItem(
    icon = painterResource(icon),
    title = { Text(title) },
    highlightKey = title,
    searchText = title,
    descriptionText = description,
    iconShape = CircleShape,
    enabled = enabled,
    onClick = { onChange(!checked) },
    trailingContent = { AppSwitch(checked = checked, onCheckedChange = onChange, enabled = enabled) },
)
