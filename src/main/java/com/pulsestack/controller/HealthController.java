package com.pulsestack.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public String checkHealth() {
        return "backend running fine";
    }

    @GetMapping("/secure")
    public String secureEndpoint() {
        return "this is a secured endpoint congratulations if you managed to get this far";
    }
}

