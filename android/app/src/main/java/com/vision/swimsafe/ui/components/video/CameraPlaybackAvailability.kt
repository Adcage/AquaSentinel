package com.vision.swimsafe.ui.components.video

import com.vision.swimsafe.data.remote.CameraDeviceVo

data class CameraPlaybackAvailability(
    val canPlay: Boolean,
    val message: String? = null,
)

fun resolveDevicePlaybackAvailability(camera: CameraDeviceVo?): CameraPlaybackAvailability {
    if (camera == null) {
        return CameraPlaybackAvailability(
            canPlay = false,
            message = "设备不存在或已删除",
        )
    }

    if ((camera.id ?: 0L) <= 0L) {
        return CameraPlaybackAvailability(
            canPlay = false,
            message = "设备编号缺失，暂无视频画面",
        )
    }

    if ((camera.enabled ?: 1) != 1) {
        return CameraPlaybackAvailability(
            canPlay = false,
            message = "设备未启用，暂无视频画面",
        )
    }

    val status = camera.deviceStatus?.trim()?.uppercase()
    if (!status.isNullOrBlank() && status != "ONLINE") {
        return CameraPlaybackAvailability(
            canPlay = false,
            message = "设备离线，暂无视频画面",
        )
    }

    return CameraPlaybackAvailability(canPlay = true)
}

fun resolveAlarmPlaybackAvailability(
    cameraId: Long?,
    camera: CameraDeviceVo?,
): CameraPlaybackAvailability {
    if (cameraId == null || cameraId <= 0) {
        return CameraPlaybackAvailability(
            canPlay = false,
            message = "未绑定摄像头，暂无视频画面",
        )
    }
    return resolveDevicePlaybackAvailability(camera)
}
