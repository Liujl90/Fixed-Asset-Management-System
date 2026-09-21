package com.fixedasset.operation.job;

import com.fixedasset.operation.service.MaintenanceService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class MaintenanceDueCheckJob extends QuartzJobBean {

    private final MaintenanceService maintenanceService;

    public MaintenanceDueCheckJob(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        maintenanceService.checkDuePlans();
    }
}
