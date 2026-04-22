package com.springboot.service.stream.impl;

import com.springboot.common.ErrorCode;
import com.springboot.config.AppStreamProxyProperties;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;
import com.springboot.service.stream.StreamOpenRequest;
import com.springboot.service.stream.StreamProvider;
import com.springboot.service.stream.StreamProviderException;
import com.springboot.service.stream.StreamSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class FfmpegMjpegStreamProvider implements StreamProvider {

    private static final String MJPEG_CONTENT_TYPE = "multipart/x-mixed-replace; boundary=frame";

    private final AppStreamProxyProperties appStreamProxyProperties;

    public FfmpegMjpegStreamProvider(AppStreamProxyProperties appStreamProxyProperties) {
        this.appStreamProxyProperties = appStreamProxyProperties;
    }

    @Override
    public String name() {
        return "ffmpeg";
    }

    @Override
    public boolean supports(String sourceProtocol) {
        if (StringUtils.isBlank(sourceProtocol)) {
            return true;
        }
        String normalized = sourceProtocol.trim().toUpperCase();
        return "RTSP".equals(normalized)
                || "HTTP".equals(normalized)
                || "HTTPS".equals(normalized)
                || "HTTP-FLV".equals(normalized)
                || "FLV".equals(normalized)
                || "PTZ".equals(normalized);
    }

    @Override
    public StreamSession open(CameraDevice cameraDevice, StreamOpenRequest request) {
        String source = StringUtils.trimToEmpty(cameraDevice.getStream_url());
        if (StringUtils.isBlank(source)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头流地址为空");
        }
        Process process = startProcess(source);
        InputStream inputStream = process.getInputStream();
        return new StreamSession(
                name(),
                MJPEG_CONTENT_TYPE,
                source,
                outputStream -> pipe(inputStream, outputStream),
                () -> closeProcess(process, inputStream));
    }

    List<String> buildCommand(String source) {
        List<String> command = new ArrayList<>();
        command.add(StringUtils.defaultIfBlank(appStreamProxyProperties.getFfmpegPath(), "ffmpeg"));
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add(StringUtils.defaultIfBlank(appStreamProxyProperties.getFfmpegLogLevel(), "error"));
        if (source.toLowerCase().startsWith("rtsp")) {
            command.add("-rtsp_transport");
            command.add("tcp");
        }
        command.add("-fflags");
        command.add("nobuffer");
        command.add("-flags");
        command.add("low_delay");
        command.add("-i");
        command.add(source);
        command.add("-an");
        command.add("-vf");
        command.add("fps=10,scale=960:-2");
        command.add("-c:v");
        command.add("mjpeg");
        command.add("-q:v");
        command.add(String.valueOf(Math.max(2, appStreamProxyProperties.getJpegQuality())));
        command.add("-f");
        command.add("mpjpeg");
        command.add("-boundary_tag");
        command.add("frame");
        command.add("pipe:1");
        return command;
    }

    private Process startProcess(String source) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(source));
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
            return processBuilder.start();
        } catch (IOException e) {
            throw new StreamProviderException("启动FFmpeg失败: " + e.getMessage(), e);
        }
    }

    private void pipe(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        while (true) {
            int len = inputStream.read(buffer);
            if (len < 0) {
                break;
            }
            outputStream.write(buffer, 0, len);
            outputStream.flush();
        }
    }

    private void closeProcess(Process process, InputStream inputStream) {
        try {
            inputStream.close();
        } catch (Exception ignored) {
        }
        try {
            process.destroy();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        } catch (Exception ignored) {
        }
    }
}
