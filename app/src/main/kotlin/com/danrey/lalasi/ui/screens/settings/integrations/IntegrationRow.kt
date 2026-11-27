package com.danrey.lalasi.ui.screens.settings.integrations

import com.danrey.lalasi.ui.theme.LalasiInk
import com.danrey.lalasi.ui.theme.lalasiAdapt

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardColor get() = lalasiAdapt(0xFF1C1C1E)
private val CardShape = RoundedCornerShape(12.dp)
private val IconShape = RoundedCornerShape(7.dp)

/** grey line under a settings page's big title, as in the integrations and discord mocks */
@Composable
internal fun IntegrationPageSubtitle(text: String) {
    Text(
        text = text,
        color = LalasiInk.copy(alpha = 0.5f),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 2.dp, bottom = 18.dp),
    )
}

/** one service on the integrations page: its app icon, its name and one short line about it */
@Composable
internal fun IntegrationRow(
    name: String,
    description: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    // the colour behind the icon, for a round logo that should still read as a square tile
    iconBackground: Color = Color.Transparent,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(CardShape)
            .background(CardColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(34.dp)
                .clip(IconShape)
                .background(iconBackground),
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                text = name,
                color = LalasiInk,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                color = LalasiInk.copy(alpha = 0.55f),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
