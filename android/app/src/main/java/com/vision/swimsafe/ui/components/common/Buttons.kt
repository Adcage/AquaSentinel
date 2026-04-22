package com.vision.swimsafe.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vision.swimsafe.ui.theme.AlarmRed
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.DividerColor
import com.vision.swimsafe.ui.theme.SafetyBlue
import com.vision.swimsafe.ui.theme.SuccessGreen
import com.vision.swimsafe.ui.theme.TextSecondary

@Composable
fun PrimaryActionButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SafetyBlue,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.primaryButtonHeight),
        shape = RoundedCornerShape(AppDimens.buttonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = CardBackground,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = SafetyBlue,
    borderColor: Color = SafetyBlue,
    containerColor: Color = Color.Transparent,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.secondaryButtonHeight),
        shape = RoundedCornerShape(AppDimens.buttonRadius),
        border = BorderStroke(1.5.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            containerColor = containerColor
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun AlarmPrimaryButton(text: String, onClick: () -> Unit) {
    PrimaryActionButton(text = text, containerColor = AlarmRed, onClick = onClick)
}

@Composable
fun SuccessPrimaryButton(text: String, onClick: () -> Unit) {
    PrimaryActionButton(text = text, containerColor = SuccessGreen, onClick = onClick)
}

@Composable
fun NeutralOutlinedButton(text: String, onClick: () -> Unit) {
    SecondaryActionButton(
        text = text,
        contentColor = TextSecondary,
        borderColor = DividerColor,
        onClick = onClick,
    )
}
