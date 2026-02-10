package com.chaos.task_manager.service;

import com.arena.proto.TaskAck;
import com.arena.proto.TaskCommand;
import com.chaos.task_manager.config.template.NatsJetStreamTemplate;
import com.chaos.task_manager.domain.rollback.RollbackParser;
import com.chaos.task_manager.domain.rollback.RollbackSpec;
import com.chaos.task_manager.domain.validator.CommandValidator;
import com.chaos.task_manager.dto.request.arena.ArenaCommandDto;
import com.chaos.task_manager.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArenaTaskService {

    public static final String MESSAGE_TYPE_COMMAND = "ARENA_COMMAND";

    private final NatsJetStreamTemplate natsTemplate;

    @Value("${chaos.nats.cmdSubject}")
    @NonFinal
    String cmdSubject;

    public Mono<TaskAck> submitFromScript(TaskCommand cmd) {
        return Mono.fromCallable(() -> {
            String type = CommonUtils.normalizeText(
                    CommonUtils.toUpperCase(cmd.getType()));
            String target = CommonUtils.normalizeText(cmd.getTarget());
            String reason = CommonUtils.normalizeText(cmd.getReason());

            ArenaCommandDto dto = switch (type) {
                case "SCALE" -> {
                    CommandValidator.validateScale(cmd.getTarget(),
                            cmd.getValue());
                    yield ArenaCommandDto.builder()
                            .arenaId(cmd.getArenaId())
                            .taskId(cmd.getTaskId())
                            .type("SCALE")
                            .target(target)
                            .value(cmd.getValue())
                            .reason(reason)
                            .build();
                }
                case "KILL_PODS" -> {
                    CommandValidator.validateKillPods(cmd.getTarget(),
                            cmd.getValue());
                    yield ArenaCommandDto.builder()
                            .arenaId(cmd.getArenaId())
                            .taskId(cmd.getTaskId())
                            .type("KILL_PODS")
                            .target(target)
                            .value(cmd.getValue())
                            .reason(reason)
                            .build();
                }
                case "ROLLBACK" -> {
                    // "SCALE|default|cart|3"
                    RollbackSpec spec = RollbackParser.parse(cmd.getTarget());

                    if (!"SCALE".equals(spec.getAction())) {
                        throw new IllegalArgumentException(
                                "ROLLBACK currently supports only SCALE, got=" +
                                        spec.getAction());
                    }
                    if (spec.getValue() <= 0) {
                        throw new IllegalArgumentException(
                                "ROLLBACK SCALE replicas must be > 0");
                    }

                    String scaleTarget = spec.getNamespace() + "/deployment/" +
                            spec.getName();
                    CommandValidator.validateScale(scaleTarget,
                            spec.getValue());

                    String rbReason = "ROLLBACK(" + target + "): " +
                            (reason == null ? "" : reason);

                    yield ArenaCommandDto.builder()
                            .arenaId(cmd.getArenaId())
                            .taskId(cmd.getTaskId())
                            .type("SCALE")
                            .target(scaleTarget)
                            .value(spec.getValue())
                            .reason(rbReason)
                            .build();
                }
                default -> throw new IllegalArgumentException(
                        "unsupported type=" + cmd.getType());
            };

            Map<String, String> headers = new HashMap<>();
            headers.put("X-Lang", "en");
            // headers.put("X-Tenant-Id", ...); // idem
            headers.put("X-Trace-Id", cmd.getTaskId());
            headers.put("X-Request-Id", cmd.getTaskId());

            natsTemplate.publishRequest(cmdSubject, MESSAGE_TYPE_COMMAND, dto,
                    headers);

            log.info(
                    "[TASK][SUBMIT->NATS] arenaId={} taskId={} type={} target={} value={}",
                    dto.getArenaId(), dto.getTaskId(), dto.getType(),
                    dto.getTarget(), dto.getValue());

            return TaskAck.newBuilder()
                    .setArenaId(cmd.getArenaId())
                    .setTaskId(cmd.getTaskId())
                    .setAccepted(true)
                    .setMessage("Published to JetStream subject=" + cmdSubject +
                            " type=" + dto.getType())
                    .build();
        });
    }
}