package com.dashenbank.cms.controller;

import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NbeReportStatusTest {

    private ProcessController processController;
    private HistoricProcessInstance mockProcessInstance;

    @BeforeEach
    void setUp() {
        processController = new ProcessController(null, null, null, null, null, null, null, null, null, null, null);
        mockProcessInstance = Mockito.mock(HistoricProcessInstance.class);
    }

    @Test
    @DisplayName("Priority 1: Referred to NBE takes highest precedence")
    void testReferredToNbePrecedence() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("decision", "DECLINED");
        vars.put("status", "CLOSED");

        String status = processController.resolveNbeStatus(mockProcessInstance, vars, true);
        assertEquals("Referred to NBE", status);
    }

    @Test
    @DisplayName("Priority 2: Rejection (DECLINED) takes precedence over process completion")
    void testDeclinedPrecedenceOverResolved() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("classification", "COMPLAINT");
        vars.put("decision", "DECLINED");
        vars.put("status", "CLOSED");
        vars.put("dbcTicketId", "DBC-007/2026-27");

        Mockito.when(mockProcessInstance.getEndTime()).thenReturn(new Date());

        String status = processController.resolveNbeStatus(mockProcessInstance, vars, false);
        assertEquals("DECLINED", status);
    }

    @Test
    @DisplayName("Priority 3: Case Resolution when process ended or status is CLOSED/RESOLVED")
    void testResolvedPrecedence() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("classification", "COMPLAINT");
        vars.put("status", "CLOSED");
        vars.put("dbcTicketId", "DBC-001/2026-27");

        Mockito.when(mockProcessInstance.getEndTime()).thenReturn(new Date());

        String status = processController.resolveNbeStatus(mockProcessInstance, vars, false);
        assertEquals("RESOLVED", status);
    }

    @Test
    @DisplayName("Priority 4: Case Escalation when requiresInvestigation is true")
    void testEscalatedPrecedence() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("classification", "COMPLAINT");
        vars.put("requiresInvestigation", true);
        vars.put("dbcTicketId", "DBC-002/2026-27");

        Mockito.when(mockProcessInstance.getEndTime()).thenReturn(null);

        String status = processController.resolveNbeStatus(mockProcessInstance, vars, false);
        assertEquals("ESCALATED", status);
    }

    @Test
    @DisplayName("Priority 5: Recorded default active case")
    void testPendingDefaultStatus() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("classification", "COMPLAINT");
        vars.put("dbcTicketId", "DBC-005/2026-27");

        Mockito.when(mockProcessInstance.getEndTime()).thenReturn(null);

        String status = processController.resolveNbeStatus(mockProcessInstance, vars, false);
        assertEquals("RECORDED", status);
    }

    @Test
    @DisplayName("Regression Test: Ticket IDs starting with DBC- do NOT cause Declined status")
    void testDbcTicketIdDoesNotCauseDeclinedStatus() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("classification", "COMPLAINT");
        vars.put("dbcTicketId", "DBC-006/2026-27");
        vars.put("generalTicketId", "CM-006/2026-27");

        Mockito.when(mockProcessInstance.getEndTime()).thenReturn(null);

        String status = processController.resolveNbeStatus(mockProcessInstance, vars, false);
        assertEquals("RECORDED", status);
    }
}
