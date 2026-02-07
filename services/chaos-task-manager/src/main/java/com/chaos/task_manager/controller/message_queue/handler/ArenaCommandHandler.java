package com.chaos.task_manager.controller.message_queue.handler;

import com.chaos.task_manager.dto.common.NatsJetStreamResponseInfo;
import com.chaos.task_manager.dto.request.arena.ArenaCommandDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ArenaCommandHandler implements HandlerBase<NatsJetStreamResponseInfo<Object>> {

    @Override
    public void handle(NatsJetStreamResponseInfo<Object> ctx) {
        ArenaCommandDto dto = (ArenaCommandDto) ctx.getBody();

        log.info("[HANDLER][ARENA] reqId={} traceId={} arenaId={} taskId={} type={} target={}",
                ctx.getRequestId(), ctx.getTraceId(),
                dto.getArenaId(), dto.getTaskId(), dto.getType(), dto.getTarget());
    }
}