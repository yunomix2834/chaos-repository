package com.chaos.task_manager.controller.message_queue.handler;

import com.chaos.task_manager.dto.common.NatsJetStreamResponseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ArenaEventHandler implements HandlerBase<NatsJetStreamResponseInfo<Object>> {

    @Override
    public void handle(NatsJetStreamResponseInfo<Object> ctx) {
        // Tạm thời log raw để debug structure
        log.info("[HANDLER][ARENA_EVENT] subject={} reqId={} traceId={} bodyJson={}",
                ctx.getSubject(), ctx.getRequestId(), ctx.getTraceId(), ctx.getBodyJson());
    }
}