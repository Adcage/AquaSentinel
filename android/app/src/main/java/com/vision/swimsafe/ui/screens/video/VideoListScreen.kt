package com.vision.swimsafe.ui.screens.video

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vision.swimsafe.data.remote.CameraDeviceVo
import com.vision.swimsafe.data.remote.RemoteCameraRepository
import com.vision.swimsafe.ui.components.common.AppTopBar
import com.vision.swimsafe.ui.components.video.MjpegStreamPlayer
import com.vision.swimsafe.ui.components.video.VideoUnavailablePlaceholder
import com.vision.swimsafe.ui.components.video.WsJpegFramePlayer
import com.vision.swimsafe.ui.components.video.resolveDevicePlaybackAvailability
import com.vision.swimsafe.ui.theme.AlarmRedDark
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.PageBackground
import com.vision.swimsafe.ui.theme.SafetyBlue
import com.vision.swimsafe.ui.theme.SuccessGreen
import com.vision.swimsafe.ui.theme.TextPrimary
import com.vision.swimsafe.ui.theme.TextSecondary

@Composable
fun VideoListScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var selectedCamera by remember { mutableStateOf<CameraDeviceVo?>(null) }
    var cameras by remember { mutableStateOf<List<CameraDeviceVo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        loadError = null
        try {
            cameras = RemoteCameraRepository.getCameraList()
        } catch (e: Exception) {
            loadError = e.message ?: "加载摄像头列表失败"
            Toast.makeText(context, loadError, Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize().background(PageBackground)) {
        AppTopBar(
            title = if (selectedCamera != null) selectedCamera!!.cameraName ?: "摄像头详情" else "监控列表",
            containerColor = SafetyBlue,
            titleColor = CardBackground,
            onBack = if (selectedCamera != null) {{ selectedCamera = null }} else onBack,
        )

        if (selectedCamera != null) {
            val availability = resolveDevicePlaybackAvailability(selectedCamera)
            if (availability.canPlay) {
                WsJpegFramePlayer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(AppDimens.spacingLg),
                    cameraId = selectedCamera!!.id,
                    fallbackContent = {
                        MjpegStreamPlayer(
                            modifier = Modifier.fillMaxSize(),
                            cameraId = selectedCamera!!.id,
                            fallbackStreamUrl = selectedCamera!!.streamUrl,
                        )
                    },
                )
            } else {
                VideoUnavailablePlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(AppDimens.spacingLg),
                    message = availability.message ?: "当前设备暂无可用视频",
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.spacingLg),
                shape = RoundedCornerShape(AppDimens.cardRadius),
            ) {
                Column(modifier = Modifier.padding(AppDimens.spacingLg)) {
                    Text(text = "摄像头信息", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "编号：${selectedCamera!!.cameraCode ?: "--"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = AppDimens.spacingSm),
                    )
                    Text(
                        text = "状态：${when (selectedCamera!!.deviceStatus) {
                            "ONLINE" -> "在线"
                            "OFFLINE" -> "离线"
                            else -> selectedCamera!!.deviceStatus ?: "未知"
                        }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (selectedCamera!!.deviceStatus) {
                            "ONLINE" -> SuccessGreen
                            else -> AlarmRedDark
                        },
                        modifier = Modifier.padding(top = AppDimens.spacingXs),
                    )
                    Text(
                        text = "协议：${selectedCamera!!.protocol ?: "--"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = AppDimens.spacingXs),
                    )
                }
            }
        } else {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = SafetyBlue)
                    }
                }
                loadError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = loadError ?: "加载失败",
                            color = AlarmRedDark,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                cameras.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无监控设备",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(AppDimens.spacingLg),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.spacingMd),
                    ) {
                        items(cameras) { camera ->
                            CameraCard(
                                camera = camera,
                                onClick = { selectedCamera = camera },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraCard(
    camera: CameraDeviceVo,
    onClick: () -> Unit,
) {
    val isOnline = camera.deviceStatus == "ONLINE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AppDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = SafetyBlue,
                    )
                    if (!isOnline) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Circle,
                                contentDescription = "离线",
                                tint = AlarmRedDark,
                            )
                        }
                    }
                }
                Text(
                    text = camera.cameraName ?: "摄像头 ${camera.id}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = AppDimens.spacingSm),
                )
                Text(
                    text = camera.cameraCode ?: "--",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}
