package com.danrey.lalasi.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The widget collection is declared in several places (manifest, provider XML, layouts, previews).
 * This checks they stay consistent, and that layouts only use views RemoteViews is allowed to inflate
 * (anything else crashes the launcher's inflation at runtime).
 */
class WidgetResourcesTest {
    private val resDir = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
    private val manifest = File(resDir.parentFile, "AndroidManifest.xml").readText()

    private val keys = listOf("compact", "square", "wide", "expanded", "large", "lyrics", "clyrics", "wlyrics", "minimal", "artwork")
    private val receivers = listOf(
        "LalasiCompactWidgetReceiver", "LalasiSquareWidgetReceiver", "LalasiWideWidgetReceiver",
        "LalasiExpandedWidgetReceiver", "LalasiLargeSquareWidgetReceiver", "LalasiLyricsWidgetReceiver",
        "LalasiCompactLyricsWidgetReceiver", "LalasiWideLyricsWidgetReceiver", "LalasiMinimalWidgetReceiver",
        "LalasiArtworkWidgetReceiver",
    )
    private val allowed = setOf(
        "FrameLayout", "LinearLayout", "RelativeLayout", "GridLayout", "ImageView", "ImageButton",
        "TextView", "Button", "ProgressBar", "Chronometer", "TextClock", "AnalogClock", "ViewFlipper",
    )

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    @Test fun `every widget has a provider, functional layout, preview layout and picker image`() {
        keys.forEach { key ->
            assertTrue("provider $key", File(resDir, "xml/lalasi_widget_${key}_info.xml").isFile)
            assertTrue("provider v31 $key", File(resDir, "xml-v31/lalasi_widget_${key}_info.xml").isFile)
            assertTrue("layout $key", File(resDir, "layout/widget_lw_$key.xml").isFile)
            assertTrue("preview layout $key", File(resDir, "layout/widget_lw_${key}_preview.xml").isFile)
            assertTrue("picker image $key", File(resDir, "drawable-nodpi/widget_preview_$key.png").isFile)
            assertTrue("manifest points at provider $key", manifest.contains("@xml/lalasi_widget_${key}_info"))
        }
    }

    @Test fun `every widget receiver is declared exactly once in the manifest`() {
        receivers.forEach { name ->
            assertEquals("receiver $name", 1, Regex("\\.widget\\.$name\"").findAll(manifest).count())
        }
    }

    @Test fun `widget layouts only use views RemoteViews can inflate`() {
        keys.forEach { key ->
            listOf("widget_lw_$key.xml", "widget_lw_${key}_preview.xml").forEach { name ->
                val nodes = parse(File(resDir, "layout/$name")).getElementsByTagName("*")
                for (i in 0 until nodes.length) {
                    val tag = nodes.item(i).nodeName
                    assertTrue("$name uses $tag", tag in allowed)
                }
            }
        }
    }
}
