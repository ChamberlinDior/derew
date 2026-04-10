package com.oviro.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ReferenceGenerator {

    private final AtomicLong counter = new AtomicLong(1000L);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generateRideReference() {
        return "RD-" + LocalDateTime.now().format(FMT) + "-" + counter.incrementAndGet();
    }

    public String generateTransactionReference() {
        return "TX-" + LocalDateTime.now().format(FMT) + "-" + counter.incrementAndGet();
    }
}
