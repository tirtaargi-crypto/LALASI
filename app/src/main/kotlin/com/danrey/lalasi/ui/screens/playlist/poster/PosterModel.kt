package com.danrey.lalasi.ui.screens.playlist.poster

import android.graphics.Color
import android.graphics.PorterDuff
import org.json.JSONArray
import org.json.JSONObject

enum class LayerType { PLAYER, CHARACTER, IMAGE, STICKER, TEXT, EFFECT, FRAME, OVERLAY }

/** the marker that stands for the photo's subject in the layer list: it is drawn where the marker sits, so what is above it covers the subject */
const val DEPTH_KIND = "__depth"
val Layer.isDepthMarker: Boolean get() = type == LayerType.FRAME && kind == DEPTH_KIND

val BlendNames = listOf("Normal", "Screen", "Multiply", "Lapisan", "Terangkan", "Gelapkan", "Tambah")

fun blendModeOf(index: Int): PorterDuff.Mode? = when (index) {
    1 -> PorterDuff.Mode.SCREEN
    2 -> PorterDuff.Mode.MULTIPLY
    3 -> PorterDuff.Mode.OVERLAY
    4 -> PorterDuff.Mode.LIGHTEN
    5 -> PorterDuff.Mode.DARKEN
    6 -> PorterDuff.Mode.ADD
    else -> null
}

val DepthStyleNames = listOf("Mati", "Melayang", "Menonjol", "3D Lembut", "Stiker", "Berlapis", "Potong Kertas", "Editorial")
val ShadowModeNames = listOf("Lembut", "Alami", "Dramatis", "Melayang")
val PlayerStyleNames = listOf("Arang", "Kaca", "Terang", "Vintage")

/** One independent, editable element on the poster. Everything is stored as plain values so the whole document is an immutable snapshot (cheap undo/redo). */
data class Layer(
    val id: String,
    val type: LayerType,
    val name: String,
    val kind: String = "",
    val uri: String? = null,
    val srcUri: String? = null,
    val text: String = "",
    val songId: String? = null,
    val preset: Boolean = false,
    // transform (x,y = centre as fraction of poster; size = width as fraction of poster width)
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val size: Float = 0.3f,
    val rotation: Float = 0f,
    val tiltY: Float = 0f,
    val tiltX: Float = 0f,
    val opacity: Float = 1f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val blend: Int = 0,
    val intensity: Float = 0.6f,
    val color: Int = Color.WHITE,
    val color2: Int = Color.BLACK,
    // shadow (follows the alpha shape of the object)
    val shadowOn: Boolean = false,
    val shadowMode: Int = 0,
    val shadowBlur: Float = 0.3f,
    val shadowSpread: Float = 0f,
    val shadowDist: Float = 0.1f,
    val shadowAngle: Float = 90f,
    val shadowOpacity: Float = 0.5f,
    val shadowColor: Int = Color.BLACK,
    // outline / sticker border
    val outlineOn: Boolean = false,
    val outlineWidth: Float = 0.01f,
    val outlineColor: Int = Color.WHITE,
    // interactive depth
    val depthStyle: Int = 0,
    val depthIntensity: Float = 0.6f,
    val edgeSoftness: Float = 0f,
    val elevation: Float = 0.5f,
    val highlight: Float = 0.25f,
    // player
    val artSize: Float = 1f,
    val playerStyle: Int = 0,
    val progress: Float = 0.38f,
    val glint: Boolean = true,
    val glintCorner: Int = 0,
    val glintSize: Float = 1f,
    val glintGlow: Float = 1f,
    /** where the glint sits on the card's outline, 0..1 clockwise from the middle of the top edge; negative = the corner in [glintCorner] (older posters) */
    val glintPos: Float = -1f,
    /** how far the glint streaks along the rim */
    val glintLen: Float = 1f,
    /** colour of the light: -1 cool, 0 white, 1 warm */
    val glintTone: Float = 0f,
    /** kept so older posters still load; nothing is placed automatically any more */
    val autoLayout: Boolean = false,
    // text
    val font: Int = 0,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val align: Int = 1,
    val letterSpacing: Float = 0f,
    val lineHeight: Float = 1.1f,
    val textBgOn: Boolean = false,
    val textBg: Int = 0xCC000000.toInt(),
)

/** a poster size: [label] is width:height, [id] is what gets saved in the poster (0-3 and 4 = custom keep their old meaning), [aspect] is height / width */
class RatioDef(val label: String, val id: Int, val aspect: Float)
const val RATIO_CUSTOM = 4
val RatioDefs = listOf(
    RatioDef("3:4", 0, 4f / 3f), RatioDef("4:5", 1, 5f / 4f), RatioDef("9:16", 2, 16f / 9f), RatioDef("1:1", 3, 1f),
    RatioDef("2:3", 5, 3f / 2f), RatioDef("3:5", 6, 5f / 3f), RatioDef("5:7", 7, 7f / 5f), RatioDef("9:19,5", 8, 19.5f / 9f),
    RatioDef("4:3", 9, 3f / 4f), RatioDef("3:2", 10, 2f / 3f), RatioDef("16:9", 11, 9f / 16f), RatioDef("21:9", 12, 9f / 21f),
)

data class PosterDoc(
    val ratio: Int = 0,
    val customW: Float = 3f,
    val customH: Float = 4f,
    val bgUri: String? = null,
    val bgColor: Int = 0xFF1C1C1E.toInt(),
    val bgScale: Float = 1f,
    val bgX: Float = 0f,
    val bgY: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val blur: Float = 0f,
    val exposure: Float = 0f,
    val overlayColor: Int = Color.BLACK,
    val overlayOpacity: Float = 0f,
    val bgLocked: Boolean = false,
    // subject of the background photo lifted in front of players/text (3D pop-out)
    val bgCutUri: String? = null,
    val bgPop: Boolean = false,
    val bgPopIntensity: Float = 0.6f,
    // effect applied to the photo only (the popped-out subject stays clean)
    val bgFx: String = "",
    val bgFxAmount: Float = 0.7f,
    val layers: List<Layer> = emptyList(),
    val nextId: Int = 1,
) {
    /** height / width of the poster */
    val aspect: Float
        get() = RatioDefs.firstOrNull { it.id == ratio }?.aspect ?: (customH / customW).coerceIn(0.4f, 2.5f)

    fun layer(id: String?): Layer? = layers.firstOrNull { it.id == id }

    fun update(id: String, f: (Layer) -> Layer): PosterDoc =
        copy(layers = layers.map { if (it.id == id) f(it) else it })

    fun add(make: (String) -> Layer): Pair<PosterDoc, Layer> {
        val layer = make("L$nextId")
        return copy(layers = layers + layer, nextId = nextId + 1) to layer
    }

    fun remove(id: String): PosterDoc = copy(layers = layers.filterNot { it.id == id })

    fun move(id: String, delta: Int): PosterDoc {
        val i = layers.indexOfFirst { it.id == id }
        if (i < 0) return this
        val j = (i + delta).coerceIn(0, layers.lastIndex)
        if (i == j) return this
        val list = layers.toMutableList()
        val item = list.removeAt(i)
        list.add(j, item)
        return copy(layers = list)
    }

    /**
     * Depth belongs to the objects (characters / images with a depth style), never to the players:
     * such an object is always drawn in front of every player, so it covers the card and casts its shadow onto it,
     * while a player never casts a shadow over the object. Everything else keeps the user's own order.
     * The renderer and the touch hit-test both use this order so what you see is what you can grab.
     */
    fun renderOrder(): List<Layer> {
        val lastPlayer = layers.indexOfLast { it.type == LayerType.PLAYER && it.visible }
        if (lastPlayer < 0) return layers
        fun lifted(l: Layer) = (l.type == LayerType.CHARACTER || l.type == LayerType.IMAGE) && l.depthStyle != 0
        if (layers.withIndex().none { (i, l) -> i < lastPlayer && lifted(l) }) return layers
        val out = ArrayList<Layer>(layers.size)
        val pending = ArrayList<Layer>()
        for ((i, l) in layers.withIndex()) {
            if (i < lastPlayer && lifted(l)) { pending.add(l); continue }
            out.add(l)
            if (i == lastPlayer) { out.addAll(pending); pending.clear() }
        }
        return out
    }

    /**
     * Keeps the subject's depth as one layer in the list: present while the depth is on and a cut-out exists, gone otherwise. It starts right above
     * the last player / text / sticker (where the subject always was) so everything added later lands above it; the user can move it up and down.
     */
    fun syncDepthMarker(): PosterDoc {
        val has = layers.any { it.isDepthMarker }
        if (!(bgPop && bgCutUri != null)) return if (has) copy(layers = layers.filterNot { it.isDepthMarker }) else this
        if (has) return this
        val behind = setOf(LayerType.PLAYER, LayerType.TEXT, LayerType.STICKER)
        val at = layers.indexOfLast { it.visible && it.type in behind } + 1
        val marker = Layer(id = "L$nextId", type = LayerType.FRAME, name = "Depth karakter", kind = DEPTH_KIND, size = 1f)
        val list = layers.toMutableList()
        list.add(at.coerceIn(0, list.size), marker)
        return copy(layers = list, nextId = nextId + 1)
    }

    fun toFront(id: String): PosterDoc = move(id, layers.size)
    fun toBack(id: String): PosterDoc = move(id, -layers.size)

    fun duplicate(id: String): Pair<PosterDoc, Layer?> {
        val src = layer(id) ?: return this to null
        val copy = src.copy(id = "L$nextId", name = src.name + " salinan", x = (src.x + 0.05f).coerceAtMost(1f), y = (src.y + 0.04f).coerceAtMost(1f), locked = false, preset = false)
        val i = layers.indexOfFirst { it.id == id }
        val list = layers.toMutableList()
        list.add(i + 1, copy)
        return copy(layers = list, nextId = nextId + 1) to copy
    }

    val playerCount: Int get() = layers.count { it.type == LayerType.PLAYER }
}

// ---------------------------------------------------------------- JSON (manual: release builds are minified, so no reflection)

private fun JSONObject.f(k: String, d: Float) = optDouble(k, d.toDouble()).toFloat()
private fun JSONObject.i(k: String, d: Int) = optInt(k, d)
private fun JSONObject.b(k: String, d: Boolean) = optBoolean(k, d)
private fun JSONObject.pf(k: String, v: Float) { put(k, v.toDouble()) }

fun Layer.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("type", type.name); put("name", name); put("kind", kind)
    uri?.let { put("uri", it) }; srcUri?.let { put("srcUri", it) }; songId?.let { put("songId", it) }
    put("text", text); put("preset", preset)
    pf("x", x); pf("y", y); pf("size", size); pf("rotation", rotation); pf("tiltY", tiltY); pf("tiltX", tiltX)
    pf("opacity", opacity); put("flipH", flipH); put("flipV", flipV); put("visible", visible); put("locked", locked)
    put("blend", blend); pf("intensity", intensity); put("color", color); put("color2", color2)
    put("shadowOn", shadowOn); put("shadowMode", shadowMode); pf("shadowBlur", shadowBlur); pf("shadowSpread", shadowSpread)
    pf("shadowDist", shadowDist); pf("shadowAngle", shadowAngle); pf("shadowOpacity", shadowOpacity); put("shadowColor", shadowColor)
    put("outlineOn", outlineOn); pf("outlineWidth", outlineWidth); put("outlineColor", outlineColor)
    put("depthStyle", depthStyle); pf("depthIntensity", depthIntensity); pf("edgeSoftness", edgeSoftness)
    pf("elevation", elevation); pf("highlight", highlight)
    pf("artSize", artSize); put("playerStyle", playerStyle); pf("progress", progress); put("glint", glint); put("glintCorner", glintCorner); pf("glintSize", glintSize); pf("glintGlow", glintGlow); pf("glintPos", glintPos); pf("glintLen", glintLen); pf("glintTone", glintTone); put("autoLayout", autoLayout)
    put("font", font); put("bold", bold); put("italic", italic); put("align", align)
    pf("letterSpacing", letterSpacing); pf("lineHeight", lineHeight); put("textBgOn", textBgOn); put("textBg", textBg)
}

fun layerFromJson(o: JSONObject): Layer {
    val d = Layer(id = "", type = LayerType.STICKER, name = "")
    return Layer(
        id = o.optString("id"),
        type = runCatching { LayerType.valueOf(o.optString("type")) }.getOrDefault(LayerType.STICKER),
        name = o.optString("name"), kind = o.optString("kind"),
        uri = o.optString("uri").takeIf { it.isNotBlank() }, srcUri = o.optString("srcUri").takeIf { it.isNotBlank() },
        text = o.optString("text"), songId = o.optString("songId").takeIf { it.isNotBlank() }, preset = o.b("preset", false),
        x = o.f("x", d.x), y = o.f("y", d.y), size = o.f("size", d.size), rotation = o.f("rotation", 0f),
        tiltY = o.f("tiltY", 0f), tiltX = o.f("tiltX", 0f), opacity = o.f("opacity", 1f),
        flipH = o.b("flipH", false), flipV = o.b("flipV", false), visible = o.b("visible", true), locked = o.b("locked", false),
        blend = o.i("blend", 0), intensity = o.f("intensity", d.intensity), color = o.i("color", d.color), color2 = o.i("color2", d.color2),
        shadowOn = o.b("shadowOn", false), shadowMode = o.i("shadowMode", 0), shadowBlur = o.f("shadowBlur", d.shadowBlur),
        shadowSpread = o.f("shadowSpread", 0f), shadowDist = o.f("shadowDist", d.shadowDist), shadowAngle = o.f("shadowAngle", d.shadowAngle),
        shadowOpacity = o.f("shadowOpacity", d.shadowOpacity), shadowColor = o.i("shadowColor", d.shadowColor),
        outlineOn = o.b("outlineOn", false), outlineWidth = o.f("outlineWidth", d.outlineWidth), outlineColor = o.i("outlineColor", d.outlineColor),
        depthStyle = o.i("depthStyle", 0), depthIntensity = o.f("depthIntensity", d.depthIntensity), edgeSoftness = o.f("edgeSoftness", 0f),
        elevation = o.f("elevation", d.elevation), highlight = o.f("highlight", d.highlight),
        artSize = o.f("artSize", 1f), playerStyle = o.i("playerStyle", 0), progress = o.f("progress", d.progress), glint = o.b("glint", true), glintCorner = o.i("glintCorner", 0), glintSize = o.f("glintSize", 1f), glintGlow = o.f("glintGlow", 1f), glintPos = o.f("glintPos", -1f), glintLen = o.f("glintLen", 1f), glintTone = o.f("glintTone", 0f), autoLayout = false,
        font = o.i("font", 0), bold = o.b("bold", false), italic = o.b("italic", false), align = o.i("align", 1),
        letterSpacing = o.f("letterSpacing", 0f), lineHeight = o.f("lineHeight", 1.1f), textBgOn = o.b("textBgOn", false), textBg = o.i("textBg", d.textBg),
    )
}

fun PosterDoc.toJson(): JSONObject = JSONObject().apply {
    put("version", 2)
    put("ratio", ratio); pf("customW", customW); pf("customH", customH)
    bgUri?.let { put("bgUri", it) }
    put("bgColor", bgColor); pf("bgScale", bgScale); pf("bgX", bgX); pf("bgY", bgY)
    pf("brightness", brightness); pf("contrast", contrast); pf("saturation", saturation); pf("blur", blur); pf("exposure", exposure)
    put("overlayColor", overlayColor); pf("overlayOpacity", overlayOpacity); put("bgLocked", bgLocked)
    bgCutUri?.let { put("bgCutUri", it) }; put("bgPop", bgPop); pf("bgPopIntensity", bgPopIntensity); put("bgFx", bgFx); pf("bgFxAmount", bgFxAmount)
    put("nextId", nextId)
    put("layers", JSONArray().also { arr -> layers.forEach { arr.put(it.toJson()) } })
}

fun docFromJson(o: JSONObject): PosterDoc {
    val arr = o.optJSONArray("layers") ?: JSONArray()
    val layers = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(::layerFromJson) }
    return PosterDoc(
        ratio = o.i("ratio", 0), customW = o.f("customW", 3f), customH = o.f("customH", 4f),
        bgUri = o.optString("bgUri").takeIf { it.isNotBlank() }, bgColor = o.i("bgColor", 0xFF1C1C1E.toInt()),
        bgScale = o.f("bgScale", 1f), bgX = o.f("bgX", 0f), bgY = o.f("bgY", 0f),
        brightness = o.f("brightness", 0f), contrast = o.f("contrast", 1f), saturation = o.f("saturation", 1f),
        blur = o.f("blur", 0f), exposure = o.f("exposure", 0f),
        overlayColor = o.i("overlayColor", Color.BLACK), overlayOpacity = o.f("overlayOpacity", 0f), bgLocked = o.b("bgLocked", false),
        bgCutUri = o.optString("bgCutUri").takeIf { it.isNotBlank() }, bgPop = o.b("bgPop", false), bgPopIntensity = o.f("bgPopIntensity", 0.6f), bgFx = o.optString("bgFx"), bgFxAmount = o.f("bgFxAmount", 0.7f),
        layers = layers, nextId = maxOf(o.i("nextId", 1), layers.size + 1),
    )
}

// ---------------------------------------------------------------- catalogs

data class StickerDef(val key: String, val label: String, val aspect: Float = 1f, val color: Int = Color.WHITE, val color2: Int = Color.BLACK)
data class StickerCategory(val name: String, val items: List<StickerDef>)

private fun c(hex: Long) = hex.toInt()

val StickerCategories: List<StickerCategory> = listOf(
    StickerCategory("Dekoratif", listOf(
        StickerDef("star", "Bintang", 1f, c(0xFFFFD84D), c(0xFFE0A100)),
        StickerDef("sparkle", "Kilau", 1f, c(0xFFFFFFFF), c(0xFFFFE08A)),
        StickerDef("heart", "Hati", 0.92f, c(0xFFFF5C8A), c(0xFFFFFFFF)),
        StickerDef("flower", "Bunga", 1f, c(0xFFFFFFFF), c(0xFFFFC83D)),
        StickerDef("butterfly", "Kupu-kupu", 0.8f, c(0xFF7FB7FF), c(0xFF3C5BD6)),
        StickerDef("ribbon", "Pita", 0.45f, c(0xFFE24A4A), c(0xFF9E2323)),
        StickerDef("bow", "Simpul", 0.7f, c(0xFFFF8FB1), c(0xFFD84C7B)),
        StickerDef("bubbles", "Gelembung", 1f, c(0xFFBFE6FF), c(0xFFFFFFFF)),
        StickerDef("doodle", "Coretan", 0.6f, c(0xFFFFFFFF), c(0xFFFFD84D)),
        StickerDef("hand_shapes", "Gambar tangan", 0.8f, c(0xFFFFFFFF), c(0xFFFF5C8A)),
        StickerDef("deco_line", "Garis hias", 0.22f, c(0xFFFFFFFF), c(0xFFFFD84D)),
        StickerDef("ornament", "Ornamen", 0.5f, c(0xFFE8D7B8), c(0xFF8A6A3A)),
    )),
    StickerCategory("Musik", listOf(
        StickerDef("vinyl", "Vinil", 1f, c(0xFFF2E3C0), c(0xFF111111)),
        StickerDef("cassette", "Kaset", 0.64f, c(0xFFEDE6D4), c(0xFF2A2A2A)),
        StickerDef("cd", "Cakram CD", 1f, c(0xFFD9DEE8), c(0xFF8FA0C0)),
        StickerDef("headphones", "Headphone", 1f, c(0xFFFFFFFF), c(0xFF222222)),
        StickerDef("earphones", "Earphone", 1.1f, c(0xFFFFFFFF), c(0xFFBBBBBB)),
        StickerDef("notes", "Not musik", 1f, c(0xFFFFFFFF), c(0xFF111111)),
        StickerDef("equalizer", "Equalizer", 0.8f, c(0xFF7CF29C), c(0xFF1B5E3A)),
        StickerDef("waveform", "Gelombang suara", 0.45f, c(0xFFFFFFFF), c(0xFFFF5C8A)),
        StickerDef("speaker", "Speaker", 1.2f, c(0xFF2B2B2E), c(0xFFBBBBBB)),
        StickerDef("music_label", "Label musik", 0.32f, c(0xFFFFD84D), c(0xFF222222)),
        StickerDef("retro_player", "Pemutar retro", 0.62f, c(0xFFCFC6B0), c(0xFF2B2B2E)),
        StickerDef("music_symbols", "Simbol", 0.7f, c(0xFFFFFFFF), c(0xFF111111)),
    )),
    StickerCategory("Scrapbook", listOf(
        StickerDef("tape", "Selotip", 0.36f, c(0xFFF3D98B), c(0xFFD6B25A)),
        StickerDef("paperclip", "Klip kertas", 2.2f, c(0xFFB8BEC8), c(0xFF6C7380)),
        StickerDef("torn_paper", "Kertas sobek", 0.8f, c(0xFFF4EFE3), c(0xFFCFC6B0)),
        StickerDef("stamp", "Perangko", 1.2f, c(0xFFF8F3E6), c(0xFFB5483A)),
        StickerDef("newspaper", "Koran", 1.15f, c(0xFFE9E2D0), c(0xFF2B2B2B)),
        StickerDef("note", "Catatan tulisan tangan", 1f, c(0xFFFFE97A), c(0xFF444444)),
        StickerDef("film_frame", "Bingkai film", 0.55f, c(0xFF1B1B1D), c(0xFFE9E2D0)),
        StickerDef("polaroid", "Polaroid", 1.18f, c(0xFFFFFFFF), c(0xFFB8B8B8)),
        StickerDef("scrap_label", "Label scrap", 0.4f, c(0xFFF4EFE3), c(0xFF8A3A2A)),
        StickerDef("paper_pieces", "Potongan kertas", 0.9f, c(0xFFF2B8A8), c(0xFF9CC5C9)),
        StickerDef("collage_deco", "Dekor kolase", 1f, c(0xFFFF6B4A), c(0xFF2F5D8C)),
    )),
    StickerCategory("Estetik", listOf(
        StickerDef("vintage_badge", "Vintage", 1f, c(0xFFEBD9A8), c(0xFF5C3B1E)),
        StickerDef("retro_sun", "Retro", 0.62f, c(0xFFFF8A3D), c(0xFFFFD84D)),
        StickerDef("smiley", "Indie", 1f, c(0xFFFFE04A), c(0xFF222222)),
        StickerDef("y2k_star", "Y2K", 1f, c(0xFFE6ECFF), c(0xFF7B8CFF)),
        StickerDef("grunge_patch", "Grunge", 0.7f, c(0xFF2B2B2B), c(0xFFE9E2D0)),
        StickerDef("minimal_circle", "Minimal", 1f, c(0xFFFFFFFF), c(0xFFFFFFFF)),
        StickerDef("soft_cloud", "Lembut", 0.62f, c(0xFFFFFFFF), c(0xFFCDE3FF)),
        StickerDef("editorial_tag", "Editorial", 0.42f, c(0xFF111111), c(0xFFFFFFFF)),
        StickerDef("analog_camera", "Analog", 0.78f, c(0xFFD9D4C8), c(0xFF222222)),
        StickerDef("film_roll", "Foto film", 1.2f, c(0xFFF2A81D), c(0xFF222222)),
    )),
)

fun stickerDef(key: String): StickerDef? = StickerCategories.firstNotNullOfOrNull { cat -> cat.items.firstOrNull { it.key == key } }

data class EffectDef(val key: String, val label: String, val group: String, val blend: Int = 0, val intensity: Float = 0.7f, val color: Int = Color.WHITE, val color2: Int = Color.BLACK, val size: Float = 0.6f, val x: Float = 0.5f, val y: Float = 0.5f)

val EffectGroups = listOf("Cahaya", "Suasana", "Tekstur", "Warna")

val EffectCatalog: List<EffectDef> = listOf(
    EffectDef("lens_flare", "Lens Flare", "Cahaya", 1, 0.8f, c(0xFFFFE6B0), size = 0.7f, x = 0.78f, y = 0.18f),
    EffectDef("light_leak", "Cahaya Bocor", "Cahaya", 1, 0.75f, c(0xFFFF8A3D), size = 0.8f, x = 0.05f, y = 0.35f),
    EffectDef("soft_glow", "Cahaya Lembut", "Cahaya", 1, 0.55f, c(0xFFFFF3DC), size = 0.9f),
    EffectDef("bloom", "Bloom", "Cahaya", 1, 0.7f, size = 0.5f),
    EffectDef("spotlight", "Sorot", "Cahaya", 0, 0.7f, c(0xFFFFFFFF), size = 0.55f, x = 0.5f, y = 0.4f),
    EffectDef("film_flash", "Blitz Film", "Cahaya", 1, 0.55f, c(0xFFFFF0C8), size = 0.9f),
    EffectDef("sun_flare", "Kilau Matahari", "Cahaya", 1, 0.85f, c(0xFFFFC46B), size = 0.7f, x = 0.85f, y = 0.1f),
    EffectDef("light_streak", "Garis Cahaya", "Cahaya", 1, 0.8f, c(0xFFBFD8FF), size = 0.9f, y = 0.35f),
    EffectDef("prism_light", "Cahaya Prisma", "Cahaya", 1, 0.65f, size = 0.8f),
    EffectDef("rgb_light", "Cahaya RGB", "Cahaya", 1, 0.6f, size = 0.8f),
    EffectDef("holographic", "Holografik", "Cahaya", 3, 0.6f, size = 0.9f),
    EffectDef("camera_flash", "Blitz Kamera", "Cahaya", 1, 0.8f, c(0xFFFFFFFF), size = 0.8f, x = 0.2f, y = 0.2f),
    EffectDef("fog", "Kabut", "Suasana", 0, 0.6f, size = 0.8f),
    EffectDef("smoke", "Asap", "Suasana", 0, 0.6f, c(0xFFD8D8D8), size = 0.8f),
    EffectDef("dust_particles", "Debu", "Suasana", 1, 0.7f),
    EffectDef("floating_particles", "Melayang", "Suasana", 1, 0.7f),
    EffectDef("bokeh", "Bokeh", "Suasana", 1, 0.7f),
    EffectDef("sparkles", "Percikan", "Suasana", 1, 0.7f, c(0xFFFFFFFF)),
    EffectDef("star_particles", "Partikel Bintang", "Suasana", 1, 0.7f, c(0xFFFFE9A8)),
    EffectDef("rain", "Hujan", "Suasana", 1, 0.6f),
    EffectDef("snow", "Salju", "Suasana", 0, 0.7f),
    EffectDef("soft_haze", "Kabut Lembut", "Suasana", 0, 0.6f),
    EffectDef("motion_blur", "Blur Gerak", "Suasana", 0, 0.5f),
    EffectDef("grain", "Butiran Film", "Tekstur", 3, 0.6f),
    EffectDef("vintage_noise", "Noise Vintage", "Tekstur", 3, 0.6f),
    EffectDef("paper_texture", "Kertas", "Tekstur", 2, 0.7f),
    EffectDef("dust_texture", "Tekstur Debu", "Tekstur", 0, 0.7f),
    EffectDef("scratches", "Goresan", "Tekstur", 1, 0.7f),
    EffectDef("halftone", "Halftone", "Tekstur", 2, 0.6f),
    EffectDef("newspaper", "Koran", "Tekstur", 2, 0.7f),
    EffectDef("photocopy", "Fotokopi", "Tekstur", 2, 0.7f),
    EffectDef("vhs", "VHS", "Tekstur", 0, 0.7f),
    EffectDef("analog_film", "Film Analog", "Tekstur", 0, 0.7f),
    EffectDef("grunge", "Grunge", "Tekstur", 2, 0.7f),
    EffectDef("old_paper", "Kertas Tua", "Tekstur", 2, 0.8f),
    EffectDef("monochrome", "Monokrom", "Warna", 0, 1f),
    EffectDef("duotone", "Duotone", "Warna", 0, 1f, c(0xFF2A1B6B), c(0xFFFFB38A)),
    EffectDef("vintage_fade", "Pudar Vintage", "Warna", 0, 1f),
    EffectDef("color_leak", "Warna Bocor", "Warna", 1, 0.7f, c(0xFFFF6B8A)),
    EffectDef("rgb_split", "RGB Split", "Warna", 0, 0.6f),
    EffectDef("chromatic", "Aberasi Kromatik", "Warna", 0, 0.6f),
    EffectDef("color_grading", "Grading Warna", "Warna", 0, 1f),
    EffectDef("selective_color", "Warna Selektif", "Warna", 0, 1f, c(0xFFE53935)),
    EffectDef("soft_contrast", "Kontras Lembut", "Warna", 0, 1f),
    EffectDef("film_color", "Warna Film", "Warna", 0, 1f),
    EffectDef("warm_tone", "Nada Hangat", "Warna", 0, 1f),
    EffectDef("cool_tone", "Nada Dingin", "Warna", 0, 1f),
)

fun effectDef(key: String) = EffectCatalog.firstOrNull { it.key == key }

data class FrameDef(val key: String, val label: String, val blend: Int = 0)

val FrameCatalog: List<FrameDef> = listOf(
    FrameDef("polaroid", "Polaroid"), FrameDef("film_border", "Tepi film"), FrameDef("viewfinder", "Jendela bidik"),
    FrameDef("vintage_cam", "Kamera vintage"), FrameDef("film_burn", "Film terbakar", 1), FrameDef("torn_edges", "Tepi sobek"),
    FrameDef("news_border", "Koran"), FrameDef("scrap_frame", "Scrapbook"), FrameDef("contact_sheet", "Lembar kontak"),
    FrameDef("deco_border", "Dekoratif"), FrameDef("camera_details", "Detail kamera"),
)
val OverlayCatalog: List<FrameDef> = listOf(
    FrameDef("paper_tex", "Kertas", 2), FrameDef("film_tex", "Film", 3), FrameDef("vintage_tex", "Vintage", 2),
    FrameDef("pattern_dots", "Titik"), FrameDef("pattern_grid", "Kisi"), FrameDef("pattern_stripes", "Garis"),
    FrameDef("pattern_checker", "Kotak-kotak"), FrameDef("pattern_stars", "Bintang"),
    FrameDef("color_solid", "Warna"), FrameDef("color_gradient", "Gradasi"), FrameDef("blur", "Blur"), FrameDef("light", "Terang", 1),
)

val SwatchColors = listOf(
    0xFFFFFFFF, 0xFF111111, 0xFFFFD84D, 0xFFFF8A3D, 0xFFE53935, 0xFFFF5C8A, 0xFF8E5CF7,
    0xFF3C5BD6, 0xFF4FC3F7, 0xFF3DDC97, 0xFFE8D7B8, 0xFF8A6A3A, 0xFF7A7A7E, 0xFF292522,
).map { it.toInt() }

class FontDef(val name: String, val category: String)

val FontCatalog = listOf(
    FontDef("Editorial Serif", "Editorial"), FontDef("Playfair Bold", "Editorial"),
    FontDef("Classic Serif", "Serif"), FontDef("Book Serif", "Serif"),
    FontDef("Inter", "Sans Serif"), FontDef("Clean Sans", "Sans Serif"), FontDef("Condensed", "Sans Serif"),
    FontDef("Script", "Handwritten"), FontDef("Casual", "Handwritten"),
    FontDef("Old Print", "Vintage"), FontDef("Retro Slab", "Retro"),
    FontDef("Display Black", "Display"), FontDef("Bartle", "Display"),
    FontDef("Typewriter", "Typewriter"), FontDef("Grunge Mono", "Grunge"),
)
val FontCategories = listOf("Editorial", "Serif", "Sans Serif", "Handwritten", "Vintage", "Retro", "Display", "Typewriter", "Grunge")

class TextPreset(val name: String, val transform: (Layer) -> Layer)

val TextPresets = listOf(
    TextPreset("White Outline") { it.copy(color = Color.BLACK, outlineOn = true, outlineColor = Color.WHITE, outlineWidth = 0.09f, shadowOn = true, shadowOpacity = 0.35f, shadowBlur = 0.2f, shadowDist = 0.08f, textBgOn = false, depthStyle = 0) },
    TextPreset("3D Shadow") { it.copy(color = Color.WHITE, outlineOn = false, depthStyle = 3, depthIntensity = 0.7f, shadowOn = true, shadowColor = 0xFF3A2A6B.toInt(), shadowOpacity = 0.9f, shadowAngle = 45f, textBgOn = false) },
    TextPreset("Cutout Typography") { it.copy(color = 0xFFF4EFE3.toInt(), outlineOn = true, outlineColor = Color.WHITE, outlineWidth = 0.12f, shadowOn = true, shadowOpacity = 0.55f, shadowBlur = 0.1f, shadowDist = 0.07f, textBgOn = false, depthStyle = 6) },
    TextPreset("Retro Title") { it.copy(font = 10, bold = true, color = 0xFFFF8A3D.toInt(), outlineOn = true, outlineColor = 0xFF3A1D0C.toInt(), outlineWidth = 0.07f, shadowOn = true, shadowColor = 0xFF3A1D0C.toInt(), shadowOpacity = 1f, shadowBlur = 0f, shadowDist = 0.06f, shadowAngle = 60f, textBgOn = false) },
    TextPreset("Film Subtitle") { it.copy(font = 4, bold = false, color = 0xFFFFE97A.toInt(), outlineOn = true, outlineColor = Color.BLACK, outlineWidth = 0.06f, textBgOn = false, shadowOn = false) },
    TextPreset("Vintage Label") { it.copy(font = 9, color = 0xFF3A2A1C.toInt(), textBgOn = true, textBg = 0xFFE8D7B8.toInt(), outlineOn = false, shadowOn = true, shadowOpacity = 0.3f, shadowDist = 0.05f) },
    TextPreset("Newspaper Text") { it.copy(font = 2, bold = true, color = 0xFF151515.toInt(), textBgOn = true, textBg = 0xFFE9E2D0.toInt(), outlineOn = false, shadowOn = false) },
    TextPreset("Handwritten Label") { it.copy(font = 7, color = 0xFFFFFFFF.toInt(), textBgOn = true, textBg = 0xFFC8946A.toInt(), outlineOn = false, shadowOn = true, shadowOpacity = 0.3f, shadowDist = 0.05f) },
)

val PresetNames = listOf(
    "Kolase Musik Indie", "Playlist Vintage", "Kaset Retro", "Poster Musik Y2K", "Scrapbook Estetik",
    "Fotografi Film", "Editorial Gelap", "Vintage Lembut", "Musik Minimal", "Kolase Grunge",
)


/** the glint's place on the outline: the one the user set, or for older posters the corner they had chosen */
fun Layer.glintPlace(): Float = if (glintPos >= 0f) glintPos else when (glintCorner) {
    1 -> GLINT_TOP_LEFT
    2 -> GLINT_BOTTOM_RIGHT
    3 -> GLINT_BOTTOM_LEFT
    else -> GLINT_TOP_RIGHT
}
