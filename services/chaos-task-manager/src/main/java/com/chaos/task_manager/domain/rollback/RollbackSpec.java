package com.chaos.task_manager.domain.rollback;

import lombok.Value;

@Value
public class RollbackSpec {
    String action;     // SCALE
    String namespace;  // default
    String name;       // cart
    int value;         // 3
}