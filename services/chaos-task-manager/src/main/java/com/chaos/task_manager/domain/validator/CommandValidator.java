package com.chaos.task_manager.domain.validator;

public final class CommandValidator {
    private CommandValidator() {}

    public static void validateScale(String target, int replicas) {
        if (replicas <= 0) throw new IllegalArgumentException("SCALE replicas must be > 0");
        if (target == null || target.isBlank()) throw new IllegalArgumentException("SCALE target is blank");
        // accepted: deployment/<name> OR <ns>/deployment/<name>
        String[] parts = target.trim().split("/");
        boolean ok = (parts.length == 2 && "deployment".equals(parts[0]))
                || (parts.length == 3 && "deployment".equals(parts[1]));
        if (!ok) {
            throw new IllegalArgumentException("SCALE target format invalid: " + target +
                    " (expected deployment/<name> or <ns>/deployment/<name>)");
        }
    }

    public static void validateKillPods(String selector, int percent) {
        if (selector == null || selector.isBlank()) throw new IllegalArgumentException("KILL_PODS selector is blank");
        if (percent <= 0 || percent > 100) throw new IllegalArgumentException("KILL_PODS percent must be 1..100");
    }
}