package com.vision.swimsafe.ui.components.video

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView

sealed class PlayerState {
    data object Idle : PlayerState()
    data object Loading : PlayerState()
    data object Playing : PlayerState()
    data class Error(val message: String) : PlayerState()
}

/**
 * 视频流播放器组件
 * 支持 RTSP、HLS、HTTP 等多种视频流协议
 *
 * @param modifier Compose 修饰符
 * @param streamUrl 视频流 URL
 * @param onStateChange 播放状态变化回调
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoStreamPlayer(
    modifier: Modifier = Modifier,
    streamUrl: String?,
    onStateChange: ((PlayerState) -> Unit)? = null,
) {
    val context = LocalContext.current
    var playerState by remember { mutableStateOf<PlayerState>(PlayerState.Idle) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(playerState) {
        onStateChange?.invoke(playerState)
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    LaunchedEffect(streamUrl) {
        // 释放旧播放器
        exoPlayer?.release()
        exoPlayer = null

        if (streamUrl.isNullOrBlank()) {
            playerState = PlayerState.Error("暂无视频流地址")
            return@LaunchedEffect
        }

        playerState = PlayerState.Loading

        try {
            val player = ExoPlayer.Builder(context).build()
            
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            playerState = PlayerState.Loading
                        }
                        Player.STATE_READY -> {
                            playerState = PlayerState.Playing
                        }
                        Player.STATE_ENDED -> {
                            playerState = PlayerState.Idle
                        }
                        Player.STATE_IDLE -> {
                            // 保持当前状态
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    playerState = PlayerState.Error(error.message ?: "视频播放失败")
                }
            })

            // 根据 URL 协议选择合适的媒体源
            val mediaItem = MediaItem.fromUri(streamUrl)
            
            if (streamUrl.startsWith("rtsp://", ignoreCase = true)) {
                // RTSP 流使用专门的媒体源
                val rtspMediaSource = RtspMediaSource.Factory()
                    .createMediaSource(mediaItem)
                player.setMediaSource(rtspMediaSource)
            } else {
                // 其他格式（HLS、HTTP 等）
                player.setMediaItem(mediaItem)
            }
            
            player.prepare()
            player.playWhenReady = true
            
            exoPlayer = player
        } catch (e: Exception) {
            playerState = PlayerState.Error(e.message ?: "视频播放器初始化失败")
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center,
    ) {
        // ExoPlayer 视频视图
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false // 不显示控制器
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
        )

        // 叠加状态层
        when (val state = playerState) {
            is PlayerState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC1A1A1A)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "等待加载视频",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            is PlayerState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x801A1A1A)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is PlayerState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC1A1A1A)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            is PlayerState.Playing -> {
                // 播放中，不显示覆盖层
            }
        }
    }
}
