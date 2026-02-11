package com.chaos.task_manager.domain.rollback;

import com.chaos.task_manager.dto.common.ErrorResponse;
import com.chaos.task_manager.exception.AppException;

public class RollbackParser {

    private RollbackParser() {
    }

    // "SCALE|default|cart|3"
    public static RollbackSpec parse(String target) {
        if (target == null) {
            throw new AppException(
                    new ErrorResponse("rollback target is null"));
        }
        String t = target.trim();
        String[] parts = t.split("\\|");
        if (parts.length != 4) {
            throw new AppException(new ErrorResponse(
                    String.format("invalid rollback target: " + target +
                            " (expected ACTION|ns|name|value)")));
        }
        String action = parts[0].trim().toUpperCase();
        String ns = parts[1].trim();
        String name = parts[2].trim();
        int v;
        try {
            v = Integer.parseInt(parts[3].trim());
        } catch (Exception e) {
            throw new AppException(new ErrorResponse(
                    String.format("invalid rollback value: " + parts[3])));
        }
        return new RollbackSpec(action, ns, name, v);
    }
}
