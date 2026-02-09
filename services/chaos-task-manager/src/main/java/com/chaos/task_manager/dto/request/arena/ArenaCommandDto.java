package com.chaos.task_manager.dto.request.arena;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArenaCommandDto {
    String arenaId;
    String taskId;
    String type;     // SCALE | KILL_PODS
    String target;   // deployment/cart OR ns/deployment/cart OR app=cart
    Integer value;   // replicas OR percent
    String reason;
}
