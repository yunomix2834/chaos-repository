package com.chaos.task_manager.config;

import io.nats.client.Connection;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.RetentionPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class NatsJetStreamConfig {

    public static final String url = EnvConfig.NATS_URL;
    public static final String user = EnvConfig.NATS_USER;
    public static final String password = EnvConfig.NATS_PASS;

    public static final String STREAM = "TASKS";

    public static final String CMD_FILTER = "task.cmd.*";
    public static final String EVT_FILTER = "task.events.*";
    public static final String DURABLE_CMD = "task-manager-cmd";
    public static final String DURABLE_EVT = "task-manager-evt";

    private Connection connection;

    @Bean
    @SneakyThrows
    public Connection natsConnection() throws Exception {
        Options.Builder builder = new Options.Builder()
                .server(url)
                .connectionTimeout(Duration.ofSeconds(5))

                .maxReconnects(-1)
                .reconnectWait(Duration.ofSeconds(5))
                .pingInterval(Duration.ofSeconds(30))
                .maxPingsOut(5)

                .connectionListener((conn, type) ->
                        log.info("[NATS]: {} connectedUrl={}", type, conn.getConnectedUrl())
                )
                .errorListener(new ErrorListener() {
                    @Override
                    public void exceptionOccurred(Connection conn, Exception exp) {
                        log.warn("[NATS][EX]: {}", exp.getMessage(), exp);
                    }

                    @Override
                    public void errorOccurred(Connection conn, String error) {
                        log.warn("[NATS][ERR]: {}", error);
                    }

                    @Override
                    public void slowConsumerDetected(Connection conn, Consumer consumer) {
                        log.warn("[NATS][SLOW] consumer = {}", consumer);
                    }
                });

        if (user != null && password != null) {
            builder.userInfo(user, password);
        }

        this.connection = Nats.connect(builder.build());
        log.info("[NATS] connected OK url = {}", this.connection.getConnectedUrl());
        return this.connection;
    }

    @Bean
    @SneakyThrows
    public JetStream jetStream(Connection connection) {
        return connection.jetStream();
    }

    @Bean
    @SneakyThrows
    public JetStreamManagement jetStreamManagement(Connection connection) {
        return connection.jetStreamManagement();
    }

    @SneakyThrows
    public void ensureStreamAndConsumers(JetStreamManagement jetStreamManagement) {
        StreamConfiguration streamConfiguration = StreamConfiguration.builder()
                .name(STREAM)
                .subjects(CMD_FILTER, EVT_FILTER)
                .storageType(StorageType.File)
                .retentionPolicy(RetentionPolicy.Limits)
                .maxAge(Duration.ofDays(7))
                .replicas(1)
                .build();

        try {
            jetStreamManagement.addStream(streamConfiguration);
            log.info("[NATS][JS] stream created name={}", STREAM);
        } catch (JetStreamApiException ex) {
            jetStreamManagement.updateStream(streamConfiguration);
            log.info("[NATS][JS] stream updated name={}", STREAM);
        }

        ConsumerConfiguration cmdConsumer = ConsumerConfiguration.builder()
                .durable(DURABLE_CMD)
                .ackPolicy(AckPolicy.Explicit)
                .ackWait(Duration.ofSeconds(30))
                .maxDeliver(10)
                .maxAckPending(10_000)
                .filterSubject(CMD_FILTER)
                .build();

        ConsumerConfiguration evtConsumer = ConsumerConfiguration.builder()
                .durable(DURABLE_EVT)
                .ackPolicy(AckPolicy.Explicit)
                .ackWait(Duration.ofSeconds(30))
                .maxDeliver(10)
                .maxAckPending(10_000)
                .filterSubject(EVT_FILTER)
                .build();

        jetStreamManagement.addOrUpdateConsumer(STREAM, cmdConsumer);
        jetStreamManagement.addOrUpdateConsumer(STREAM, evtConsumer);

        log.info("[NATS][JS] consumer ensured durable={} filter={}", DURABLE_CMD, CMD_FILTER);
        log.info("[NATS][JS] consumer ensured durable={} filter={}", DURABLE_EVT, EVT_FILTER);
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (connection != null) {
                connection.drain(Duration.ofSeconds(3));
                connection.close();
            }
        } catch (Exception e) {
            log.warn("[NATS] shutdown warn: {}", e.getMessage());
        }
    }
}
