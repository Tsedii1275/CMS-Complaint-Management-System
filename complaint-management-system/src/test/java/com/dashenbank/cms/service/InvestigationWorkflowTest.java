package com.dashenbank.cms.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvestigationWorkflowTest {

    @Test
    void cxoDecisionDefaultsToSendToAuditForLegacyPayloads() {
        assertEquals(InvestigationWorkflow.CXO_SEND_TO_AUDIT, InvestigationWorkflow.normalizeCxoDecision(null));
        assertEquals(InvestigationWorkflow.CXO_SEND_TO_AUDIT, InvestigationWorkflow.normalizeCxoDecision(""));
        assertEquals(InvestigationWorkflow.CXO_SEND_TO_AUDIT,
                InvestigationWorkflow.normalizeCxoDecision("something else"));
        assertEquals(InvestigationWorkflow.CXO_RETURN_TO_SENIOR_MANAGER,
                InvestigationWorkflow.normalizeCxoDecision("return to senior manager"));
        assertEquals(InvestigationWorkflow.CXO_RETURN_TO_SENIOR_MANAGER,
                InvestigationWorkflow.normalizeCxoDecision("RETURN_TO_SENIOR_MANAGER"));
    }

    @Test
    void returningToSeniorManagerRequiresAReason() {
        Map<String, Object> payload = new HashMap<>();
        payload.put(InvestigationWorkflow.KEY_CXO_DECISION, InvestigationWorkflow.CXO_RETURN_TO_SENIOR_MANAGER);

        assertNotNull(InvestigationWorkflow.validate(SlaAlertScope.TASK_CXO, payload));
        payload.put(InvestigationWorkflow.KEY_CXO_RETURN_REASON, "   ");
        assertNotNull(InvestigationWorkflow.validate(SlaAlertScope.TASK_CXO, payload));
        payload.put(InvestigationWorkflow.KEY_CXO_RETURN_REASON, "Not an audit matter");
        assertNull(InvestigationWorkflow.validate(SlaAlertScope.TASK_CXO, payload));
    }

    @Test
    void sendingToAuditNeedsNoReasonAndOtherTasksAreNotValidated() {
        Map<String, Object> send = Map.of(InvestigationWorkflow.KEY_CXO_DECISION,
                InvestigationWorkflow.CXO_SEND_TO_AUDIT);
        assertNull(InvestigationWorkflow.validate(SlaAlertScope.TASK_CXO, send));
        assertNull(InvestigationWorkflow.validate(SlaAlertScope.TASK_SM_REVIEW, Map.of()));
        assertNull(InvestigationWorkflow.validate(SlaAlertScope.TASK_SQ_REVIEW, Map.of()));
    }

    @Test
    void closingWithoutAuditRequiresASummaryForTheCustomer() {
        assertNotNull(InvestigationWorkflow.validate(SlaAlertScope.TASK_SM_CLOSURE, Map.of()));
        assertNotNull(InvestigationWorkflow.validate(SlaAlertScope.TASK_SM_CLOSURE,
                Map.of(InvestigationWorkflow.KEY_REVIEW_REMARKS, "  ")));
        assertNull(InvestigationWorkflow.validate(SlaAlertScope.TASK_SM_CLOSURE,
                Map.of(InvestigationWorkflow.KEY_REVIEW_REMARKS, "Resolved by the CXO review.")));
    }

    @Test
    void onlyReviewStepsAndCxoReturnAreHandledByTheChain() {
        assertTrue(InvestigationWorkflow.isHandledByChain(SlaAlertScope.TASK_SM_REVIEW, Map.of()));
        assertTrue(InvestigationWorkflow.isHandledByChain(SlaAlertScope.TASK_SQ_REVIEW, Map.of()));
        assertTrue(InvestigationWorkflow.isHandledByChain(SlaAlertScope.TASK_SM_CLOSURE, Map.of()));
        assertTrue(InvestigationWorkflow.isHandledByChain(SlaAlertScope.TASK_CXO,
                Map.of(InvestigationWorkflow.KEY_CXO_DECISION, InvestigationWorkflow.CXO_RETURN_TO_SENIOR_MANAGER)));
        assertFalse(InvestigationWorkflow.isHandledByChain(SlaAlertScope.TASK_CXO,
                Map.of(InvestigationWorkflow.KEY_CXO_DECISION, InvestigationWorkflow.CXO_SEND_TO_AUDIT)));
        assertFalse(InvestigationWorkflow.isHandledByChain(SlaAlertScope.TASK_CXO, Map.of()));
        assertFalse(InvestigationWorkflow.isHandledByChain(SlaAlertScope.TASK_CMD_SCREENING, Map.of()));
    }

    @Test
    void trailKeepsEarlierEntriesInOrder() {
        Map<String, Object> first = InvestigationWorkflow.trailEntry(SlaAlertScope.STAGE_SENIOR_MANAGER_REVIEW,
                "FORWARDED_TO_SERVICE_QUALITY", "manager", "ROLE_CUSTOMER_CARE_SENIOR_MANAGER", "ok", 1);
        List<Map<String, Object>> trail = InvestigationWorkflow.appendTrail(null, first);
        Map<String, Object> second = InvestigationWorkflow.trailEntry(SlaAlertScope.STAGE_SERVICE_QUALITY_REVIEW,
                "FORWARDED_TO_CHIEF_EXPERIENCE_OFFICER", "sq", "ROLE_SERVICE_QUALITY_DIRECTOR", "fine", 0);
        trail = InvestigationWorkflow.appendTrail(trail, second);

        assertEquals(2, trail.size());
        assertEquals("FORWARDED_TO_SERVICE_QUALITY", trail.get(0).get("action"));
        assertEquals("FORWARDED_TO_CHIEF_EXPERIENCE_OFFICER", trail.get(1).get("action"));
        assertEquals(1, trail.get(0).get("fileCount"));
    }

    @Test
    void mergeFilesAppendsNewUploadsToExistingEvidence() {
        List<Object> merged = InvestigationWorkflow.mergeFiles(List.of("a"), List.of("b", "c"));
        assertEquals(List.of("a", "b", "c"), merged);
        assertEquals(0, InvestigationWorkflow.mergeFiles(null, null).size());
        assertEquals(2, InvestigationWorkflow.countFiles(List.of("x", "y")));
        assertEquals(0, InvestigationWorkflow.countFiles("not a list"));
    }

    @Test
    void reviewTasksMapToRoleScopedStages() {
        assertEquals(SlaAlertScope.STAGE_SENIOR_MANAGER_REVIEW,
                SlaAlertScope.stageCodeFromTaskKey(SlaAlertScope.TASK_SM_REVIEW));
        assertEquals(SlaAlertScope.STAGE_SERVICE_QUALITY_REVIEW,
                SlaAlertScope.stageCodeFromTaskKey(SlaAlertScope.TASK_SQ_REVIEW));
        assertEquals(SlaAlertScope.STAGE_SENIOR_MANAGER_CLOSURE,
                SlaAlertScope.stageCodeFromTaskKey(SlaAlertScope.TASK_SM_CLOSURE));
        // Stage names must not be swallowed by the generic INVESTIGATION / SERVICE_QUALITY rules.
        assertEquals(SlaAlertScope.STAGE_SENIOR_MANAGER_REVIEW,
                SlaAlertScope.canonicalizeStage("SENIOR_MANAGER_REVIEW"));
        assertEquals(SlaAlertScope.STAGE_SENIOR_MANAGER_CLOSURE,
                SlaAlertScope.canonicalizeStage("SENIOR_MANAGER_CLOSURE"));
    }

    @Test
    void eachReviewTaskIsVisibleOnlyToItsOwnRole() {
        assertTrue(SlaAlertScope.roleMayHandleTask("ROLE_CUSTOMER_CARE_SENIOR_MANAGER", SlaAlertScope.TASK_SM_REVIEW));
        assertTrue(SlaAlertScope.roleMayHandleTask("ROLE_CUSTOMER_CARE_SENIOR_MANAGER", SlaAlertScope.TASK_SM_CLOSURE));
        assertFalse(SlaAlertScope.roleMayHandleTask("ROLE_CUSTOMER_CARE_SENIOR_MANAGER", SlaAlertScope.TASK_SQ_REVIEW));
        assertFalse(SlaAlertScope.roleMayHandleTask("ROLE_CUSTOMER_CARE_SENIOR_MANAGER", SlaAlertScope.TASK_CXO));

        assertTrue(SlaAlertScope.roleMayHandleTask("ROLE_SERVICE_QUALITY_DIRECTOR", SlaAlertScope.TASK_SQ_REVIEW));
        assertFalse(SlaAlertScope.roleMayHandleTask("ROLE_SERVICE_QUALITY_DIRECTOR", SlaAlertScope.TASK_SM_REVIEW));
        assertFalse(SlaAlertScope.roleMayHandleTask("ROLE_SERVICE_QUALITY_DIRECTOR", SlaAlertScope.TASK_SM_CLOSURE));

        assertTrue(SlaAlertScope.roleMayHandleTask("ROLE_CHIEF_EXPERIENCE_OFFICER", SlaAlertScope.TASK_CXO));
        assertFalse(SlaAlertScope.roleMayHandleTask("ROLE_CHIEF_EXPERIENCE_OFFICER", SlaAlertScope.TASK_SM_REVIEW));
        assertFalse(SlaAlertScope.roleMayHandleTask("ROLE_CHIEF_EXPERIENCE_OFFICER", SlaAlertScope.TASK_SQ_REVIEW));

        for (String key : List.of(SlaAlertScope.TASK_SM_REVIEW, SlaAlertScope.TASK_SQ_REVIEW,
                SlaAlertScope.TASK_SM_CLOSURE)) {
            assertFalse(SlaAlertScope.roleMayHandleTask("ROLE_CUSTOMER_CARE_OFFICER", key), key);
            assertFalse(SlaAlertScope.roleMayHandleTask("ROLE_CUSTOMER_CARE_TEAM_LEADER", key), key);
            assertFalse(SlaAlertScope.roleMayHandleTask("ROLE_AUDIT_INVESTIGATION_TEAM", key), key);
            assertTrue(SlaAlertScope.roleMayHandleTask("ROLE_ADMIN", key), key);
        }
    }
}
