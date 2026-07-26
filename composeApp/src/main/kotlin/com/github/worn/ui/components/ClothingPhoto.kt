package com.github.worn.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.github.worn.ui.theme.WornColors
import java.io.File

/**
 * A stored clothing photo with a garment-icon placeholder behind it.
 *
 * Two deliberate performance choices:
 *
 * 1. The [File] is `remember`ed on the path. Allocating it in the composable body would produce a
 *    new instance every recomposition, which changes Coil's model identity and defeats its
 *    memory cache.
 * 2. There is no `File.exists()` check. That is a blocking `stat()` syscall, and running it in a
 *    composable body means one syscall per visible cell per recomposition — on the main thread,
 *    while the grid scrolls. Instead the placeholder is always composed *underneath* the image,
 *    so a missing or unreadable file simply leaves the placeholder visible.
 */
@Composable
fun ClothingPhoto(
    photoPath: String,
    contentDescription: String?,
    shape: Shape,
    placeholderIconSize: Dp,
    modifier: Modifier = Modifier,
    placeholderTint: Color = WornColors.IconMuted,
) {
    val photoFile = remember(photoPath) {
        photoPath.takeIf { it.isNotEmpty() }?.let(::File)
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        Icon(
            imageVector = Icons.Outlined.Checkroom,
            contentDescription = if (photoFile == null) contentDescription else null,
            tint = placeholderTint,
            modifier = Modifier.size(placeholderIconSize),
        )
        if (photoFile != null) {
            AsyncImage(
                model = photoFile,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape),
            )
        }
    }
}
