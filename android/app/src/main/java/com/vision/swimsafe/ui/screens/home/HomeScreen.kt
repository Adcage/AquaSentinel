package com.vision.swimsafe.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.vision.swimsafe.data.alert.RealtimeAlertNotifier
import com.vision.swimsafe.data.remote.RemoteHomeRepository
import com.vision.swimsafe.data.remote.RemoteAlarmAction
import com.vision.swimsafe.data.remote.RemoteAlarmRepository
import com.vision.swimsafe.ui.components.alarm.AlarmDialog
import com.vision.swimsafe.ui.components.common.AlertBanner
import com.vision.swimsafe.ui.components.common.SecondaryActionButton
import com.vision.swimsafe.ui.components.common.StatusChip
import com.vision.swimsafe.ui.components.common.AppTopBar
import com.vision.swimsafe.ui.components.home.LeaveReportSheet
import com.vision.swimsafe.ui.components.home.ReturnToDutySheet
import com.vision.swimsafe.ui.theme.AndroidTheme
import com.vision.swimsafe.ui.theme.AlarmRed
import com.vision.swimsafe.ui.theme.AlarmRedDark
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.PageBackground
import com.vision.swimsafe.ui.theme.SafetyBlue
import com.vision.swimsafe.ui.theme.SuccessGreen
import com.vision.swimsafe.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenAlarmDetail: (String) -> Unit,
    onNavigateToVideoList: () -> Unit,
    onNavigateToAlarmList: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var refreshVersion by remember { mutableStateOf(0) }
    val state by produceState(
        initialValue = com.vision.swimsafe.ui.model.HomeUiState(
            statusTitle = "加载中",
            venueName = "--",
            currentTime = "--",
            todayAlarmCount = 0,
            online = false,
            outOfFence = false,
            networkWarning = null,
            activeAlarm = null,
        ),
        key1 = refreshVersion,
    ) {
        value = RemoteHomeRepository.getHomeUiState()
    }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var showLeaveSheet by remember { mutableStateOf(false) }
    var showReturnToDutySheet by remember { mutableStateOf(false) }
    var realtimeAlarm by remember { mutableStateOf<com.vision.swimsafe.ui.model.AlarmBrief?>(null) }

    LaunchedEffect(state.activeAlarm?.id) {
        showAlarmDialog = RemoteHomeRepository.shouldShowDialogOnHomeLoad(state.activeAlarm?.id)
    }

    LaunchedEffect(Unit) {
        RealtimeAlertNotifier.uiEvents.collect { event ->
            realtimeAlarm = com.vision.swimsafe.ui.model.AlarmBrief(
                id = event.alertId,
                type = event.alarmType,
                cameraName = event.cameraName,
                locationDescription = event.locationDescription,
                emergencyContact = event.emergencyContact,
                time = event.eventTime,
                status = event.status,
            )
            showAlarmDialog = true
            refreshVersion += 1
        }
    }

    Column(modifier = modifier.fillMaxSize().background(PageBackground)) {
        AppTopBar(
            title = "在岗监控",
            containerColor = SafetyBlue,
            titleColor = CardBackground,
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Refresh, contentDescription = "刷新", tint = CardBackground)
                }
            },
        )

        if (state.outOfFence) {
            AlertBanner(text = "已超出在岗区域，请立即返回")
        }
        val networkWarning = state.networkWarning
        if (networkWarning != null) {
            AlertBanner(text = networkWarning, warning = true)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingLg),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(AppDimens.spacingLg),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(AppDimens.cardRadius),
                    colors = CardDefaults.cardColors(containerColor = SafetyBlue),
                ) {
                    Column(modifier = Modifier.padding(AppDimens.spacingLg)) {
                        Text(text = "当前状态", style = MaterialTheme.typography.labelSmall, color = CardBackground)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = AppDimens.spacingSm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val statusColor = if (state.isOffDuty) AlarmRed else SuccessGreen
                                Box(
                                    modifier = Modifier
                                        .background(statusColor, CircleShape)
                                        .height(AppDimens.spacingSm)
                                        .padding(AppDimens.spacingSm),
                                )
                                Text(
                                    text = state.statusTitle,
                                    modifier = Modifier.padding(start = AppDimens.spacingSm),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = CardBackground,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = state.venueName, style = MaterialTheme.typography.bodyMedium, color = CardBackground)
                                Text(text = state.currentTime, style = MaterialTheme.typography.bodySmall, color = CardBackground)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = AppDimens.spacingLg),
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.spacingMd),
                        ) {
                            val buttonBg = Color.White.copy(alpha = 0.2f)
                            if (state.isOffDuty) {
                                SecondaryActionButton(
                                    text = "回岗",
                                    modifier = Modifier.weight(1f),
                                    contentColor = CardBackground,
                                    borderColor = Color.White.copy(alpha = 0.5f),
                                    containerColor = buttonBg,
                                    onClick = { showReturnToDutySheet = true },
                                )
                            } else {
                                SecondaryActionButton(
                                    text = "离岗报备",
                                    modifier = Modifier.weight(1f),
                                    contentColor = CardBackground,
                                    borderColor = Color.White.copy(alpha = 0.5f),
                                    containerColor = buttonBg,
                                    onClick = { showLeaveSheet = true },
                                )
                            }
                            SecondaryActionButton(
                                text = "刷新",
                                modifier = Modifier.weight(1f),
                                contentColor = CardBackground,
                                borderColor = Color.White.copy(alpha = 0.5f),
                                containerColor = buttonBg,
                                onClick = {
                                    refreshVersion += 1
                                },
                            )
                        }
                    }
                }
            }

            item {
                SummaryCard(count = state.todayAlarmCount)
            }

            item {
                Text(text = "快捷通道", style = MaterialTheme.typography.titleMedium)
            }

            items(listOf("查看实时视频", "报警记录")) { title ->
                QuickEntryCard(
                    title = title,
                    onClick = {
                        when (title) {
                            "查看实时视频" -> onNavigateToVideoList()
                            "报警记录" -> onNavigateToAlarmList()
                        }
                    },
                )
            }
        }
    }

    val dialogAlarm = realtimeAlarm ?: state.activeAlarm
    if (showAlarmDialog && dialogAlarm != null) {
        AlarmDialog(
            alarm = dialogAlarm,
            onAcknowledge = {
                scope.launch {
                    if (dialogAlarm.id.toLongOrNull() != null) {
                        RemoteAlarmRepository.submitAlarmAction(
                            alarmId = dialogAlarm.id,
                            action = RemoteAlarmAction.ACKNOWLEDGED,
                            note = "救生员已收到",
                        )
                    }
                }
                realtimeAlarm = null
                showAlarmDialog = false
            },
            onOpenDetail = {
                if (dialogAlarm.id.toLongOrNull() != null) {
                    onOpenAlarmDetail(dialogAlarm.id)
                }
                realtimeAlarm = null
                showAlarmDialog = false
            },
        )
    }

    if (showLeaveSheet) {
        LeaveReportSheet(
            onDismiss = { showLeaveSheet = false },
            onLeaveSuccess = {
                scope.launch {
                    delay(800)
                    refreshVersion += 1
                }
            },
        )
    }

    if (showReturnToDutySheet) {
        ReturnToDutySheet(
            onDismiss = { showReturnToDutySheet = false },
            onReturnSuccess = { refreshVersion += 1 },
        )
    }
}

@Composable
private fun SummaryCard(count: Int) {
    Card(shape = RoundedCornerShape(AppDimens.cardRadius)) {
        Column(modifier = Modifier.padding(AppDimens.spacingLg)) {
            Text(text = "今日报警", style = MaterialTheme.typography.titleMedium)
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = if (count == 0) SuccessGreen else AlarmRedDark,
            )
            Text(text = "今日共收到 $count 条报警", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun QuickEntryCard(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(AppDimens.cardRadius),
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.spacingLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (title.contains("视频")) Icons.Filled.Videocam else Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = SafetyBlue,
            )
            Text(
                text = title,
                modifier = Modifier.padding(start = AppDimens.spacingMd),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AndroidTheme {
        HomeScreen(
            onOpenAlarmDetail = {},
            onNavigateToVideoList = {},
            onNavigateToAlarmList = {},
        )
    }
}
