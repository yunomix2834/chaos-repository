package com.chaos.task_manager.dto.response.arena;

import lombok.Data;

@Data
public class ArenaEventDto {
    private String arenaId;
    private String taskId;
    private String type;
    private String target;
    private Integer value;

    private String status;     // RUNNING | SUCCEEDED | FAILED
    private String message;

    private String ts;         // Go dùng json:"ts"
    private String traceId;
    private String requestId;
    private String tenantId;
    private String lang;
    private String source;
}