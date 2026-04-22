package com.vision.swimsafe.ui.components.alarm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vision.swimsafe.ui.components.common.PrimaryActionButton
import com.vision.swimsafe.ui.components.common.SecondaryActionButton
import com.vision.swimsafe.ui.model.AlarmBrief
import com.vision.swimsafe.ui.theme.AlarmBorder
import com.vision.swimsafe.ui.theme.AlarmRed
import com.vision.swimsafe.ui.theme.AlarmRedDark
import com.vision.swimsafe.ui.theme.AlarmSurface
import com.vision.swimsafe.ui.theme.NeutralGray
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.TextPrimary
import com.vision.swimsafe.ui.theme.TextSecondary

@Composable
fun AlarmDialog(
    alarm: AlarmBrief,
    onAcknowledge: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = AlarmSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spacingSm)) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = AlarmRed,
                )
                Column {
                    Text(text = alarm.type, style = MaterialTheme.typography.titleLarge, color = AlarmRedDark)
                    Text(text = alarm.time, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spacingLg)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, AlarmBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.spacingLg),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.spacingSm),
                    ) {
                        Text(text = alarm.cameraName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(text = alarm.locationDescription, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(text = "紧急联系人：${alarm.emergencyContact}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
                PrimaryActionButton(text = "立即查看", onClick = onOpenDetail)
                SecondaryActionButton(
                    text = "已收到",
                    contentColor = TextPrimary,
                    borderColor = NeutralGray,
                    containerColor = CardBackground,
                    onClick = onAcknowledge,
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}
