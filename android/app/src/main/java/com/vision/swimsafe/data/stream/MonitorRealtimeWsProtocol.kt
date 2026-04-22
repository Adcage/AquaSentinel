package com.vision.swimsafe.data.stream

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class MonitorVideoFrameHeader(
    val cameraId: Long,
    val frameTs: Long,
    val seq: Long,
)

object MonitorRealtimeWsProtocol {
    private const val FRAME_MESSAGE_TYPE = "MONITOR_VIDEO_FRAME"
    private const val ACTION_SUBSCRIBE = "SUBSCRIBE_MONITOR_REALTIME"
    private const val ACTION_UNSUBSCRIBE = "UNSUBSCRIBE_MONITOR_REALTIME"

    fun buildWsUrl(apiBaseUrl: String, token: String): String {
        val base = apiBaseUrl.trim().ifBlank { "/api" }
        val normalized = base.removeSuffix("/")
        val wsBase = normalized
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
        return "$wsBase/ws/alerts?token=$encodedToken"
    }

    fun parseFrameHeader(raw: String): MonitorVideoFrameHeader? {
        val root = runCatching {
            JsonParser.parseString(raw).asJsonObject
        }.getOrNull() ?: return null

        val messageType = root.getAsStringOrNull("messageType")
        if (!messageType.equals(FRAME_MESSAGE_TYPE, ignoreCase = true)) {
            return null
        }

        val data = root.getAsJsonObjectOrNull("data") ?: return null
        val cameraId = data.getAsLongOrNull("cameraId") ?: return null
        if (cameraId <= 0) {
            return null
        }
        val frameTs = data.getAsLongOrNull("frameTs") ?: 0L
        val seq = data.getAsLongOrNull("seq") ?: 0L
        return MonitorVideoFrameHeader(cameraId = cameraId, frameTs = frameTs, seq = seq)
    }

    fun buildSubscribeMessage(cameraIds: Set<Long>): String {
        val normalizedIds = cameraIds
            .filter { it > 0 }
            .distinct()
            .sorted()
            .joinToString(separator = ",")

        return "{\"action\":\"$ACTION_SUBSCRIBE\",\"cameraIds\":[${normalizedIds}]}"
    }

    fun buildUnsubscribeMessage(): String {
        return "{\"action\":\"$ACTION_UNSUBSCRIBE\"}"
    }

    private fun JsonObject.getAsJsonObjectOrNull(key: String): JsonObject? {
        val value = this.get(key) ?: return null
        if (!value.isJsonObject) {
            return null
        }
        return value.asJsonObject
    }

    private fun JsonObject.getAsStringOrNull(key: String): String? {
        val value = this.get(key) ?: return null
        if (value.isJsonNull) {
            return null
        }
        return runCatching { value.asString }.getOrNull()
    }

    private fun JsonObject.getAsLongOrNull(key: String): Long? {
        val value = this.get(key) ?: return null
        if (value.isJsonNull) {
            return null
        }
        return runCatching { value.asLong }.getOrNull()
    }
}
