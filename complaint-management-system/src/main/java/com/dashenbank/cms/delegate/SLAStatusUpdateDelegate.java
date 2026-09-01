package com.dashenbank.cms.delegate;

import com.dashenbank.cms.service.SlaTrackingService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("slaStatusUpdateDelegate")
public class SLAStatusUpdateDelegate implements JavaDelegate {

    @Autowired
    private SlaTrackingService slaTrackingService;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();

        slaTrackingService.getMetricsByProcessInstanceId(processInstanceId).ifPresent(metrics -> {
            slaTrackingService.recalculateSlaStatus(metrics);

            // Sync to process variables
            Map<String, Object> slaVars = new HashMap<>();
            slaVars.put("totalAllowedMinutes", metrics.getTotalAllowedMinutes());
            slaVars.put("totalElapsedMinutes", metrics.getTotalElapsedMinutes());
            slaVars.put("remainingMinutes", metrics.getRemainingMinutes());
            slaVars.put("status", metrics.getSlaStatus());
            slaVars.put("deadline", metrics.getDeadline().toString());
            slaVars.put("isBreached", metrics.getBreached());

            execution.setVariable("sla", slaVars);
            appendHistory(execution, "SLA status updated: " + metrics.getSlaStatus());
        });
    }

    private void appendHistory(DelegateExecution execution, String event) {
        Object historyVar = execution.getVariable("caseHistory");
        if (historyVar == null) {
            execution.setVariable("caseHistory", event);
        } else {
            execution.setVariable("caseHistory", historyVar.toString() + "\n" + event);
        }
    }
}
