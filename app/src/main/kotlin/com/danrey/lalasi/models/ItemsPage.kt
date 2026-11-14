// ItemsPage.kt

package com.danrey.lalasi.models

import com.music.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
