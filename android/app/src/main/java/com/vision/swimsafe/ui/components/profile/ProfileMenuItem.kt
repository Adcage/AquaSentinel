package com.vision.swimsafe.ui.components.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vision.swimsafe.ui.model.ProfileMenuItemModel
import com.vision.swimsafe.ui.theme.AlarmRed
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.SafetyBlue
import com.vision.swimsafe.ui.theme.TextPrimary
import com.vision.swimsafe.ui.theme.TextTertiary

@Composable
fun ProfileMenuItem(
    item: ProfileMenuItemModel,
    onClick: () -> Unit,
) {
    val titleColor = if (item.accent) AlarmRed else TextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.spacingLg, vertical = AppDimens.spacingLg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = if (item.accent) AlarmRed else SafetyBlue,
            )
            Text(
                text = item.title,
                modifier = Modifier.padding(start = AppDimens.spacingMd),
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextTertiary,
        )
    }
}
