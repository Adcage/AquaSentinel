package com.vision.swimsafe.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vision.swimsafe.ui.theme.AppDimens

@Composable
fun AppTopBar(
    title: String,
    containerColor: Color,
    titleColor: Color,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimens.toolbarHeight)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        // 返回按钮（左侧）
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = titleColor,
                )
            }
        }

        // 标题（居中）
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
        )

        // 操作按钮（右侧）
        if (actions != null) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                actions()
            }
        }
    }
}
