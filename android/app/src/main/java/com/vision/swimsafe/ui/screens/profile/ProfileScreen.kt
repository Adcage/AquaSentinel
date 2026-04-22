package com.vision.swimsafe.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.vision.swimsafe.data.alert.RealtimeAlertNotifier
import com.vision.swimsafe.data.remote.RemoteAuthRepository
import com.vision.swimsafe.data.remote.RemoteProfileRepository
import com.vision.swimsafe.ui.model.ProfileMenuItemModel
import com.vision.swimsafe.ui.model.ProfileUiState
import com.vision.swimsafe.ui.components.common.AppTopBar
import com.vision.swimsafe.ui.components.profile.ProfileMenuItem
import com.vision.swimsafe.ui.theme.AndroidTheme
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.AlarmRed
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.PageBackground
import com.vision.swimsafe.ui.theme.SafetyBlue
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var activeDialog by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val state by produceState(
        initialValue = ProfileUiState(
            name = "加载中",
            account = "--",
            venueName = "--",
            networkStatus = "--",
            tokenExpireText = "--",
            menuItems = listOf(ProfileMenuItemModel("退出登录", accent = true)),
        ),
    ) {
        value = RemoteProfileRepository.getProfileUiState()
    }
    Column(modifier = modifier.fillMaxSize().background(PageBackground)) {
        AppTopBar(title = "我的", containerColor = CardBackground, titleColor = com.vision.swimsafe.ui.theme.TextPrimary)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.spacingLg),
            shape = RoundedCornerShape(AppDimens.cardRadius),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SafetyBlue)
                    .padding(AppDimens.spacingXl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimens.spacingSm),
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CardBackground)
                        .padding(AppDimens.spacingLg),
                ) {
                    Text(text = state.name.take(1), style = MaterialTheme.typography.headlineLarge, color = SafetyBlue)
                }
                Text(text = state.name, style = MaterialTheme.typography.headlineLarge, color = CardBackground)
                Text(text = state.account, style = MaterialTheme.typography.bodyMedium, color = CardBackground)
                Text(text = state.venueName, style = MaterialTheme.typography.bodyMedium, color = CardBackground)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.spacingLg),
            shape = RoundedCornerShape(AppDimens.cardRadius),
        ) {
            Column {
                state.menuItems.forEach { item ->
                    ProfileMenuItem(
                        item = item,
                        onClick = {
                            when {
                                item.accent -> showLogoutConfirm = true
                                else -> activeDialog = item.title
                            }
                        },
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.spacingLg),
            shape = RoundedCornerShape(AppDimens.cardRadius),
        ) {
            Column(modifier = Modifier.padding(AppDimens.spacingLg)) {
                Text(text = "网络状态", style = MaterialTheme.typography.titleMedium)
                Text(text = state.networkStatus, style = MaterialTheme.typography.bodyMedium)
                Text(text = state.tokenExpireText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    when (activeDialog) {
        "个人信息" -> PersonalInfoDialog(state = state, onDismiss = { activeDialog = null })
        "修改密码" -> ChangePasswordDialog(onDismiss = { activeDialog = null })
        "关于系统" -> AboutSystemDialog(onDismiss = { activeDialog = null })
    }

    if (showLogoutConfirm) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutConfirm = false
                scope.launch {
                    RemoteAuthRepository.logout()
                    RealtimeAlertNotifier.disconnect()
                    onLogout()
                }
            },
            onDismiss = { showLogoutConfirm = false },
        )
    }
}

@Composable
private fun PersonalInfoDialog(state: ProfileUiState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("个人信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spacingSm)) {
                Text("姓名：${state.name}", style = MaterialTheme.typography.bodyMedium)
                Text("账号：${state.account}", style = MaterialTheme.typography.bodyMedium)
                Text("${state.venueName}", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun ChangePasswordDialog(onDismiss: () -> Unit) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spacingSm)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it; errorMsg = null },
                    label = { Text("当前密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMsg = null },
                    label = { Text("新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMsg = null },
                    label = { Text("确认新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = AlarmRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    oldPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() ->
                        errorMsg = "请填写所有字段"
                    newPassword != confirmPassword ->
                        errorMsg = "两次密码输入不一致"
                    newPassword.length < 6 ->
                        errorMsg = "新密码长度不能少于6位"
                    else ->
                        errorMsg = "功能暂未开放，请联系管理员修改密码"
                }
            }) { Text("提交") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun AboutSystemDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于系统") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spacingSm)) {
                Text("SwimSafe 防溺水监测系统", style = MaterialTheme.typography.titleSmall)
                Text("版本：1.0.0", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "基于AI视觉技术的泳池溺水智能监测预警系统，实时保障游泳者安全。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认退出") },
        text = { Text("确定要退出登录吗？") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    AndroidTheme {
        ProfileScreen(onLogout = {})
    }
}
