// OnboardingAvatar.kt
//
// The plain profile picture + name the onboarding shows where the ID card used to be:
// a round avatar, the name under it and an "AKUN TAMU" label. No card, no barcode.

package com.danrey.lalasi.ui.screens.setup.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.danrey.lalasi.R

/** Avatar, name and label. With [onPhotoClick] the avatar is tappable and shows a small pencil/plus badge. */
@Composable
internal fun OnbProfileAvatar(
    username: String,
    photo: Any?,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 124.dp,
    onPhotoClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.size(avatarSize + 8.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(avatarSize)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0xFF4F4F4F))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    .then(if (onPhotoClick != null) Modifier.clickable(onClick = onPhotoClick) else Modifier),
            ) {
                if (photo != null) {
                    AsyncImage(
                        model = photo,
                        contentDescription = "Foto profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.person),
                        contentDescription = "Foto profil",
                        tint = Color(0xFFE5E5E5),
                        modifier = Modifier.size(avatarSize * 0.46f),
                    )
                }
            }
            if (onPhotoClick != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Onb.Badge)
                        .clickable(onClick = onPhotoClick),
                ) {
                    Icon(
                        painterResource(if (photo != null) R.drawable.edit else R.drawable.add),
                        contentDescription = "Ganti foto",
                        tint = Color(0xFF1C1C1C),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = username.trim().ifBlank { "Tamu LALASI" },
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "AKUN TAMU",
            color = Onb.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
    }
}
