package com.chaos.task_manager.service;

import com.arena.proto.TaskAck;
import com.arena.proto.TaskCommand;
import com.chaos.task_manager.config.template.NatsJetStreamTemplate;
import com.chaos.task_manager.dto.request.arena.ArenaCommandDto;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArenaTaskService {

    public static final String messageType = "ARENA_COMMAND";

    private final NatsJetStreamTemplate natsTemplate;

    @Value("${chaos.nats.cmbSubjects}")
    @NonFinal
    String cmdSubject;

    public Mono<TaskAck> submitFromScript(TaskCommand cmd) {
        return Mono.fromCallable(() -> {
            ArenaCommandDto dto = ArenaCommandDto.builder()
                    .arenaId(cmd.getArenaId())
                    .taskId(cmd.getTaskId())
                    .type(cmd.getType())
                    .target(cmd.getTarget())
                    .value(cmd.getValue())
                    .reason(cmd.getReason())
                    .build();

            natsTemplate.publishRequest(
                    cmdSubject,
                    messageType,
                    dto,
                    Map.of("X-Lang", "en")
            );

            log.info("[TASK][SUBMIT] arenaId={} taskId={} type={} target={}",
                    dto.getArenaId(), dto.getTaskId(), dto.getType(), dto.getTarget());

            return TaskAck.newBuilder()
                    .setArenaId(cmd.getArenaId())
                    .setTaskId(cmd.getTaskId())
                    .setAccepted(true)
                    .setMessage("Published to JetStream subject=" + cmdSubject)
                    .build();
        });
    }
}
