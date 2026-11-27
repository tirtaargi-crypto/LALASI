// SimilarRecommendation.kt

package com.danrey.lalasi.models

import com.music.innertube.models.YTItem
import com.danrey.lalasi.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
