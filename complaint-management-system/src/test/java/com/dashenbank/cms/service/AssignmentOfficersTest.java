package com.dashenbank.cms.service;

import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
