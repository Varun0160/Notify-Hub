package com.notifyhub.common;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PingController {

    private final PingService pingService;   // final: immutable, can't be forgotten

    // Constructor injection: Spring sees one constructor and passes the PingService bean in.
    public PingController(PingService pingService) {
        this.pingService = pingService;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return pingService.status();
    }
}