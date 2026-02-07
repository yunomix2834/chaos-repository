package com.chaos.task_manager.dto.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NatsJetStreamRequestInfo {
    String subject;
    String messageType;

    String requestId;
    String traceId;
    String tenantId;
    String lang;

    Instant createdAt;

    String body;

    Map<String, String> headers;
}
