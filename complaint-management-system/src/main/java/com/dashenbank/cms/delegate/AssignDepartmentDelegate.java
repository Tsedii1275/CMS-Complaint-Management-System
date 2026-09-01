package com.dashenbank.cms.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("assignDepartmentDelegate")
public class AssignDepartmentDelegate implements JavaDelegate {

    private static final String WORK_UNIT = "work-unit";
    private static final String KEY_ASSIGNED_DEPARTMENT = "assignedDepartment";
    private static final String KEY_CASE_HISTORY = "caseHistory";

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        Map<String, Object> complaint = (Map<String, Object>) execution.getVariable("complaint");
        
        // Strictly honor user-driven department assignment by Customer Care Officer
        String assigned = (String) execution.getVariable(KEY_ASSIGNED_DEPARTMENT);
        if ((assigned == null || assigned.isBlank())
                && complaint != null
                && complaint.get(KEY_ASSIGNED_DEPARTMENT) != null) {
            assigned = (String) complaint.get(KEY_ASSIGNED_DEPARTMENT);
        }

        if (assigned == null || assigned.isBlank()) {
            assigned = WORK_UNIT; // Safe fallback if unassigned
        }

        execution.setVariable(KEY_ASSIGNED_DEPARTMENT, assigned);
        execution.setVariable("caseOwner", assigned + "-user");

        Object history = execution.getVariable(KEY_CASE_HISTORY);
        String event = "Assigned department: " + assigned;
        if (history == null) {
            execution.setVariable(KEY_CASE_HISTORY, event);
        } else {
            execution.setVariable(KEY_CASE_HISTORY, history.toString() + "\n" + event);
        }
    }
}

