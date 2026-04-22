package com.springboot.service.stream.impl;

import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;
import com.springboot.service.stream.StreamOpenRequest;
import com.springboot.service.stream.StreamProvider;
import com.springboot.service.stream.StreamProviderException;
import com.springboot.service.stream.StreamSession;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class JavacvMjpegStreamProvider implements StreamProvider {

    private static final String MJPEG_CONTENT_TYPE = "multipart/x-mixed-replace; boundary=frame";

    @Override
    public String name() {
        return "javacv";
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
        ensureJavacvAvailable();
        return new StreamSession(
                name(),
                MJPEG_CONTENT_TYPE,
                source,
                outputStream -> pipeByJavacv(source, outputStream),
                null);
    }

    private void ensureJavacvAvailable() {
        try {
            Class.forName("org.bytedeco.javacv.FFmpegFrameGrabber");
            Class.forName("org.bytedeco.javacv.Java2DFrameConverter");
            Class.forName("org.bytedeco.javacv.Frame");
        } catch (Exception e) {
            throw new StreamProviderException("JavaCV依赖不存在，无法启用javacv provider", e);
        }
    }

    private void pipeByJavacv(String source, OutputStream outputStream) throws IOException {
        Object grabber = null;
        Object converter = null;
        try {
            Class<?> frameClass = Class.forName("org.bytedeco.javacv.Frame");
            Class<?> grabberClass = Class.forName("org.bytedeco.javacv.FFmpegFrameGrabber");
            Class<?> converterClass = Class.forName("org.bytedeco.javacv.Java2DFrameConverter");

            grabber = grabberClass.getConstructor(String.class).newInstance(source);
            grabberClass.getMethod("setOption", String.class, String.class)
                    .invoke(grabber, "rtsp_transport", "tcp");
            grabberClass.getMethod("start").invoke(grabber);
            converter = converterClass.getConstructor().newInstance();

            while (true) {
                Object frame = grabberClass.getMethod("grabImage").invoke(grabber);
                if (frame == null) {
                    continue;
                }
                BufferedImage bufferedImage = (BufferedImage) converterClass
                        .getMethod("convert", frameClass)
                        .invoke(converter, frame);
                if (bufferedImage == null) {
                    continue;
                }
                byte[] jpegBytes = toJpeg(bufferedImage);
                outputStream.write("--frame\r\n".getBytes());
                outputStream.write("Content-Type: image/jpeg\r\n\r\n".getBytes());
                outputStream.write(jpegBytes);
                outputStream.write("\r\n".getBytes());
                outputStream.flush();
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new StreamProviderException("JavaCV转流失败: " + e.getMessage(), e);
        } finally {
            closeJavacv(grabber, converter);
        }
    }

    private byte[] toJpeg(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private void closeJavacv(Object grabber, Object converter) {
        if (grabber != null) {
            tryInvoke(grabber, "stop");
            tryInvoke(grabber, "release");
            tryInvoke(grabber, "close");
        }
        if (converter != null) {
            tryInvoke(converter, "close");
        }
    }

    private void tryInvoke(Object target, String methodName) {
        try {
            target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignored) {
        }
    }
}
