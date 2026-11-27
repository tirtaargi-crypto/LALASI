package com.danrey.lalasi.ui.screens.playlist.poster

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.FlipToFront
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

private val Accent = Color.White

// ------------------------------------------------------------------------------------------------ small building blocks

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Box(Modifier.size(width = 3.dp, height = 12.dp).clip(RoundedCornerShape(2.dp)).background(Accent))
            Spacer(Modifier.width(7.dp))
            Text(title.uppercase(), color = Accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
        content()
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, st: StudioState, onChange: (Float) -> Unit) {
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { st.dragging = false } }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.LightGray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(92.dp))
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = { v -> st.dragging = true; onChange(v) }, onValueChangeFinished = { st.endTx() }, valueRange = range, modifier = Modifier.weight(1f).height(32.dp), colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Color(0x33FFFFFF)))
    }
}

@Composable
private fun LayerSlider(label: String, st: StudioState, l: Layer, range: ClosedFloatingPointRange<Float>, get: (Layer) -> Float, set: (Layer, Float) -> Layer) {
    LabeledSlider(label, get(l), range, st) { v -> st.edit(l.id) { set(it, v) } }
}

@Composable
private fun ColorRow(selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SwatchColors.forEach { c ->
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color(c)).border(if (c == selected) 3.dp else 1.dp, if (c == selected) Accent else Color(0x66FFFFFF), CircleShape).clickable { onPick(c) })
        }
    }
}

@Composable
private fun ChipRow(items: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, s -> StudioChip(s, i == selected) { onPick(i) } }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = onChange, colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color(0xFF0A0F1A), checkedTrackColor = Accent, uncheckedTrackColor = Color(0x33FFFFFF), uncheckedBorderColor = Color(0x44FFFFFF)))
    }
}

private val effectThumbs = ConcurrentHashMap<String, ImageBitmap>()

@Composable
private fun EffectThumb(key: String) {
    val img by produceState<ImageBitmap?>(effectThumbs[key], key) {
        if (value == null) value = withContext(Dispatchers.Default) { PosterRenderer.effectPreview(key, 96, 128).asImageBitmap().also { effectThumbs[key] = it } }
    }
    Box(Modifier.size(64.dp, 84.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF333338))) { img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }
}

@Composable
private fun StickerThumb(def: StickerDef) {
    Canvas(Modifier.size(58.dp)) {
        val s = size.width * .88f
        val (w, h) = if (def.aspect <= 1f) s to s * def.aspect else s / def.aspect to s
        drawIntoCanvas { c ->
            c.nativeCanvas.save(); c.nativeCanvas.translate((size.width - w) / 2, (size.height - h) / 2)
            PosterVectors.drawSticker(c.nativeCanvas, def.key, w, h, def.color, def.color2); c.nativeCanvas.restore()
        }
    }
}

@Composable
private fun FrameThumb(def: FrameDef, overlay: Boolean) {
    Canvas(Modifier.size(54.dp, 72.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF55555C))) {
        drawIntoCanvas { c ->
            if (!overlay) PosterVectors.drawFrame(c.nativeCanvas, def.key, size.width, size.height)
            else {
                val l = Layer(id = "t", type = LayerType.OVERLAY, name = def.key, kind = def.key, color = android.graphics.Color.WHITE, color2 = 0xFF111111.toInt(), intensity = .7f)
                if (def.key == "blur" || def.key == "light") PosterEffects.drawOverlay(c.nativeCanvas, "light", size.width, size.height, l) else PosterEffects.drawOverlay(c.nativeCanvas, def.key, size.width, size.height, l)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ router

@Composable
fun StudioPanel(st: StudioState, infos: Map<String, SongInfo>, playlistName: String, assets: PosterAssets, onDetectBg: () -> Unit = {}, onPickBg: () -> Unit, onPickChar: () -> Unit, onPickImage: () -> Unit) {
    val l = st.layer
    if (st.inspecting && l != null) { Inspector(st, l, assets); return }
    when (st.tab) {
        StudioTab.BACKGROUND -> BackgroundPanel(st, onPickBg, onDetectBg)
        StudioTab.MUSIC -> MusicPanel(st, infos)
        StudioTab.OBJECTS -> ObjectsPanel(st, onPickChar, onPickImage)
        StudioTab.EFFECTS -> EffectsPanel(st)
        StudioTab.STICKERS -> StickersPanel(st)
        StudioTab.TEXT -> TextPanel(st)
        StudioTab.LAYERS -> LayersPanel(st)
    }
}

// ------------------------------------------------------------------------------------------------ background

@Composable
private fun BackgroundPanel(st: StudioState, onPickBg: () -> Unit, onDetectBg: () -> Unit) {
    val d = st.doc
    var refining by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
        Section("Foto") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                PillButton(onClick = onPickBg) { Icon(Icons.Rounded.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (d.bgUri == null) "Pilih dari galeri" else "Ganti foto") }
                if (d.bgUri != null) GhostButton(onClick = { st.commit { it.copy(bgUri = null, bgCutUri = null, bgPop = false) } }) { Text("Hapus") }
                GhostButton(onClick = { st.commit { it.copy(bgScale = 1f, bgX = 0f, bgY = 0f) } }) { Text("Reset posisi") }
                GhostButton(onClick = { st.commit { it.copy(brightness = 0f, contrast = 1f, saturation = 1f, blur = 0f, exposure = 0f, overlayOpacity = 0f) } }) { Text("Reset tampilan") }
            }
        }
        // the photo can also be moved and resized with sliders: left-right, up-down and size
        if (d.bgUri != null) Section("Geser & ukuran latar") {
            val reach = 0.25f + 0.5f * (d.bgScale - 1f)
            LabeledSlider("Kiri – kanan", d.bgX.coerceIn(-reach, reach), -reach..reach, st) { v -> st.live { it.copy(bgX = v) } }
            LabeledSlider("Atas – bawah", d.bgY.coerceIn(-reach, reach), -reach..reach, st) { v -> st.live { it.copy(bgY = v) } }
            LabeledSlider("Ukuran", d.bgScale, 1f..5f, st) { v -> st.live { it.copy(bgScale = v) } }
        }
        if (d.bgUri != null) Section("Kedalaman (subjek di depan)") {
            val hasCut = d.bgCutUri != null
            SwitchRow("Subjek menonjol", d.bgPop && hasCut) { v -> if (hasCut) st.commit { it.copy(bgPop = v) } else onDetectBg() }
            if (hasCut && d.bgPop) LabeledSlider("Kedalaman", d.bgPopIntensity, 0f..1f, st) { v -> st.live { it.copy(bgPopIntensity = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                GhostButton(onClick = onDetectBg) { Text(if (hasCut) "Deteksi ulang" else "Deteksi subjek") }
                if (hasCut) GhostButton(onClick = { refining = true }) { Text("Rapikan dengan kuas") }
            }
            if (refining && d.bgCutUri != null && d.bgUri != null) CutoutEditor(d.bgCutUri, d.bgUri, onDismiss = { refining = false }, onSave = { path -> st.commit { it.copy(bgCutUri = path, bgPop = true) }; refining = false })
        }
        if (d.bgUri != null) Section("Efek latar") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                item {
                    val on = d.bgFx.isBlank()
                    Column(Modifier.clip(RoundedCornerShape(10.dp)).clickable { st.commit { it.copy(bgFx = "") } }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(64.dp, 84.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF333338)).border(if (on) 2.dp else 0.dp, Accent, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("Tidak ada", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                        Text("Mati", color = if (on) Accent else Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
                items(BgFxKeys, key = { it }) { key ->
                    val def = effectDef(key) ?: return@items
                    val on = d.bgFx == key
                    Column(Modifier.clip(RoundedCornerShape(10.dp)).clickable { st.commit { it.copy(bgFx = key, bgFxAmount = def.intensity) } }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).border(if (on) 2.dp else 0.dp, Accent, RoundedCornerShape(8.dp))) { EffectThumb(key) }
                        Text(def.label, color = if (on) Accent else Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.width(64.dp), overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (d.bgFx.isNotBlank()) LabeledSlider("Jumlah", d.bgFxAmount, 0f..1f, st) { v -> st.live { it.copy(bgFxAmount = v) } }
        }
        Section("Rasio") {
            val ratioIds = RatioDefs.map { it.id } + RATIO_CUSTOM
            ChipRow(RatioDefs.map { it.label } + "Kustom", ratioIds.indexOf(d.ratio).coerceAtLeast(0)) { i -> st.commit { it.copy(ratio = ratioIds[i]) } }
            if (d.ratio == RATIO_CUSTOM) {
                LabeledSlider("Lebar", d.customW, 1f..16f, st) { v -> st.live { it.copy(customW = v.toInt().toFloat()) } }
                LabeledSlider("Tinggi", d.customH, 1f..16f, st) { v -> st.live { it.copy(customH = v.toInt().toFloat()) } }
            }
        }
        if (d.bgUri == null) Section("Warna") { ColorRow(d.bgColor) { c -> st.commit { it.copy(bgColor = c) } } }
        Section("Penyesuaian foto") {
            LabeledSlider("Zoom", d.bgScale, 1f..4f, st) { v -> st.live { it.copy(bgScale = v) } }
            LabeledSlider("Kecerahan", d.brightness, -1f..1f, st) { v -> st.live { it.copy(brightness = v) } }
            LabeledSlider("Kontras", d.contrast, .5f..1.8f, st) { v -> st.live { it.copy(contrast = v) } }
            LabeledSlider("Saturasi", d.saturation, 0f..2f, st) { v -> st.live { it.copy(saturation = v) } }
            LabeledSlider("Eksposur", d.exposure, -1f..1f, st) { v -> st.live { it.copy(exposure = v) } }
            LabeledSlider("Blur", d.blur, 0f..1f, st) { v -> st.live { it.copy(blur = v) } }
        }
        Section("Lapisan warna") {
            ColorRow(d.overlayColor) { c -> st.commit { it.copy(overlayColor = c, overlayOpacity = max2(it.overlayOpacity, .25f)) } }
            LabeledSlider("Opasitas", d.overlayOpacity, 0f..1f, st) { v -> st.live { it.copy(overlayOpacity = v) } }
        }
    }
}

private val BgFxKeys = listOf("soft_glow", "bloom", "lens_flare", "light_leak", "sparkles", "bokeh", "fog", "grain", "vintage_fade", "monochrome", "warm_tone", "cool_tone")

private fun max2(a: Float, b: Float) = if (a > b) a else b

// ------------------------------------------------------------------------------------------------ music

@Composable
private fun MusicPanel(st: StudioState, infos: Map<String, SongInfo>) {
    var search by remember { mutableStateOf("") }
    val list = remember(infos, search) { infos.values.filter { it.title.contains(search, true) || it.artist.contains(search, true) } }
    val count = st.doc.playerCount
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Cari playlist") })
            Text("  $count/5", color = Accent, fontWeight = FontWeight.Bold)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 10.dp)) {
            items(list, key = { it.id }) { s ->
                val existing = st.doc.layers.firstOrNull { it.type == LayerType.PLAYER && it.songId == s.id }
                fun toggle() {
                    if (existing != null) { st.commit { it.remove(existing.id) }; if (st.selected == existing.id) st.select(null) }
                    else if (count >= 5) st.message = "Maksimal 5 lagu"
                    else st.addLayer(PosterPresets.newPlayer(st.doc, s, st.doc.layers.firstOrNull { it.type == LayerType.PLAYER }?.playerStyle ?: 0))
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { toggle() }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    coil3.compose.AsyncImage(s.thumb, null, Modifier.size(42.dp).clip(RoundedCornerShape(7.dp)), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(s.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(s.artist, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    }
                    if (existing != null) TextButton(onClick = { st.select(existing.id) }) { Text("Ubah", color = Accent) }
                    Checkbox(checked = existing != null, onCheckedChange = { toggle() })
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ objects

@Composable
private fun ObjectsPanel(st: StudioState, onPickChar: () -> Unit, onPickImage: () -> Unit) {
    val items = st.doc.layers.filter { it.type == LayerType.CHARACTER || it.type == LayerType.IMAGE || it.type == LayerType.PLAYER }.asReversed()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
        Section("Tambah") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                PillButton(onClick = onPickChar) { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Karakter / objek") }
                GhostButton(onClick = onPickImage) { Text("Foto tambahan") }
            }
        }
        Section("Di poster") {
            if (items.isEmpty()) Text("Belum ada karakter.", color = Color.Gray)
            items.forEach { l ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { st.select(l.id) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(l.name, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(l.type.name.lowercase(), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ effects

@Composable
private fun EffectsPanel(st: StudioState) {
    var group by remember { mutableIntStateOf(0) }
    var frames by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(top = 6.dp)) {
        Box(Modifier.padding(horizontal = 14.dp)) { ChipRow(listOf("Efek", "Bingkai & lapisan"), if (frames) 1 else 0) { frames = it == 1 } }
        if (frames) { FramesPanel(st); return }
        Box(Modifier.padding(horizontal = 14.dp)) { ChipRow(EffectGroups, group) { group = it } }
        LazyRow(contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(EffectCatalog.filter { it.group == EffectGroups[group] }, key = { it.key }) { def ->
                Column(Modifier.clip(RoundedCornerShape(10.dp)).clickable { st.addLayer(PosterPresets.newEffect(st.doc, def)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    EffectThumb(def.key)
                    Text(def.label, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.width(64.dp), overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ stickers

@Composable
private fun StickersPanel(st: StudioState) {
    var cat by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(top = 6.dp)) {
        Box(Modifier.padding(horizontal = 14.dp)) { ChipRow(StickerCategories.map { it.name }, cat) { cat = it } }
        LazyRow(contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(StickerCategories[cat].items, key = { it.key }) { def ->
                Column(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF2B2B31)).clickable { st.addLayer(PosterPresets.newSticker(st.doc, def)) }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    StickerThumb(def)
                    Text(def.label, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.width(62.dp), overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ text

@Composable
private fun TextPanel(st: StudioState) {
    val texts = st.doc.layers.filter { it.type == LayerType.TEXT }.asReversed()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
        Section("Tambah teks") {
            PillButton(onClick = { st.addLayer(PosterPresets.newText(st.doc, "Teks kamu", .5f, .15f, .1f) { it.copy(font = 12, shadowOn = true, shadowOpacity = .4f) }) }) { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Teks baru") }
        }
        Section("Preset teks") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextPresets.forEach { p -> FilterChip(selected = false, onClick = { st.addLayer(PosterPresets.newText(st.doc, "Teks kamu", .5f, .15f, .1f) { l -> p.transform(l.copy(font = 12)) }) }, label = { Text(p.name) }) }
            }
        }
        if (texts.isNotEmpty()) Section("Di poster") {
            texts.forEach { l -> Row(Modifier.fillMaxWidth().clickable { st.select(l.id) }.padding(vertical = 6.dp)) { Text(l.text.replace("\n", " "), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        }
    }
}

// ------------------------------------------------------------------------------------------------ frames + overlays

@Composable
private fun FramesPanel(st: StudioState) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
        Section("Bingkai & tepi") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                items(FrameCatalog, key = { it.key }) { def ->
                    Column(Modifier.clip(RoundedCornerShape(8.dp)).clickable { st.addLayer(PosterPresets.newFrame(st.doc, def, false)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        FrameThumb(def, false); Text(def.label, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.width(60.dp), overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Section("Lapisan dekorasi") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                items(OverlayCatalog, key = { it.key }) { def ->
                    Column(Modifier.clip(RoundedCornerShape(8.dp)).clickable { st.addLayer(PosterPresets.newFrame(st.doc, def, true)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        FrameThumb(def, true); Text(def.label, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.width(60.dp), overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ layers

@Composable
private fun LayersPanel(st: StudioState) {
    var renaming by remember { mutableStateOf<Layer?>(null) }
    val list = st.doc.layers.asReversed()
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
            items(list, key = { it.id }) { l ->
                val sel = st.selected == l.id
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (sel) Color(0x33CFE0FF) else Color.Transparent).clickable { if (!l.isDepthMarker) st.select(l.id) }.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(l.name, color = if (sel) Accent else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(if (l.isDepthMarker) "Subjek foto · di atasnya menutupi" else layerKindName(l.type), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { st.commit { it.update(l.id) { q -> q.copy(visible = !q.visible) } } }) { Icon(if (l.visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, "Tampil/sembunyi", tint = if (l.visible) Color.White else Color.Gray) }
                    if (!l.isDepthMarker) IconButton(onClick = { st.commit { it.update(l.id) { q -> q.copy(locked = !q.locked) } } }) { Icon(if (l.locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, "Kunci", tint = if (l.locked) Accent else Color.Gray) }
                    IconButton(onClick = { st.commit { it.move(l.id, 1) } }) { Icon(Icons.Rounded.ArrowUpward, "Maju", tint = Color.White) }
                    IconButton(onClick = { st.commit { it.move(l.id, -1) } }) { Icon(Icons.Rounded.ArrowDownward, "Mundur", tint = Color.White) }
                }
            }
            item {
                val sel = st.selected == BG
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (sel) Color(0x33CFE0FF) else Color.Transparent).clickable { st.selected = BG; st.tab = StudioTab.BACKGROUND; st.inspecting = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Latar", color = if (sel) Accent else Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { st.commit { it.copy(bgLocked = !it.bgLocked) } }) { Icon(if (st.doc.bgLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, "Kunci", tint = if (st.doc.bgLocked) Accent else Color.Gray) }
                }
            }
        }
        val l = st.layer
        if (l != null) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            LayerActions(st, l) { renaming = l }
        }
    }
    renaming?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(onDismissRequest = { renaming = null }, title = { Text("Ganti nama lapisan") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it.take(32) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { st.commit { it.update(target.id) { q -> q.copy(name = text.ifBlank { q.name }) } }; renaming = null }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Batal") } })
    }
}

private fun layerKindName(t: LayerType) = when (t) {
    LayerType.PLAYER -> "Pemutar musik"; LayerType.CHARACTER -> "Karakter"; LayerType.IMAGE -> "Gambar"; LayerType.STICKER -> "Stiker"
    LayerType.TEXT -> "Tipografi"; LayerType.EFFECT -> "Efek"; LayerType.FRAME -> "Bingkai"; LayerType.OVERLAY -> "Lapisan"
}

/** Action strip: the six things people actually reach for, big and always visible; the rest sit behind "Lainnya". */
@Composable
private fun LayerActions(st: StudioState, l: Layer, onRename: () -> Unit) {
    var more by remember { mutableStateOf(false) }
    @Composable fun act(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, tint: Color = Color.White, enabled: Boolean = true, onClick: () -> Unit) {
        ActionTile(icon, desc, onClick, active = tint == Accent, danger = tint == Color(0xFFFF6B6B), enabled = enabled)
    }
    act(Icons.Rounded.ContentCopy, "Salin", enabled = !(l.type == LayerType.PLAYER && st.doc.playerCount >= 5)) { val (nd, c) = st.doc.duplicate(l.id); st.commit { nd }; if (c != null) st.select(c.id) }
    act(Icons.Rounded.Delete, "Hapus", Color(0xFFFF6B6B)) { st.commit { it.remove(l.id) }; st.select(null) }
    act(Icons.Rounded.FlipToFront, "Ke depan") { st.commit { it.toFront(l.id) } }
    act(Icons.Rounded.FlipToBack, "Ke belakang") { st.commit { it.toBack(l.id) } }
    act(if (l.locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, if (l.locked) "Terkunci" else "Kunci", if (l.locked) Accent else Color.White) { st.set(l.id) { it.copy(locked = !it.locked) } }
    act(Icons.Rounded.Flip, "Balik", if (l.flipH) Accent else Color.White) { st.set(l.id) { it.copy(flipH = !it.flipH) } }
    act(if (more) Icons.Rounded.Close else Icons.Rounded.MoreHoriz, if (more) "Lebih sedikit" else "Lainnya") { more = !more }
    if (more) {
        act(Icons.Rounded.ArrowUpward, "Naik") { st.commit { it.move(l.id, 1) } }
        act(Icons.Rounded.ArrowDownward, "Turun") { st.commit { it.move(l.id, -1) } }
        act(Icons.Rounded.Flip, "Balik V", if (l.flipV) Accent else Color.White) { st.set(l.id) { it.copy(flipV = !it.flipV) } }
        act(if (l.visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, if (l.visible) "Sembunyikan" else "Tampilkan") { st.set(l.id) { it.copy(visible = !it.visible) } }
        act(Icons.Rounded.Edit, "Ganti nama") { onRename() }
        act(Icons.Rounded.RestartAlt, "Reset") { st.set(l.id) { resetLayer(it) } }
    }
}

// ------------------------------------------------------------------------------------------------ inspector (only the controls relevant to the selected object)

private fun subTabs(t: LayerType): List<String> = when (t) {
    LayerType.PLAYER -> listOf("Pemutar", "Ubah bentuk")
    LayerType.CHARACTER -> listOf("Potongan", "Ubah bentuk", "Bayangan", "Garis tepi")
    LayerType.IMAGE -> listOf("Bingkai", "Ubah bentuk", "Bayangan", "Garis tepi")
    LayerType.STICKER -> listOf("Ubah bentuk", "Warna", "Bayangan", "Garis tepi")
    LayerType.TEXT -> listOf("Teks", "Font", "Gaya", "Ubah bentuk", "Bayangan", "Garis tepi")
    LayerType.EFFECT -> listOf("Atur", "Ubah bentuk")
    LayerType.FRAME, LayerType.OVERLAY -> listOf("Atur", "Ubah bentuk")
}

@Composable
private fun Inspector(st: StudioState, l: Layer, assets: PosterAssets) {
    var sub by remember(l.id) { mutableIntStateOf(0) }
    var renaming by remember { mutableStateOf(false) }
    val tabs = subTabs(l.type)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(l.name, color = Accent, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(layerKindName(l.type) + if (l.locked) " · terkunci" else "", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { st.select(null) }) { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Selesai") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp)) { LayerActions(st, l) { renaming = true } }
        Box(Modifier.padding(horizontal = 14.dp)) { ChipRow(tabs, sub.coerceAtMost(tabs.lastIndex)) { sub = it } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 8.dp).padding(horizontal = 14.dp)) {
            when (tabs[sub.coerceAtMost(tabs.lastIndex)]) {
                "Pemutar" -> PlayerControls(st, l)
                "Ubah bentuk" -> TransformControls(st, l)
                "Bayangan" -> ShadowControls(st, l)
                "Garis tepi" -> OutlineControls(st, l)
                "Potongan" -> CutoutControls(st, l)
                "Bingkai" -> ImageFrameControls(st, l)
                "Warna" -> { ColorRow(l.color) { c -> st.set(l.id) { it.copy(color = c) } }; Text("Warna kedua", color = Color.LightGray, style = MaterialTheme.typography.labelSmall); ColorRow(l.color2) { c -> st.set(l.id) { it.copy(color2 = c) } } }
                "Teks" -> TextControls(st, l)
                "Font" -> FontControls(st, l)
                "Gaya" -> TextStyleControls(st, l)
                "Atur" -> AdjustControls(st, l)
            }
        }
    }
    if (renaming) {
        var text by remember(l.id) { mutableStateOf(l.name) }
        AlertDialog(onDismissRequest = { renaming = false }, title = { Text("Ganti nama lapisan") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it.take(32) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { st.commit { it.update(l.id) { q -> q.copy(name = text.ifBlank { q.name }) } }; renaming = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Batal") } })
    }
}

@Composable
private fun PlayerControls(st: StudioState, l: Layer) {
    Text("Gaya pemutar", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
    ChipRow(PlayerStyleNames, l.playerStyle) { i -> st.set(l.id) { it.copy(playerStyle = i) } }
    PoseChips(st, l)
    SwitchRow("Kilau cahaya di tepi", l.glint) { v -> st.set(l.id) { it.copy(glint = v) } }
    if (l.glint) {
        Text("Posisi cepat", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
        val quick = listOf(GLINT_TOP_RIGHT, GLINT_TOP_LEFT, GLINT_BOTTOM_RIGHT, GLINT_BOTTOM_LEFT)
        ChipRow(listOf("Kanan atas", "Kiri atas", "Kanan bawah", "Kiri bawah"), quick.indexOfFirst { kotlin.math.abs(it - l.glintPlace()) < .004f }) { i ->
            st.set(l.id) { it.copy(glintPos = quick[i]) }
        }
        LayerSlider("Posisi di sepanjang tepi", st, l, 0f..1f, { it.glintPlace() }) { q, v -> q.copy(glintPos = v) }
        LayerSlider("Kekuatan cahaya", st, l, .25f..1.8f, { it.glintGlow }) { q, v -> q.copy(glintGlow = v) }
        LayerSlider("Ukuran kilau", st, l, .65f..1.8f, { it.glintSize }) { q, v -> q.copy(glintSize = v) }
        LayerSlider("Panjang goresan", st, l, .4f..2f, { it.glintLen }) { q, v -> q.copy(glintLen = v) }
        LayerSlider("Warna cahaya (dingin–hangat)", st, l, -1f..1f, { it.glintTone }) { q, v -> q.copy(glintTone = v) }
    }
    LayerSlider("Ukuran sampul", st, l, .5f..1f, { it.artSize }) { q, v -> q.copy(artSize = v) }
    LayerSlider("Progres", st, l, 0f..1f, { it.progress }) { q, v -> q.copy(progress = v) }
}

/** One-tap 3D poses (real perspective turns). Also usable for characters / images, where the pose is what makes the object itself feel 3D. */
@Composable
private fun PoseChips(st: StudioState, l: Layer) {
    Text("Pose 3D", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
    ChipRow(listOf("Datar", "Miring kiri", "Miring kanan", "Melayang", "Meja", "Hero"), -1) { i ->
        st.set(l.id) {
            when (i) {
                0 -> it.copy(tiltX = 0f, tiltY = 0f, rotation = 0f)
                1 -> it.copy(tiltX = 4f, tiltY = -18f, rotation = -3f)
                2 -> it.copy(tiltX = 4f, tiltY = 18f, rotation = 3f)
                3 -> it.copy(tiltX = 12f, tiltY = -12f, rotation = -2f)
                4 -> it.copy(tiltX = 26f, tiltY = 0f, rotation = 0f)
                else -> it.copy(tiltX = -6f, tiltY = 14f, rotation = 2f)
            }
        }
    }
}

@Composable
private fun TransformControls(st: StudioState, l: Layer) {
    val sizeRange = when (l.type) { LayerType.PLAYER -> .12f..0.7f; LayerType.CHARACTER -> .1f..1.5f; LayerType.IMAGE -> .1f..1.4f; LayerType.STICKER -> .04f..1f; LayerType.TEXT -> .02f..0.35f; LayerType.EFFECT -> .1f..1.5f; else -> .3f..2f }
    LayerSlider("Ukuran", st, l, sizeRange, { it.size }) { q, v -> q.copy(size = v) }
    LayerSlider("Rotasi", st, l, -180f..180f, { it.rotation }) { q, v -> q.copy(rotation = v) }
    LayerSlider("Opasitas", st, l, 0f..1f, { it.opacity }) { q, v -> q.copy(opacity = v) }
    if (l.type == LayerType.PLAYER || l.type == LayerType.CHARACTER || l.type == LayerType.IMAGE || l.type == LayerType.STICKER) {
        if (l.type != LayerType.PLAYER) PoseChips(st, l)
        run {
            LayerSlider("Miring (Y)", st, l, -45f..45f, { it.tiltY }) { q, v -> q.copy(tiltY = v) }
            LayerSlider("Miring (X)", st, l, -45f..45f, { it.tiltX }) { q, v -> q.copy(tiltX = v) }
        }
    }
    if (l.type != LayerType.PLAYER && l.type != LayerType.CHARACTER) {
        Text("Mode campur", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
        ChipRow(BlendNames, l.blend) { i -> st.set(l.id) { it.copy(blend = i) } }
    }
}

@Composable
private fun ShadowControls(st: StudioState, l: Layer) {
    SwitchRow("Bayangan", l.shadowOn) { v -> st.set(l.id) { it.copy(shadowOn = v) } }
    if (!l.shadowOn) return
    ChipRow(ShadowModeNames, l.shadowMode) { i -> st.set(l.id) { it.copy(shadowMode = i) } }
    LayerSlider("Blur", st, l, 0f..1f, { it.shadowBlur }) { q, v -> q.copy(shadowBlur = v) }
    LayerSlider("Sebaran", st, l, 0f..1f, { it.shadowSpread }) { q, v -> q.copy(shadowSpread = v) }
    LayerSlider("Jarak", st, l, 0f..0.5f, { it.shadowDist }) { q, v -> q.copy(shadowDist = v) }
    LayerSlider("Sudut", st, l, 0f..360f, { it.shadowAngle }) { q, v -> q.copy(shadowAngle = v) }
    LayerSlider("Opasitas", st, l, 0f..1f, { it.shadowOpacity }) { q, v -> q.copy(shadowOpacity = v) }
    Text("Warna", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
    ColorRow(l.shadowColor) { c -> st.set(l.id) { it.copy(shadowColor = c) } }
}

@Composable
private fun OutlineControls(st: StudioState, l: Layer) {
    SwitchRow("Garis tepi / border stiker", l.outlineOn) { v -> st.set(l.id) { it.copy(outlineOn = v) } }
    if (!l.outlineOn) return
    LayerSlider("Ketebalan", st, l, .002f..0.15f, { it.outlineWidth }) { q, v -> q.copy(outlineWidth = v) }
    ColorRow(l.outlineColor) { c -> st.set(l.id) { it.copy(outlineColor = c) } }
}

@Composable
private fun ImageFrameControls(st: StudioState, l: Layer) {
    val keys = listOf("", "polaroid", "round", "film", "paper"); val names = listOf("None", "Polaroid", "Bulat", "Film", "Kertas")
    ChipRow(names, keys.indexOf(l.kind).coerceAtLeast(0)) { i -> st.set(l.id) { it.copy(kind = keys[i]) } }
}

// ---- character cutout: automatic person segmentation (on-device) + manual erase/restore brush

@Composable
private fun CutoutControls(st: StudioState, l: Layer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        PillButton(onClick = {
            scope.launch {
                st.busy = "Memotong subjek"
                val result = withContext(Dispatchers.Default) {
                    runCatching {
                        val bmp = decodeFile(l.srcUri ?: l.uri ?: error("Tidak ada gambar"), 1800) ?: error("Gambar tidak bisa dibuka")
                        val cut = com.danrey.lalasi.ui.screens.playlist.segmentSubject(bmp, context)
                        val f = PosterStore.saveBitmapPng(context, cut)
                        bmp.recycle(); cut.recycle(); f.absolutePath
                    }
                }
                st.busy = null
                result.onSuccess { path -> st.set(l.id) { it.copy(uri = path) }; st.message = "Potongan siap" }.onFailure { st.message = "Potongan gagal: ${it.message ?: "try the brush"}" }
            }
        }) { Text("Potong otomatis") }
        GhostButton(onClick = { editing = true }) { Text("Rapikan dengan kuas") }
        GhostButton(enabled = l.srcUri != null && l.srcUri != l.uri, onClick = { st.set(l.id) { it.copy(uri = it.srcUri) } }) { Text("Pakai asli") }
    }
    if (editing) CutoutEditor(l.uri ?: "", l.srcUri ?: l.uri ?: "", onDismiss = { editing = false }, onSave = { path -> st.set(l.id) { it.copy(uri = path) }; editing = false })
}

/** Cuts the subject out of the background photo; the PNG has the photo's aspect so it lines up with it exactly. */
internal suspend fun makeBackgroundCutout(context: android.content.Context, bgPath: String): Result<String> = runCatching {
    val bmp = decodeFile(bgPath, 1800) ?: error("Foto tidak dapat dibuka")
    val cut = com.danrey.lalasi.ui.screens.playlist.segmentSubject(bmp, context)
    var hit = 0; var tot = 0
    for (y in 0 until cut.height step 6) for (x in 0 until cut.width step 6) { tot++; if ((cut.getPixel(x, y) ushr 24) > 128) hit++ }
    val frac = if (tot == 0) 0f else hit.toFloat() / tot
    if (frac < .02f || frac > .8f) { cut.recycle(); bmp.recycle(); error("Subjek tidak terdeteksi dengan jelas pada foto ini") }
    val f = PosterStore.saveBitmapPng(context, cut, "bgcut")
    bmp.recycle(); cut.recycle(); f.absolutePath
}

internal fun decodeFile(path: String, maxDim: Int): Bitmap? = runCatching {
    val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeFile(path, b)
    var sample = 1; while (b.outWidth / (sample * 2) >= maxDim || b.outHeight / (sample * 2) >= maxDim) sample *= 2
    var bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }) ?: return null
    val big = maxOf(bmp.width, bmp.height)
    if (big > maxDim) { val s = maxDim.toFloat() / big; bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt().coerceAtLeast(1), (bmp.height * s).toInt().coerceAtLeast(1), true) }
    if (!bmp.isMutable || bmp.config != Bitmap.Config.ARGB_8888) bmp = bmp.copy(Bitmap.Config.ARGB_8888, true)
    bmp
}.getOrNull()

@Composable
private fun CutoutEditor(currentPath: String, originalPath: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var work by remember { mutableStateOf<Bitmap?>(null) }
    var orig by remember { mutableStateOf<Bitmap?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var erase by remember { mutableStateOf(true) }
    var brush by remember { androidx.compose.runtime.mutableFloatStateOf(.05f) }
    androidx.compose.runtime.LaunchedEffect(currentPath) {
        withContext(Dispatchers.IO) {
            val cur = decodeFile(currentPath, 1800); val o = decodeFile(originalPath, 1800)
            if (cur != null && o != null) { work = cur; orig = if (o.width == cur.width && o.height == cur.height) o else Bitmap.createScaledBitmap(o, cur.width, cur.height, true) }
        }
        version++
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color(0xFF101012)).padding(12.dp)) {
            Text("Rapikan potongan", color = Color.White, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                val w = work; val o = orig
                if (w != null && o != null) {
                    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val maxW = constraints.maxWidth.toFloat(); val maxH = constraints.maxHeight.toFloat()
                        val scale = min(maxW / w.width, maxH / w.height)
                        val dw = w.width * scale; val dh = w.height * scale
                        val dens = androidx.compose.ui.platform.LocalDensity.current
                        Canvas(Modifier.size(with(dens) { dw.toDp() }, with(dens) { dh.toDp() }).pointerInput(w, erase, brush) {
                            val r = brush * w.width / 2f
                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                maskFilter = android.graphics.BlurMaskFilter(r * .35f + 1f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                if (erase) xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                else shader = android.graphics.BitmapShader(o, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
                            }
                            val cv = android.graphics.Canvas(w)
                            var last: androidx.compose.ui.geometry.Offset? = null
                            fun stamp(p: androidx.compose.ui.geometry.Offset) {
                                val x = p.x / size.width * w.width; val y = p.y / size.height * w.height
                                cv.drawCircle(x, y, r, paint)
                                last?.let { q ->
                                    val lx = q.x / size.width * w.width; val ly = q.y / size.height * w.height
                                    val steps = (kotlin.math.hypot(x - lx, y - ly) / (r * .4f)).toInt().coerceIn(1, 60)
                                    for (i in 1 until steps) cv.drawCircle(lx + (x - lx) * i / steps, ly + (y - ly) * i / steps, r, paint)
                                }
                                last = p; version++
                            }
                            detectDragGestures(onDragStart = { last = null; stamp(it) }, onDrag = { c, _ -> c.consume(); stamp(c.position) }, onDragEnd = { last = null })
                        }) {
                            version.let { }
                            // checkerboard so transparency is visible
                            val cell = 14f; val cols = (size.width / cell).toInt() + 1; val rows = (size.height / cell).toInt() + 1
                            for (yy in 0..rows) for (xx in 0..cols) drawRect(if ((xx + yy) % 2 == 0) Color(0xFF3A3A3F) else Color(0xFF2A2A2E), androidx.compose.ui.geometry.Offset(xx * cell, yy * cell), androidx.compose.ui.geometry.Size(cell, cell))
                            drawIntoCanvas { c -> c.nativeCanvas.drawBitmap(w, null, android.graphics.RectF(0f, 0f, size.width, size.height), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)) }
                        }
                    }
                } else Text("Memuat…", color = Color.Gray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = erase, onClick = { erase = true }, label = { Text("Hapus") })
                FilterChip(selected = !erase, onClick = { erase = false }, label = { Text("Pulihkan") })
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Kuas", color = Color.LightGray, modifier = Modifier.width(60.dp)); Slider(value = brush, onValueChange = { brush = it }, valueRange = .01f..0.2f, modifier = Modifier.weight(1f)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GhostButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Batal") }
                PillButton(modifier = Modifier.weight(1f), onClick = {
                    val w = work ?: return@PillButton
                    scope.launch { val f = withContext(Dispatchers.IO) { PosterStore.saveBitmapPng(context, w, "cutout") }; onSave(f.absolutePath) }
                }) { Text("Terapkan") }
            }
        }
    }
}

// ---- text

@Composable
private fun TextControls(st: StudioState, l: Layer) {
    OutlinedTextField(
        value = l.text, onValueChange = { v -> st.edit(l.id) { it.copy(text = v.take(120), name = v.lines().firstOrNull()?.take(18)?.ifBlank { it.name } ?: it.name) } },
        modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) st.endTx() }, minLines = 1, maxLines = 3, label = { Text("Teks") },
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { st.set(l.id) { it.copy(bold = !it.bold) } }) { Icon(Icons.Rounded.FormatBold, "Tebal", tint = if (l.bold) Accent else Color.White) }
        IconButton(onClick = { st.set(l.id) { it.copy(italic = !it.italic) } }) { Icon(Icons.Rounded.FormatItalic, "Miring", tint = if (l.italic) Accent else Color.White) }
        IconButton(onClick = { st.set(l.id) { it.copy(align = 0) } }) { Icon(Icons.Rounded.FormatAlignLeft, "Kiri", tint = if (l.align == 0) Accent else Color.White) }
        IconButton(onClick = { st.set(l.id) { it.copy(align = 1) } }) { Icon(Icons.Rounded.FormatAlignCenter, "Tengah", tint = if (l.align == 1) Accent else Color.White) }
        IconButton(onClick = { st.set(l.id) { it.copy(align = 2) } }) { Icon(Icons.Rounded.FormatAlignRight, "Kanan", tint = if (l.align == 2) Accent else Color.White) }
    }
    LayerSlider("Ukuran font", st, l, .02f..0.35f, { it.size }) { q, v -> q.copy(size = v) }
    LayerSlider("Jarak huruf", st, l, -.1f..1f, { it.letterSpacing }) { q, v -> q.copy(letterSpacing = v) }
    LayerSlider("Tinggi baris", st, l, .7f..2f, { it.lineHeight }) { q, v -> q.copy(lineHeight = v) }
    Text("Preset", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextPresets.forEach { p -> FilterChip(selected = false, onClick = { st.set(l.id) { p.transform(it) } }, label = { Text(p.name) }) }
    }
}

@Composable
private fun FontControls(st: StudioState, l: Layer) {
    var cat by remember { mutableIntStateOf(FontCategories.indexOf(FontCatalog.getOrNull(l.font)?.category).coerceAtLeast(0)) }
    ChipRow(FontCategories, cat) { cat = it }
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FontCatalog.forEachIndexed { i, f -> if (f.category == FontCategories[cat]) FilterChip(selected = l.font == i, onClick = { st.set(l.id) { it.copy(font = i) } }, label = { Text(f.name) }) }
    }
}

@Composable
private fun TextStyleControls(st: StudioState, l: Layer) {
    Text("Warna teks", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
    ColorRow(l.color) { c -> st.set(l.id) { it.copy(color = c) } }
    SwitchRow("Latar label", l.textBgOn) { v -> st.set(l.id) { it.copy(textBgOn = v) } }
    if (l.textBgOn) ColorRow(l.textBg) { c -> st.set(l.id) { it.copy(textBg = c) } }
}

// ---- effect / frame / overlay

@Composable
private fun AdjustControls(st: StudioState, l: Layer) {
    LayerSlider("Intensitas", st, l, 0f..1f, { it.intensity }) { q, v -> q.copy(intensity = v) }
    LayerSlider("Opasitas", st, l, 0f..1f, { it.opacity }) { q, v -> q.copy(opacity = v) }
    val usesColor = l.type == LayerType.OVERLAY || l.kind in setOf("lens_flare", "light_leak", "soft_glow", "spotlight", "film_flash", "sun_flare", "light_streak", "camera_flash", "smoke", "sparkles", "star_particles", "duotone", "selective_color", "color_leak")
    if (usesColor) { Text(if (l.kind == "duotone") "Warna bayangan" else "Warna", color = Color.LightGray, style = MaterialTheme.typography.labelSmall); ColorRow(l.color) { c -> st.set(l.id) { it.copy(color = c) } } }
    if (l.kind == "duotone" || l.kind == "color_gradient" || l.kind == "pattern_checker") { Text("Warna kedua", color = Color.LightGray, style = MaterialTheme.typography.labelSmall); ColorRow(l.color2) { c -> st.set(l.id) { it.copy(color2 = c) } } }
    if (l.type == LayerType.EFFECT || l.type == LayerType.OVERLAY) { Text("Mode campur", color = Color.LightGray, style = MaterialTheme.typography.labelSmall); ChipRow(BlendNames, l.blend) { i -> st.set(l.id) { it.copy(blend = i) } } }
}
