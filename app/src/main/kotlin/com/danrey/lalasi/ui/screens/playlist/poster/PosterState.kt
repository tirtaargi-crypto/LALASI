package com.danrey.lalasi.ui.screens.playlist.poster

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

const val BG = "__bg"

enum class StudioTab(val label: String) { BACKGROUND("Photo"), MUSIC("Music"), OBJECTS("Objects"), EFFECTS("Effects"), STICKERS("Stickers"), TEXT("Text"), LAYERS("Layers") }

/** Editing state with snapshot-based undo/redo. Slider drags and gestures are grouped into one undo step (live + endTx). */
@Stable
class StudioState {
    var doc by mutableStateOf(PosterDoc())
    var selected by mutableStateOf<String?>(null)
    var tab by mutableStateOf(StudioTab.BACKGROUND)
    var message by mutableStateOf<String?>(null)
    var busy by mutableStateOf<String?>(null)
    var undoCount by mutableIntStateOf(0)
    var redoCount by mutableIntStateOf(0)
    var inspecting by mutableStateOf(false)

    /** Document as it looks *during* a finger gesture. Only the renderer and the selection overlay read it, so moving things never recomposes the panels. Folded into [doc] by [endTx]. */
    var preview by mutableStateOf<PosterDoc?>(null)
    val view: PosterDoc get() = preview ?: doc

    private val undoStack = ArrayList<PosterDoc>()
    private val redoStack = ArrayList<PosterDoc>()
    private var open = false

    private fun push() {
        undoStack.add(doc); if (undoStack.size > 80) undoStack.removeAt(0)
        redoStack.clear(); undoCount = undoStack.size; redoCount = 0
    }

    /** continuous edit (slider / drag): first call opens one undo step */
    fun live(f: (PosterDoc) -> PosterDoc) { if (!open) { push(); open = true }; doc = f(doc) }
    /** same as [live] but for drags / pinches: edits a cheap preview copy, committed to the real document when the finger lifts */
    fun gLive(f: (PosterDoc) -> PosterDoc) { if (!open) { push(); open = true }; preview = f(preview ?: doc) }
    fun gEdit(id: String, f: (Layer) -> Layer) = gLive { it.update(id, f) }
    /** background work (auto subject cut-out) lands here: applied to the live document, any gesture preview and the undo history, without becoming an undo step of its own */
    fun patch(f: (PosterDoc) -> PosterDoc) {
        doc = f(doc); preview = preview?.let(f)
        for (i in undoStack.indices) undoStack[i] = f(undoStack[i])
        for (i in redoStack.indices) redoStack[i] = f(redoStack[i])
    }
    fun endTx() { open = false; preview?.let { doc = it; preview = null } }
    fun commit(f: (PosterDoc) -> PosterDoc) { open = false; preview = null; push(); doc = f(doc) }
    fun reset(newDoc: PosterDoc) { open = false; preview = null; undoStack.clear(); redoStack.clear(); undoCount = 0; redoCount = 0; doc = newDoc; selected = null; inspecting = false }

    fun undo() {
        open = false; preview = null
        if (undoStack.isEmpty()) return
        redoStack.add(doc); doc = undoStack.removeAt(undoStack.lastIndex)
        undoCount = undoStack.size; redoCount = redoStack.size; fixSelection()
    }

    fun redo() {
        open = false; preview = null
        if (redoStack.isEmpty()) return
        undoStack.add(doc); doc = redoStack.removeAt(redoStack.lastIndex)
        undoCount = undoStack.size; redoCount = redoStack.size; fixSelection()
    }

    private fun fixSelection() { val s = selected; if (s != null && s != BG && doc.layer(s) == null) { selected = null; inspecting = false } }

    fun select(id: String?) { selected = id; inspecting = id != null && id != BG }
    val layer: Layer? get() = doc.layer(selected)

    /** apply to the selected layer as one continuous edit */
    fun edit(id: String, f: (Layer) -> Layer) = live { it.update(id, f) }
    fun set(id: String, f: (Layer) -> Layer) = commit { it.update(id, f) }

    fun addLayer(pair: Pair<PosterDoc, Layer>) { commit { pair.first }; select(pair.second.id) }
}

fun quickDepth(l: Layer, outline: Boolean = true): Layer = l.copy(
    depthStyle = 2, depthIntensity = .6f, shadowOn = true, shadowMode = 1, shadowOpacity = .5f, shadowBlur = .3f, shadowDist = .1f,
    shadowAngle = 90f, shadowSpread = 0f, edgeSoftness = .15f, elevation = .5f, highlight = .25f,
    outlineOn = outline, outlineWidth = .012f, outlineColor = android.graphics.Color.WHITE,
)

fun applyDepthStyle(l: Layer, style: Int): Layer = when (style) {
    0 -> l.copy(depthStyle = 0)
    1 -> l.copy(depthStyle = 1, shadowOn = true, shadowMode = 3, shadowBlur = .35f, shadowDist = .12f, shadowOpacity = .45f, outlineOn = false)
    2 -> quickDepth(l, false)
    3 -> l.copy(depthStyle = 3, shadowOn = true, shadowMode = 0, shadowColor = 0xFF1E1B3A.toInt(), shadowAngle = 45f, shadowOpacity = .55f, shadowBlur = .2f, shadowDist = .06f, outlineOn = false)
    4 -> l.copy(depthStyle = 4, shadowOn = true, shadowMode = 0, shadowBlur = .25f, shadowDist = .08f, shadowOpacity = .45f, outlineOn = true, outlineColor = android.graphics.Color.WHITE, outlineWidth = .02f)
    5 -> l.copy(depthStyle = 5, shadowOn = true, shadowMode = 0, shadowBlur = .3f, shadowDist = .08f, shadowOpacity = .5f, outlineOn = false)
    6 -> l.copy(depthStyle = 6, shadowOn = true, shadowMode = 0, shadowBlur = .05f, shadowDist = .06f, shadowOpacity = .5f, outlineOn = true, outlineColor = 0xFFF8F4EA.toInt(), outlineWidth = .035f)
    else -> l.copy(depthStyle = 7, shadowOn = true, shadowMode = 2, shadowBlur = 0f, shadowDist = .08f, shadowOpacity = 1f, shadowColor = android.graphics.Color.BLACK, outlineOn = true, outlineWidth = .03f)
}

/** reset transform + look of one object back to the defaults of its type */
fun resetLayer(l: Layer): Layer {
    val base = Layer(id = l.id, type = l.type, name = l.name, kind = l.kind, uri = l.srcUri ?: l.uri, srcUri = l.srcUri, text = l.text, songId = l.songId, color = l.color, color2 = l.color2, font = l.font)
    return base.copy(
        size = when (l.type) { LayerType.PLAYER -> .3f; LayerType.CHARACTER -> .55f; LayerType.IMAGE -> .4f; LayerType.TEXT -> l.size; LayerType.STICKER -> .28f; else -> 1f.coerceAtMost(effectDef(l.kind)?.size ?: 1f) },
        x = if (l.type == LayerType.EFFECT) effectDef(l.kind)?.x ?: .5f else .5f, y = if (l.type == LayerType.EFFECT) effectDef(l.kind)?.y ?: .5f else .5f,
        blend = if (l.type == LayerType.EFFECT) effectDef(l.kind)?.blend ?: 0 else 0, intensity = effectDef(l.kind)?.intensity ?: .6f,
        shadowOn = l.type == LayerType.PLAYER || l.type == LayerType.STICKER,
        shadowOpacity = if (l.type == LayerType.STICKER) .35f else .5f,
    )
}
