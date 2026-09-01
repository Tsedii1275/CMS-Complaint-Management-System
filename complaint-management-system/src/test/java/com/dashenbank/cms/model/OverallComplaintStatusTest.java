package com.dashenbank.cms.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverallComplaintStatusTest {

    @Test
    void newlyRegisteredIsRecorded() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", "RECORDED");
        vars.put("currentStage", "CMD_SCREENING");
        assertEquals(OverallComplaintStatus.RECORDED, OverallComplaintStatus.resolve(vars, false));
    }

    @Test
    void investigationIsEscalated() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("requiresInvestigation", true);
        vars.put("currentStage", "CHIEF_EXPERIENCE_REVIEW");
        assertEquals(OverallComplaintStatus.ESCALATED, OverallComplaintStatus.resolve(vars, false));
    }

    @Test
    void workUnitAssignmentIsOnTrack() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", "ONTRACK");
        vars.put("currentStage", "WORK_UNIT_RESOLUTION");
        vars.put("assignedDepartment", "Cards");
        assertEquals(OverallComplaintStatus.ON_TRACK, OverallComplaintStatus.resolve(vars, false));
    }

    @Test
    void committeeDecisionIsStillEscalatedUntilCcoCloses() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", "RESOLVED");
        vars.put("decision", "RESOLVED");
        vars.put("currentStage", "COMMITTEE_ACCEPTED");
        vars.put("committeeStatus", "ACCEPTED");
        assertEquals(OverallComplaintStatus.ESCALATED, OverallComplaintStatus.resolve(vars, false));
    }

    @Test
    void ccoCloseIsResolved() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", "CLOSED");
        vars.put("currentStage", "CLOSED");
        vars.put("resolutionAccepted", true);
        assertEquals(OverallComplaintStatus.RESOLVED, OverallComplaintStatus.resolve(vars, false));
    }

    @Test
    void fcrCloseCaseIsResolvedEvenWhenDepartmentIsSet() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("fcrAction", "approve");
        vars.put("fcrStatus", "VERIFIED");
        vars.put("status", "RESOLVED");
        vars.put("currentStage", "RESOLVED");
        vars.put("decision", "FCR_APPROVED");
        vars.put("department", "Cards");
        vars.put("assignedDepartment", "Cards");
        vars.put("requiresInvestigation", false);
        assertEquals(OverallComplaintStatus.RESOLVED, OverallComplaintStatus.resolve(vars, false));
    }

    @Test
    void persistedFcrFlagKeepsResolvedOnAnalyticsReload() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("fcrStatus", true);
        vars.put("status", "RESOLVED");
        vars.put("currentStage", "RESOLVED");
        vars.put("department", "Cards");
        assertEquals(OverallComplaintStatus.RESOLVED, OverallComplaintStatus.resolve(vars, false));
    }

    @Test
    void customerFeedbackIsClosed() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", "CLOSED");
        vars.put("resolutionAccepted", true);
        assertEquals(OverallComplaintStatus.CLOSED, OverallComplaintStatus.resolve(vars, true));
    }

    @Test
    void declinedIsDeclined() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", "DECLINED");
        vars.put("decision", "DECLINED");
        assertEquals(OverallComplaintStatus.DECLINED, OverallComplaintStatus.resolve(vars, false));
    }
}
