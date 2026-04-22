package com.vision.swimsafe.data.stream

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.vision.swimsafe.data.remote.ApiClient
import com.vision.swimsafe.data.remote.AuthSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

enum class MonitorWsConnectionState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR,
}

private data class FrameListenerEntry(
    val cameraId: Long,
    val onFrame: (MonitorVideoFrameHeader, ByteArray) -> Unit,
    val onStateChange: (MonitorWsConnectionState) -> Unit,
)

object MonitorRealtimeFrameWsClient {
    private const val TAG = "MonitorFrameWs"
    private const val RECONNECT_DELAY_MS = 2000L

    private val wsClient: OkHttpClient by lazy { OkHttpClient.Builder().build() }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = ConcurrentHashMap<String, FrameListenerEntry>()

    @Volatile
    private var webSocket: WebSocket? = null

    private val frameAssembler = VideoFrameAssembler()

    @Volatile
    private var connectionState: MonitorWsConnectionState = MonitorWsConnectionState.DISCONNECTED

    @Volatile
    private var connectedToken: String = ""

    @Volatile
    private var reconnectScheduled = false

    fun subscribe(
        cameraId: Long,
        onFrame: (MonitorVideoFrameHeader, ByteArray) -> Unit,
        onStateChange: (MonitorWsConnectionState) -> Unit = {},
    ): String? {
        if (cameraId <= 0) {
            return null
        }
        val subscriptionId = UUID.randomUUID().toString()
        listeners[subscriptionId] = FrameListenerEntry(cameraId, onFrame, onStateChange)
        dispatchStateToSingleListener(onStateChange, connectionState)
        syncConnectionAndSubscription()
        return subscriptionId
    }

    fun unsubscribe(subscriptionId: String) {
        listeners.remove(subscriptionId)
        syncConnectionAndSubscription()
    }

    @Synchronized
    private fun syncConnectionAndSubscription() {
        val cameraIds = activeCameraIds()
        if (cameraIds.isEmpty()) {
            Log.d(TAG, "no active camera subscriptions, closing ws")
            webSocket?.send(MonitorRealtimeWsProtocol.buildUnsubscribeMessage())
            closeSocketQuietly()
            updateState(MonitorWsConnectionState.DISCONNECTED)
            return
        }

        val token = AuthSession.getAccessToken()?.trim().orEmpty()
        if (token.isBlank()) {
            Log.w(TAG, "token is blank, ws cannot connect")
            closeSocketQuietly()
            updateState(MonitorWsConnectionState.ERROR)
            return
        }

        if (webSocket != null && connectedToken != token) {
            closeSocketQuietly()
        }

        if (webSocket == null) {
            updateState(MonitorWsConnectionState.CONNECTING)
            connectedToken = token
            val wsUrl = MonitorRealtimeWsProtocol.buildWsUrl(ApiClient.currentBaseUrl(), token)
            Log.d(TAG, "opening ws: $wsUrl")
            val request = Request.Builder().url(wsUrl).build()
            webSocket = wsClient.newWebSocket(request, WsListener())
            return
        }

        if (connectionState == MonitorWsConnectionState.CONNECTED) {
            Log.d(TAG, "sending subscribe for cameras=$cameraIds")
            webSocket?.send(MonitorRealtimeWsProtocol.buildSubscribeMessage(cameraIds))
        }
    }

    private fun activeCameraIds(): Set<Long> {
        return listeners.values.map { it.cameraId }.filter { it > 0 }.toSet()
    }

    @Synchronized
    private fun closeSocketQuietly() {
        frameAssembler.clear()
        webSocket?.close(1000, "monitor frame ws close")
        webSocket = null
        connectedToken = ""
    }

    private fun updateState(state: MonitorWsConnectionState) {
        connectionState = state
        dispatchStateToAllListeners(state)
    }

    private fun dispatchStateToAllListeners(state: MonitorWsConnectionState) {
        val callbacks = listeners.values.map { it.onStateChange }
        for (callback in callbacks) {
            dispatchStateToSingleListener(callback, state)
        }
    }

    private fun dispatchStateToSingleListener(
        callback: (MonitorWsConnectionState) -> Unit,
        state: MonitorWsConnectionState,
    ) {
        mainHandler.post { callback(state) }
    }

    private fun dispatchFrame(header: MonitorVideoFrameHeader, bytes: ByteArray) {
        val targets = listeners.values.filter { it.cameraId == header.cameraId }
        if (targets.isEmpty()) {
            return
        }
        for (listener in targets) {
            mainHandler.post { listener.onFrame(header, bytes) }
        }
    }

    @Synchronized
    private fun scheduleReconnectIfNeeded() {
        if (reconnectScheduled) {
            return
        }
        if (activeCameraIds().isEmpty()) {
            return
        }
        if (AuthSession.getAccessToken().isNullOrBlank()) {
            return
        }
        reconnectScheduled = true
        mainHandler.postDelayed({
            reconnectScheduled = false
            syncConnectionAndSubscription()
        }, RECONNECT_DELAY_MS)
    }

    private class WsListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            updateState(MonitorWsConnectionState.CONNECTED)
            val cameraIds = activeCameraIds()
            if (cameraIds.isNotEmpty()) {
                Log.d(TAG, "ws connected, subscribe cameras=$cameraIds")
                webSocket.send(MonitorRealtimeWsProtocol.buildSubscribeMessage(cameraIds))
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            val header = MonitorRealtimeWsProtocol.parseFrameHeader(text)
            if (header != null) {
                frameAssembler.pushHeader(header)
                Log.d(TAG, "received frame header, cameraId=${header.cameraId}, frameTs=${header.frameTs}")
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            val header = frameAssembler.pollHeader() ?: return
            Log.d(TAG, "received frame binary, cameraId=${header.cameraId}, size=${bytes.size}")
            dispatchFrame(header, bytes.toByteArray())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            Log.w(TAG, "ws failed: ${t.message}")
            this@MonitorRealtimeFrameWsClient.webSocket = null
            frameAssembler.clear()
            updateState(MonitorWsConnectionState.ERROR)
            scheduleReconnectIfNeeded()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.d(TAG, "ws closed, code=$code, reason=$reason")
            this@MonitorRealtimeFrameWsClient.webSocket = null
            frameAssembler.clear()
            updateState(MonitorWsConnectionState.DISCONNECTED)
            scheduleReconnectIfNeeded()
        }
    }
}
