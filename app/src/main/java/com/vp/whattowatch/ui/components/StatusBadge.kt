package com.vp.whattowatch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vp.whattowatch.R
import com.vp.whattowatch.domain.model.WatchStatus
import com.vp.whattowatch.ui.theme.Spacing
import com.vp.whattowatch.ui.theme.StatusNotWatchedColor
import com.vp.whattowatch.ui.theme.StatusWatchedColor
import com.vp.whattowatch.ui.theme.StatusWatchingColor

@Composable
fun StatusBadge(
    status: WatchStatus,
    modifier: Modifier = Modifier
) {
    val (labelRes, color, icon) = when (status) {
        WatchStatus.NOT_WATCHED -> Triple(R.string.status_not_watched, StatusNotWatchedColor, Icons.Rounded.Schedule)
        WatchStatus.WATCHING -> Triple(R.string.status_watching, StatusWatchingColor, Icons.Rounded.PlayArrow)
        WatchStatus.WATCHED -> Triple(R.string.status_watched, StatusWatchedColor, Icons.Rounded.CheckCircle)
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.xSmall))
            Text(
                text = stringResource(id = labelRes),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = color
            )
        }
    }
}
