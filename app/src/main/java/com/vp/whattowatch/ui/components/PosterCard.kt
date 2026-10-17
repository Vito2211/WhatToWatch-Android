package com.vp.whattowatch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vp.whattowatch.R
import com.vp.whattowatch.data.local.image.ImageStorageManager
import com.vp.whattowatch.domain.model.ContentItem
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.ui.theme.CinemaBlack
import com.vp.whattowatch.ui.theme.CinemaBorder
import com.vp.whattowatch.ui.theme.CinemaCard
import com.vp.whattowatch.ui.theme.CinemaCardElevated
import com.vp.whattowatch.ui.theme.CinemaGold
import com.vp.whattowatch.ui.theme.CinemaTextPrimary
import com.vp.whattowatch.ui.theme.Spacing

@Composable
fun PosterCard(
    item: ContentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val posterFile = remember(item.posterPath) {
        item.posterPath?.let { ImageStorageManager.getPosterFile(context, it) }
    }
    val hasValidPoster = posterFile != null && posterFile.exists() && posterFile.length() > 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CinemaBorder), MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = CinemaCard)
    ) {
        Column {
            // Poster frame with fixed aspect ratio (2:3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(Spacing.POSTER_ASPECT_RATIO)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF20232E),
                                CinemaCardElevated,
                                CinemaBlack
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (hasValidPoster) {
                    AsyncImage(
                        model = posterFile,
                        contentDescription = stringResource(R.string.cd_poster_placeholder),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Subtle cinema emblem badge in center for items without poster artwork
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(CinemaGold.copy(alpha = 0.08f), CircleShape)
                            .border(BorderStroke(1.dp, CinemaGold.copy(alpha = 0.22f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getContentTypeIcon(item.contentType),
                            contentDescription = stringResource(R.string.cd_poster_placeholder),
                            tint = CinemaGold.copy(alpha = 0.85f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Content Type Badge (Top-Start)
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = CinemaBlack.copy(alpha = 0.88f),
                    border = BorderStroke(0.5.dp, CinemaGold.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.small)
                ) {
                    Text(
                        text = getContentTypeLabel(item.contentType).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = CinemaGold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Bottom subtle scrim for visual grounding
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, CinemaBlack.copy(alpha = 0.6f))
                            )
                        )
                )
            }

            // Metadata column
            Column(
                modifier = Modifier.padding(Spacing.small)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = CinemaTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Spacing.xSmall))

                StatusBadge(status = item.watchStatus)
            }
        }
    }
}

fun getContentTypeIcon(type: ContentType): ImageVector {
    return when (type) {
        ContentType.MOVIE -> Icons.Rounded.Movie
        ContentType.SERIES -> Icons.Rounded.Tv
        ContentType.VIDEO -> Icons.Rounded.Videocam
        ContentType.OTHER -> Icons.Rounded.VideoLibrary
    }
}

@Composable
fun getContentTypeLabel(type: ContentType): String {
    return when (type) {
        ContentType.MOVIE -> stringResource(R.string.type_movie)
        ContentType.SERIES -> stringResource(R.string.type_series)
        ContentType.VIDEO -> stringResource(R.string.type_video)
        ContentType.OTHER -> stringResource(R.string.type_other)
    }
}
