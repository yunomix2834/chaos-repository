package com.chaos.task_manager.config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnvConfig {

    public static final String DB_USERNAME;
    public static final String DB_PASSWORD;

    // GRPC
    public static final Integer GRPC_SERVER_PORT;
    public static final Integer GRPC_MAX_INBOUND_METADATA_SIZE;
    public static final Integer GRPC_MAX_INBOUND_MESSAGE_SIZE;

    // NATS
    public static final String NATS_URL;
    public static final String NATS_USER;
    public static final String NATS_PASS;

    static {
        DB_USERNAME = getValue("DB_USERNAME");
        DB_PASSWORD = getValue("DB_PASSWORD");

        // GRPC
        GRPC_SERVER_PORT =
                Integer.valueOf(getValue("GRPC_SERVER_PORT", "9999"));
        GRPC_MAX_INBOUND_METADATA_SIZE = Integer.valueOf(
                getValue("GRPC_MAX_INBOUND_METADATA_SIZE", "40960"));
        GRPC_MAX_INBOUND_MESSAGE_SIZE = Integer.valueOf(
                getValue("GRPC_MAX_INBOUND_MESSAGE_SIZE", "2147483647"));

        // NATS
        NATS_URL = getValue("NATS_URL", "nats://localhost:4222");
        NATS_USER = getValue("NATS_USER");
        NATS_PASS = getValue("NATS_PASS");
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
