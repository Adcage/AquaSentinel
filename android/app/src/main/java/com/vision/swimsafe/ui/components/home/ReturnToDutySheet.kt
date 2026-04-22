package com.vision.swimsafe.ui.components.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vision.swimsafe.data.remote.RemoteHomeRepository
import com.vision.swimsafe.ui.components.common.PrimaryActionButton
import com.vision.swimsafe.ui.components.common.SecondaryActionButton
import com.vision.swimsafe.ui.theme.AppDimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnToDutySheet(
    onDismiss: () -> Unit,
    onReturnSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingMd),
        ) {
            Text(
                text = "回岗确认",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "您确定要返回岗位吗？",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(AppDimens.spacingMd))
            SecondaryActionButton(
                text = "取消",
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss,
            )
            PrimaryActionButton(
                text = if (isSubmitting) "提交中..." else "确认回岗",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (isSubmitting) return@PrimaryActionButton
                    isSubmitting = true
                    scope.launch {
                        try {
                            RemoteHomeRepository.returnToDuty()
                            Toast.makeText(context, "回岗成功", Toast.LENGTH_SHORT).show()
                            onDismiss()
                            onReturnSuccess()
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "回岗失败", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
            )
        }
    }
}
