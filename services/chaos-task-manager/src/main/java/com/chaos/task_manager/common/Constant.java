package com.chaos.task_manager.common;

public class Constant {
    private Constant() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static class STATUS {
        public static final int SUCCESS = 1;
        public static final int ERROR = 0;
        private STATUS() {
            throw new UnsupportedOperationException("Utility class");
        }
    }
}
