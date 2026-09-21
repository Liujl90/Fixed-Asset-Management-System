package com.fixedasset.operation.job;

import com.fixedasset.operation.service.DepreciationService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class DepreciationJob extends QuartzJobBean {

    private final DepreciationService depreciationService;

    public DepreciationJob(DepreciationService depreciationService) {
        this.depreciationService = depreciationService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        depreciationService.runMonthlyDepreciation(null);
    }
}
