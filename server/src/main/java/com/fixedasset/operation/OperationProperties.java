package com.fixedasset.operation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Quartz 任务表达式配置。
 */
@ConfigurationProperties(prefix = "app.quartz")
public record OperationProperties(String depreciationCron, String maintenanceCron) {
}
