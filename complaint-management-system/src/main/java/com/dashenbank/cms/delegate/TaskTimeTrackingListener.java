package com.dashenbank.cms.delegate;

import com.dashenbank.cms.service.SlaTrackingService;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.engine.delegate.TaskListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("taskTimeTrackingListener")
public class TaskTimeTrackingListener implements TaskListener {

    @Autowired
    private SlaTrackingService slaTrackingService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskId = delegateTask.getId();
        String processInstanceId = delegateTask.getProcessInstanceId();
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String taskName = delegateTask.getName();
        String assignee = delegateTask.getAssignee();

        // Robust resolution of complaintId / ticketId from process variables
        String complaintId = null;
        Object dbcTicketObj = delegateTask.getVariable("dbcTicketId");
        if (dbcTicketObj != null && !dbcTicketObj.toString().isBlank()) {
            complaintId = dbcTicketObj.toString();
        }
        if (complaintId == null) {
            Object cIdObj = delegateTask.getVariable("complaintId");
            if (cIdObj != null && !cIdObj.toString().isBlank()) {
                complaintId = cIdObj.toString();
            }
        }
        if (complaintId == null) {
            Object complaintObj = delegateTask.getVariable("complaint");
            if (complaintObj instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) complaintObj;
                if (map.get("dbcTicketId") != null && !map.get("dbcTicketId").toString().isBlank()) {
                    complaintId = map.get("dbcTicketId").toString();
                } else if (map.get("id") != null && !map.get("id").toString().isBlank()) {
                    complaintId = map.get("id").toString();
                }
            }
        }
        if (complaintId == null || complaintId.isBlank() || "unknown".equalsIgnoreCase(complaintId)) {
            complaintId = processInstanceId;
        }

        if ("create".equalsIgnoreCase(eventName)) {
            slaTrackingService.recordTaskStart(processInstanceId, complaintId, taskId, taskDefinitionKey, taskName,
                    assignee);
        } else if ("complete".equalsIgnoreCase(eventName)) {
            slaTrackingService.recordTaskCompletion(taskId, assignee);
        }
    }
}
