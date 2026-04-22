package com.springboot.service.stream;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StreamOpenRequest {

    private String preferredProvider;

    private boolean allowFallback;

    private boolean internalRequest;

    public static StreamOpenRequest external(String preferredProvider) {
        return StreamOpenRequest.builder()
                .preferredProvider(preferredProvider)
                .allowFallback(true)
                .internalRequest(false)
                .build();
    }

    public static StreamOpenRequest internal(String preferredProvider) {
        return StreamOpenRequest.builder()
                .preferredProvider(preferredProvider)
                .allowFallback(true)
                .internalRequest(true)
                .build();
    }
}
