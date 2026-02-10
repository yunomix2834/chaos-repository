package com.chaos.task_manager.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.springframework.http.HttpStatus;

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

    // Normalize message from scripter tool before send to k8s go agent
    public static String normalizeText(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();

        if (t.contains("\\\"")) {
            t = t.replace("\\\"", "\"");
        }

        t = stripWrappingQuotes(t);
        t = stripWrappingQuotes(t);

        return t.trim();
    }

    private static String stripWrappingQuotes(String t) {
        if (t == null) {
            return null;
        }
        String s = t.trim();
        while (s.length() >= 2) {
            char a = s.charAt(0);
            char b = s.charAt(s.length() - 1);

            if ((a == '"' && b == '"') || (a == '\'' && b == '\'')) {
                s = s.substring(1, s.length() - 1).trim();
                continue;
            }
            break;
        }

        return s;
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

    public static String toUpperCase(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    public static String toCamel(String s) {
        String[] parts = s.split("[^a-z0-9]+");
        if (parts.length == 0) {
            return s;
        }
        StringBuilder stringBuilder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            stringBuilder.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1));
        }
        return stringBuilder.toString();
    }
}
