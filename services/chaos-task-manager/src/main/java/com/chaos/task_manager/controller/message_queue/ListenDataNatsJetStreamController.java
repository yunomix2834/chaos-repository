package com.chaos.task_manager.controller.message_queue;

import com.chaos.task_manager.config.NatsJetStreamConfig;
import com.chaos.task_manager.controller.message_queue.handler.HandlerBase;
import com.chaos.task_manager.controller.message_queue.handler.HandlerRegistry;
import com.chaos.task_manager.dto.common.NatsJetStreamRequestInfo;
import com.chaos.task_manager.dto.common.NatsJetStreamResponseInfo;
import com.chaos.task_manager.utils.CommonUtils;
import io.nats.client.*;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListenDataNatsJetStreamController {

    private final JetStream jetStream;
    private final JetStreamManagement jetStreamManagement;
    private final NatsJetStreamConfig natsJetStreamConfig;
    private final HandlerRegistry handlerRegistry;

    private volatile boolean running = true;
    private JetStreamSubscription sub;

    @Value("${chaos.nats.stream}")
    private String stream;

    @Value("${chaos.nats.evtSubject}")
    private String evtSubject;

    @EventListener(ApplicationReadyEvent.class)
    public void startWhenReady() {
        Mono.fromRunnable(this::startWithRetry)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private void startWithRetry() {
        int attempt = 0;

        while (running) {
            try {
                attempt++;

                natsJetStreamConfig.ensureStreamAndConsumers(jetStreamManagement);

                String durableEvt = NatsJetStreamConfig.DURABLE_EVT;

                ConsumerConfiguration consumerConfiguration = ConsumerConfiguration.builder()
                        .durable(durableEvt)
                        .ackPolicy(AckPolicy.Explicit)
                        .ackWait(Duration.ofSeconds(30))
                        .maxDeliver(10)
                        .maxAckPending(10_000)
                        .filterSubject(evtSubject)
                        .build();

                PullSubscribeOptions pullSubscribeOptions = PullSubscribeOptions.builder()
                        .stream(stream)
                        .durable(durableEvt)
                        .configuration(consumerConfiguration)
                        .build();

                sub = jetStream.subscribe(evtSubject, pullSubscribeOptions);

                log.info("[NATS][EVT] subscribed OK stream={} subject={} durable={}",
                        stream, evtSubject, durableEvt);

                pullLoop();
                return;

            } catch (Exception e) {
                long backoffMs = Math.min(10_000, 500L * attempt);
                log.warn("[NATS][EVT] start failed attempt={} err='{}' -> retry in {}ms",
                        attempt, e.getMessage(), backoffMs);
                sleepSilently(backoffMs);
            }
        }
    }

    private void pullLoop() {
        while (running) {
            try {
                sub.pull(10);
                List<Message> messages = sub.fetch(10, Duration.ofSeconds(1));
                for (Message m : messages) handleMessage(m);
            } catch (Exception e) {
                log.error("[NATS][EVT][PULL] error: {}", e.getMessage(), e);
                sleepSilently(300);
            }
        }
    }

    private void handleMessage(Message message) {
        try {
            NatsJetStreamResponseInfo<Object> responseInfo = parseToResponseInfo(message);
            route(responseInfo);
            message.ack();
        } catch (Exception e) {
            log.error("[NATS][EVT] handle failed: {}", e.getMessage(), e);
            try { message.nak(); } catch (Exception ignore) {}
        }
    }

    @SneakyThrows
    private NatsJetStreamResponseInfo<Object> parseToResponseInfo(Message message) {
        byte[] data = message.getData();

        try {
            NatsJetStreamRequestInfo requestInfo =
                    CommonUtils.OBJECT_MAPPER.readValue(data, NatsJetStreamRequestInfo.class);

            return NatsJetStreamResponseInfo.builder()
                    .raw(message)
                    .subject(requestInfo.getSubject())
                    .messageType(requestInfo.getMessageType())
                    .requestId(requestInfo.getRequestId())
                    .traceId(requestInfo.getTraceId())
                    .tenantId(requestInfo.getTenantId())
                    .lang(requestInfo.getLang())
                    .headers(requestInfo.getHeaders())
                    .bodyJson(requestInfo.getBody())
                    .body(requestInfo.getBody())
                    .build();

        } catch (Exception ignored) {
            String body = new String(data, StandardCharsets.UTF_8);

            String msgType = message.getHeaders() != null ? message.getHeaders().getFirst("X-Message-Type") : null;
            String reqId   = message.getHeaders() != null ? message.getHeaders().getFirst("X-Request-Id") : null;
            String trace   = message.getHeaders() != null ? message.getHeaders().getFirst("X-Trace-Id") : null;
            String tenant  = message.getHeaders() != null ? message.getHeaders().getFirst("X-Tenant-Id") : null;
            String lang    = message.getHeaders() != null ? message.getHeaders().getFirst("X-Lang") : "en";

            return NatsJetStreamResponseInfo.builder()
                    .raw(message)
                    .subject(message.getSubject())
                    .messageType(msgType != null ? msgType : "ARENA_EVENT")
                    .requestId(reqId)
                    .traceId(trace)
                    .tenantId(tenant)
                    .lang(lang)
                    .headers(null)
                    .bodyJson(body)
                    .body(body)
                    .build();
        }
    }

    private void route(NatsJetStreamResponseInfo<Object> responseInfo) {
        String beanName = CommonUtils.toBeanName(responseInfo.getMessageType());

        if (!handlerRegistry.contains(beanName)) {
            log.warn("[HANDLER] not found messageType={} bean={} subject={}",
                    responseInfo.getMessageType(), beanName, responseInfo.getSubject());
            return;
        }

        HandlerBase<NatsJetStreamResponseInfo<Object>> handler = handlerRegistry.get(beanName);
        handler.handle(responseInfo);
    }

    private void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    @PreDestroy
    public void stop() {
        running = false;
        try { if (sub != null) sub.unsubscribe(); } catch (Exception ignored) {}
    }
}
