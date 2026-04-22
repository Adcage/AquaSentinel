package com.vision.swimsafe.ui.components.video

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.vision.swimsafe.data.stream.MonitorRealtimeFrameWsClient
import com.vision.swimsafe.data.stream.MonitorWsConnectionState

@Composable
fun WsJpegFramePlayer(
    modifier: Modifier = Modifier,
    cameraId: Long?,
    reloadKey: Any? = Unit,
    onStateChange: ((PlayerState) -> Unit)? = null,
    fallbackContent: (@Composable () -> Unit)? = null,
) {
    val tag = "WsJpegFramePlayer"
    var playerState by remember(cameraId) { mutableStateOf<PlayerState>(PlayerState.Idle) }
    var frameBitmap by remember(cameraId) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(playerState) {
        onStateChange?.invoke(playerState)
    }

    DisposableEffect(cameraId, reloadKey) {
        frameBitmap = null
        if (cameraId == null || cameraId <= 0) {
            Log.w(tag, "invalid cameraId=$cameraId, skip ws subscribe")
            playerState = PlayerState.Error("暂无可用摄像头")
            return@DisposableEffect onDispose {}
        }

        Log.d(tag, "subscribe ws frames, cameraId=$cameraId")
        playerState = PlayerState.Loading
        val subscriptionId = MonitorRealtimeFrameWsClient.subscribe(
            cameraId = cameraId,
            onFrame = { header, bytes ->
                if (header.cameraId != cameraId) {
                    Log.w(tag, "drop mismatched frame: expected=$cameraId, actual=${header.cameraId}")
                    return@subscribe
                }
                val bitmap = runCatching {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }.getOrNull()
                if (bitmap == null) {
                    if (frameBitmap == null) {
                        playerState = PlayerState.Error("视频帧解码失败")
                    }
                    return@subscribe
                }
                frameBitmap = bitmap.asImageBitmap()
                playerState = PlayerState.Playing
            },
            onStateChange = { state ->
                Log.d(tag, "ws state changed: $state, cameraId=$cameraId")
                if (frameBitmap != null) {
                    return@subscribe
                }
                playerState = when (state) {
                    MonitorWsConnectionState.CONNECTING,
                    MonitorWsConnectionState.CONNECTED -> PlayerState.Loading

                    MonitorWsConnectionState.DISCONNECTED -> PlayerState.Error("视频连接已断开")
                    MonitorWsConnectionState.ERROR -> PlayerState.Error("视频连接失败")
                }
            },
        )

        onDispose {
            if (!subscriptionId.isNullOrBlank()) {
                Log.d(tag, "unsubscribe ws frames, cameraId=$cameraId")
                MonitorRealtimeFrameWsClient.unsubscribe(subscriptionId)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center,
    ) {
        if (frameBitmap != null) {
            Image(
                bitmap = frameBitmap!!,
                contentDescription = "视频流",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else if (fallbackContent != null) {
            fallbackContent()
        }

        if (frameBitmap == null && fallbackContent == null) {
            when (val state = playerState) {
                is PlayerState.Idle -> {
                    Text(
                        text = "等待加载视频",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                is PlayerState.Loading -> {
                    CircularProgressIndicator(color = Color.White)
                }

                is PlayerState.Error -> {
                    Text(
                        text = state.message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                is PlayerState.Playing -> Unit
            }
        }
    }
}
