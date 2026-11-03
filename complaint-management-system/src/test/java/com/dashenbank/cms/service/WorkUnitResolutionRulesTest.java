package com.dashenbank.cms.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkUnitResolutionRulesTest {

    private static final String TASK = SlaAlertScope.TASK_WORK_UNIT;

    @Test
    void emptyResolutionIsRejected() {
        assertNotNull(WorkUnitResolutionRules.validate(TASK, "Work Unit", null));
        assertNotNull(WorkUnitResolutionRules.validate(TASK, "Work Unit", Map.of()));
        assertEquals("Resolution Details is required.",
                WorkUnitResolutionRules.validate(TASK, "Work Unit", Map.of("resolutionDetails", "   ",
                        "actionTaken", "done")));
    }

    @Test
    void actionTakenIsRequiredOutsideSecondaryReview() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("resolutionDetails", "Fixed the posting");
        assertEquals("Action Taken is required.", WorkUnitResolutionRules.validate(TASK, "Work Unit", vars));
        vars.put("actionTaken", "Reversed the entry");
        assertNull(WorkUnitResolutionRules.validate(TASK, "Work Unit", vars));
    }

    @Test
    void secondaryReviewOnlyNeedsFindings() {
        Map<String, Object> vars = Map.of("resolutionDetails", "Findings");
        assertNull(WorkUnitResolutionRules.validate(TASK, "Secondary Resolution Review", vars));
        assertTrue(WorkUnitResolutionRules.validate(TASK, "Secondary Resolution Review", Map.of())
                .contains("Findings"));
    }

    @Test
    void otherTasksAreNotValidated() {
        assertNull(WorkUnitResolutionRules.validate(SlaAlertScope.TASK_CMD_SCREENING, "Screening", Map.of()));
        assertNull(WorkUnitResolutionRules.validate(null, null, Map.of()));
    }
}
