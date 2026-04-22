package com.vision.swimsafe.ui.components.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vision.swimsafe.data.remote.RemoteHomeRepository
import com.vision.swimsafe.ui.components.common.PrimaryActionButton
import com.vision.swimsafe.ui.components.common.SecondaryActionButton
import com.vision.swimsafe.ui.theme.AppDimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveReportSheet(
    onDismiss: () -> Unit,
    onLeaveSuccess: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedReason by remember { mutableStateOf("喝水") }
    var selectedDuration by remember { mutableStateOf<Int?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "离岗报备",
            modifier = Modifier.padding(horizontal = AppDimens.spacingLg),
            style = MaterialTheme.typography.titleLarge,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.spacingLg),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spacingSm),
        ) {
            listOf("喝水", "如厕", "换班", "其他").forEach { reason ->
                FilterChip(
                    selected = selectedReason == reason,
                    onClick = { selectedReason = reason },
                    label = { Text(reason) },
                )
            }
        }
        Text(
            text = "预计返回时间（可选）",
            modifier = Modifier.padding(horizontal = AppDimens.spacingLg, vertical = AppDimens.spacingSm),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.spacingLg),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spacingSm),
        ) {
            listOf(5, 10, 15).forEach { minutes ->
                FilterChip(
                    selected = selectedDuration == minutes,
                    onClick = { selectedDuration = if (selectedDuration == minutes) null else minutes },
                    label = { Text("${minutes}分钟") },
                )
            }
        }
        SecondaryActionButton(
            text = "取消",
            modifier = Modifier.padding(horizontal = AppDimens.spacingLg, vertical = AppDimens.spacingSm),
            onClick = onDismiss,
        )
        PrimaryActionButton(
            text = if (isSubmitting) "提交中..." else "确认报备",
            modifier = Modifier.padding(horizontal = AppDimens.spacingLg, vertical = AppDimens.spacingSm),
            onClick = {
                if (isSubmitting) return@PrimaryActionButton
                isSubmitting = true
                scope.launch {
                    try {
                        RemoteHomeRepository.submitLeaveReport(selectedReason, selectedDuration)
                        Toast.makeText(context, "离岗报备成功", Toast.LENGTH_SHORT).show()
                        onLeaveSuccess()
                        onDismiss()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "提交失败", Toast.LENGTH_SHORT).show()
                    } finally {
                        isSubmitting = false
                    }
                }
            },
        )
    }
}
