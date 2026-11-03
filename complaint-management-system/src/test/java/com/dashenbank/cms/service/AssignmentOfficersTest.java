package com.dashenbank.cms.service;

import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentOfficersTest {

    @Test
    void keepsOrderAndDropsBlankOrRepeatedNames() {
        assertEquals(List.of("abebe", "sara"),
                AssignmentOfficers.distinctUsernames(List.of(" abebe ", "sara", "Abebe", ""), null));
    }

    @Test
    void singleQueryNameIsUsedWhenTheBodyIsEmpty() {
        assertEquals(List.of("sara"), AssignmentOfficers.distinctUsernames(null, "sara"));
    }

    @Test
    void coAssigneeIsRecognisedFromTheStoredList() {
        Map<String, Object> vars = Map.of(
                AssignmentOfficers.KEY_USERNAMES, List.of("abebe", "sara"));
        assertTrue(AssignmentOfficers.includesUsername(vars, "Sara"));
        assertFalse(AssignmentOfficers.includesUsername(vars, "lemlem"));
    }

    @Test
    void workUnitAssigneeIsRecognisedFromTheWorkUnitList() {
        Map<String, Object> vars = Map.of(
                AssignmentOfficers.KEY_WORK_UNIT_USERNAMES, List.of("east.dd", "west.dd"));
        assertTrue(AssignmentOfficers.includesUsername(vars, "WEST.DD"));
        assertFalse(AssignmentOfficers.includesUsername(vars, "north.dd"));
    }

    @Test
    void workUnitAssigneeListIsValidated() {
        assertNull(AssignmentOfficers.validateWorkUnitAssignees(Map.of()));
        assertNull(AssignmentOfficers.validateWorkUnitAssignees(
                Map.of(AssignmentOfficers.KEY_WORK_UNIT_USERNAMES, List.of("a", "b"))));
        assertNotNull(AssignmentOfficers.validateWorkUnitAssignees(
                Map.of(AssignmentOfficers.KEY_WORK_UNIT_USERNAMES, List.of(" ", ""))));

        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i <= AssignmentOfficers.MAX_WORK_UNIT_ASSIGNEES; i++) {
            tooMany.add("user" + i);
        }
        assertNotNull(AssignmentOfficers.validateWorkUnitAssignees(
                Map.of(AssignmentOfficers.KEY_WORK_UNIT_USERNAMES, tooMany)));
    }

    @Test
    void normalizingKeepsTheFirstPersonAsPrimaryAssignee() {
        Map<String, Object> vars = new HashMap<>();
        vars.put(AssignmentOfficers.KEY_WORK_UNIT_USERNAMES, List.of("east.dd", "East.DD", "west.dd"));
        AssignmentOfficers.normalizeWorkUnitAssignees(vars);

        assertEquals(List.of("east.dd", "west.dd"), vars.get(AssignmentOfficers.KEY_WORK_UNIT_USERNAMES));
        assertEquals("east.dd", vars.get("assignedOfficerUsername"));
        assertEquals("east.dd", vars.get("assigneeUsername"));
    }

    @Test
    void onlyCustomerCareOfficersAndTeamLeadersCanBeAssigned() {
        assertTrue(AssignmentOfficers.canReceiveCustomerCareCase(user(Role.ROLE_CUSTOMER_CARE_OFFICER)));
        assertTrue(AssignmentOfficers.canReceiveCustomerCareCase(user(Role.ROLE_CUSTOMER_CARE_TEAM_LEADER)));
        assertFalse(AssignmentOfficers.canReceiveCustomerCareCase(user(Role.ROLE_CUSTOMER_CARE_SENIOR_MANAGER)));
    }

    private static User user(Role role) {
        User user = new User();
        user.setUsername("person");
        user.setRole(role);
        return user;
    }
}
