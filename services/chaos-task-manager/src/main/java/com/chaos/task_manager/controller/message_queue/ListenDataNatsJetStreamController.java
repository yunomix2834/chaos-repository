package com.chaos.task_manager.controller.message_queue;

import com.chaos.task_manager.controller.message_queue.handler.HandlerBase;
import com.chaos.task_manager.controller.message_queue.handler.HandlerRegistry;
import com.chaos.task_manager.dto.common.NatsJetStreamRequestInfo;
import com.chaos.task_manager.dto.common.NatsJetStreamResponseInfo;
import com.chaos.task_manager.utils.CommonUtils;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.ReplayPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListenDataNatsJetStreamController {

    private final Connection connection;
    private final JetStream jetStream;
    private final HandlerRegistry handlerRegistry;

    @Value("${chaos.nats.stream}")
    private String stream;

    @Value("${chaos.nats.evtSubject}")
    private String evtSubject;

    @Value("${chaos.nats.durable}")
    private String durable;

    @Value("${chaos.nats.queue}")
    private String queue;

    @PostConstruct
    public void start() throws Exception {
        Dispatcher dispatcher = connection.createDispatcher(this::onMessage);

        ConsumerConfiguration consumerConfiguration = ConsumerConfiguration.builder()
                .durable(durable + "-events")
                .ackPolicy(AckPolicy.Explicit)
                .ackWait(Duration.ofSeconds(30))
                .deliverPolicy(DeliverPolicy.New)
                .replayPolicy(ReplayPolicy.Instant)
                .filterSubject(evtSubject)
                .build();

        PushSubscribeOptions pushSubscribeOptions = PushSubscribeOptions.builder()
                .stream(stream)
                .configuration(consumerConfiguration)
                .build();

        jetStream.subscribe(evtSubject, queue, pushSubscribeOptions);

        log.info("[NATS][SUB] stream={} subject={} durable={} queue={}",
                stream, evtSubject, durable + "-events", queue);
    }

    private void onMessage(Message message) {
        Mono.fromRunnable(() -> {
            try {
                byte[] data = message.getData();

                NatsJetStreamRequestInfo requestInfo = CommonUtils.OBJECT_MAPPER.readValue(data, NatsJetStreamRequestInfo.class);
                NatsJetStreamResponseInfo<Object> responseInfo = NatsJetStreamResponseInfo.builder()
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

                route(responseInfo);

                message.ack();
            } catch (Exception e) {
                log.error("[NATS][SUB] handle failed: {}", e.getMessage(), e);
                message.nakWithDelay(Duration.ofSeconds(5));
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
    }

    private void route(NatsJetStreamResponseInfo<Object> ctx) {
        String beanName = CommonUtils.toBeanName(ctx.getMessageType());

        if (!handlerRegistry.contains(beanName)) {
            log.warn("[HANDLER] not found messageType={} bean={}", ctx.getMessageType(), beanName);
            return;
        }

        HandlerBase<NatsJetStreamResponseInfo<Object>> handler = handlerRegistry.get(beanName);
        handler.handle(ctx);
    }
}
