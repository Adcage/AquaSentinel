package com.vision.swimsafe.ui.components.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vision.swimsafe.ui.components.common.AlarmPrimaryButton
import com.vision.swimsafe.ui.components.common.NeutralOutlinedButton
import com.vision.swimsafe.ui.components.common.SuccessPrimaryButton
import com.vision.swimsafe.ui.theme.AppDimens

@Composable
fun AlarmActionPanel(
    modifier: Modifier = Modifier,
    onDispatch: () -> Unit,
    onResolved: () -> Unit,
    onFalseAlarm: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.spacingMd),
    ) {
        AlarmPrimaryButton(text = "确认出警", onClick = onDispatch)
        SuccessPrimaryButton(text = "处理完成", onClick = onResolved)
        NeutralOutlinedButton(text = "标记误报", onClick = onFalseAlarm)
    }
}
