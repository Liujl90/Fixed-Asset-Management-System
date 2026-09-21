package com.fixedasset.operation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.quartz")
public record OperationProperties(String depreciationCron, String maintenanceCron) {
}
