package com.example.demo.util;

import org.springframework.util.StringUtils;

import javax.persistence.AttributeConverter;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class LocalDateTimeConverter implements AttributeConverter<String, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(String localTime) {
        Timestamp timestamp = null;
        if(StringUtils.hasText(localTime)){
            LocalDateTime localDateTime = LocalDateTimeUtil.setISOTime(localTime);
            timestamp = Timestamp.valueOf(localDateTime);
        }
        return timestamp;
    }

    @Override
    public String convertToEntityAttribute(Timestamp localTime) {
        String isoTime = "";
        if (localTime != null){
            LocalDateTime localDateTime = localTime.toLocalDateTime();
            isoTime = LocalDateTimeUtil.getISOTime(localDateTime);
        }
        return isoTime;
    }
}
