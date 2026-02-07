package com.chaos.task_manager.config.template;

import com.chaos.task_manager.dto.common.NatsJetStreamRequestInfo;
import com.chaos.task_manager.utils.CommonUtils;
import io.nats.client.JetStream;
import io.nats.client.Message;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NatsJetStreamTemplate {

    private final JetStream jetStream;

    public <T> PublishAck publishRequest(
            String subject,
            String messageType,
            T bodyObject,
            Map<String, String> headers
    ) {
        try {

            String requestId = UUID.randomUUID().toString();

            Map<String, String> hdr = headers == null ? new HashMap<>() : new HashMap<>(headers);
            hdr.putIfAbsent("X-Request-Id", requestId);
            hdr.putIfAbsent("X-Trace-Id", hdr.getOrDefault("X-Trace-Id", requestId));
            hdr.putIfAbsent("X-Message-Type", messageType);
            hdr.putIfAbsent("X-Lang", hdr.getOrDefault("X-Lang", "en"));
            String bodyJson = CommonUtils.OBJECT_MAPPER.writeValueAsString(bodyObject);

            NatsJetStreamRequestInfo requestInfo = NatsJetStreamRequestInfo.builder()
                    .subject(subject)
                    .messageType(messageType)
                    .requestId(requestId)
                    .traceId(hdr.get("X-Trace-Id"))
                    .tenantId(hdr.get("X-Tenant-Id"))
                    .lang(hdr.get("X-Lang"))
                    .createdAt(Instant.now())
                    .headers(hdr)
                    .body(bodyJson)
                    .build();

            byte[] data = CommonUtils.OBJECT_MAPPER.writeValueAsBytes(requestInfo);

            // Headers
            Headers natsHeaders = new Headers();
            hdr.forEach(natsHeaders::add);
            natsHeaders.add("Nats-Msg-Id", requestId);

            // Message
            Message message = NatsMessage.builder()
                    .subject(subject)
                    .headers(natsHeaders)
                    .data(data)
                    .build();

            // Publish
            PublishAck ack = jetStream.publish(message);
            log.info("[NATS][PUB] subject={} reqId={} seq={}", subject, requestId, ack.getSeqno());
            return ack;
        } catch (Exception e) {
            log.error("[NATS][PUB] failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
