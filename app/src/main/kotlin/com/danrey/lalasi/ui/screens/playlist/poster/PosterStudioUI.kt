package com.danrey.lalasi.ui.screens.playlist.poster

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FilterFrames
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.danrey.lalasi.db.entities.PlaylistSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val Accent = Color.White
private val Ink = Color(0xFF101012)
private val Panel = Color(0xFF1A1A1F)

private val tabIcons: Map<StudioTab, ImageVector> = mapOf(
    StudioTab.BACKGROUND to Icons.Rounded.Wallpaper, StudioTab.MUSIC to Icons.Rounded.MusicNote, StudioTab.OBJECTS to Icons.Rounded.Face,
    StudioTab.EFFECTS to Icons.Rounded.AutoAwesome, StudioTab.STICKERS to Icons.Rounded.EmojiEmotions, StudioTab.TEXT to Icons.Rounded.TextFields,
    StudioTab.LAYERS to Icons.Rounded.Layers,
)

private enum class Drag { NONE, MOVE, SCALE, DELETE, DUPLICATE, BG }

/** Entry composable. Playlist -> Edit Playlist -> Poster Studio opens this full-screen dialog. */
@Composable
fun PosterStudioScreen(songs: List<PlaylistSong>, playlistName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val infos = remember(songs) {
        songs.associate {
            val s = it.song.song
            s.id to SongInfo(s.id, s.title, it.song.artists.joinToString { a -> a.name }, s.duration, s.thumbnailUrl)
        }
    }
    val st = remember { StudioState() }
    var started by remember { mutableStateOf(false) }
    var askClose by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { st.doc }.collectLatest { d -> if (started) { delay(1200); withContext(Dispatchers.IO) { PosterStore.saveDraft(context, d, playlistName) } } }
    }

    Dialog(
        onDismissRequest = { if (started) askClose = true else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Ink).systemBarsPadding().imePadding()) {
            if (!started) StartScreen(st, songs, infos, playlistName, onDismiss) { started = true }
            else Studio(st, infos, playlistName, onClose = { askClose = true })
            st.busy?.let {
                Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = Accent); Spacer(Modifier.height(10.dp)); Text(it, color = Color.White) }
                }
            }
            st.message?.let { msg ->
                LaunchedEffect(msg) { delay(2600); if (st.message == msg) st.message = null }
                Box(Modifier.align(Alignment.TopCenter).padding(top = 64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xEE2A2A30)).padding(horizontal = 16.dp, vertical = 10.dp)) { Text(msg, color = Accent) }
            }
        }
    }

    if (askClose) AlertDialog(
        onDismissRequest = { askClose = false },
        title = { Text("Tutup Poster Studio?") },
        confirmButton = { TextButton(onClick = { PosterStore.saveDraft(context, st.doc, playlistName); onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Simpan & tutup") } },
        dismissButton = { Row { TextButton(onClick = { askClose = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Lanjut edit") } } },
    )
}

// ------------------------------------------------------------------------------------------------ start screen

@Composable
private fun StartScreen(st: StudioState, songs: List<PlaylistSong>, infos: Map<String, SongInfo>, playlistName: String, onDismiss: () -> Unit, onOpen: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selected = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var search by remember { mutableStateOf("") }
    var bg by remember { mutableStateOf<String?>(null) }
    var ratio by remember { mutableIntStateOf(0) }
    var preset by remember { mutableIntStateOf(-1) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch { st.busy = "Mengimpor foto"; bg = withContext(Dispatchers.IO) { PosterStore.importImage(context, uri, 3072)?.absolutePath }; st.busy = null; if (bg == null) st.message = "Gambar tidak bisa dibuka" }
    }
    val filtered = remember(songs, search) { songs.filter { it.song.song.title.contains(search, true) || it.song.artists.any { a -> a.name.contains(search, true) } } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Tutup", tint = Color.White) }
            Text("Poster", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${selected.size}/5", color = Color.LightGray, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 12.dp))
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            item {
                Text("Latar", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(84.dp, 110.dp).clip(RoundedCornerShape(10.dp)).background(Panel), contentAlignment = Alignment.Center) {
                        if (bg != null) AsyncImage(File(bg!!), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Icon(Icons.Rounded.Image, null, tint = Color.Gray)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(if (bg == null) "Pilih dari galeri" else "Ganti foto") }
                        if (bg != null) GhostButton(onClick = { bg = null }) { Text("Pakai warna polos") }
                    }
                }
                Text("Rasio", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatioDefs.forEach { def -> StudioChip(def.label, ratio == def.id) { ratio = def.id } }
                }
                Text("Preset", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioChip("Kosong", preset == -1) { preset = -1 }
                    PresetNames.forEachIndexed { i, n -> StudioChip(n, preset == i) { preset = i } }
                }
                Text("Lagu", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Cari lagu") }, singleLine = true)
            }
            items(filtered, key = { it.song.song.id }) { item ->
                val id = item.song.song.id; val checked = id in selected
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { if (checked) selected.remove(id) else if (selected.size < 5) selected.add(id) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(item.song.song.thumbnailUrl, null, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(item.song.song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(item.song.artists.joinToString { it.name }, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    }
                    Checkbox(checked = checked, onCheckedChange = { v -> if (v && selected.size < 5) selected.add(id) else selected.remove(id) })
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(modifier = Modifier.fillMaxWidth().height(52.dp), onClick = {
                var d = PosterDoc(ratio = ratio, bgUri = bg, bgScale = if (bg != null) 1.15f else 1f)
                selected.forEach { id -> infos[id]?.let { d = PosterPresets.newPlayer(d, it).first } }
                if (preset >= 0) d = PosterPresets.apply(d, preset, playlistName)
                st.reset(d); st.tab = StudioTab.OBJECTS; onOpen()
            }) { Text("Lanjutkan") }
            if (PosterStore.hasDraft(context)) TextButton(modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = Color.White), onClick = {
                PosterStore.loadDraft(context)?.let { st.reset(it); st.tab = StudioTab.LAYERS; onOpen() } ?: run { st.message = "Draf tidak bisa dibaca" }
            }) { Text("Lanjutkan draf", color = Color.White) }
        }
    }
}

// ------------------------------------------------------------------------------------------------ studio

@Composable
private fun Studio(st: StudioState, infos: Map<String, SongInfo>, playlistName: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExport by remember { mutableStateOf(false) }
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    var previewSize by remember { mutableStateOf(IntArray(2)) }
    val boxesState = remember { mutableStateOf<Map<String, FloatArray>>(emptyMap()) }
    val masksState = remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    // the cache grows with the phone's memory, so photos and objects are not decoded again on every frame; the preview needs smaller sources than the export
    val assets = remember(infos) { PosterAssets(context, infos, (Runtime.getRuntime().maxMemory() * 0.22).toLong().coerceIn(56L shl 20, 128L shl 20).toInt(), srcMax = 1280) }

    val pickBg = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            st.busy = "Mengimpor foto"
            val f = withContext(Dispatchers.IO) { PosterStore.importImage(context, uri, 3072) }
            st.busy = null
            if (f != null) st.commit { it.copy(bgUri = f.absolutePath, bgScale = 1.15f, bgX = 0f, bgY = 0f, bgCutUri = null, bgPop = false) } else st.message = "Gambar tidak bisa dibuka"
        }
    }
    val pickChar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            st.busy = "Mengimpor foto"
            val f = withContext(Dispatchers.IO) { PosterStore.importImage(context, uri, 2560) }
            st.busy = null
            if (f != null) { st.addLayer(PosterPresets.newCharacter(st.doc, f.absolutePath)); st.tab = StudioTab.OBJECTS } else st.message = "Gambar tidak bisa dibuka"
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            st.busy = "Mengimpor foto"
            val f = withContext(Dispatchers.IO) { PosterStore.importImage(context, uri, 2560) }
            st.busy = null
            if (f != null) st.addLayer(PosterPresets.newImage(st.doc, f.absolutePath)) else st.message = "Gambar tidak bisa dibuka"
        }
    }
    val popTried = remember { HashSet<String>() }
    var popWorking by remember { mutableStateOf(false) }
    /** Makes sure the photo's subject has been cut out (once per photo). Runs by itself when a photo appears and never switches depth back on after the user turned it off. */
    fun touchBackground(force: Boolean = false) {
        val d = st.doc; val uri = d.bgUri ?: return
        if (!force && d.bgCutUri?.let { File(it).exists() } == true) return
        if (popWorking || (!force && !popTried.add(uri))) return
        popWorking = true
        scope.launch {
            val r = withContext(Dispatchers.Default) { makeBackgroundCutout(context, uri) }
            popWorking = false
            r.onSuccess { path ->
                st.patch { cur -> if (cur.bgUri == uri) cur.copy(bgCutUri = path, bgPop = true) else cur }
                if (force) st.message = "Depth aktif"
            }.onFailure { st.message = (it.message ?: "Subjek tidak terdeteksi") }
        }
    }
    // depth is automatic: as soon as a photo is in the poster its subject is found, nothing to tap
    val bgUriNow by remember { androidx.compose.runtime.derivedStateOf { st.doc.bgUri } } // only changes with the photo, so doc edits don't recompose here
    LaunchedEffect(bgUriNow) { touchBackground() }
    val launchImage = { l: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest> -> l.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Column(Modifier.fillMaxSize()) {
        // ---- top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Tutup", tint = Color.White) }
            IconButton(enabled = st.undoCount > 0, onClick = { st.undo() }) { Icon(Icons.AutoMirrored.Rounded.Undo, "Urungkan", tint = if (st.undoCount > 0) Color.White else Color.DarkGray) }
            IconButton(enabled = st.redoCount > 0, onClick = { st.redo() }) { Icon(Icons.AutoMirrored.Rounded.Redo, "Ulangi", tint = if (st.redoCount > 0) Color.White else Color.DarkGray) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { PosterStore.saveDraft(context, st.doc, playlistName); st.message = "Draf tersimpan" }) { Icon(Icons.Rounded.Save, "Simpan draf", tint = Color.White) }
            PillButton(onClick = { st.select(null); showExport = true }, contentPadding = PaddingValues(horizontal = 14.dp)) { Icon(Icons.Rounded.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Ekspor") }
            Spacer(Modifier.width(6.dp))
        }

        // ---- canvas
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            val availW = constraints.maxWidth.toFloat(); val availH = constraints.maxHeight.toFloat()
            val aspect = st.doc.aspect
            val pw = min(availW, availH / aspect); val ph = pw * aspect
            val densityObj = LocalDensity.current
            val dens = densityObj.density
            val pwDp = with(densityObj) { pw.toDp() }; val phDp = with(densityObj) { ph.toDp() }
            val renderW = pw.roundToInt().coerceIn(360, 1000)
            val renderH = (renderW * aspect).roundToInt()
            val buffers = remember(renderW, renderH) { Array(3) { Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888) } }
            // while a finger is down the poster is drawn at ~55% size (about 3x fewer pixels); full quality returns the moment it lifts
            val lowW = (renderW * .5f).roundToInt().coerceAtLeast(240); val lowH = (lowW * aspect).roundToInt()
            val lowBuffers = remember(lowW, lowH) { Array(3) { Bitmap.createBitmap(lowW, lowH, Bitmap.Config.ARGB_8888) } }

            LaunchedEffect(buffers, lowBuffers) {
                var turn = 0; var lowTurn = 0
                // conflate: a render already running is never thrown away, so frames keep appearing while the finger is still moving
                snapshotFlow { Pair(st.preview ?: st.doc, st.preview != null || st.dragging) }.conflate().collect { (d, fast) ->
                    val rw = if (fast) lowW else renderW; val rh = if (fast) lowH else renderH
                    val out = withContext(Dispatchers.Default) {
                        assets.prepare(d, rw, rh)
                        val buf = if (fast) lowBuffers[lowTurn++ % 3] else buffers[turn++ % 3]
                        val r = PosterRenderer.render(buf, d, assets, false) { ensureActive() }
                        // hit-test masks only change when the document really changes, not while dragging
                        val masks = if (fast) null else HashMap<String, Bitmap>().also { m ->
                            d.layers.forEach { if (it.type == LayerType.CHARACTER && it.visible) assets.alphaMask(it, rw)?.let { mk -> m[it.id] = mk } }
                        }
                        // boxes as fractions of the poster so low- and full-resolution frames can be mixed freely
                        val boxes = r.boxes.mapValues { floatArrayOf(it.value[0] / rw, it.value[1] / rh) }
                        Triple(buf, boxes, masks)
                    }
                    previewSize = intArrayOf(rw, rh)
                    boxesState.value = out.second; out.third?.let { masksState.value = it }
                    frame = out.first.asImageBitmap()
                }
            }

            var zoom by remember { mutableFloatStateOf(1f) }
            var pan by remember { mutableStateOf(Offset.Zero) }
            val tabNow by rememberUpdatedState(st.tab)

            fun toPoster(p: Offset) = Offset((p.x - availW / 2 - pan.x) / zoom + pw / 2, (p.y - availH / 2 - pan.y) / zoom + ph / 2)

            fun boxPx(id: String): Pair<Float, Float>? {
                val b = boxesState.value[id] ?: return null
                return (b[0] * pw) to (b[1] * ph)
            }

            fun corner(l: Layer, bw: Float, bh: Float, sx: Float, sy: Float): Offset {
                val a = l.rotation * PI.toFloat() / 180f
                val dx = sx * bw / 2; val dy = sy * bh / 2
                return Offset(l.x * pw + dx * cos(a) - dy * sin(a), l.y * ph + dx * sin(a) + dy * cos(a))
            }

            fun hit(p: Offset): Layer? {
                val n = toPoster(p)
                for (l in st.view.renderOrder().asReversed()) {
                    if (!l.visible || l.locked) continue
                    if (l.type == LayerType.EFFECT || l.type == LayerType.FRAME || l.type == LayerType.OVERLAY) continue
                    val (bw, bh) = boxPx(l.id) ?: continue
                    val a = -l.rotation * PI.toFloat() / 180f
                    val dx = n.x - l.x * pw; val dy = n.y - l.y * ph
                    val lx = dx * cos(a) - dy * sin(a); val ly = dx * sin(a) + dy * cos(a)
                    if (kotlin.math.abs(lx) > bw / 2 || kotlin.math.abs(ly) > bh / 2) continue
                    if (l.type == LayerType.CHARACTER) {
                        val m = masksState.value[l.id]
                        if (m != null) {
                            var u = (lx / bw + .5f); var v = (ly / bh + .5f)
                            if (l.flipH) u = 1 - u; if (l.flipV) v = 1 - v
                            val px = (u * (m.width - 1)).roundToInt().coerceIn(0, m.width - 1); val py = (v * (m.height - 1)).roundToInt().coerceIn(0, m.height - 1)
                            if ((m.getPixel(px, py) ushr 24) < 24) continue
                        }
                    }
                    return l
                }
                return null
            }

            Box(
                Modifier.fillMaxSize().pointerInput(buffers) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val slop = viewConfiguration.touchSlop
                        val t0 = System.currentTimeMillis()
                        var mode = Drag.NONE; var target: String? = null; var moved = false; var total = 0f
                        val r = 34f * dens / zoom
                        val sel = st.layer
                        // 1) handles of the current selection
                        if (sel != null && sel.visible && !sel.locked) boxPx(sel.id)?.let { (bw, bh) ->
                            val p = toPoster(down.position)
                            if ((p - corner(sel, bw, bh, -1f, -1f)).getDistance() < r) { mode = Drag.DELETE; target = sel.id }
                            else if ((p - corner(sel, bw, bh, 1f, -1f)).getDistance() < r) { mode = Drag.DUPLICATE; target = sel.id }
                            else if ((p - corner(sel, bw, bh, 1f, 1f)).getDistance() < r) { mode = Drag.SCALE; target = sel.id }
                        }
                        // 2) an object under the finger
                        if (mode == Drag.NONE) {
                            val h = hit(down.position)
                            if (h != null) { target = h.id; mode = Drag.MOVE; if (st.selected != h.id) st.select(h.id) }
                            else if (sel != null && !sel.locked && (sel.type == LayerType.EFFECT || sel.type == LayerType.FRAME || sel.type == LayerType.OVERLAY) && st.selected == sel.id) { target = sel.id; mode = Drag.MOVE }
                            else if (!st.doc.bgLocked && st.doc.bgUri != null && (tabNow == StudioTab.BACKGROUND || zoom <= 1.01f)) mode = Drag.BG
                            else if (tabNow == StudioTab.BACKGROUND && !st.doc.bgLocked) mode = Drag.BG
                        }
                        var last = down.position
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.count { it.pressed }
                            if (event.changes.size >= 2 && pressed >= 2) {
                                val z = event.calculateZoom(); val rot = event.calculateRotation(); val pn = event.calculatePan()
                                total += 100f; moved = true
                                val id = target
                                if (id != null && mode != Drag.DELETE && mode != Drag.DUPLICATE) {
                                    st.gEdit(id) { l -> followPlayerSide(l, l.copy(size = (l.size * z).coerceIn(.02f, 3f), rotation = l.rotation + rot, x = l.x + pn.x / zoom / pw, y = l.y + pn.y / zoom / ph)) }
                                } else if (mode == Drag.BG && tabNow == StudioTab.BACKGROUND) {
                                    st.gLive { d -> d.copy(bgScale = (d.bgScale * z).coerceIn(1f, 5f), bgX = d.bgX + pn.x / zoom / pw, bgY = d.bgY + pn.y / zoom / ph) }
                                } else {
                                    zoom = (zoom * z).coerceIn(1f, 4f); pan = if (zoom <= 1.01f) Offset.Zero else pan + pn
                                }
                                event.changes.forEach { it.consume() }
                            } else {
                                val c = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
                                val d = c.position - last; last = c.position
                                total += d.getDistance()
                                if (total > slop) moved = true
                                if (moved && total > slop) {
                                    val id = target
                                    when (mode) {
                                        Drag.MOVE -> if (id != null) st.gEdit(id) { l -> followPlayerSide(l, l.copy(x = (l.x + d.x / zoom / pw).coerceIn(-.3f, 1.3f), y = (l.y + d.y / zoom / ph).coerceIn(-.3f, 1.3f))) }
                                        Drag.SCALE -> if (id != null) st.view.layer(id)?.let { l ->
                                            val cp = Offset(l.x * pw, l.y * ph)
                                            val pNew = toPoster(c.position); val pOld = toPoster(c.position - d)
                                            val vN = pNew - cp; val vO = pOld - cp
                                            if (vO.getDistance() > 1f) {
                                                val f = vN.getDistance() / vO.getDistance()
                                                val dr = (atan2(vN.y, vN.x) - atan2(vO.y, vO.x)) * 180f / PI.toFloat()
                                                st.gEdit(id) { q -> q.copy(size = (q.size * f).coerceIn(.02f, 3f), rotation = q.rotation + dr) }
                                            }
                                        }
                                        Drag.BG -> st.gLive { s -> s.copy(bgX = s.bgX + d.x / zoom / pw, bgY = s.bgY + d.y / zoom / ph) }
                                        else -> if (zoom > 1.01f && mode == Drag.NONE) pan += d
                                    }
                                    c.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        st.endTx()
                        val tap = !moved && System.currentTimeMillis() - t0 < 500
                                        // Depth is a direct canvas interaction: tap or move an existing subject.
                        // No extra character, mode switch, or depth settings screen is required.
                        if ((tap || moved) && mode == Drag.MOVE) {
                            target?.let { id ->
                                val subject = st.doc.layer(id)
                                if (subject != null &&
                                    (subject.type == LayerType.CHARACTER || subject.type == LayerType.IMAGE)
                                ) {
                                    // Treat the tapped/dragged subject as a real foreground cut-out:
                                    // apply a shape-following shadow and lift it above player cards.
                                    // This is deliberately automatic; no separate Depth tool is needed.
                                    st.commit { doc ->
                                        val lifted = doc.update(id) { current ->
                                            if (current.depthStyle == 0) quickDepth(current, outline = false)
                                            else current.copy(shadowOn = true, shadowMode = 1)
                                        }
                                        lifted.toFront(id)
                                    }
                                }
                            }
                        }
                        if (tap) {
                            when (mode) {
                                Drag.DELETE -> target?.let { id -> st.commit { it.remove(id) }; st.select(null) }
                                Drag.DUPLICATE -> target?.let { id ->
                                    val l = st.doc.layer(id)
                                    if (l?.type == LayerType.PLAYER && st.doc.playerCount >= 5) st.message = "Maksimal 5 lagu"
                                    else { val (nd, c) = st.doc.duplicate(id); st.commit { nd }; if (c != null) st.select(c.id) }
                                }
                                Drag.MOVE -> Unit
                                Drag.NONE -> st.select(null)
                                else -> Unit
                            }
                        }
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(pwDp, phDp).graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y }.clip(RoundedCornerShape(2.dp)).background(Color(0xFF222226))) {
                    frame?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
                    Canvas(Modifier.fillMaxSize()) {
                        val sel = st.view.layer(st.selected) ?: return@Canvas
                        if (!sel.visible) return@Canvas
                        val (bw, bh) = boxPx(sel.id) ?: return@Canvas
                        val s = dens / zoom
                        rotate(sel.rotation, Offset(sel.x * pw, sel.y * ph)) {
                            drawRect(Color.White, Offset(sel.x * pw - bw / 2, sel.y * ph - bh / 2), Size(bw, bh), style = Stroke(1.6f * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * s, 8f * s))))
                        }
                        if (sel.locked) return@Canvas
                        val rr = 17f * s
                        fun handle(c: Offset, fill: Color, draw: (Offset) -> Unit) { drawCircle(fill, rr, c); drawCircle(Color.Black.copy(alpha = .35f), rr, c, style = Stroke(1.2f * s)); draw(c) }
                        val ink = Stroke(2f * s, cap = StrokeCap.Round)
                        handle(corner(sel, bw, bh, -1f, -1f), Color(0xFFE53935)) { c -> drawLine(Color.White, c - Offset(5f * s, 5f * s), c + Offset(5f * s, 5f * s), 2f * s, StrokeCap.Round); drawLine(Color.White, c + Offset(-5f * s, 5f * s), c + Offset(5f * s, -5f * s), 2f * s, StrokeCap.Round) }
                        handle(corner(sel, bw, bh, 1f, -1f), Color(0xFF3C5BD6)) { c ->
                            drawRect(Color.White, c + Offset(-6f * s, -3f * s), Size(8f * s, 8f * s), style = ink); drawRect(Color.White, c + Offset(-2f * s, -7f * s), Size(8f * s, 8f * s), style = ink)
                        }
                        handle(corner(sel, bw, bh, 1f, 1f), Color.White) { c ->
                            val p = Path().apply { moveTo(c.x - 5f * s, c.y + 5f * s); lineTo(c.x + 5f * s, c.y - 5f * s); moveTo(c.x + 5f * s, c.y - 5f * s); lineTo(c.x + 1f * s, c.y - 5f * s); moveTo(c.x + 5f * s, c.y - 5f * s); lineTo(c.x + 5f * s, c.y - 1f * s) }
                            drawPath(p, Color.Black, style = ink)
                        }
                    }
                }
                if (popWorking) Box(Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xAA000000)).padding(horizontal = 10.dp, vertical = 5.dp)) { Text("Mendeteksi subjek…", color = Accent, style = MaterialTheme.typography.labelSmall) }
                if (zoom > 1.01f) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xAA000000)).clickable { zoom = 1f; pan = Offset.Zero }.padding(horizontal = 10.dp, vertical = 5.dp)) { Text("${(zoom * 100).roundToInt()}%  reset", color = Color.White, style = MaterialTheme.typography.labelSmall) }
            }
        }

        // ---- panel + bottom navigation
        Box(Modifier.fillMaxWidth().heightIn(max = 270.dp).background(Panel)) {
            StudioPanel(st, infos, playlistName, assets, onDetectBg = { touchBackground(force = true) }, onPickBg = { launchImage(pickBg) }, onPickChar = { launchImage(pickChar) }, onPickImage = { launchImage(pickImage) })
        }
        // floating dock: every tool on screen at once; the active one lifts up on a silver key
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp).dockSurface().padding(horizontal = 4.dp)) {
            StudioTab.entries.forEach { t ->
                DockItem(
                    icon = tabIcons.getValue(t), label = t.label, selected = st.tab == t && !st.inspecting, modifier = Modifier.weight(1f),
                    onClick = { st.tab = t; st.inspecting = false; if (t != StudioTab.LAYERS && st.selected != BG) st.selected = if (t == StudioTab.BACKGROUND) BG else null },
                )
            }
        }
    }

    if (showExport) ExportDialog(st, infos, frame, onDismiss = { showExport = false })
}

private fun Modifier.widthIn72(): Modifier = this.then(Modifier.width(72.dp))

// ------------------------------------------------------------------------------------------------ export

@Composable
private fun ExportDialog(st: StudioState, infos: Map<String, SongInfo>, preview: ImageBitmap?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var png by remember { mutableStateOf(true) }
    var res by remember { mutableIntStateOf(1) }
    var custom by remember { mutableFloatStateOf(1600f) }
    var transparent by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<Uri?>(null) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val width = when (res) { 0 -> 1080; 1 -> 2160; else -> custom.roundToInt() }
    val (ew, eh) = PosterRenderer.sizeFor(st.doc, width)

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text("Pratinjau & ekspor") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().heightIn(max = 300.dp), contentAlignment = Alignment.Center) {
                    preview?.let { Image(it, null, Modifier.heightIn(max = 300.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Fit) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioChip("PNG", png) { png = true }
                    StudioChip("JPEG", !png) { png = false; transparent = false }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioChip("Standar", res == 0) { res = 0 }
                    StudioChip("Tinggi", res == 1) { res = 1 }
                    StudioChip("Kustom", res == 2) { res = 2 }
                }
                if (res == 2) Slider(value = custom, onValueChange = { custom = it }, valueRange = 720f..3600f)
                Text("$ew × $eh px", style = MaterialTheme.typography.labelMedium)
                if (png) Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = transparent, onCheckedChange = { transparent = it }, colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color(0xFF111114), checkedTrackColor = Color.White, uncheckedTrackColor = Color(0x33FFFFFF), uncheckedBorderColor = Color(0x44FFFFFF))); Spacer(Modifier.width(8.dp)); Text("Transparan") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                saved?.let { u ->
                    Text("Tersimpan di Pictures/LALASI", color = Accent)
                    GhostButton(onClick = {
                        runCatching {
                            val send = Intent(Intent.ACTION_SEND).apply { type = if (png) "image/png" else "image/jpeg"; putExtra(Intent.EXTRA_STREAM, u); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            context.startActivity(Intent.createChooser(send, "Bagikan poster").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }) { Icon(Icons.Rounded.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Bagikan") }
                }
            }
        },
        confirmButton = {
            PillButton(enabled = !working, onClick = {
                working = true; error = null; saved = null
                // Commit any in-flight gesture before taking the immutable export snapshot.
                st.endTx()
                val doc = st.view
                scope.launch {
                    val result = withContext(Dispatchers.Default) {
                        runCatching {
                            // export uses its own assets + full resolution, never the preview buffers
                            val assets = PosterAssets(context, infos, 120 * 1024 * 1024)
                            assets.prepare(doc, ew, eh)
                            val bmp = Bitmap.createBitmap(ew, eh, Bitmap.Config.ARGB_8888)
                            PosterRenderer.render(bmp, doc, assets, transparent && png)
                            val uri = PosterStore.export(context, bmp, png)
                            bmp.recycle(); uri
                        }
                    }
                    result.onSuccess { saved = it }.onFailure { error = "Ekspor gagal: ${it.message ?: "out of memory, try a lower resolution"}" }
                    working = false
                }
            }) { Text(if (working) "Rendering…" else "Simpan") }
        },
        dismissButton = { TextButton(enabled = !working, onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Tutup") } },
    )
}
