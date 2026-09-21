package com.dashenbank.cms.service;

import com.dashenbank.cms.model.ComplaintSlaMetrics;
import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class SlaAlertAuthorizationServiceTest {

    private SlaAlertAuthorizationService service;

    @BeforeEach
    void setUp() {
        SlaTrackingService tracking = mock(SlaTrackingService.class);
        doNothing().when(tracking).recalculateSlaStatus(any());
        service = new SlaAlertAuthorizationService(null, null, null, tracking);
    }

    @Test
    void customerCareSeesOnlyScreeningStageBreach() {
        ComplaintSlaMetrics metrics = metrics("CMD_SCREENING", "BREACHED", "Loans", "Bole");
        User cco = user(Role.ROLE_CUSTOMER_CARE_OFFICER, null, null);
        User workUnit = user(Role.ROLE_DEPARTMENT_WORKUNIT, "Loans", "Bole");

        assertTrue(service.isStageSlaAlertVisible("ROLE_CUSTOMER_CARE_OFFICER", cco,
                SlaAlertScope.TASK_CMD_SCREENING, metrics));
        assertFalse(service.isStageSlaAlertVisible("ROLE_DEPARTMENT_WORKUNIT", workUnit,
                SlaAlertScope.TASK_CMD_SCREENING, metrics));
        assertFalse(service.isStageSlaAlertVisible("ROLE_AUDIT_INVESTIGATION_TEAM",
                user(Role.ROLE_AUDIT_INVESTIGATION_TEAM, null, null),
                SlaAlertScope.TASK_CMD_SCREENING, metrics));
        assertFalse(service.isStageSlaAlertVisible("ROLE_SERVICE_QUALITY_DIRECTOR",
                user(Role.ROLE_SERVICE_QUALITY_DIRECTOR, null, null),
                SlaAlertScope.TASK_CMD_SCREENING, metrics));
    }

    @Test
    void workUnitSeesOnlyWorkUnitStageBreach() {
        ComplaintSlaMetrics metrics = metrics("WORK_UNIT_RESOLUTION", "BREACHED", "Loans", "Bole");
        User workUnit = user(Role.ROLE_DEPARTMENT_WORKUNIT, "Loans", "Bole");
        User cco = user(Role.ROLE_CUSTOMER_CARE_OFFICER, null, null);

        assertTrue(service.isStageSlaAlertVisible("ROLE_DEPARTMENT_WORKUNIT", workUnit,
                SlaAlertScope.TASK_WORK_UNIT, metrics));
        assertFalse(service.isStageSlaAlertVisible("ROLE_CUSTOMER_CARE_OFFICER", cco,
                SlaAlertScope.TASK_WORK_UNIT, metrics));
        assertFalse(service.isStageSlaAlertVisible("ROLE_CUSTOMER_CARE_TEAM_LEADER",
                user(Role.ROLE_CUSTOMER_CARE_TEAM_LEADER, null, null),
                SlaAlertScope.TASK_WORK_UNIT, metrics));
    }

    @Test
    void auditSeesOnlyInvestigationStageBreach() {
        ComplaintSlaMetrics metrics = metrics("INVESTIGATION", "APPROACHING", null, null);
        User audit = user(Role.ROLE_AUDIT_INVESTIGATION_TEAM, null, null);

        assertTrue(service.isStageSlaAlertVisible("ROLE_AUDIT_INVESTIGATION_TEAM", audit,
                SlaAlertScope.TASK_INVESTIGATION, metrics));
        assertFalse(service.isStageSlaAlertVisible("ROLE_CUSTOMER_CARE_OFFICER",
                user(Role.ROLE_CUSTOMER_CARE_OFFICER, null, null),
                SlaAlertScope.TASK_INVESTIGATION, metrics));
        assertFalse(service.isStageSlaAlertVisible("ROLE_DEPARTMENT_WORKUNIT",
                user(Role.ROLE_DEPARTMENT_WORKUNIT, "Loans", null),
                SlaAlertScope.TASK_INVESTIGATION, metrics));
    }

    @Test
    void serviceQualitySeesOnlyServiceQualityStageBreach() {
        ComplaintSlaMetrics metrics = metrics("SERVICE_QUALITY_REVIEW", "BREACHED", null, null);
        assertTrue(service.isStageSlaAlertVisible("ROLE_SERVICE_QUALITY_DIRECTOR",
                user(Role.ROLE_SERVICE_QUALITY_DIRECTOR, null, null),
                SlaAlertScope.TASK_SERVICE_QUALITY, metrics));
        assertFalse(service.isStageSlaAlertVisible("ROLE_CUSTOMER_CARE_OFFICER",
                user(Role.ROLE_CUSTOMER_CARE_OFFICER, null, null),
                SlaAlertScope.TASK_SERVICE_QUALITY, metrics));
    }

    @Test
    void complaintMovingFromScreeningToWorkUnitChangesVisibility() {
        User cco = user(Role.ROLE_CUSTOMER_CARE_OFFICER, null, null);
        User workUnit = user(Role.ROLE_DEPARTMENT_WORKUNIT, "Loans", "Bole");

        ComplaintSlaMetrics screening = metrics("CMD_SCREENING", "BREACHED", "Loans", "Bole");
        assertTrue(service.isStageSlaAlertVisible("ROLE_CUSTOMER_CARE_OFFICER", cco,
                SlaAlertScope.TASK_CMD_SCREENING, screening));
        assertFalse(service.isStageSlaAlertVisible("ROLE_DEPARTMENT_WORKUNIT", workUnit,
                SlaAlertScope.TASK_CMD_SCREENING, screening));

        ComplaintSlaMetrics workUnitStage = metrics("WORK_UNIT_RESOLUTION", "BREACHED", "Loans", "Bole");
        assertFalse(service.isStageSlaAlertVisible("ROLE_CUSTOMER_CARE_OFFICER", cco,
                SlaAlertScope.TASK_WORK_UNIT, workUnitStage));
        assertTrue(service.isStageSlaAlertVisible("ROLE_DEPARTMENT_WORKUNIT", workUnit,
                SlaAlertScope.TASK_WORK_UNIT, workUnitStage));
    }

    @Test
    void overallBreachDoesNotCreateAlertForMismatchedActiveStage() {
        ComplaintSlaMetrics stale = metrics("CMD_SCREENING", "BREACHED", "Loans", "Bole");
        stale.setSlaStatus("BREACHED");
        stale.setBreached(true);
        User workUnit = user(Role.ROLE_DEPARTMENT_WORKUNIT, "Loans", "Bole");

        assertFalse(service.isStageSlaAlertVisible("ROLE_DEPARTMENT_WORKUNIT", workUnit,
                SlaAlertScope.TASK_WORK_UNIT, stale));
        assertEquals("ON_TIME", service.stageSlaStatusForTask(stale, SlaAlertScope.TASK_WORK_UNIT));
        assertEquals("OVERDUE", service.stageSlaStatusForTask(stale, SlaAlertScope.TASK_CMD_SCREENING));
    }

    @Test
    void organizationalScopeBlocksOtherDepartmentWorkUnitAlerts() {
        ComplaintSlaMetrics metrics = metrics("WORK_UNIT_RESOLUTION", "BREACHED", "Loans", "Bole");
        User otherDept = user(Role.ROLE_DEPARTMENT_WORKUNIT, "Trade", "Bole");
        User sameDept = user(Role.ROLE_DEPARTMENT_WORKUNIT, "Loans", "Bole");

        assertFalse(service.isStageSlaAlertVisible("ROLE_DEPARTMENT_WORKUNIT", otherDept,
                SlaAlertScope.TASK_WORK_UNIT, metrics));
        assertTrue(service.isStageSlaAlertVisible("ROLE_DEPARTMENT_WORKUNIT", sameDept,
                SlaAlertScope.TASK_WORK_UNIT, metrics));
    }

    @Test
    void unusedRolesDoNotReceiveForeignStageAlerts() {
        ComplaintSlaMetrics metrics = metrics("CMD_SCREENING", "BREACHED", null, null);
        User branch = user(Role.ROLE_BRANCH_MANAGER, null, "Bole");

        assertTrue(SlaAlertScope.canAssignTasks("ROLE_CUSTOMER_CARE_TEAM_LEADER"));
        assertFalse(SlaAlertScope.canAssignTasks("ROLE_DEPARTMENT_WORKUNIT"));
        assertTrue(SlaAlertScope.authorizedTaskKeys("ROLE_BRANCH_MANAGER").contains(SlaAlertScope.TASK_BRANCH_CAPTURE));
        assertFalse(SlaAlertScope.authorizedTaskKeys("ROLE_DEPARTMENT_WORKUNIT")
                .contains(SlaAlertScope.TASK_CMD_SCREENING));
        assertEquals("Customer Care Screening", SlaAlertScope.stageLabel(SlaAlertScope.STAGE_CMD_SCREENING));
        assertEquals("Department / Work Unit Resolution",
                SlaAlertScope.stageLabel(SlaAlertScope.STAGE_WORK_UNIT_RESOLUTION));
        assertFalse(service.isStageSlaAlertVisible("ROLE_BRANCH_MANAGER", branch,
                SlaAlertScope.TASK_CMD_SCREENING, metrics));
        assertFalse(service.isStageSlaAlertVisible("ROLE_ANONYMOUS", branch,
                SlaAlertScope.TASK_CMD_SCREENING, metrics));
    }

    @Test
    void committeeAndCxoAreIsolatedFromEachOther() {
        ComplaintSlaMetrics committee = metrics("COMMITTEE_REVIEW", "BREACHED", null, null);
        ComplaintSlaMetrics cxo = metrics("CHIEF_EXPERIENCE_REVIEW", "BREACHED", null, null);

        assertTrue(service.isStageSlaAlertVisible("ROLE_COMMITTEE_SECRETARY",
                user(Role.ROLE_COMMITTEE_SECRETARY, null, null), SlaAlertScope.TASK_COMMITTEE, committee));
        assertFalse(service.isStageSlaAlertVisible("ROLE_CHIEF_EXPERIENCE_OFFICER",
                user(Role.ROLE_CHIEF_EXPERIENCE_OFFICER, null, null), SlaAlertScope.TASK_COMMITTEE, committee));
        assertTrue(service.isStageSlaAlertVisible("ROLE_CHIEF_EXPERIENCE_OFFICER",
                user(Role.ROLE_CHIEF_EXPERIENCE_OFFICER, null, null), SlaAlertScope.TASK_CXO, cxo));
        assertFalse(service.isStageSlaAlertVisible("ROLE_COMMITTEE_SECRETARY",
                user(Role.ROLE_COMMITTEE_SECRETARY, null, null), SlaAlertScope.TASK_CXO, cxo));
    }

    @Test
    void canViewSlaRecordRejectsOutOfScopeTicketIdLookups() {
        ComplaintSlaMetrics screening = metrics("CMD_SCREENING", "BREACHED", "Loans", "Bole");
        User workUnit = user(Role.ROLE_DEPARTMENT_WORKUNIT, "Loans", "Bole");
        User cco = user(Role.ROLE_CUSTOMER_CARE_OFFICER, null, null);

        assertFalse(service.canViewSlaRecord("ROLE_DEPARTMENT_WORKUNIT", workUnit, screening,
                SlaAlertScope.TASK_CMD_SCREENING));
        assertTrue(service.canViewSlaRecord("ROLE_CUSTOMER_CARE_OFFICER", cco, screening,
                SlaAlertScope.TASK_CMD_SCREENING));
        assertTrue(service.canViewSlaRecord("ROLE_ADMIN", user(Role.ROLE_ADMIN, null, null), screening,
                SlaAlertScope.TASK_CMD_SCREENING));
    }

    @Test
    void onTrackStageIsNotAnAlert() {
        ComplaintSlaMetrics metrics = metrics("CMD_SCREENING", "ON_TRACK", null, null);
        assertFalse(service.isStageSlaAlertVisible("ROLE_CUSTOMER_CARE_OFFICER",
                user(Role.ROLE_CUSTOMER_CARE_OFFICER, null, null),
                SlaAlertScope.TASK_CMD_SCREENING, metrics));
    }

    private static User user(Role role, String department, String branch) {
        return User.builder()
                .username(role.name().toLowerCase())
                .email("user@example.com")
                .password("secret")
                .role(role)
                .department(department)
                .branch(branch)
                .build();
    }

    private static ComplaintSlaMetrics metrics(String stage, String stageStatus, String department, String branch) {
        return ComplaintSlaMetrics.builder()
                .processInstanceId("proc-1")
                .complaintId("DBC-003/2026-27")
                .currentStage(stage)
                .currentStageStatus(stageStatus)
                .department(department)
                .branch(branch)
                .slaStatus("ON_TRACK")
                .breached(false)
                .build();
    }
}
