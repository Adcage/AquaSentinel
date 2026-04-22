package com.springboot.service.stream;

import java.io.IOException;
import java.io.OutputStream;
import lombok.Getter;

@Getter
public class StreamSession implements AutoCloseable {

    @FunctionalInterface
    public interface StreamPipe {

        void pipe(OutputStream outputStream) throws IOException;
    }

    private final String providerName;

    private final String contentType;

    private final String sourceUrl;

    private final StreamPipe streamPipe;

    private final AutoCloseable closeable;

    public StreamSession(String providerName, String contentType, String sourceUrl, StreamPipe streamPipe,
                         AutoCloseable closeable) {
        this.providerName = providerName;
        this.contentType = contentType;
        this.sourceUrl = sourceUrl;
        this.streamPipe = streamPipe;
        this.closeable = closeable;
    }

    public void pipeTo(OutputStream outputStream) throws IOException {
        if (streamPipe == null) {
            throw new IOException("当前流会话不支持字节流输出");
        }
        streamPipe.pipe(outputStream);
    }

    public boolean supportsPipe() {
        return streamPipe != null;
    }

    @Override
    public void close() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }
}
