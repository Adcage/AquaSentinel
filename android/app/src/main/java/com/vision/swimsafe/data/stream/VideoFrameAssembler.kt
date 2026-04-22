package com.vision.swimsafe.data.stream

internal class VideoFrameAssembler(
    private val maxPendingHeaders: Int = 120,
) {
    private val headers = ArrayDeque<MonitorVideoFrameHeader>()

    @Synchronized
    fun pushHeader(header: MonitorVideoFrameHeader) {
        if (headers.size >= maxPendingHeaders) {
            headers.removeFirstOrNull()
        }
        headers.addLast(header)
    }

    @Synchronized
    fun pollHeader(): MonitorVideoFrameHeader? {
        return headers.removeFirstOrNull()
    }

    @Synchronized
    fun clear() {
        headers.clear()
    }
}
