package com.dashenbank.cms.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Component("caseHistoryLoggerDelegate")
public class CaseHistoryLoggerDelegate implements JavaDelegate {

    private static final String KEY_CASE_HISTORY = "caseHistory";

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        String event = (String) execution.getVariable("caseHistoryEvent");
        if (event == null) {
            event = "Task " + execution.getCurrentActivityId() + " executed";
        }

        Object history = execution.getVariable(KEY_CASE_HISTORY);
        String historyData = history != null ? history.toString() : "";
        historyData = historyData + (historyData.isEmpty() ? "" : "\n") + LocalDateTime.now(ZoneId.systemDefault()) + " - " + event;

        execution.setVariable(KEY_CASE_HISTORY, historyData);
        execution.setVariable("caseHistoryEvent", null);

        Map<String, Object> sla = (Map<String, Object>) execution.getVariable("sla");
        if (sla != null) {
            sla.put("updatedAt", LocalDateTime.now(ZoneId.systemDefault()).toString());
            execution.setVariable("sla", sla);
        }
    }
}
