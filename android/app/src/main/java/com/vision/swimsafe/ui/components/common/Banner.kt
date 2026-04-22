package com.vision.swimsafe.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.FenceWarningRed
import com.vision.swimsafe.ui.theme.WarningOrange

@Composable
fun AlertBanner(text: String, warning: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (warning) WarningOrange else FenceWarningRed)
            .padding(horizontal = AppDimens.spacingLg, vertical = AppDimens.spacingMd),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = CardBackground,
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = AppDimens.spacingSm),
            color = CardBackground,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
