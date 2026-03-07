package com.myspringboot.SpringBootApp.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Component
public class LocalDateConverter implements Converter<String, LocalDate> {

    @Override
    public LocalDate convert(String source) {
        if (source == null || source.isBlank()) return null;

        // yyyy-MM  (from <input type="month">)
        if (source.matches("^\\d{4}-\\d{2}$")) {
            return YearMonth.parse(source, DateTimeFormatter.ofPattern("yyyy-MM"))
                            .atDay(1);  // store as first day of that month
        }

        // yyyy-MM-dd  (from <input type="date">)
        return LocalDate.parse(source, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}