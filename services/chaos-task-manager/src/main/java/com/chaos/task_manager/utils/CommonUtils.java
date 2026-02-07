package com.chaos.task_manager.utils;

import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

@Log4j2
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
}
