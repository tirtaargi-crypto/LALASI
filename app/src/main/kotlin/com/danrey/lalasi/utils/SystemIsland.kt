package com.danrey.lalasi.utils

import android.os.Build

/**
 * Some phones ship their own island: Samsung's Now Bar (One UI 7+), Xiaomi HyperOS, ColorOS / OxygenOS
 * Fluid Cloud, vivo OriginOS. They pick up the music player notification by themselves, so the app's own
 * island steps aside there and nothing shows twice. Nothing of the system's is switched off or touched.
 *
 * Android gives no API to ask for this, so it is read from the vendor's build properties. A wrong guess
 * is fixable by the person: Customize > Dynamic Island has a switch to show LALASI's island anyway.
 */
object SystemIsland {
    /** the name of the built-in island on this phone, or null when there is none we know of */
    val name: String? by lazy { runCatching { detect() }.getOrNull() }

    private fun prop(key: String): String? = runCatching {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, key) as? String
    }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    private fun major(version: String?): Int? =
        version?.let { Regex("\\d+").find(it)?.value?.toIntOrNull() }

    private fun detect(): String? {
        val maker = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        // Samsung One UI 7 is semPlatformInt 190000; it brings the Now Bar and live notifications
        if (maker == "samsung") {
            val sem = runCatching {
                Build.VERSION::class.java.getField("SEM_PLATFORM_INT").getInt(null)
            }.getOrDefault(0)
            if (sem >= 190000) return "Now Bar"
        }

        // Xiaomi / Redmi / POCO on HyperOS
        if (prop("ro.mi.os.version.name") != null && Build.VERSION.SDK_INT >= 34) return "HyperOS Island"

        // OPPO / OnePlus / realme: ColorOS and OxygenOS 15 have Fluid Cloud
        if (maker in setOf("oppo", "oneplus", "realme") || brand in setOf("oppo", "oneplus", "realme")) {
            val rom = major(prop("ro.build.version.oplusrom"))
            if (rom != null && rom >= 15) return "Fluid Cloud"
        }

        // vivo / iQOO: OriginOS 5 has Origin Island
        if (maker == "vivo" || brand == "iqoo") {
            val os = major(prop("ro.vivo.os.version"))
            if (prop("ro.vivo.os.name").equals("OriginOS", ignoreCase = true) && os != null && os >= 5) return "Origin Island"
        }
        return null
    }
}
