package com.chaos.task_manager.controller.message_queue.handler;

import com.chaos.task_manager.dto.common.NatsJetStreamResponseInfo;
import com.chaos.task_manager.dto.response.arena.ArenaEventDto;
import com.chaos.task_manager.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ArenaEventHandler implements HandlerBase<NatsJetStreamResponseInfo<Object>> {

    @Override
    public void handle(NatsJetStreamResponseInfo<Object> ctx) {
        try {
            ArenaEventDto evt;

            Object body = ctx.getBody();
            if (body instanceof String s) {
                evt = CommonUtils.OBJECT_MAPPER.readValue(s, ArenaEventDto.class);
            } else {
                evt = CommonUtils.OBJECT_MAPPER.convertValue(body, ArenaEventDto.class);
            }

            log.info("[EVT] arenaId={} taskId={} status={} type={} target={} value={} msg={} ts={} src={}",
                    evt.getArenaId(), evt.getTaskId(), evt.getStatus(),
                    evt.getType(), evt.getTarget(), evt.getValue(),
                    evt.getMessage(), evt.getTs(), evt.getSource());

        } catch (Exception e) {
            log.error("[EVT] parse failed: {}", e.getMessage(), e);
            log.info("[EVT] raw bodyJson={}", ctx.getBodyJson());
        }
    }
}