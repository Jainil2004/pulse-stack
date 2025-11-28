package com.pulsestack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DashboardKafkaConsumer {

    private final SystemService systemService;

    @Autowired
    public DashboardKafkaConsumer(SystemService systemService) {
        this.systemService = systemService;
    }

    @KafkaListener(topics = "logsRT", groupId = "dashboard-consumer-group")
    public void listen(String message) {
        systemService.consumeDashboardLog(message);
    }
}
