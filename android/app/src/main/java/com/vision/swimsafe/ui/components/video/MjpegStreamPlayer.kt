package com.vision.swimsafe.ui.components.video

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.vision.swimsafe.data.remote.ApiClient
import com.vision.swimsafe.data.remote.AuthSession
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 基于 WebView 的 MJPEG 视频流播放器
 * 用于播放后端代理的 MJPEG 流
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MjpegStreamPlayer(
    modifier: Modifier = Modifier,
    cameraId: Long?,
    fallbackStreamUrl: String? = null,
    onStateChange: ((PlayerState) -> Unit)? = null,
) {
    var playerState by remember { mutableStateOf<PlayerState>(PlayerState.Idle) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(playerState) {
        onStateChange?.invoke(playerState)
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }

    // 构建视频流 URL
    val streamUrl = remember(cameraId, fallbackStreamUrl) {
        val resolved = resolveMjpegStreamUrl(
            cameraId = cameraId,
            fallbackStreamUrl = fallbackStreamUrl,
            baseUrl = ApiClient.currentBaseUrl(),
            token = AuthSession.getAccessToken(),
        )
        Log.d(
            "MjpegStreamPlayer",
            "resolve stream: cameraId=$cameraId, fallback=$fallbackStreamUrl, baseUrl=${ApiClient.currentBaseUrl()}, resolved=$resolved",
        )
        if (resolved == null) {
            Log.w("MjpegStreamPlayer", "No valid stream url for cameraId=$cameraId, fallback=$fallbackStreamUrl")
        }
        resolved
    }

    Log.d("MjpegStreamPlayer", "Final stream URL: $streamUrl")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center,
    ) {
        if (streamUrl.isNullOrBlank()) {
            playerState = PlayerState.Error("暂无视频流地址")
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                playerState = PlayerState.Loading
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                playerState = PlayerState.Playing
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    val errorDesc = error?.description?.toString() ?: "未知错误"
                                    Log.e("MjpegStreamPlayer", "WebView error: $errorDesc, url=${request.url}")
                                    playerState = PlayerState.Error("视频流加载失败: $errorDesc")
                                }
                            }
                        }

                        // 加载 MJPEG 流页面
                        val html = buildMjpegHtml(streamUrl)
                        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                        webView = this
                    }
                },
                update = { _ ->
                    // WebView 的 URL 更新由 factory 处理
                    // 如果需要刷新，可以通过 key 参数触发重组
                },
            )
        }

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

internal fun resolveMjpegStreamUrl(
    cameraId: Long?,
    fallbackStreamUrl: String?,
    baseUrl: String,
    token: String?,
): String? {
    if (cameraId != null && cameraId > 0) {
        val normalizedBase = baseUrl.trim().trimEnd('/')
        if (normalizedBase.isNotBlank()) {
            val proxyUrl = "$normalizedBase/streams/cameras/$cameraId/preview?provider=auto"
            if (!token.isNullOrBlank()) {
                val encoded = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
                return "$proxyUrl&token=$encoded"
            }
            return proxyUrl
        }
    }

    val fallback = fallbackStreamUrl?.trim().orEmpty()
    if (fallback.isBlank()) {
        return null
    }
    if (!fallback.startsWith("http://", ignoreCase = true) &&
        !fallback.startsWith("https://", ignoreCase = true)
    ) {
        return null
    }
    val host = runCatching { URI(fallback).host?.lowercase() }.getOrNull()
    if (host == "127.0.0.1" || host == "localhost" || host == "::1") {
        return null
    }
    return fallback
}

private fun buildMjpegHtml(streamUrl: String): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { 
            width: 100%; 
            height: 100%; 
            background: #1a1a1a; 
            overflow: hidden;
        }
        img {
            width: 100%;
            height: 100%;
            object-fit: contain;
        }
        .error {
            color: white;
            text-align: center;
            padding: 20px;
            font-size: 14px;
        }
    </style>
</head>
<body>
    <img id="videoStream" src="$streamUrl" onerror="handleError()" />
    <script>
        console.log('Loading video stream:', '$streamUrl');
        function handleError() {
            console.error('Video stream load failed');
            document.body.innerHTML='<div class=error>视频流加载失败<br/>请检查网络连接或视频源</div>';
        }
    </script>
</body>
</html>
""".trimIndent()
}
