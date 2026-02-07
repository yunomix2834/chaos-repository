package com.chaos.task_manager.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class CommonUtils {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final DataFormatter DATA_FORMATTER = new DataFormatter();

    public static HttpStatus getHttpStatus(int statusCode) {
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (Exception e) {
            return HttpStatus.BAD_REQUEST;
        }
    }

    public static String toBeanName(String messageType) {
        // ARENA_COMMAND -> arenaCommandHandler
        if (messageType == null || messageType.isBlank()) {
            return "defaultHandler";
        }
        String lower = messageType.toLowerCase();
        String camel = toCamel(lower);
        return camel + "Handler";
    }

    public static String toCamel(String s) {
        String[] parts = s.split("[^a-z0-9]+");
        if (parts.length == 0) {
            return s;
        }
        StringBuilder stringBuilder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            stringBuilder.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return stringBuilder.toString();
    }
}
