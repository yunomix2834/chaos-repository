package com.chaos.task_manager.config;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EnvConfig {

    public static final String DB_USERNAME;
    public static final String DB_PASSWORD;

    // GRPC
    public static final Integer GRPC_SERVER_PORT;

    static {
        DB_USERNAME = getValue("DB_USERNAME");
        DB_PASSWORD = getValue("DB_PASSWORD");

        GRPC_SERVER_PORT = Integer.valueOf(getValue("GRPC_SERVER_PORT", "9090"));
    }

    private static String getValue(String key, String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }

    private static String getValue(String key) {
        String value = getValue(key, null);
        if (value == null) {
            log.error("env {} has not been assigned a value", key);
            log.info("env {} has not been assigned a value", key);
        }

        return value;
    }
}
