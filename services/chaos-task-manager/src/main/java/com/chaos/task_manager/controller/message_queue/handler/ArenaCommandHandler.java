package com.chaos.task_manager.controller.message_queue.handler;

import com.chaos.task_manager.dto.common.NatsJetStreamResponseInfo;
import com.chaos.task_manager.dto.request.arena.ArenaCommandDto;
import com.chaos.task_manager.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class ArenaCommandHandler
        implements HandlerBase<NatsJetStreamResponseInfo<Object>> {

    @Override
    public void handle(NatsJetStreamResponseInfo<Object> ctx) {
        try {
            ArenaCommandDto dto;

            Object body = ctx.getBody();
            if (body instanceof String s) {
                dto = CommonUtils.OBJECT_MAPPER.readValue(s,
                        ArenaCommandDto.class);
            } else {
                dto = CommonUtils.OBJECT_MAPPER.convertValue(body,
                        ArenaCommandDto.class);
            }

            log.info(
                    "[HANDLER][ARENA_CMD] reqId={} traceId={} arenaId={} taskId={} type={} target={} value={}",
                    ctx.getRequestId(), ctx.getTraceId(),
                    dto.getArenaId(), dto.getTaskId(), dto.getType(),
                    dto.getTarget(), dto.getValue());

        } catch (Exception e) {
            log.error("[HANDLER][ARENA_CMD] parse failed: {}", e.getMessage(),
                    e);
        }
    }
}