package com.vision.swimsafe.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vision.swimsafe.data.alert.RealtimeAlertNotifier
import com.vision.swimsafe.data.remote.RemoteAuthRepository
import com.vision.swimsafe.ui.components.common.PrimaryActionButton
import com.vision.swimsafe.ui.theme.AndroidTheme
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.PageBackground
import com.vision.swimsafe.ui.theme.SafetyBlue
import com.vision.swimsafe.ui.theme.TextPrimary
import com.vision.swimsafe.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val account = remember { mutableStateOf("lifeguard01") }
    val password = remember { mutableStateOf("123456") }
    val passwordVisible = remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(SafetyBlue),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .background(CardBackground, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = SafetyBlue,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Spacer(modifier = Modifier.height(AppDimens.spacingLg))
                Text(
                    text = "AI 防溺水监测系统",
                    style = MaterialTheme.typography.headlineLarge,
                    color = CardBackground,
                )
                Text(
                    text = "救生员端",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CardBackground,
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.spacingLg),
            shape = RoundedCornerShape(AppDimens.cardRadius),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Column(modifier = Modifier.padding(AppDimens.spacingXl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "账号登录",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(AppDimens.spacingLg))
                OutlinedTextField(
                    value = account.value,
                    onValueChange = { account.value = it },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("账号") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppDimens.spacingLg))
                OutlinedTextField(
                    value = password.value,
                    onValueChange = { password.value = it },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    label = { Text("密码") },
                    visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                            Icon(
                                imageVector = if (passwordVisible.value) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible.value) "隐藏密码" else "显示密码",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppDimens.spacingXl))
                PrimaryActionButton(
                    text = if (loading) "登录中..." else "登录",
                    onClick = {
                        if (loading) {
                            return@PrimaryActionButton
                        }
                        loading = true
                        errorMessage = null
                        scope.launch {
                            val result = RemoteAuthRepository.login(account.value.trim(), password.value)
                            loading = false
                            if (result.isSuccess) {
                                RealtimeAlertNotifier.connectIfNeeded()
                                onLoginClick()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "登录失败，请稍后重试"
                            }
                        }
                    },
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(AppDimens.spacingMd))
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.spacingLg))
        Text(
            text = "忘记密码请联系管理员重置",
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Normal,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    AndroidTheme {
        LoginScreen(onLoginClick = {})
    }
}
