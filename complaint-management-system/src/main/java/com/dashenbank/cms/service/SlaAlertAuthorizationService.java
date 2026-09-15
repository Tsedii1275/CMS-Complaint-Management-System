package com.dashenbank.cms.service;

import com.dashenbank.cms.model.ComplaintSlaMetrics;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.repository.UserRepository;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class SlaAlertAuthorizationService {

    public static final String STATUS_APPROACHING = "APPROACHING";
    public static final String STATUS_BREACHED = "BREACHED";
    public static final String STATUS_OVERDUE = "OVERDUE";
    public static final String STATUS_ON_TIME = "ON_TIME";

    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TaskService taskService;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;
    private final UserRepository userRepository;
    private final SlaTrackingService slaTrackingService;

    public SlaAlertAuthorizationService(TaskService taskService,
            ComplaintSlaMetricsRepository slaMetricsRepository,
            UserRepository userRepository,
            SlaTrackingService slaTrackingService) {
        this.taskService = taskService;
        this.slaMetricsRepository = slaMetricsRepository;
        this.userRepository = userRepository;
        this.slaTrackingService = slaTrackingService;
    }

    public User requireCurrentUser() {
        String username = currentUsername();
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            return SlaAlertScope.normalizeRole(auth.getAuthorities().iterator().next().getAuthority());
        }
        return "ROLE_ANONYMOUS";
    }

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    /**
     * SLA alert is visible only when the active workflow task belongs to the
     * user's role, the stage SLA on that task is approaching or breached, and
     * the user's organizational unit matches the complaint.
     */
    public boolean isStageSlaAlertVisible(String role, User user, String activeTaskDefinitionKey,
            ComplaintSlaMetrics metrics) {
        if (user == null || activeTaskDefinitionKey == null || metrics == null) {
            return false;
        }
        if (!SlaAlertScope.roleMayHandleTask(role, activeTaskDefinitionKey)) {
            return false;
        }
        if (!SlaAlertScope.isAdmin(role)
                && !SlaAlertScope.matchesOrganizationalScope(user, metrics.getBranch(), metrics.getDistrict(),
                        metrics.getDepartment())) {
            return false;
        }
        String activeStage = SlaAlertScope.stageCodeFromTaskKey(activeTaskDefinitionKey);
        if (activeStage == null && SlaAlertScope.roleMayHandleTask(role, activeTaskDefinitionKey)) {
            activeStage = SlaAlertScope.STAGE_WORK_UNIT_RESOLUTION;
        }
        return isApproachingOrBreachedForActiveStage(metrics, activeStage);
    }

    public boolean canViewSlaRecord(String role, User user, ComplaintSlaMetrics metrics, String activeTaskKey) {
        if (SlaAlertScope.isAdmin(role)) {
            return true;
        }
        if (user == null || metrics == null) {
            return false;
        }
        if (!SlaAlertScope.matchesOrganizationalScope(user, metrics.getBranch(), metrics.getDistrict(),
                metrics.getDepartment())) {
            return false;
        }
        String effectiveStage = activeTaskKey != null
                ? SlaAlertScope.stageCodeFromTaskKey(activeTaskKey)
                : SlaAlertScope.canonicalizeStage(metrics.getCurrentStage());
        if (effectiveStage == null) {
            effectiveStage = SlaAlertScope.canonicalizeStage(metrics.getCurrentStage());
        }
        return SlaAlertScope.roleMaySeeStage(role, effectiveStage);
    }

    public boolean isApproachingOrBreachedForActiveStage(ComplaintSlaMetrics metrics, String activeStage) {
        if (metrics == null || activeStage == null) {
            return false;
        }
        if (!SlaAlertScope.sameStage(metrics.getCurrentStage(), activeStage)) {
            return false;
        }
        return isWarningStatus(metrics.getCurrentStageStatus());
    }

    public String stageSlaStatusForTask(ComplaintSlaMetrics metrics, String taskDefinitionKey) {
        String activeStage = SlaAlertScope.stageCodeFromTaskKey(taskDefinitionKey);
        if (metrics == null || activeStage == null) {
            return STATUS_ON_TIME;
        }
        slaTrackingService.recalculateSlaStatus(metrics);
        if (!SlaAlertScope.sameStage(metrics.getCurrentStage(), activeStage)) {
            return STATUS_ON_TIME;
        }
        if (isWarningStatus(metrics.getCurrentStageStatus())) {
            return normalizeAlertStatus(metrics.getCurrentStageStatus());
        }
        return STATUS_ON_TIME;
    }

    public List<Task> retainAuthorizedTasks(List<Task> tasks, String role, User user,
            Map<String, Map<String, Object>> taskVarsCache) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        if (SlaAlertScope.isAdmin(role)) {
            return tasks;
        }
        Set<String> allowedKeys = SlaAlertScope.authorizedTaskKeys(role);
        if (allowedKeys.isEmpty()) {
            return List.of();
        }
        List<Task> scoped = new ArrayList<>();
        for (Task task : tasks) {
            if (isAuthorizedActiveTask(task, role, user, taskVarsCache)) {
                scoped.add(task);
            }
        }
        return scoped;
    }

    private boolean isAuthorizedActiveTask(Task task, String role, User user,
            Map<String, Map<String, Object>> taskVarsCache) {
        if (!SlaAlertScope.roleMayHandleTask(role, task.getTaskDefinitionKey())) {
            return false;
        }
        Map<String, Object> vars = taskVarsCache != null
                ? taskVarsCache.getOrDefault(task.getId(), Map.of())
                : Map.of();
        String branch = firstNonBlank(vars.get("branch"), metricsBranch(task.getProcessInstanceId()));
        String district = firstNonBlank(vars.get("district"), metricsDistrict(task.getProcessInstanceId()));
        String department = firstNonBlank(vars.get("department"), metricsDepartment(task.getProcessInstanceId()));
        return user == null || SlaAlertScope.matchesOrganizationalScope(user, branch, district, department);
    }

    public enum TaskAction {
        VIEW,
        CLAIM,
        COMPLETE,
        ASSIGN
    }

    public Task requireAuthorizedTask(String taskId, TaskAction action) {
        if (taskId == null || taskId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task id is required");
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        if (!canOperateOnTask(task, action)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this task");
        }
        return task;
    }

    public boolean canOperateOnTask(Task task, TaskAction action) {
        if (task == null) {
            return false;
        }
        String role = currentRole();
        if (SlaAlertScope.isAdmin(role)) {
            return true;
        }
        User user;
        try {
            user = requireCurrentUser();
        } catch (ResponseStatusException e) {
            return false;
        }
        if (!SlaAlertScope.roleMayHandleTask(role, task.getTaskDefinitionKey())) {
            return false;
        }
        Map<String, Object> vars = Map.of();
        try {
            vars = taskService.getVariables(task.getId());
        } catch (Exception ignored) {
            // org scope falls back to SLA metrics
        }
        if (!isAuthorizedActiveTask(task, role, user, Map.of(task.getId(), vars))) {
            return false;
        }
        if (action == TaskAction.ASSIGN) {
            return SlaAlertScope.canAssignTasks(role);
        }
        String assignee = task.getAssignee();
        boolean claimed = assignee != null && !assignee.isBlank()
                && !"initiator".equalsIgnoreCase(assignee)
                && !"unassigned".equalsIgnoreCase(assignee);
        if (claimed && !assignee.equalsIgnoreCase(user.getUsername()) && !SlaAlertScope.canAssignTasks(role)) {
            return false;
        }
        return true;
    }

    public List<ComplaintSlaMetrics> filterMetricsForUser(List<ComplaintSlaMetrics> metrics, String role, User user) {
        if (metrics == null || metrics.isEmpty()) {
            return List.of();
        }
        if (SlaAlertScope.isAdmin(role)) {
            return metrics;
        }
        Map<String, String> activeTaskByProcess = loadActiveTaskKeys();
        List<ComplaintSlaMetrics> result = new ArrayList<>();
        for (ComplaintSlaMetrics row : metrics) {
            String taskKey = activeTaskByProcess.get(row.getProcessInstanceId());
            if (canViewSlaRecord(role, user, row, taskKey)) {
                result.add(row);
            }
        }
        return result;
    }

    public void assertCanViewProcessSla(String processInstanceId) {
        String role = currentRole();
        if (SlaAlertScope.isAdmin(role)) {
            return;
        }
        User user = requireCurrentUser();
        Optional<ComplaintSlaMetrics> metrics = slaMetricsRepository.findByProcessInstanceId(processInstanceId);
        if (metrics.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SLA record not found");
        }
        String taskKey = resolveActiveTaskKey(processInstanceId);
        if (!canViewSlaRecord(role, user, metrics.get(), taskKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view this SLA record");
        }
    }

    public void assertCanViewComplaintSla(String complaintId) {
        String role = currentRole();
        if (SlaAlertScope.isAdmin(role)) {
            return;
        }
        User user = requireCurrentUser();
        Optional<ComplaintSlaMetrics> metrics = slaMetricsRepository.findByComplaintId(complaintId);
        if (metrics.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SLA record not found");
        }
        String taskKey = resolveActiveTaskKey(metrics.get().getProcessInstanceId());
        if (!canViewSlaRecord(role, user, metrics.get(), taskKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view this SLA record");
        }
    }

    public List<Map<String, Object>> listVisibleAlerts() {
        User user = requireCurrentUser();
        String role = currentRole();
        List<Task> tasks = taskService.createTaskQuery().list();
        List<Map<String, Object>> alerts = new ArrayList<>();
        for (Task task : tasks) {
            toAlertIfVisible(role, user, task).ifPresent(alerts::add);
        }
        return alerts;
    }

    private Optional<Map<String, Object>> toAlertIfVisible(String role, User user, Task task) {
        Optional<ComplaintSlaMetrics> metricsOpt = slaMetricsRepository
                .findByProcessInstanceId(task.getProcessInstanceId());
        if (metricsOpt.isEmpty()) {
            return Optional.empty();
        }
        ComplaintSlaMetrics metrics = metricsOpt.get();
        slaTrackingService.recalculateSlaStatus(metrics);
        if (!isStageSlaAlertVisible(role, user, task.getTaskDefinitionKey(), metrics)) {
            return Optional.empty();
        }
        return Optional.of(toAlertPayload(task, metrics));
    }

    public Map<String, Object> toAlertPayload(Task task, ComplaintSlaMetrics metrics) {
        String activeStage = SlaAlertScope.stageCodeFromTaskKey(task.getTaskDefinitionKey());
        if (activeStage == null) {
            activeStage = SlaAlertScope.canonicalizeStage(metrics.getCurrentStage());
        }
        Map<String, Object> alert = new HashMap<>();
        alert.put("id", task.getId());
        alert.put("taskId", task.getId());
        alert.put("processInstanceId", task.getProcessInstanceId());
        alert.put("definitionKey", task.getTaskDefinitionKey());
        alert.put("name", SlaAlertScope.stageLabel(activeStage));
        alert.put("currentStage", activeStage);
        alert.put("currentStageLabel", SlaAlertScope.stageLabel(activeStage));
        alert.put("slaName", SlaAlertScope.slaNameForStage(activeStage));
        alert.put("slaStatus", normalizeAlertStatus(metrics.getCurrentStageStatus()));
        alert.put("currentStageStatus", metrics.getCurrentStageStatus());
        alert.put("slaDeadline", formatDeadline(metrics.getCurrentStageDueTime()));
        alert.put("dueDate", formatDeadline(metrics.getCurrentStageDueTime()));
        alert.put("dbcTicketId", metrics.getComplaintId());
        alert.put("complaintId", metrics.getComplaintId());
        alert.put("generalTicketId", metrics.getGeneralTicketId());
        alert.put("branch", metrics.getBranch());
        alert.put("district", metrics.getDistrict());
        alert.put("department", metrics.getDepartment());
        return alert;
    }

    public String resolveActiveTaskKey(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return null;
        }
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        for (Task task : tasks) {
            if (SlaAlertScope.stageCodeFromTaskKey(task.getTaskDefinitionKey()) != null) {
                return task.getTaskDefinitionKey();
            }
        }
        return tasks.get(0).getTaskDefinitionKey();
    }

    private Map<String, String> loadActiveTaskKeys() {
        Map<String, String> map = new HashMap<>();
        for (Task task : taskService.createTaskQuery().list()) {
            map.putIfAbsent(task.getProcessInstanceId(), task.getTaskDefinitionKey());
        }
        return map;
    }

    private static boolean isWarningStatus(String status) {
        if (status == null) {
            return false;
        }
        String upper = status.toUpperCase(Locale.ROOT);
        return STATUS_APPROACHING.equals(upper) || STATUS_BREACHED.equals(upper) || STATUS_OVERDUE.equals(upper);
    }

    public static String normalizeAlertStatus(String status) {
        if (status == null) {
            return STATUS_ON_TIME;
        }
        String upper = status.toUpperCase(Locale.ROOT);
        if (STATUS_BREACHED.equals(upper) || STATUS_OVERDUE.equals(upper)) {
            return STATUS_OVERDUE;
        }
        if (STATUS_APPROACHING.equals(upper)) {
            return STATUS_APPROACHING;
        }
        return STATUS_ON_TIME;
    }

    private static String formatDeadline(LocalDateTime dueTime) {
        return dueTime != null ? dueTime.format(ISO_DT) : null;
    }

    private String metricsBranch(String processInstanceId) {
        return slaMetricsRepository.findByProcessInstanceId(processInstanceId)
                .map(ComplaintSlaMetrics::getBranch).orElse(null);
    }

    private String metricsDistrict(String processInstanceId) {
        return slaMetricsRepository.findByProcessInstanceId(processInstanceId)
                .map(ComplaintSlaMetrics::getDistrict).orElse(null);
    }

    private String metricsDepartment(String processInstanceId) {
        return slaMetricsRepository.findByProcessInstanceId(processInstanceId)
                .map(ComplaintSlaMetrics::getDepartment).orElse(null);
    }

    private static String firstNonBlank(Object first, String second) {
        if (first != null && !first.toString().isBlank()) {
            return first.toString();
        }
        return second;
    }
}
