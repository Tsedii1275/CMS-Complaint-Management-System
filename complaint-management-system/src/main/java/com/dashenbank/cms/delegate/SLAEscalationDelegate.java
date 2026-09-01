package com.dashenbank.cms.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component("slaEscalationDelegate")
public class SLAEscalationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SLAEscalationDelegate.class);
    private static final String VAR_SLA = "sla";
    private static final String VAR_CASE_HISTORY = "caseHistory";

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        Object rawSla = execution.getVariable(VAR_SLA);
        Map<String, Object> sla;
        if (rawSla instanceof Map<?, ?> map) {
            sla = (Map<String, Object>) map;
        } else {
            sla = new HashMap<>();
        }

        sla.put("breached", true);
        execution.setVariable(VAR_SLA, sla);
        execution.setVariable("sla.breachedTime", LocalDateTime.now(ZoneId.systemDefault()).toString());

        log.info("SLA Breach Alert logged for process instance {}", execution.getProcessInstanceId());
        appendHistory(execution, "SLA Breach Alert logged at " + LocalDateTime.now(ZoneId.systemDefault()));
    }

    private void appendHistory(DelegateExecution execution, String event) {
        Object historyVar = execution.getVariable(VAR_CASE_HISTORY);
        if (historyVar == null) {
            execution.setVariable(VAR_CASE_HISTORY, event);
        } else {
            execution.setVariable(VAR_CASE_HISTORY, historyVar.toString() + System.lineSeparator() + event);
        }
    }
}
