package com.danrey.lalasi.ui.screens.playlist.poster

import android.graphics.Color

private class Slot(val x: Float, val y: Float, val size: Float, val rot: Float, val tiltY: Float)

/** Layer factories and the ten starting compositions. Presets only place normal layers, so everything stays editable afterwards. */
object PosterPresets {
    // reference-style scatter: tilted portrait players around a centre character
    private val scatter = listOf(
        Slot(.40f, .27f, .27f, -4f, 12f), Slot(.80f, .50f, .27f, -3f, 14f), Slot(.19f, .57f, .27f, 6f, -14f),
        Slot(.82f, .80f, .26f, -6f, 12f), Slot(.20f, .86f, .26f, 8f, -12f),
    )
    // Four-player composition: two upright cards on the left and two on the right.
    // Card width adapts to poster aspect so both rows stay inside portrait, square and landscape canvases.
    private fun balancedGrid(aspect: Float): List<Slot> {
        val size = minOf(.22f, (aspect.coerceIn(.4f, 2.5f) * .36f) / PLAYER_ASPECT)
        return listOf(
            Slot(.26f, .28f, size, 0f, 0f), Slot(.74f, .28f, size, 0f, 0f),
            Slot(.26f, .72f, size, 0f, 0f), Slot(.74f, .72f, size, 0f, 0f),
            Slot(.50f, .50f, size, 0f, 0f),
        )
    }
    private val fan = listOf(
        Slot(.16f, .72f, .30f, -12f, 0f), Slot(.33f, .70f, .30f, -6f, 0f), Slot(.50f, .68f, .30f, 0f, 0f),
        Slot(.67f, .70f, .30f, 6f, 0f), Slot(.84f, .72f, .30f, 12f, 0f),
    )

    fun newPlayer(doc: PosterDoc, song: SongInfo, style: Int = 0): Pair<PosterDoc, Layer> {
        val nextCount = (doc.playerCount + 1).coerceAtMost(5)
        val slots = balancedGrid(doc.aspect)
        val slot = slots[(nextCount - 1).coerceAtMost(slots.lastIndex)]
        val (addedDoc, added) = doc.add { id ->
            Layer(id = id, type = LayerType.PLAYER, name = song.title, songId = song.id, x = slot.x, y = slot.y, size = slot.size,
                rotation = slot.rot, tiltY = slot.tiltY, playerStyle = style, shadowOn = true, shadowOpacity = .5f, shadowBlur = .35f, shadowDist = .12f)
        }
        // Reflow the first four players together. This prevents sequentially-added cards from
        // drifting into the old diagonal scatter composition.
        var index = 0
        val reflowed = addedDoc.copy(layers = addedDoc.layers.map { layer ->
            if (layer.type != LayerType.PLAYER) layer else {
                val target = slots[index.coerceAtMost(slots.lastIndex)]; index++
                layer.copy(x = target.x, y = target.y, size = target.size, rotation = 0f, tiltY = 0f, tiltX = 0f)
            }
        })
        return reflowed to (reflowed.layer(added.id) ?: added)
    }

    fun newCharacter(doc: PosterDoc, path: String, name: String = "Character"): Pair<PosterDoc, Layer> = doc.add { id ->
        quickDepth(Layer(id = id, type = LayerType.CHARACTER, name = name, uri = path, srcUri = path, x = .5f, y = .66f, size = .55f), outline = false)
    }

    fun newImage(doc: PosterDoc, path: String): Pair<PosterDoc, Layer> = doc.add { id ->
        Layer(id = id, type = LayerType.IMAGE, name = "Photo", uri = path, srcUri = path, x = .5f, y = .4f, size = .4f, shadowOn = true, shadowOpacity = .4f, shadowBlur = .3f, shadowDist = .08f)
    }

    fun newSticker(doc: PosterDoc, def: StickerDef, x: Float = .5f, y: Float = .45f, size: Float = .28f, rot: Float = 0f, preset: Boolean = false): Pair<PosterDoc, Layer> = doc.add { id ->
        Layer(id = id, type = LayerType.STICKER, name = def.label, kind = def.key, x = x, y = y, size = size, rotation = rot, color = def.color, color2 = def.color2,
            shadowOn = true, shadowOpacity = .35f, shadowBlur = .2f, shadowDist = .06f, preset = preset)
    }

    fun newText(doc: PosterDoc, text: String, x: Float = .5f, y: Float = .12f, size: Float = .09f, preset: Boolean = false, f: (Layer) -> Layer = { it }): Pair<PosterDoc, Layer> = doc.add { id ->
        f(Layer(id = id, type = LayerType.TEXT, name = text.lines().first().take(18), text = text, x = x, y = y, size = size, color = Color.WHITE, font = 5, bold = true, preset = preset))
    }

    fun newEffect(doc: PosterDoc, def: EffectDef, preset: Boolean = false, f: (Layer) -> Layer = { it }): Pair<PosterDoc, Layer> = doc.add { id ->
        f(Layer(id = id, type = LayerType.EFFECT, name = def.label, kind = def.key, x = def.x, y = def.y, size = def.size, intensity = def.intensity, blend = def.blend, color = def.color, color2 = def.color2, preset = preset))
    }

    fun newFrame(doc: PosterDoc, def: FrameDef, overlay: Boolean, preset: Boolean = false, f: (Layer) -> Layer = { it }): Pair<PosterDoc, Layer> = doc.add { id ->
        f(Layer(id = id, type = if (overlay) LayerType.OVERLAY else LayerType.FRAME, name = def.label, kind = def.key, size = 1f, blend = def.blend, intensity = .6f,
            color = Color.WHITE, color2 = 0xFF111111.toInt(), opacity = if (overlay) .7f else 1f, preset = preset))
    }

    fun withPlayerLayout(doc: PosterDoc, variant: Int, style: Int): PosterDoc {
        val slots = when (variant) { 1 -> balancedGrid(doc.aspect); 2 -> fan; else -> scatter }
        var i = 0
        return doc.copy(layers = doc.layers.map {
            if (it.type == LayerType.PLAYER) {
                val s = slots[i.coerceAtMost(slots.lastIndex)]; i++
                it.copy(x = s.x, y = s.y, size = s.size, rotation = s.rot, tiltY = s.tiltY, tiltX = 0f, playerStyle = style)
            } else it
        })
    }

    private fun rank(t: LayerType) = when (t) {
        LayerType.OVERLAY -> 0; LayerType.PLAYER, LayerType.IMAGE -> 1; LayerType.CHARACTER -> 2
        LayerType.STICKER, LayerType.TEXT -> 3; LayerType.FRAME -> 4; LayerType.EFFECT -> 5
    }

    fun apply(base: PosterDoc, index: Int, title: String): PosterDoc {
        var d = base.copy(layers = base.layers.filterNot { it.preset })
        fun st(key: String, x: Float, y: Float, size: Float, rot: Float = 0f, f: (Layer) -> Layer = { it }) {
            val def = stickerDef(key) ?: return
            val (nd, l) = newSticker(d, def, x, y, size, rot, true); d = nd.update(l.id, f)
        }
        fun tx(text: String, x: Float, y: Float, size: Float, f: (Layer) -> Layer) { d = newText(d, text, x, y, size, true, f).first }
        fun fx(key: String, f: (Layer) -> Layer = { it }) { val def = effectDef(key) ?: return; d = newEffect(d, def, true, f).first }
        fun fr(key: String, overlay: Boolean = false, f: (Layer) -> Layer = { it }) {
            val def = (if (overlay) OverlayCatalog else FrameCatalog).firstOrNull { it.key == key } ?: return; d = newFrame(d, def, overlay, true, f).first
        }
        val preset = { n: Int -> TextPresets[n].transform }
        d = d.copy(overlayOpacity = 0f, brightness = if (d.bgUri != null) d.brightness else 0f)
        when (index) {
            0 -> { // Indie Music Collage (reference composition)
                d = withPlayerLayout(d, 0, 0)
                tx("My Top 5\nFav Songs", .5f, .13f, .13f) { preset(0)(it.copy(font = 12, rotation = -3f, color = 0xFF777777.toInt(), outlineWidth = .06f)) }
                st("vinyl", .06f, .13f, .26f); st("cassette", .86f, .09f, .2f, 10f); st("earphones", .83f, .42f, .22f)
                tx("#musicmylifegue", .5f, .76f, .045f) { preset(7)(it) }
                fx("lens_flare"); fx("grain") { it.copy(intensity = .35f) }
            }
            1 -> { // Vintage Playlist
                d = d.copy(bgColor = 0xFF3A2A1C.toInt()); d = withPlayerLayout(d, 0, 3)
                tx("Vintage\nPlaylist", .5f, .11f, .11f) { preset(5)(it.copy(font = 0, color = 0xFFF4EFE3.toInt(), textBgOn = false, outlineOn = false)) }
                st("polaroid", .84f, .22f, .22f, 8f); st("stamp", .13f, .22f, .2f, -8f); st("tape", .84f, .12f, .14f, 30f); st("vintage_badge", .2f, .5f, .18f, -10f)
                fx("old_paper") { it.copy(intensity = .55f, blend = 2) }; fx("vintage_fade") { it.copy(intensity = .8f) }; fx("scratches") { it.copy(intensity = .4f) }
            }
            2 -> { // Retro Cassette
                d = d.copy(bgColor = 0xFFE8D7B8.toInt()); d = withPlayerLayout(d, 2, 2)
                tx("RETRO\nCASSETTE", .5f, .15f, .12f) { preset(3)(it.copy(rotation = -2f)) }
                st("cassette", .5f, .42f, .5f, -4f); st("retro_sun", .85f, .1f, .24f); st("equalizer", .13f, .1f, .16f); st("music_label", .5f, .93f, .4f)
                fx("grain") { it.copy(intensity = .4f) }; fx("warm_tone") { it.copy(intensity = .6f) }
            }
            3 -> { // Y2K Music Poster
                d = d.copy(bgColor = 0xFF1B1F4D.toInt()); d = withPlayerLayout(d, 0, 1)
                tx("y2k\nplaylist", .5f, .13f, .14f) { preset(1)(it.copy(font = 11, color = 0xFFE6ECFF.toInt(), shadowColor = 0xFF7B8CFF.toInt())) }
                st("y2k_star", .14f, .12f, .22f, -10f); st("y2k_star", .88f, .38f, .14f, 12f); st("butterfly", .86f, .08f, .16f, 14f); st("heart", .12f, .42f, .12f, -14f); st("sparkle", .5f, .5f, .1f)
                fx("holographic") { it.copy(intensity = .45f) }; fx("sparkles") { it.copy(intensity = .8f) }
            }
            4 -> { // Scrapbook Aesthetic
                d = d.copy(bgColor = 0xFFD9CDB5.toInt()); d = withPlayerLayout(d, 0, 2)
                tx("my favs", .5f, .12f, .12f) { preset(7)(it.copy(font = 7, size = .12f)) }
                st("torn_paper", .5f, .5f, .75f, 2f); st("tape", .2f, .27f, .16f, -35f); st("tape", .8f, .62f, .16f, 40f); st("note", .84f, .16f, .2f, 8f); st("paperclip", .66f, .22f, .06f, 15f); st("polaroid", .16f, .1f, .2f, -8f)
                fx("paper_texture") { it.copy(intensity = .6f) }
            }
            5 -> { // Film Photography
                d = d.copy(bgColor = 0xFF111111.toInt()); d = withPlayerLayout(d, 1, 0)
                tx("35mm — top five", .5f, .93f, .05f) { preset(4)(it.copy(font = 13)) }
                st("film_roll", .87f, .1f, .14f, 10f); st("analog_camera", .14f, .1f, .18f, -8f)
                fr("film_border"); fr("camera_details"); fx("analog_film") { it.copy(intensity = .6f) }; fx("film_color")
            }
            6 -> { // Dark Editorial
                d = d.copy(bgColor = 0xFF0E0E10.toInt()); d = withPlayerLayout(d, 1, 0)
                tx("TOP\n5", .5f, .5f, .3f) { it.copy(font = 0, color = 0x22FFFFFF, align = 1, rotation = 0f) }
                st("editorial_tag", .9f, .1f, .12f); tx("THE PLAYLIST ISSUE", .3f, .06f, .035f) { it.copy(font = 4, bold = true, letterSpacing = .3f) }
                fr("deco_border"); fx("spotlight") { it.copy(intensity = .5f) }; fx("grain") { it.copy(intensity = .3f) }
            }
            7 -> { // Soft Vintage
                d = d.copy(bgColor = 0xFFEFE3D3.toInt()); d = withPlayerLayout(d, 2, 2)
                tx("soft hours", .5f, .15f, .14f) { preset(7)(it.copy(font = 7, textBg = 0x00000000, textBgOn = false, color = 0xFF7A5A48.toInt(), shadowOn = false)) }
                st("soft_cloud", .2f, .36f, .3f); st("flower", .84f, .3f, .16f, 12f); st("bow", .5f, .5f, .16f, -8f); st("sparkle", .12f, .1f, .08f)
                fx("soft_glow") { it.copy(intensity = .4f) }; fx("vintage_fade") { it.copy(intensity = .6f) }
            }
            8 -> { // Minimal Music
                d = d.copy(bgColor = 0xFFF2F2F4.toInt()); d = withPlayerLayout(d, 1, 2)
                d = d.copy(layers = d.layers.map { if (it.type == LayerType.PLAYER) it.copy(rotation = 0f, tiltY = 0f, shadowOpacity = .25f) else it })
                tx("top five", .5f, .06f, .05f) { it.copy(font = 4, color = 0xFF222222.toInt(), letterSpacing = .2f, bold = false) }
                st("minimal_circle", .86f, .9f, .12f, 0f) { it.copy(color = 0xFF222222.toInt()) }
            }
            else -> { // Grunge Collage
                d = d.copy(bgColor = 0xFF1A1A1A.toInt()); d = withPlayerLayout(d, 0, 0)
                tx("NOISE\nTOP 5", .5f, .13f, .13f) { preset(2)(it.copy(font = 14, rotation = -4f)) }
                st("grunge_patch", .15f, .1f, .2f, -8f); st("torn_paper", .85f, .65f, .3f, 6f); st("tape", .5f, .4f, .16f, -20f); st("hand_shapes", .12f, .45f, .14f, 0f)
                fx("grunge") { it.copy(intensity = .6f) }; fx("scratches"); fx("photocopy") { it.copy(intensity = .3f, opacity = .6f) }
            }
        }
        return d.copy(layers = d.layers.sortedBy { rank(it.type) })
    }
}
