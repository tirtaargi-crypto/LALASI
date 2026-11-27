// ProfileScreen.kt
//
// The LALASI profile page, in the plain layout most apps use: the avatar, the name, an "Edit profil"
// button, a row of small stats, and a list of menu rows (statistics, settings, version, switch to guest).
// There is no ID card, no barcode and no scratch-off any more.

package com.danrey.lalasi.ui.screens.settings

import com.danrey.lalasi.ui.theme.LalasiBlack

import com.danrey.lalasi.ui.theme.LalasiInk
import com.danrey.lalasi.ui.theme.lalasiAdapt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.danrey.lalasi.BuildConfig
import com.danrey.lalasi.LocalDatabase
import com.danrey.lalasi.R
import com.danrey.lalasi.constants.InnerTubeCookieKey
import com.danrey.lalasi.constants.ProfilePicUriKey
import com.danrey.lalasi.constants.UsernameKey
import com.danrey.lalasi.ui.component.LocalZoomOutOverlayState
import com.danrey.lalasi.ui.screens.profile.ProfileStore
import com.danrey.lalasi.ui.screens.profile.showEditProfileSheet
import com.danrey.lalasi.ui.screens.update.showAboutSheet
import com.danrey.lalasi.ui.screens.update.showUpdateSheet
import com.danrey.lalasi.ui.screens.update.rememberUpdateState
import com.danrey.lalasi.ui.theme.InterFontFamily
import com.danrey.lalasi.ui.utils.stableSystemBars
import com.danrey.lalasi.utils.rememberPreference
import com.danrey.lalasi.viewmodels.UserProfileViewModel
import kotlinx.coroutines.launch

private val PageTitle get() = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 23.sp,
    letterSpacing = (-0.6).sp,
    color = LalasiInk,
)

private val SurfaceTop get() = lalasiAdapt(0xFF1B1F25)
private val SurfaceBottom get() = lalasiAdapt(0xFF101216)
private val Hairline get() = LalasiInk.copy(alpha = 0.09f)

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val popup = LocalZoomOutOverlayState.current
    val updateState by rememberUpdateState()
    val statusBarTop = WindowInsets.stableSystemBars.asPaddingValues().calculateTopPadding()

    val (localUsername) = rememberPreference(UsernameKey, "")
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val (profilePicUri) = rememberPreference(ProfilePicUriKey, "")
    val profileViewModel: UserProfileViewModel = hiltViewModel()

    // a signed-in YouTube account can lend its name when no local name was ever set
    var liveAccountName by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(innerTubeCookie) {
        if (innerTubeCookie.isBlank()) {
            liveAccountName = null
            return@LaunchedEffect
        }
        com.music.innertube.YouTube.cookie = innerTubeCookie
        com.music.innertube.YouTube.accountInfo().onSuccess { liveAccountName = it.name }
    }
    val accountName = localUsername.ifBlank { liveAccountName?.takeIf { it.isNotBlank() } ?: ProfileStore.GuestName }
    val isGuest = ProfileStore.isGuest(localUsername, profilePicUri)
    val photo: Any? = profilePicUri.takeIf { it.isNotBlank() }?.let { if (it.contains("://")) it else "file://$it" }

    val totalListeningMs by database.totalListeningTimeMs().collectAsState(initial = 0L)
    val likedCount by profileViewModel.likedCount.collectAsState()
    val playlists by profileViewModel.playlists.collectAsState()

    val hasUpdate = updateState is com.danrey.lalasi.core.updater.UpdateState.Available
    val openDevSpace: (Int) -> Unit = { personIndex ->
        popup.dismissAll()
        navController.navigate("developer_space?person=$personIndex")
    }
    val openEditor: () -> Unit = { popup.showEditProfileSheet(currentName = accountName, currentPhoto = photo) }

    // "Ganti ke Akun Tamu": asks first, then only the name and the picture go back to the guest ones
    var showGuestDialog by remember { mutableStateOf(false) }
    if (showGuestDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showGuestDialog = false },
            title = { Text("Ganti ke Akun Tamu") },
            text = {
                Text(
                    if (isGuest) "Kamu sudah memakai sesi tamu."
                    else "Kamu akan beralih ke sesi tamu. Nama dan foto profil kembali ke bawaan. Koleksi, playlist, antrean, dan pemutaran musik tetap aman.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showGuestDialog = false
                        if (!isGuest) scope.launch { ProfileStore.resetToGuest(context) }
                    },
                ) { Text(if (isGuest) "Oke" else "Ganti") }
            },
            dismissButton = if (isGuest) {
                null
            } else {
                @Composable { androidx.compose.material3.TextButton(onClick = { showGuestDialog = false }) { Text("Batal") } }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lalasiAdapt(0xFF090B0E), LalasiBlack))),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(statusBarTop + 28.dp))
            Text(
                "Profil",
                style = PageTitle.copy(fontSize = 26.sp),
                modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
            )
            Spacer(Modifier.height(26.dp))

            // avatar, with a small pencil badge: tapping either opens the editor
            Box(modifier = Modifier.size(112.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(104.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(lalasiAdapt(0xFF303236))
                        .border(1.dp, LalasiInk.copy(alpha = 0.14f), CircleShape)
                        .clickable(onClick = openEditor),
                ) {
                    if (photo != null) {
                        AsyncImage(
                            model = photo,
                            contentDescription = "Foto profil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = "Foto profil",
                            tint = lalasiAdapt(0xFFE5E5E5),
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(lalasiAdapt(0xFFE9E9E9))
                        .clickable(onClick = openEditor),
                ) {
                    Icon(painterResource(R.drawable.edit), contentDescription = "Edit profil", tint = lalasiAdapt(0xFF1A1A1A), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                accountName,
                style = PageTitle.copy(fontSize = 24.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isGuest) "AKUN TAMU" else "PROFIL LOKAL",
                style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.2.sp, color = Color(0xFF9A9A9A)),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Edit profil",
                style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LalasiInk),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(lalasiAdapt(0xFF2E3136))
                    .clickable(onClick = openEditor)
                    .padding(horizontal = 26.dp, vertical = 10.dp),
            )
            Spacer(Modifier.height(26.dp))

            // stats
            val totalMinutes = (totalListeningMs / 60_000L).coerceAtLeast(0L)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("${totalMinutes / 60}j ${totalMinutes % 60}m", "Waktu dengar", Modifier.weight(1f))
                StatTile(likedCount.toString(), "Disukai", Modifier.weight(1f))
                StatTile((playlists?.size ?: 0).toString(), "Playlist", Modifier.weight(1f))
            }
            Spacer(Modifier.height(22.dp))

            // menu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(SurfaceTop, SurfaceBottom)))
                    .border(1.dp, Hairline, RoundedCornerShape(22.dp)),
            ) {
                MenuRow(R.drawable.stats, "Recap", onClick = { navController.navigate("stats") })
                MenuDivider()
                MenuRow(R.drawable.settings, "Pengaturan", onClick = { navController.navigate("lalasi_settings") })
                MenuDivider()
                MenuRow(
                    icon = R.drawable.info,
                    title = "Versi & tentang aplikasi",
                    trailing = if (hasUpdate) "Update tersedia" else BuildConfig.VERSION_NAME.substringBefore(' '),
                    trailingColor = if (hasUpdate) Color(0xFF88FF76) else Color(0xFF8A8A8A),
                    onClick = {
                        if (hasUpdate) popup.showUpdateSheet(updateState, onOpenDevSpace = openDevSpace)
                        else popup.showAboutSheet(onOpenDevSpace = openDevSpace)
                    },
                )
                MenuDivider()
                MenuRow(R.drawable.logout, "Ganti ke Akun Tamu", onClick = { showGuestDialog = true })
            }
            // room to scroll the last row clear of the mini player and the nav bar
            Spacer(Modifier.height(180.dp))
        }
    }
}

/** A big figure's size in dp: it follows the card's width, not the font-size setting, so it never overflows. */
@Composable
internal fun figureSp(dp: Float): TextUnit = with(LocalDensity.current) { dp.dp.toSp() }

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(SurfaceTop, SurfaceBottom)))
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .padding(vertical = 16.dp, horizontal = 6.dp),
    ) {
        Text(
            value,
            style = PageTitle.copy(fontSize = 18.sp, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Color(0xFF8A8A8A), textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MenuRow(
    icon: Int,
    title: String,
    onClick: () -> Unit,
    trailing: String? = null,
    trailingColor: Color = Color(0xFF8A8A8A),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(34.dp).clip(CircleShape).background(lalasiAdapt(0xFF2B2F35)),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = lalasiAdapt(0xFFE5E5E5), modifier = Modifier.size(18.dp))
        }
        Text(
            title,
            style = PageTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 14.dp),
        )
        if (trailing != null) {
            Text(
                trailing,
                style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = trailingColor),
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Icon(
            painterResource(R.drawable.navigate_next),
            contentDescription = null,
            tint = LalasiInk.copy(alpha = 0.35f),
            modifier = Modifier.padding(start = 6.dp).size(20.dp),
        )
    }
}

@Composable
private fun MenuDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(1.dp).background(Hairline))
}
