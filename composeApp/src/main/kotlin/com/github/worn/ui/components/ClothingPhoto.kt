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
 * Deliberately does not check `File.exists()`: that is a blocking `stat()` syscall, and in a
 * composable body it runs once per visible cell per recomposition. The placeholder sits
 * underneath instead, so a missing file just leaves it visible. The [File] is remembered so
 * Coil's model identity — and therefore its memory cache — stays stable.
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
