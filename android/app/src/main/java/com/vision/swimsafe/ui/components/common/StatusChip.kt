package com.vision.swimsafe.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vision.swimsafe.ui.theme.AlarmBorder
import com.vision.swimsafe.ui.theme.AlarmLight
import com.vision.swimsafe.ui.theme.AlarmRedDark
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.GrayLight
import com.vision.swimsafe.ui.theme.NeutralGray
import com.vision.swimsafe.ui.theme.OrangeBorder
import com.vision.swimsafe.ui.theme.OrangeLight
import com.vision.swimsafe.ui.theme.SuccessBorder
import com.vision.swimsafe.ui.theme.SuccessGreen
import com.vision.swimsafe.ui.theme.SuccessLight

@Composable
fun StatusChip(text: String) {
    val (container, border, content) = chipColors(text)
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(4.dp))
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .padding(horizontal = AppDimens.spacingSm, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}

private fun chipColors(text: String): Triple<Color, Color, Color> = when (text) {
    "溺水预警", "未处理" -> Triple(AlarmLight, AlarmBorder, AlarmRedDark)
    "人员越界", "处理中" -> Triple(OrangeLight, OrangeBorder, AlarmRedDark)
    "已处理", "在岗中", "在岗" -> Triple(SuccessLight, SuccessBorder, SuccessGreen)
    else -> Triple(GrayLight, GrayLight, NeutralGray)
}
