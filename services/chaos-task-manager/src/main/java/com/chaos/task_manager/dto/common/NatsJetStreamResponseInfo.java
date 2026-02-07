package com.chaos.task_manager.dto.common;

import io.nats.client.Message;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NatsJetStreamResponseInfo<T> {
    transient Message raw;

    String subject;
    String messageType;

    String requestId;
    String traceId;
    String tenantId;
    String lang;

    Map<String, String> headers;

    String bodyJson;
    T body;
}
