package com.example.demo.util;

import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Slf4j
public class LocalDateTimeUtil {

    /**
     * db format
     */
    private static final String DATE_TIME_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * ui default format
     */
    private static final String DEFAULT_DATE_TIME_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss+08:00";

    /**
     * ui default timezone
     */
    private static final String DEFAULT_TIME_ZONE = "GMT+08:00";

    private LocalDateTimeUtil() {
    }

    public static String getISOTime(LocalDateTime localDateTime) {
        try {
            if (localDateTime != null) {
                ZoneOffset offset = ZoneOffset.of("+08:00");
                OffsetDateTime dateTime = OffsetDateTime.of(localDateTime, offset);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER);
                return dateTime.format(formatter);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static LocalDateTime setISOTime(String localDateTime) {
        try {
            if (localDateTime != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER);
                return LocalDateTime.parse(localDateTime, formatter);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static String getISOTime(LocalDateTime localDateTime, String zoneTime) {
        try {
            if (localDateTime != null) {
                zoneTime = "+" + zoneTime;
                ZoneOffset offset = ZoneOffset.of(zoneTime);
                OffsetDateTime dateTime = OffsetDateTime.of(localDateTime, offset);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER);
                return dateTime.format(formatter);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static long getTimeDaysDuration(LocalDateTime startTime, LocalDateTime endTime) {
        Duration duration = Duration.between(startTime, endTime);
        return duration.toDays();
    }

    public static String convertDBTime(String dbString) {
        if (dbString==null || dbString.trim().equals("")) {
            return null;
        }
        try {
            LocalDateTimeConverter localDateTimeConverter = new LocalDateTimeConverter();
            Timestamp timestamp = localDateTimeConverter.convertToDatabaseColumn(dbString);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMATTER);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIME_ZONE));
            return simpleDateFormat.format(timestamp.getTime());
        } catch (Exception e) {
            log.warn("convertDBTime error", e);
            return dbString;
        }
    }

    public static LocalDateTime convertEpochTimeToLocalDateTime(String epochTime) {
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(epochTime)), ZoneOffset.of("+0"));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalDateTime convertStrToLocalDateTime(String str, String fmt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(fmt);
        LocalDate ld = LocalDate.parse(str, formatter);
        return ld.atStartOfDay();
    }

}
