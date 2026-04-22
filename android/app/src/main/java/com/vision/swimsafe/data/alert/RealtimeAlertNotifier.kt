package com.vision.swimsafe.data.alert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.vision.swimsafe.MainActivity
import com.vision.swimsafe.R
import com.vision.swimsafe.data.remote.ApiClient
import com.vision.swimsafe.data.remote.AuthSession
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

object RealtimeAlertNotifier {
    private const val ALERT_CHANNEL_ID = "drowning_alert_channel"
    private const val ALERT_CHANNEL_NAME = "溺水实时告警"

    private val gson = Gson()
    private val dedupMap = ConcurrentHashMap<String, Long>()
    private val wsClient: OkHttpClient by lazy { OkHttpClient.Builder().build() }
    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor()
    private val _uiEvents = MutableSharedFlow<UiAlertEvent>(extraBufferCapacity = 8)

    val uiEvents = _uiEvents.asSharedFlow()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var connectedToken: String? = null

    @Volatile
    private var keepAliveEnabled: Boolean = false

    @Volatile
    private var reconnectFuture: ScheduledFuture<*>? = null

    @Volatile
    private var reconnectAttempt: Int = 0

    @Volatile
    private var internalClosing: Boolean = false

    fun initialize(context: Context) {
        appContext = context.applicationContext
        ensureNotificationChannel()
    }

    fun connectIfNeeded() {
        val token = AuthSession.getAccessToken()?.trim().orEmpty()
        if (token.isBlank()) {
            disconnect()
            return
        }
        keepAliveEnabled = true
        if (webSocket != null && connectedToken == token) {
            return
        }
        closeSocketInternal(keepAlive = true)
        val request = Request.Builder()
            .url(buildWsUrl(token))
            .addHeader("Authorization", "Bearer $token")
            .build()
        connectedToken = token
        webSocket = wsClient.newWebSocket(request, AlertWsListener())
    }

    fun disconnect() {
        keepAliveEnabled = false
        reconnectAttempt = 0
        reconnectFuture?.cancel(false)
        reconnectFuture = null
        closeSocketInternal(keepAlive = false)
    }

    private fun closeSocketInternal(keepAlive: Boolean) {
        internalClosing = true
        webSocket?.close(1000, "manual close")
        webSocket = null
        if (!keepAlive) {
            connectedToken = null
        }
        internalClosing = false
    }

    private fun scheduleReconnectIfNeeded() {
        if (!keepAliveEnabled) {
            return
        }
        val token = AuthSession.getAccessToken()?.trim().orEmpty()
        if (token.isBlank()) {
            return
        }
        val running = reconnectFuture
        if (running != null && !running.isDone && !running.isCancelled) {
            return
        }
        reconnectAttempt = (reconnectAttempt + 1).coerceAtMost(6)
        val delaySec = when (reconnectAttempt) {
            1 -> 1L
            2 -> 2L
            3 -> 4L
            4 -> 8L
            else -> 10L
        }
        reconnectFuture = reconnectExecutor.schedule(
            {
                reconnectFuture = null
                connectIfNeeded()
            },
            delaySec,
            TimeUnit.SECONDS,
        )
    }

    private fun buildWsUrl(token: String): String {
        val base = ApiClient.currentBaseUrl()
        val httpUrl = if (base.endsWith("/")) base else "$base/"
        val wsUrl = httpUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
            .replace(Regex("/+$"), "") + "/ws/alerts"
        return "$wsUrl?token=$token"
    }

    private fun ensureNotificationChannel() {
        val context = appContext ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            ALERT_CHANNEL_ID,
            ALERT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        )
        channel.description = "检测到溺水事件时实时提醒"
        manager.createNotificationChannel(channel)
    }

    private fun notifyAlert(message: String, alertId: Long?) {
        val context = appContext ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                return
            }
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("alarmId", alertId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (alertId ?: 0L).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("溺水告警")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        NotificationManagerCompat.from(context)
            .notify((alertId ?: System.currentTimeMillis()).toInt(), builder.build())
    }

    private fun handlePayload(raw: String) {
        val payload = parseAlertPayload(raw) ?: return
        val eventUid = payload.eventUid?.trim().orEmpty()
        if (eventUid.isNotBlank()) {
            val old = dedupMap.putIfAbsent(eventUid, System.currentTimeMillis())
            if (old != null) {
                return
            }
        }
        val riskLevel = payload.data?.riskLevel?.trim().orEmpty().uppercase()
        val durationText = payload.data?.durationSec
            ?.takeIf { it > 0.0 }
            ?.let { String.format("%.1f秒", it) }
            ?: "-"
        val ruleText = payload.data?.ruleHits
            ?.filter { it.isNotBlank() }
            ?.joinToString("/")
            ?.ifBlank { "未提供" }
            ?: "未提供"
        val riskLevelText = if (riskLevel.isBlank()) "MEDIUM" else riskLevel
        val message = "检测到溺水风险，等级:$riskLevelText，持续:$durationText，规则:$ruleText"
        val data = payload.data
        val uiAlertId = data?.alertId?.toString() ?: eventUid
        val emergencyContact = buildEmergencyContactText(
            data?.emergencyContactName,
            data?.emergencyContactPhone
        )
        _uiEvents.tryEmit(
            UiAlertEvent(
                alertId = uiAlertId,
                message = message,
                alarmType = toAlarmTypeText(data?.alertType),
                cameraName = data?.cameraId?.let { "摄像头#$it" } ?: "摄像头",
                locationDescription = data?.incidentLocation ?: "请立即前往现场确认",
                emergencyContact = emergencyContact,
                eventTime = data?.createdAt ?: "刚刚",
                status = "待处理",
            )
        )
        notifyAlert(message, payload.data?.alertId)
    }

    private fun buildEmergencyContactText(name: String?, phone: String?): String {
        val hasName = !name.isNullOrBlank()
        val hasPhone = !phone.isNullOrBlank()
        return when {
            hasName && hasPhone -> "$name $phone"
            hasName -> name
            hasPhone -> phone
            else -> "暂无联系人"
        }
    }

    private fun toAlarmTypeText(alertType: String?): String {
        return when (alertType?.trim()?.uppercase()) {
            "DROWNING" -> "溺水告警"
            "CROSS_BORDER" -> "越界告警"
            "OVER_CAPACITY" -> "超员告警"
            else -> "紧急告警"
        }
    }

    internal fun parseAlertPayload(raw: String): WsPayload? {
        val payload = runCatching {
            gson.fromJson(raw, WsPayload::class.java)
        }.getOrNull() ?: return null
        if (!payload.messageType.equals("ALERT_CREATED", ignoreCase = true)) {
            return null
        }
        return payload
    }

    private class AlertWsListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            reconnectAttempt = 0
            reconnectFuture?.cancel(false)
            reconnectFuture = null
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            handlePayload(text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            if (internalClosing) {
                return
            }
            if (this@RealtimeAlertNotifier.webSocket == webSocket) {
                this@RealtimeAlertNotifier.webSocket = null
            }
            scheduleReconnectIfNeeded()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            if (internalClosing) {
                return
            }
            if (this@RealtimeAlertNotifier.webSocket == webSocket) {
                this@RealtimeAlertNotifier.webSocket = null
            }
            scheduleReconnectIfNeeded()
        }
    }

    internal data class WsPayload(
        val messageType: String? = null,
        val eventUid: String? = null,
        val data: WsData? = null,
    )

    internal data class WsData(
        val alertId: Long? = null,
        val alertType: String? = null,
        val cameraId: Long? = null,
        val incidentLocation: String? = null,
        val createdAt: String? = null,
        val riskLevel: String? = null,
        val durationSec: Double? = null,
        val ruleHits: List<String>? = null,
        val emergencyContactName: String? = null,
        val emergencyContactPhone: String? = null,
    )

    data class UiAlertEvent(
        val alertId: String,
        val message: String,
        val alarmType: String,
        val cameraName: String,
        val locationDescription: String,
        val emergencyContact: String,
        val eventTime: String,
        val status: String,
    )
}
