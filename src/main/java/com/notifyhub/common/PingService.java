package com.notifyhub.common;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;

@Service
public class PingService {

    public Map<String, Object> status() {
        return Map.of(
                "service", "notifyhub",
                "status", "UP",
                "timestamp", Instant.now().toString()
        );
    }
}