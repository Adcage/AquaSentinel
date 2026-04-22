package com.vision.swimsafe.ui.screens.alarm

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vision.swimsafe.data.remote.RemoteAlarmAction
import com.vision.swimsafe.data.remote.RemoteAlarmRepository
import com.vision.swimsafe.data.remote.RemoteCameraRepository
import com.vision.swimsafe.ui.model.AlarmDetailUiState
import com.vision.swimsafe.ui.components.alarm.AlarmActionPanel
import com.vision.swimsafe.ui.components.common.AppTopBar
import com.vision.swimsafe.ui.components.common.EmptyState
import com.vision.swimsafe.ui.components.common.PrimaryActionButton
import com.vision.swimsafe.ui.components.common.StatusChip
import com.vision.swimsafe.ui.components.video.CameraPlaybackAvailability
import com.vision.swimsafe.ui.components.video.MjpegStreamPlayer
import com.vision.swimsafe.ui.components.video.PlayerState
import com.vision.swimsafe.ui.components.video.VideoUnavailablePlaceholder
import com.vision.swimsafe.ui.components.video.WsJpegFramePlayer
import com.vision.swimsafe.ui.components.video.resolveAlarmPlaybackAvailability
import com.vision.swimsafe.ui.theme.AlarmRed
import com.vision.swimsafe.ui.theme.AndroidTheme
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.PageBackground
import com.vision.swimsafe.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDetailScreen(
    alarmId: String,
    onBack: () -> Unit,
) {
    var refreshVersion by remember { mutableStateOf(0) }
    val state by produceState<AlarmDetailUiState?>(initialValue = null, key1 = alarmId, key2 = refreshVersion) {
        value = RemoteAlarmRepository.getAlarmDetailUiState(alarmId)
        android.util.Log.d("AlarmDetailScreen", "Loaded alarm detail: videoStreamUrl=${value?.videoStreamUrl}, cameraId=${value?.cameraId}")
    }
    val cameraAvailability by produceState(
        initialValue = CameraPlaybackAvailability(canPlay = true),
        key1 = state?.cameraId,
        key2 = refreshVersion,
    ) {
        val cameraId = state?.cameraId
        if (cameraId == null || cameraId <= 0) {
            value = resolveAlarmPlaybackAvailability(cameraId, null)
            return@produceState
        }
        val cameras = RemoteCameraRepository.getCameraList()
        if (cameras.isEmpty()) {
            value = CameraPlaybackAvailability(canPlay = true)
            return@produceState
        }
        val targetCamera = cameras.firstOrNull { it.id == cameraId }
        value = resolveAlarmPlaybackAvailability(cameraId, targetCamera)
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showRemarkSheet by remember { mutableStateOf(false) }
    var remark by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf(RemoteAlarmAction.DISPATCH) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var actionSubmitting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(PageBackground)) {
        AppTopBar(
            title = "报警详情",
            containerColor = AlarmRed,
            titleColor = CardBackground,
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingLg),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(AppDimens.spacingLg),
        ) {
            if (state == null) {
                item {
                    EmptyState(
                        title = "未找到报警详情",
                        subtitle = "请返回报警列表后重新进入",
                    )
                }
            } else {
                val detailState = state ?: return@LazyColumn
                item {
                    var wsPlayerState by remember(detailState.cameraId, refreshVersion) {
                        mutableStateOf<PlayerState>(PlayerState.Idle)
                    }
                    var mjpegPlayerState by remember(detailState.cameraId, refreshVersion) {
                        mutableStateOf<PlayerState>(PlayerState.Idle)
                    }
                    val isVideoPlaying =
                        wsPlayerState is PlayerState.Playing || mjpegPlayerState is PlayerState.Playing

                    Card(shape = RoundedCornerShape(AppDimens.cardRadius)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                            ) {
                                if (cameraAvailability.canPlay) {
                                    WsJpegFramePlayer(
                                        modifier = Modifier.fillMaxSize(),
                                        cameraId = detailState.cameraId,
                                        reloadKey = refreshVersion,
                                        onStateChange = { wsPlayerState = it },
                                        fallbackContent = {
                                            MjpegStreamPlayer(
                                                modifier = Modifier.fillMaxSize(),
                                                cameraId = detailState.cameraId,
                                                fallbackStreamUrl = detailState.videoStreamUrl,
                                                onStateChange = { mjpegPlayerState = it },
                                            )
                                        },
                                    )
                                } else {
                                    VideoUnavailablePlaceholder(
                                        modifier = Modifier.fillMaxSize(),
                                        message = cameraAvailability.message ?: "当前设备暂无可用视频",
                                    )
                                }
                                Text(
                                    text = if (cameraAvailability.canPlay) detailState.videoStatusText else "视频不可用",
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(
                                            color = CardBackground.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                            if (!isVideoPlaying) {
                                PrimaryActionButton(
                                    text = "重新加载",
                                    modifier = Modifier
                                        .padding(horizontal = AppDimens.spacingLg)
                                        .padding(top = AppDimens.spacingMd, bottom = AppDimens.spacingMd),
                                    onClick = {
                                        refreshVersion += 1
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    DetailCard(title = "报警信息") {
                        DetailRow(label = "报警类型", value = detailState.alarm.type, chip = true)
                        DetailRow(label = "触发时间", value = detailState.alarm.time)
                        DetailRow(label = "摄像头位置", value = detailState.alarm.cameraName)
                        DetailRow(label = "具体位置", value = detailState.alarm.locationDescription)
                        DetailRow(label = "紧急联系人", value = detailState.alarm.emergencyContact)
                        DetailRow(label = "算法识别", value = detailState.detectionResult)
                    }
                }
                item {
                    DetailCard(title = "处置操作") {
                        AlarmActionPanel(
                            onDispatch = {
                                pendingAction = RemoteAlarmAction.DISPATCH
                                actionError = null
                                showRemarkSheet = true
                            },
                            onResolved = {
                                pendingAction = RemoteAlarmAction.RESOLVED
                                actionError = null
                                showRemarkSheet = true
                            },
                            onFalseAlarm = {
                                pendingAction = RemoteAlarmAction.FALSE_ALARM
                                actionError = null
                                showRemarkSheet = true
                            },
                        )
                    }
                }
                item {
                    DetailCard(title = "处理进展") {
                        detailState.handlingTimeline.forEach { timeline ->
                            Text(
                                text = timeline,
                                modifier = Modifier.padding(vertical = AppDimens.spacingSm),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRemarkSheet && state != null) {
        ModalBottomSheet(onDismissRequest = { showRemarkSheet = false }) {
            Text(
                text = "添加备注（可选）",
                modifier = Modifier.padding(horizontal = AppDimens.spacingLg),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = remark,
                onValueChange = { if (it.length <= 20) remark = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.spacingLg),
                label = { Text("最多 20 字") },
            )
            if (actionError != null) {
                Text(
                    text = actionError.orEmpty(),
                    modifier = Modifier.padding(horizontal = AppDimens.spacingLg),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(modifier = Modifier.padding(AppDimens.spacingLg), horizontalArrangement = Arrangement.spacedBy(AppDimens.spacingMd)) {
                com.vision.swimsafe.ui.components.common.SecondaryActionButton(
                    text = "跳过",
                    modifier = Modifier.weight(1f),
                    onClick = { showRemarkSheet = false },
                )
                PrimaryActionButton(
                    text = if (actionSubmitting) "提交中..." else "确认提交",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (actionSubmitting) {
                            return@PrimaryActionButton
                        }
                        actionSubmitting = true
                        actionError = null
                        scope.launch {
                            val result = RemoteAlarmRepository.submitAlarmAction(
                                alarmId = alarmId,
                                action = pendingAction,
                                note = remark.ifBlank { null },
                            )
                            actionSubmitting = false
                            if (result.isSuccess) {
                                showRemarkSheet = false
                                remark = ""
                                refreshVersion += 1
                                val message = when (pendingAction) {
                                    RemoteAlarmAction.DISPATCH -> "确认出警成功"
                                    RemoteAlarmAction.ACKNOWLEDGED -> "已确认收到报警"
                                    RemoteAlarmAction.RESOLVED -> "处理完成"
                                    RemoteAlarmAction.FALSE_ALARM -> "已标记为误报"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            } else {
                                actionError = result.exceptionOrNull()?.message ?: "提交失败，请重试"
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(shape = RoundedCornerShape(AppDimens.cardRadius)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.spacingLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingSm),
            content = content,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, chip: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        if (chip) {
            StatusChip(text = value)
        } else {
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmDetailScreenPreview() {
    AndroidTheme {
        AlarmDetailScreen(alarmId = "ALARM-20260321-001", onBack = {})
    }
}
