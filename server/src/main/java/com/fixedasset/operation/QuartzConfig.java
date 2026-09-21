package com.fixedasset.operation;

import com.fixedasset.operation.job.DepreciationJob;
import com.fixedasset.operation.job.MaintenanceDueCheckJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 内存调度配置。
 *
 * <p>演示环境使用 RAMJobStore，应用重启后任务定义由 Spring 重新注册。生产集群环境
 * 应切换为 JDBC JobStore，并增加 Quartz 集群配置和任务幂等约束。</p>
 */
@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail depreciationJobDetail() {
        return JobBuilder.newJob(DepreciationJob.class)
                .withIdentity("depreciationJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger depreciationTrigger(JobDetail depreciationJobDetail, OperationProperties properties) {
        return TriggerBuilder.newTrigger()
                .forJob(depreciationJobDetail)
                .withIdentity("depreciationTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(properties.depreciationCron()))
                .build();
    }

    @Bean
    public JobDetail maintenanceDueCheckJobDetail() {
        return JobBuilder.newJob(MaintenanceDueCheckJob.class)
                .withIdentity("maintenanceDueCheckJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger maintenanceDueCheckTrigger(
            JobDetail maintenanceDueCheckJobDetail,
            OperationProperties properties
    ) {
        return TriggerBuilder.newTrigger()
                .forJob(maintenanceDueCheckJobDetail)
                .withIdentity("maintenanceDueCheckTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(properties.maintenanceCron()))
                .build();
    }
}
