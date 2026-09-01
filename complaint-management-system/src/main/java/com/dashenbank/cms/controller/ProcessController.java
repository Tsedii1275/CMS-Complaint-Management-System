package com.dashenbank.cms.controller;

import com.dashenbank.cms.model.AuditLog;
import com.dashenbank.cms.model.ComplaintSlaMetrics;
import com.dashenbank.cms.model.OverallComplaintStatus;
import com.dashenbank.cms.model.Customer;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.repository.CustomerRepository;
import com.dashenbank.cms.repository.UserRepository;
import com.dashenbank.cms.delegate.NotificationDelegate;
import com.dashenbank.cms.service.AttachmentService;
import com.dashenbank.cms.service.AuditService;
import com.dashenbank.cms.service.ComplainantRelatedInformationService;
import com.dashenbank.cms.service.NbeComplianceReportService;
import com.dashenbank.cms.service.NotificationService;
import com.dashenbank.cms.service.SlaTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ProcessController {

    private static final Logger log = LoggerFactory.getLogger(ProcessController.class);

    private static final String KEY_ERROR = "error";
    private static final String KEY_CUSTOMER = "customer";
    private static final String KEY_COMPLAINT = "complaint";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ACCOUNT_NUMBER = "accountNumber";
    private static final String KEY_CHANNEL = "channel";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_PREFERRED_LANGUAGE = "preferredLanguage";
    private static final String LANG_ENGLISH = "english";
    private static final String LANG_AMHARIC = "amharic";
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_VOICE_ATTACHMENT_URL = "voiceAttachmentUrl";
    private static final String KEY_VOICE_ATTACHMENT_NAME = "voiceAttachmentName";
    private static final String KEY_EVIDENCE_URL = "evidenceUrl";
    private static final String KEY_EVIDENCE_NAME = "evidenceName";
    private static final String KEY_INITIATOR = "initiator";
    private static final String KEY_NOTIFICATION_EMAIL_SENT = "notification.ticketEmailSent";
    private static final String KEY_NOTIFICATION_SMS_SENT = "notification.ticketSmsSent";
    private static final String KEY_PROCESS_INSTANCE_ID = "processInstanceId";
    private static final String KEY_SYSTEM = "system";
    private static final String KEY_MESSAGE = "message";
    private static final String ROLE_DEPARTMENT_WORKUNIT = "ROLE_DEPARTMENT_WORKUNIT";
    private static final String KEY_FORM_TASK_43 = "FormTask_43";
    private static final String KEY_FORM_TASK_48 = "FormTask_48";
    private static final String KEY_FORM_TASK_57 = "FormTask_57";
    private static final String KEY_SERVICE_TASK_65 = "ServiceTask_65";
    private static final String KEY_FORM_TASK_CHIEF_COMMITTEE = "FormTask_ChiefCommittee";
    private static final String KEY_COMMITTEE_DECISION = "committeeDecision";
    private static final String KEY_ASSIGNEE = "assignee";
    private static final String KEY_DEPARTMENT = "department";
    private static final String STATUS_ON_TIME = "ON_TIME";
    private static final String STATUS_OVERDUE = "OVERDUE";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_DEFINITION_KEY = "definitionKey";
    private static final String KEY_SLA_STATUS = "slaStatus";
    private static final String KEY_VARIABLES = "variables";
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_UNKNOWN = "unknown";
    private static final String KEY_FCR_COMMENTS = "fcrComments";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_COMPLAINT_CATEGORY = "complaintCategory";
    private static final String DIR_UPLOADS = "uploads";
    private static final String CAT_GENERAL = "General";
    private static final String CAT_CUSTOMER_SERVICE_ISSUES = "Customer Service Issues";
    private static final String KEY_GENERAL_TICKET_ID = "generalTicketId";
    private static final String KEY_COMPLAINT_ID = "complaintId";
    private static final String KEY_DBC_TICKET_ID = "dbcTicketId";
    private static final String KEY_STATUS = "status";
    private static final String KEY_CURRENT_STAGE = "currentStage";
    private static final String STAGE_CMD_SCREENING = "CMD_SCREENING";
    private static final String STAGE_INVESTIGATION = "INVESTIGATION";
    private static final String STAGE_CHIEF_EXPERIENCE_REVIEW = "CHIEF_EXPERIENCE_REVIEW";
    private static final String STAGE_WORK_UNIT_RESOLUTION = "WORK_UNIT_RESOLUTION";
    private static final String STAGE_COMPLETED = "COMPLETED";
    private static final String VAL_VERIFIED = "VERIFIED";
    private static final String KEY_VOICE_URL = "voiceUrl";
    private static final String KEY_VOICE_NAME = "voiceName";
    private static final String KEY_IS_FCR = "isFcr";
    private static final String KEY_FCR_RESOLVED = "fcrResolved";
    private static final String KEY_FCR_NOTES = "fcrNotes";
    private static final String KEY_RESOLUTION_NOTES = "resolutionNotes";
    private static final String KEY_FCR_STATUS = "fcrStatus";
    private static final String KEY_PREFERRED_CONTACT_METHOD = "preferredContactMethod";
    private static final String KEY_CLASSIFICATION = "classification";
    private static final String KEY_DECISION = "decision";
    private static final String KEY_REQUIRES_INVESTIGATION = "requiresInvestigation";
    private static final String KEY_COMPLAINT_CLASSIFICATION = "complaintClassification";
    private static final String KEY_PRIORITY_LEVEL = "priorityLevel";
    private static final String KEY_CUSTOMER_NAME = "customerName";
    private static final String KEY_IS_COMPLAINT = "isComplaint";
    private static final String KEY_DECLINE_REASON = "declineReason";
    private static final String KEY_STAGE = "stage";
    private static final String KEY_COMMITTEE_STATUS = "committeeStatus";
    private static final String KEY_RESOLUTION_ACCEPTED = "resolutionAccepted";
    private static final String KEY_TICKET_NUMBER = "ticketNumber";
    private static final String KEY_DISTRICT = "district";
    private static final String KEY_SERVICE_TYPE = "serviceType";
    private static final String KEY_COMPLAINT_MADE_ON = "complaintMadeOn";
    private static final String KEY_RECEIVED_BY = "receivedBy";
    private static final String KEY_COMPLAINT_DESCRIPTION = "complaintDescription";
    private static final String KEY_NAME = "name";
    private static final String VAL_COMPLAINT = "COMPLAINT";
    private static final String VAL_DECLINED = "DECLINED";
    private static final String VAL_OTHER = "OTHER";
    private static final String VAL_RECORDED = "RECORDED";
    private static final String VAL_RESOLVED = "RESOLVED";
    private static final String VAL_CLOSED = "CLOSED";
    private static final String VAL_ESCALATED = "ESCALATED";
    private static final String VAL_ON_TRACK = "ON_TRACK";
    private static final String VAL_REJECTED = "REJECTED";
    private static final String VAL_APPROACHING = "APPROACHING";
    private static final String VAL_REFERRED_TO_NBE = "Referred to NBE";
    private static final String ACTOR_CMD_OFFICER = "cmd-officer";
    private static final String DECISION_FURTHER_REVIEW = "further_review";
    private static final String DECISION_APPROVED = "approved";
    private static final String DECISION_REJECTED = "rejected";
    private static final String AUDIT_TASK_PREFIX = "Task '";
    private static final String LOG_OPTIONAL_SKIPPED = "Optional operation skipped: {}";
    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Africa/Addis_Ababa");

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SlaTrackingService slaTrackingService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    private ComplainantRelatedInformationService complainantRelatedInformationService;
    @Autowired
    private NbeComplianceReportService nbeComplianceReportService;
    @Autowired
    private com.dashenbank.cms.config.AppHttpProperties appHttpProperties;
    private final NotificationDelegate notificationDelegate;

    @SuppressWarnings("java:S107")
    public ProcessController(RuntimeService runtimeService,
            TaskService taskService,
            HistoryService historyService,
            NotificationService notificationService,
            AuditService auditService,
            SlaTrackingService slaTrackingService,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            ComplaintSlaMetricsRepository slaMetricsRepository,
            NotificationDelegate notificationDelegate) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.slaTrackingService = slaTrackingService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.slaMetricsRepository = slaMetricsRepository;
        this.notificationDelegate = notificationDelegate;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String firstNonBlankString(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return "";
    }

    private static String resolveFcrNotes(Map<String, Object> complaint) {
        if (complaint.get(KEY_FCR_NOTES) != null) {
            return complaint.get(KEY_FCR_NOTES).toString();
        }
        if (complaint.get(KEY_RESOLUTION_NOTES) != null) {
            return complaint.get(KEY_RESOLUTION_NOTES).toString();
        }
        return "";
    }

    private void tryDispatchRegistrationNotification(String name, String email, String phone, String ticket,
            String preferredLanguage, Map<String, Object> vars) {
        try {
            sendRegistrationNotification(name, email, phone, ticket, preferredLanguage, vars);
        } catch (Exception eNotif) {
            log.warn("Notification dispatch warning: {}", eNotif.getMessage());
        }
    }

    private void tryInsertFcrResolution(String ticket, String processInstanceId, String name, String accountNumber,
            String category, String fcrNotesStr) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO first_contact_resolutions (complaint_id, process_instance_id, customer_name, account_number, complaint_category, fcr_notes) VALUES (?, ?, ?, ?, ?, ?)",
                    ticket, processInstanceId, name, accountNumber, category, fcrNotesStr);
        } catch (Exception ex) {
            log.info("Note: Could not insert first_contact_resolutions row: {}", ex.getMessage());
        }
    }

    private void tryInitializeSlaForNewComplaint(String processInstanceId, String ticket, String category,
            String branchVal, String channel, String customerName) {
        try {
            slaTrackingService.initializeSla(processInstanceId, ticket, category, branchVal, channel, customerName);
            List<Task> initialTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId).list();
            for (Task t : initialTasks) {
                slaTrackingService.recordTaskStart(processInstanceId, ticket,
                        t.getId(), t.getTaskDefinitionKey(), t.getName(), t.getAssignee());
            }
        } catch (Exception e) {
            log.error("SLA tracking initialization failed: {}", e.getMessage());
        }
    }

    private void tryAuditComplaintCreated(String ticket, String processInstanceId, String name, String category,
            String description) {
        try {
            auditService.log(ticket, processInstanceId, null, "COMPLAINT_CREATED", KEY_CUSTOMER, "web",
                    "New complaint submitted by " + name + " via web channel.", name, category, description);
        } catch (Exception eAudit) {
            log.warn("Audit log exception: {}", eAudit.getMessage());
        }
    }

    @PostMapping("/complaints/start")
    public ResponseEntity<Map<String, Object>> startComplaint(
            @RequestBody(required = false) Map<String, Object> payload) {
        return startComplaintInternal(payload, false);
    }

    @SuppressWarnings("java:S3776")
    private ResponseEntity<Map<String, Object>> startComplaintInternal(
            Map<String, Object> payload, boolean skipNotification) {
        try {
            if (payload == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of(KEY_ERROR, "Request body is required and must be valid JSON"));
            }
            Map<String, Object> customer = castToMap(payload.get(KEY_CUSTOMER));
            Map<String, Object> complaint = castToMap(payload.get(KEY_COMPLAINT));

            if (customer.isEmpty() || complaint.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(KEY_ERROR, "customer and complaint objects are required"));
            }

            String name = (String) customer.get(KEY_NAME);
            String email = (String) customer.get(KEY_EMAIL);
            String phone = (String) customer.get(KEY_PHONE);
            String accountNumber = (String) customer.get(KEY_ACCOUNT_NUMBER);
            String channel = (String) complaint.get(KEY_CHANNEL);
            String description = (String) complaint.get(KEY_DESCRIPTION);
            String category = (String) complaint.get(KEY_CATEGORY);

            if (category == null || category.isBlank()) {
                category = CAT_CUSTOMER_SERVICE_ISSUES;
            }

            if (name == null || name.isBlank() || phone == null
                    || phone.isBlank() || channel == null || channel.isBlank() || description == null
                    || description.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(KEY_ERROR,
                                "All required fields (Name, Phone, Channel, Description) must be filled"));
            }

            if (email != null && !email.isBlank()) {
                if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                    return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "Invalid email format"));
                }
            } else {
                email = "";
            }

            String cleanPhone = phone.replaceAll("[\\s-]", "");
            if (!cleanPhone.matches("^(\\+?2510?[79]\\d{8}|0?[79]\\d{8})$")) {
                return ResponseEntity.badRequest()
                        .body(Map.of(KEY_ERROR,
                                "Invalid phone format; expected Ethiopian phone number (e.g. +2519XXXXXXXX, +2517XXXXXXXX, or 09/07XXXXXXXX)"));
            }
            phone = cleanPhone;

            Map<String, Object> customerVars = buildCustomerVariables(customer, name, email, phone, accountNumber);
            Map<String, Object> complaintVars = buildComplaintVariables(complaint, channel, description, category);

            String ticket = generateCmTicketId();

            complaintVars.put("id", ticket);
            complaintVars.put(KEY_GENERAL_TICKET_ID, ticket);
            complaintVars.put(KEY_COMPLAINT_ID, ticket);
            complaintVars.put(KEY_DBC_TICKET_ID, "");

            Map<String, Object> vars = new HashMap<>();
            vars.put(KEY_CUSTOMER, customerVars);
            vars.put(KEY_COMPLAINT, complaintVars);
            vars.put(KEY_GENERAL_TICKET_ID, ticket);
            vars.put(KEY_COMPLAINT_ID, ticket);
            vars.put(KEY_DBC_TICKET_ID, "");
            vars.put(KEY_INITIATOR, KEY_INITIATOR);
            vars.put("createdAt", LocalDateTime.now(SYSTEM_ZONE).toString());
            vars.put(KEY_CHANNEL, channel);
            vars.put(KEY_STATUS, VAL_RECORDED);
            vars.put(KEY_CURRENT_STAGE, STAGE_CMD_SCREENING);

            if (complaintVars.containsKey(KEY_VOICE_ATTACHMENT_URL)) {
                vars.put(KEY_VOICE_ATTACHMENT_URL, complaintVars.get(KEY_VOICE_ATTACHMENT_URL));
                vars.put(KEY_VOICE_URL, complaintVars.get(KEY_VOICE_ATTACHMENT_URL));
            }
            if (complaintVars.containsKey(KEY_VOICE_ATTACHMENT_NAME)) {
                vars.put(KEY_VOICE_ATTACHMENT_NAME, complaintVars.get(KEY_VOICE_ATTACHMENT_NAME));
                vars.put(KEY_VOICE_NAME, complaintVars.get(KEY_VOICE_ATTACHMENT_NAME));
            }
            if (complaintVars.containsKey(KEY_EVIDENCE_URL)) {
                vars.put(KEY_EVIDENCE_URL, complaintVars.get(KEY_EVIDENCE_URL));
            }
            if (complaintVars.containsKey(KEY_EVIDENCE_NAME)) {
                vars.put(KEY_EVIDENCE_NAME, complaintVars.get(KEY_EVIDENCE_NAME));
            }

            String preferredLanguage = (String) customer.get(KEY_PREFERRED_LANGUAGE);
            vars.put(KEY_PREFERRED_LANGUAGE,
                    preferredLanguage != null && !preferredLanguage.isBlank() ? preferredLanguage : LANG_ENGLISH);

            boolean isFcr = Boolean.TRUE.equals(complaint.get(KEY_IS_FCR))
                    || Boolean.TRUE.equals(complaint.get(KEY_FCR_RESOLVED));
            if (isFcr) {
                vars.put(KEY_IS_FCR, true);
                String fcrNotesStr = resolveFcrNotes(complaint);
                vars.put(KEY_FCR_NOTES, fcrNotesStr);
                vars.put(KEY_RESOLUTION_NOTES, fcrNotesStr);
                vars.put(KEY_FCR_STATUS, "PENDING_CCO_VERIFICATION");
            }

            if (!skipNotification) {
                tryDispatchRegistrationNotification(name, email, phone, ticket, preferredLanguage, vars);
            }

            var instance = runtimeService.startProcessInstanceByKey("cMS", vars);

            if (isFcr) {
                tryInsertFcrResolution(ticket, instance.getId(), name, (String) customer.get(KEY_ACCOUNT_NUMBER),
                        category, resolveFcrNotes(complaint));
            }

            tryInitializeSlaForNewComplaint(instance.getId(), ticket, category,
                    (String) complaintVars.get(KEY_BRANCH), channel, (String) customerVars.get(KEY_NAME));
            tryAuditComplaintCreated(ticket, instance.getId(), name, category, description);

            Map<String, Object> response = new HashMap<>();
            response.put(KEY_PROCESS_INSTANCE_ID, instance.getId());
            response.put("businessKey", instance.getBusinessKey());
            response.put("completed", instance.isEnded());
            response.put("ticketId", ticket);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error initiating complaint process: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Failed to register complaint: " + e.getMessage(),
                            KEY_MESSAGE, "Failed to register complaint: " + e.getMessage()));
        }
    }

    private Map<String, Object> buildCustomerVariables(Map<String, Object> customer, String name, String email,
            String phone, String accountNumber) {
        Map<String, Object> customerVars = new HashMap<>();
        customerVars.put(KEY_NAME, name);
        customerVars.put(KEY_EMAIL, email);
        customerVars.put(KEY_PHONE, phone);
        customerVars.put(KEY_ACCOUNT_NUMBER, accountNumber != null && !accountNumber.isBlank() ? accountNumber : "");

        String preferredContactMethod = (String) customer.get(KEY_PREFERRED_CONTACT_METHOD);
        customerVars.put(KEY_PREFERRED_CONTACT_METHOD,
                preferredContactMethod != null && !preferredContactMethod.isBlank() ? preferredContactMethod : "Email");

        String preferredLanguage = (String) customer.get(KEY_PREFERRED_LANGUAGE);
        customerVars.put(KEY_PREFERRED_LANGUAGE,
                preferredLanguage != null && !preferredLanguage.isBlank() ? preferredLanguage : LANG_ENGLISH);

        populateCrmDetails(customerVars, accountNumber);
        return customerVars;
    }

    private void populateCrmDetails(Map<String, Object> customerVars, String accountNumber) {
        Customer dbCustomer = null;
        if (accountNumber != null && !accountNumber.isBlank()) {
            try {
                dbCustomer = customerRepository.findByAccountNumber(accountNumber.trim()).orElse(null);
            } catch (Exception e) {
                log.warn("Could not query customer by account number {}: {}", accountNumber, e.getMessage());
            }
        }

        if (dbCustomer != null) {
            customerVars.put("customerSegment",
                    dbCustomer.getCustomerSegment() != null ? dbCustomer.getCustomerSegment() : "Retail");
            customerVars.put("customerSubSegment",
                    dbCustomer.getCustomerSubSegment() != null ? dbCustomer.getCustomerSubSegment() : "Standard");
            customerVars.put("riskRating", dbCustomer.getRiskRating() != null ? dbCustomer.getRiskRating() : "LOW");
            customerVars.put("isVip", dbCustomer.isVip());
            customerVars.put(KEY_NAME, dbCustomer.getName());
            customerVars.put(KEY_EMAIL, dbCustomer.getEmail());
            customerVars.put(KEY_PHONE, dbCustomer.getPhoneNumber());
        } else {
            customerVars.put("customerSegment", "Retail");
            customerVars.put("customerSubSegment", "Standard");
            customerVars.put("riskRating", "LOW");
            customerVars.put("isVip", false);
        }
    }

    private Map<String, Object> buildComplaintVariables(Map<String, Object> complaint, String channel,
            String description, String category) {
        Map<String, Object> complaintVars = new HashMap<>();
        complaintVars.put(KEY_CHANNEL, channel);
        complaintVars.put(KEY_DESCRIPTION, description);
        complaintVars.put(KEY_CATEGORY, category);
        if (complaint.get(KEY_BRANCH) != null)
            complaintVars.put(KEY_BRANCH, complaint.get(KEY_BRANCH));
        if (complaint.get(KEY_ACCOUNT_NUMBER) != null)
            complaintVars.put(KEY_ACCOUNT_NUMBER, complaint.get(KEY_ACCOUNT_NUMBER));
        if (complaint.get("date") != null)
            complaintVars.put("date", complaint.get("date"));
        Object voiceUrlObj = complaint.get(KEY_VOICE_URL);
        if (voiceUrlObj == null)
            voiceUrlObj = complaint.get(KEY_VOICE_ATTACHMENT_URL);
        if (voiceUrlObj != null) {
            complaintVars.put(KEY_VOICE_ATTACHMENT_URL, voiceUrlObj);
            complaintVars.put(KEY_VOICE_URL, voiceUrlObj);
        }

        Object voiceNameObj = complaint.get(KEY_VOICE_NAME);
        if (voiceNameObj == null)
            voiceNameObj = complaint.get(KEY_VOICE_ATTACHMENT_NAME);
        if (voiceNameObj != null) {
            complaintVars.put(KEY_VOICE_ATTACHMENT_NAME, voiceNameObj);
            complaintVars.put(KEY_VOICE_NAME, voiceNameObj);
        }

        Object evidenceUrlObj = complaint.get(KEY_EVIDENCE_URL);
        if (evidenceUrlObj != null) {
            complaintVars.put(KEY_EVIDENCE_URL, evidenceUrlObj);
        }

        Object evidenceNameObj = complaint.get(KEY_EVIDENCE_NAME);
        if (evidenceNameObj != null) {
            complaintVars.put(KEY_EVIDENCE_NAME, evidenceNameObj);
        }

        Object isFcrVal = complaint.get(KEY_IS_FCR);
        if (isFcrVal == null)
            isFcrVal = complaint.get(KEY_FCR_RESOLVED);
        if (isFcrVal != null)
            complaintVars.put(KEY_IS_FCR, isFcrVal);

        Object fcrNotesVal = complaint.get(KEY_FCR_NOTES);
        if (fcrNotesVal == null)
            fcrNotesVal = complaint.get(KEY_RESOLUTION_NOTES);
        if (fcrNotesVal != null) {
            complaintVars.put(KEY_FCR_NOTES, fcrNotesVal);
            complaintVars.put(KEY_RESOLUTION_NOTES, fcrNotesVal);
        }
        return complaintVars;
    }

    private void sendRegistrationNotification(String name, String email, String phone, String ticket,
            String preferredLanguage, Map<String, Object> vars) {
        try {
            LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE);
            String subDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String subTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            String emailMessage;
            String subject;
            if (LANG_AMHARIC.equalsIgnoreCase(preferredLanguage)) {
                subject = "የቅሬታ ምዝገባ ማረጋገጫ";
                emailMessage = String.format(
                        "ውድ %s፣%n%n" +
                                "በ %s በ %s ያቀረቡት ቅሬታ በተሳካ ሁኔታ ተመዝግቧል።%n%n" +
                                "የቲኬት ቁጥር: %s%n%n" +
                                "ቅሬታዎ በአሁኑ ጊዜ እየተገመገመ እና በሂደት ላይ ይገኛል። ከላይ ያለውን የቲኬት ቁጥር በመጠቀም ሂደቱን መከታተል ይችላሉ።%n%n" +
                                "ዳሽን ባንክን ስላነጋገሩ እናመሰግናለን።%n%n" +
                                "ዳሽን ባንክ%n" +
                                "የደንበኞች አገልግሎት ቡድን",
                        name, subDate, subTime, ticket);
            } else {
                subject = "Complaint Registration Confirmation";
                emailMessage = String.format(
                        "Dear %s,%n%n" +
                                "Your complaint submitted on %s at %s has been successfully registered.%n%n" +
                                "Ticket ID: %s%n%n" +
                                "Your complaint is currently under review and processing. You may track its progress using the ticket ID above.%n%n"
                                +
                                "Thank you for contacting Dashen Bank.%n%n" +
                                "Dashen Bank%n" +
                                "Customer Care Team",
                        name, subDate, subTime, ticket);
            }
            if (email != null && !email.isBlank()) {
                notificationService.sendEmail(email, subject, emailMessage);
            }
            if (phone != null && !phone.isBlank()) {
                notificationService.sendSms(phone, emailMessage);
            }
            vars.put(KEY_NOTIFICATION_EMAIL_SENT, true);
            vars.put(KEY_NOTIFICATION_SMS_SENT, true);
            vars.put("ack.sent", true);
        } catch (Exception e) {
            log.error("Failed to send registration notification: {}", e.getMessage());
            vars.put(KEY_NOTIFICATION_EMAIL_SENT, false);
            vars.put(KEY_NOTIFICATION_SMS_SENT, false);
        }
    }

    @PostMapping("/complaints/staff-submit")
    public ResponseEntity<Map<String, Object>> startComplaintByStaff(
            @RequestBody(required = false) Map<String, Object> payload) {
        return startComplaintInternal(payload, false);
    }

    @PostMapping("/complaints/fcr-resolve")
    public ResponseEntity<Map<String, Object>> fcrResolveComplaint(
            @RequestBody(required = false) Map<String, Object> payload) {
        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "Request body is required"));
        }
        Map<String, Object> customer = castToMap(payload.get(KEY_CUSTOMER));
        Map<String, Object> complaint = castToMap(payload.get(KEY_COMPLAINT));

        if (customer.isEmpty() || complaint.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "customer and complaint objects are required"));
        }

        String name = (String) customer.get(KEY_NAME);
        String email = (String) customer.getOrDefault(KEY_EMAIL, "");
        String channel = (String) complaint.getOrDefault(KEY_CHANNEL, KEY_BRANCH);
        String description = (String) complaint.get(KEY_DESCRIPTION);
        String category = (String) complaint.get(KEY_CATEGORY);
        String resolutionNotes = (String) complaint.getOrDefault(KEY_RESOLUTION_NOTES, "");
        String branch = (String) complaint.getOrDefault(KEY_BRANCH, "");
        String preferredLanguage = (String) customer.getOrDefault(KEY_PREFERRED_LANGUAGE, LANG_ENGLISH);

        if (name == null || name.isBlank() || description == null || description.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(KEY_ERROR, "Customer name and complaint description are required"));
        }

        String ticket = generateDbcTicketId();

        String actor = getCurrentUsername();

        String fullDescription = String.format(
                "Complaint resolved at First Contact Resolution by branch staff. Resolution Notes: %s",
                resolutionNotes.isBlank() ? "No additional notes provided." : resolutionNotes);
        auditService.log(ticket, null, null, "FCR_RESOLVED", "branch-staff", actor,
                fullDescription, name, category, description);

        auditService.log(ticket, null, null, "CASE_CLOSED", KEY_SYSTEM, KEY_SYSTEM,
                "Case closed at first contact resolution. No further escalation required.", name, category,
                description);

        try {
            slaTrackingService.initializeFcrSla(ticket, category, branch, name, channel);
        } catch (Exception e) {
            log.error("FCR SLA initialization failed: {}", e.getMessage());
        }

        if (email != null && !email.isBlank() && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            sendFcrNotification(name, email, ticket, preferredLanguage, resolutionNotes);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ticketId", ticket);
        response.put(KEY_FCR_RESOLVED, true);
        response.put(KEY_MESSAGE, "Complaint resolved at First Contact Resolution and case closed.");
        return ResponseEntity.ok(response);
    }

    private void sendFcrNotification(String name, String email, String ticket, String preferredLanguage,
            String resolutionNotes) {
        try {
            String subDate = LocalDateTime.now(SYSTEM_ZONE).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String resDate = subDate;
            String resSummary = resolutionNotes.isBlank() ? "Resolved at first contact by staff." : resolutionNotes;

            String subject;
            String emailMessage;
            if (LANG_AMHARIC.equalsIgnoreCase(preferredLanguage)) {
                subject = "የቅሬታ መፍትሄ መረጃ";
                emailMessage = String.format(
                        "ውድ %s፣%n%n" +
                                "በ %s ያቀረቡት ቅሬታ (%s) መፍትሄ አግኝቷል።%n%n" +
                                "የመፍትሄ ማጠቃለያ:%n%s%n%n" +
                                "የተፈታበት ቀን:%n%s%n%n" +
                                "የእርስዎ ተሞክሮ ለእኛ አስፈላጊ ነው።%n%n" +
                                "እባክዎን ከታች ያለውን ሊንክ በመጠቀም በአገልግሎታችን ላይ ያለዎትን እርካታ ይመዝኑ:%n%n" +
                                "%s%n%n" +
                                "አገልግሎታችንን እንድናሻሽል ስለረዱን እናመሰግናለን።%n%n" +
                                "ዳሽን ባንክ%n" +
                                "የደንበኞች አገልግሎት ቡድን",
                        name, subDate, ticket, resSummary, resDate,
                        appHttpProperties.pageUrl("/customer-feedback?token=" + UUID.randomUUID()));
            } else {
                subject = "Complaint Resolution Update";
                emailMessage = String.format(
                        "Dear %s,%n%n" +
                                "Your complaint (%s) submitted on %s has been resolved.%n%n" +
                                "Resolution:%n%s%n%n" +
                                "Resolution Date:%n%s%n%n" +
                                "Your experience matters to us.%n%n" +
                                "Please take a moment to rate your satisfaction with our service using the link below:%n%n"
                                +
                                "%s%n%n" +
                                "Thank you for helping us improve our services.%n%n" +
                                "Dashen Bank%n" +
                                "Customer Care Team",
                        name, ticket, subDate, resSummary, resDate,
                        appHttpProperties.pageUrl("/customer-feedback?token=" + UUID.randomUUID()));
            }
            if (email != null && !email.isBlank()) {
                notificationService.sendEmail(email, subject, emailMessage);
            }
        } catch (Exception e) {
            log.error("FCR notification failed: {}", e.getMessage());
        }
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return auth != null ? auth.getName() : KEY_SYSTEM;
    }

    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority();
        }
        return "ROLE_ANONYMOUS";
    }

    private Set<String> mapRoleToTaskDefinitionKeys(String role) {
        if (role == null)
            return Set.of();
        if (role.contains("AUDIT") || role.contains(STAGE_INVESTIGATION)) {
            return Set.of(KEY_FORM_TASK_48);
        }
        return switch (role) {
            case "ROLE_CONTACT_CENTER_AGENT", "ROLE_CONTACT_CENTER_SENIOR_MANAGER" ->
                Set.of("FormTask_16", "FormTask_24", "FormTask_20", "FormTask_67", "FormTask_12");
            case "ROLE_CUSTOMER_CARE_OFFICER", "ROLE_CUSTOMER_CARE_SENIOR_MANAGER", "ROLE_CUSTOMER_CARE_TEAM_LEADER",
                    "ROLE_SERVICE_QUALITY_DIRECTOR" ->
                Set.of(KEY_FORM_TASK_43, "FormTask_67", KEY_SERVICE_TASK_65);
            case "ROLE_AUDIT_INVESTIGATION_TEAM", "ROLE_OPERATIONAL_AUDIT_SENIOR_MANAGER",
                    "ROLE_OPERATIONAL_AUDIT_DIRECTOR" ->
                Set.of(KEY_FORM_TASK_48);
            case ROLE_DEPARTMENT_WORKUNIT -> Set.of(KEY_FORM_TASK_57);
            case "ROLE_CHIEF_COMMITTEE", "ROLE_COMMITTEE_SECRETARY" -> Set.of(KEY_FORM_TASK_CHIEF_COMMITTEE);
            case "ROLE_CHIEF_EXPERIENCE_OFFICER" -> Set.of("FormTask_CEX");
            default -> Set.of();
        };
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> findTasks(
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String candidateGroup) {

        List<Task> tasks;
        if (assignee != null && !assignee.isBlank()) {
            tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        } else if (candidateGroup != null && !candidateGroup.isBlank()) {
            tasks = taskService.createTaskQuery().taskCandidateGroup(candidateGroup).list();
        } else {
            tasks = taskService.createTaskQuery().list();
        }

        var result = tasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", task.getId());
            map.put(KEY_NAME, task.getName());
            map.put(KEY_ASSIGNEE, task.getAssignee());
            map.put(KEY_PROCESS_INSTANCE_ID, task.getProcessInstanceId());
            map.put(KEY_DEFINITION_KEY, task.getTaskDefinitionKey());
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/tasks/enriched")
    public ResponseEntity<List<Map<String, Object>>> findTasksEnriched(
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String candidateGroup,
            @RequestParam(required = false) String slaFilter,
            @RequestParam(required = false) String priorityFilter,
            @RequestParam(required = false) String stateFilter) {

        String role = getCurrentUserRole();
        Set<String> allowedKeys = mapRoleToTaskDefinitionKeys(role);

        var query = taskService.createTaskQuery();
        if (assignee != null && !assignee.isBlank()) {
            query = query.taskAssignee(assignee);
        } else if (candidateGroup != null && !candidateGroup.isBlank()) {
            query = query.taskCandidateGroup(candidateGroup);
        }

        List<Task> tasks = query.list();
        tasks = filterTasksByRoleAllowedKeys(tasks, role, allowedKeys, assignee, candidateGroup);

        Map<String, Map<String, Object>> taskVarsCache = new HashMap<>();
        for (Task t : tasks) {
            try {
                taskVarsCache.put(t.getId(), taskService.getVariables(t.getId()));
            } catch (Exception e) {
                taskVarsCache.put(t.getId(), Map.of());
            }
        }

        tasks = filterTasksByUserRole(tasks, role, taskVarsCache);

        List<Map<String, Object>> enriched = tasks.stream()
                .map(t -> enrichTask(t, taskVarsCache.getOrDefault(t.getId(), Map.of()))).toList();

        List<Map<String, Object>> filtered = enriched.stream()
                .filter(r -> slaFilter == null || slaFilter.isBlank()
                        || slaFilter.equalsIgnoreCase(String.valueOf(r.get(KEY_SLA_STATUS))))
                .filter(r -> priorityFilter == null || priorityFilter.isBlank()
                        || priorityFilter.equalsIgnoreCase(String.valueOf(r.get(KEY_PRIORITY))))
                .filter(r -> stateFilter == null || stateFilter.isBlank()
                        || String.valueOf(r.get("state")).equalsIgnoreCase(stateFilter))
                .toList();

        return ResponseEntity.ok(filtered);
    }

    private List<Task> filterTasksByRoleAllowedKeys(List<Task> tasks, String role, Set<String> allowedKeys,
            String assignee, String candidateGroup) {
        if (assignee != null || candidateGroup != null || allowedKeys.isEmpty() || "ROLE_ADMIN".equals(role)) {
            return tasks;
        }
        return tasks.stream()
                .filter(t -> {
                    String key = t.getTaskDefinitionKey();
                    if (role != null && (role.contains("AUDIT") || role.contains(STAGE_INVESTIGATION))) {
                        return true;
                    }
                    if (key == null) {
                        return "SecondaryResolutionReview".equals(t.getCategory()) &&
                                ROLE_DEPARTMENT_WORKUNIT.equals(role);
                    }
                    return allowedKeys.contains(key);
                })
                .toList();
    }

    private List<Task> filterTasksByUserRole(List<Task> tasks, String role,
            Map<String, Map<String, Object>> taskVarsCache) {
        String currentUsername = getCurrentUsername();
        var currentUser = userRepository.findByUsernameIgnoreCase(currentUsername).orElse(null);
        if (currentUser == null)
            return tasks;

        String uBranch = currentUser.getBranch();
        String uDept = currentUser.getDepartment();
        if (ROLE_DEPARTMENT_WORKUNIT.equals(role)) {
            return tasks.stream()
                    .filter(t -> {
                        Map<String, Object> vars = taskVarsCache.getOrDefault(t.getId(), Map.of());
                        String tBranch = (String) vars.get(KEY_BRANCH);
                        String tDept = (String) vars.get(KEY_DEPARTMENT);
                        return orgValueUnrestrictedOrMatches(tBranch, uBranch)
                                && orgValueUnrestrictedOrMatches(tDept, uDept);
                    })
                    .toList();
        }
        return tasks;
    }

    @SuppressWarnings("java:S3776")
    private Map<String, Object> enrichTask(Task task, Map<String, Object> vars) {
        Map<String, Object> customer = castToMap(vars.getOrDefault(KEY_CUSTOMER, Map.of()));
        Map<String, Object> complaint = castToMap(vars.getOrDefault(KEY_COMPLAINT, Map.of()));
        Map<String, Object> sla = castToMap(vars.getOrDefault("sla", Map.of()));

        String slaStatus = calculateEnrichedSlaStatus(task, sla);
        String customerName = customer.getOrDefault(KEY_NAME, "").toString();

        String classificationVal = (String) vars.getOrDefault(KEY_CLASSIFICATION, "INTAKE");
        boolean isClassified = VAL_COMPLAINT.equalsIgnoreCase(classificationVal)
                || VAL_DECLINED.equalsIgnoreCase(classificationVal);

        String dbcId = null;
        if (isClassified) {
            if (vars.get(KEY_DBC_TICKET_ID) != null && !vars.get(KEY_DBC_TICKET_ID).toString().isBlank()) {
                dbcId = vars.get(KEY_DBC_TICKET_ID).toString();
            } else if (complaint.get(KEY_DBC_TICKET_ID) != null && !complaint.get(KEY_DBC_TICKET_ID).toString().isBlank()) {
                dbcId = complaint.get(KEY_DBC_TICKET_ID).toString();
            } else if (vars.get(KEY_COMPLAINT_ID) != null && vars.get(KEY_COMPLAINT_ID).toString().startsWith("DBC-")) {
                dbcId = vars.get(KEY_COMPLAINT_ID).toString();
            }

            if (dbcId == null || dbcId.isBlank() || !dbcId.startsWith("DBC-")) {
                dbcId = getOrCreateDbcTicketId(vars, (String) vars.get(KEY_GENERAL_TICKET_ID), task.getProcessInstanceId());
            }
        }

        String complaintId = "";
        if (isClassified && dbcId != null && !dbcId.isBlank()) {
            complaintId = dbcId;
        } else {
            // Unclassified INTAKE stage: return CM- intake ticket ID
            complaintId = firstNonBlankString(
                    vars.get(KEY_GENERAL_TICKET_ID),
                    vars.get(KEY_COMPLAINT_ID),
                    complaint.get("id"),
                    task.getProcessInstanceId());
        }

        String priority = complaint.getOrDefault(KEY_PRIORITY, "").toString();
        if (vars.containsKey(KEY_PRIORITY_LEVEL)) {
            priority = vars.get(KEY_PRIORITY_LEVEL).toString();
        }
        String state = task.getName();
        String createdAt = task.getCreateTime() != null
                ? DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        .format(task.getCreateTime().toInstant().atZone(SYSTEM_ZONE).toLocalDateTime())
                : "";

        boolean isClaimed = task.getAssignee() != null
                && !task.getAssignee().isBlank()
                && !KEY_INITIATOR.equalsIgnoreCase(task.getAssignee().trim())
                && !"null".equalsIgnoreCase(task.getAssignee().trim())
                && !"unassigned".equalsIgnoreCase(task.getAssignee().trim());

        String effectiveAssignee = isClaimed ? task.getAssignee() : null;

        Map<String, Object> enrichedTask = new HashMap<>();
        enrichedTask.put("id", task.getId());
        enrichedTask.put(KEY_NAME, task.getName());
        enrichedTask.put(KEY_ASSIGNEE, effectiveAssignee);
        enrichedTask.put("isClaimed", isClaimed);
        enrichedTask.put("claimedBy", effectiveAssignee);
        enrichedTask.put("claimedAt", task.getCreateTime() != null ? createdAt : null);
        enrichedTask.put("candidateGroup", "");
        enrichedTask.put(KEY_PROCESS_INSTANCE_ID, task.getProcessInstanceId());
        enrichedTask.put(KEY_DEFINITION_KEY, task.getTaskDefinitionKey());
        enrichedTask.put(KEY_DBC_TICKET_ID, isClassified ? dbcId : null);
        enrichedTask.put(KEY_COMPLAINT_ID, complaintId);
        enrichedTask.put(KEY_GENERAL_TICKET_ID, vars.getOrDefault(KEY_GENERAL_TICKET_ID, complaintId));
        enrichedTask.put(KEY_CLASSIFICATION, classificationVal);
        enrichedTask.put(KEY_CUSTOMER_NAME, customerName);
        enrichedTask.put(KEY_PRIORITY, priority);
        enrichedTask.put(KEY_SLA_STATUS, slaStatus);
        String overallStatus = slaTrackingService != null
                ? slaTrackingService.resolveOverallStatus(vars, task.getProcessInstanceId(), complaintId)
                : OverallComplaintStatus.resolve(vars, false);
        enrichedTask.put("overallStatus", overallStatus);
        enrichedTask.put(KEY_STATUS, overallStatus);
        enrichedTask.put("responseSlaStatus", isClaimed ? STATUS_ON_TIME : "UNCLAIMED");
        enrichedTask.put("resolutionSlaStatus", slaStatus);
        enrichedTask.put("state", state);
        enrichedTask.put("createdAt", createdAt);
        enrichedTask.put("notification", buildNotificationStatusMap(vars));
        enrichedTask.put(KEY_VARIABLES, vars);
        return enrichedTask;
    }

    private Map<String, Object> buildNotificationStatusMap(Map<String, Object> vars) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("ticketEmailSent", vars.getOrDefault(KEY_NOTIFICATION_EMAIL_SENT, false));
        notification.put("ticketSmsSent", vars.getOrDefault(KEY_NOTIFICATION_SMS_SENT, false));
        notification.put("resolutionEmailSent", vars.getOrDefault("notification.resolutionEmailSent", false));
        notification.put("resolutionSmsSent", vars.getOrDefault("notification.resolutionSmsSent", false));
        return notification;
    }

    private String calculateEnrichedSlaStatus(Task task, Map<String, Object> sla) {
        if (task != null && task.getProcessInstanceId() != null) {
            Optional<ComplaintSlaMetrics> metricsOpt = slaMetricsRepository
                    .findByProcessInstanceId(task.getProcessInstanceId());
            if (metricsOpt.isPresent()) {
                ComplaintSlaMetrics m = metricsOpt.get();
                slaTrackingService.recalculateSlaStatus(m);
                if (Boolean.TRUE.equals(m.getBreached())
                        || "BREACHED".equalsIgnoreCase(m.getSlaStatus())
                        || "BREACHED".equalsIgnoreCase(m.getCurrentStageStatus())) {
                    return STATUS_OVERDUE;
                }
                if (VAL_APPROACHING.equalsIgnoreCase(m.getSlaStatus())
                        || VAL_APPROACHING.equalsIgnoreCase(m.getCurrentStageStatus())) {
                    return VAL_APPROACHING;
                }
                return STATUS_ON_TIME;
            }
        }
        return calculateStandardTaskSlaStatus(sla);
    }

    private String calculateStandardTaskSlaStatus(Map<String, Object> sla) {
        boolean breached = Boolean.parseBoolean(sla.getOrDefault("breached", "false").toString());
        if (breached)
            return STATUS_OVERDUE;

        String deadline = sla.getOrDefault("deadline", "").toString();
        if (!deadline.isBlank()) {
            try {
                ZonedDateTime deadlineDate = ZonedDateTime.parse(deadline);
                Duration diff = Duration.between(ZonedDateTime.now(SYSTEM_ZONE), deadlineDate);
                if (diff.isNegative())
                    return STATUS_OVERDUE;
                if (diff.toMinutes() <= 60)
                    return VAL_APPROACHING;
                return STATUS_ON_TIME;
            } catch (Exception ignored) {
                // Ignore parsing exceptions
            }
        }
        return STATUS_ON_TIME;
    }

    @GetMapping("/tasks/{taskId}/variables")
    public ResponseEntity<Map<String, Object>> getTaskVariables(@PathVariable String taskId) {
        Map<String, Object> vars = taskService.getVariables(taskId);
        return ResponseEntity.ok(vars);
    }

    @PostMapping("/tasks/{taskId}/claim")
    public ResponseEntity<Map<String, Object>> claimTask(@PathVariable String taskId) {
        String username = getCurrentUsername();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            taskService.setAssignee(taskId, username);
        } catch (Exception e) {
            try {
                taskService.claim(taskId, username);
            } catch (Exception ex) {
                log.info("Claim info: {}", ex.getMessage());
            }
        }

        String procInstId = task.getProcessInstanceId();
        Map<String, Object> vars = new HashMap<>();
        try {
            vars = taskService.getVariables(taskId);
        } catch (Exception ignored) {
            // Ignored if variables cannot be fetched
        }
        Map<String, Object> complaint = castToMap(vars.getOrDefault(KEY_COMPLAINT, Map.of()));
        String ticketId = complaint.getOrDefault("id", taskId).toString();

        com.dashenbank.cms.model.TaskTimeTracking tracking = null;
        try {
            tracking = slaTrackingService.recordTaskClaim(taskId, username, procInstId, ticketId,
                    task.getTaskDefinitionKey(), task.getName());
        } catch (Exception e) {
            log.error("Failed to record task claim: {}", e.getMessage());
        }

        try {
            auditService.log(ticketId, procInstId, taskId, "TASK_CLAIMED", "staff", username,
                    AUDIT_TASK_PREFIX + task.getName() + "' claimed by " + username + ".", "", "", "");
        } catch (Exception e) {
            log.error("Audit log failed for claim: {}", e.getMessage());
        }

        Map<String, Object> res = new HashMap<>();
        res.put(KEY_TASK_ID, taskId);
        res.put(KEY_ASSIGNEE, username);
        res.put("isClaimed", true);
        res.put("claimedBy", username);
        res.put("claimedAt", tracking != null && tracking.getClaimedAt() != null ? tracking.getClaimedAt().toString()
                : LocalDateTime.now(SYSTEM_ZONE).toString());
        if (tracking != null) {
            res.put("responseTimeMinutes", tracking.getResponseTimeMinutes());
            res.put("responseSlaStatus", tracking.getResponseSlaStatus());
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/tasks/{taskId}/assign")
    public ResponseEntity<Object> assignTask(
            @PathVariable String taskId,
            @RequestParam String targetUsername) {
        String currentManager = getCurrentUsername();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        User targetUser = userRepository.findByUsernameIgnoreCase(targetUsername.trim()).orElse(null);
        if (targetUser == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(KEY_ERROR, "Target officer '" + targetUsername + "' not found."));
        }

        try {
            taskService.setAssignee(taskId, targetUser.getUsername());
            String assignedNow = LocalDateTime.now(SYSTEM_ZONE).toString();
            taskService.setVariable(taskId, "assignedDate", assignedNow);
            taskService.setVariable(taskId, "assignedAt", assignedNow);
            taskService.setVariable(taskId, "assignedUser", targetUser.getUsername());
            taskService.setVariable(taskId, "assignedOfficerName", targetUser.getFullName());
        } catch (Exception e) {
            try {
                taskService.claim(taskId, targetUser.getUsername());
            } catch (Exception ex) {
                log.error("Task assignment failed: {}", ex.getMessage());
                return ResponseEntity.status(500).body(Map.of(KEY_ERROR, "Failed to assign task: " + ex.getMessage()));
            }
        }

        String procInstId = task.getProcessInstanceId();
        Map<String, Object> vars = new HashMap<>();
        try {
            vars = taskService.getVariables(taskId);
        } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
        Map<String, Object> complaint = castToMap(vars.getOrDefault(KEY_COMPLAINT, Map.of()));
        String ticketId = complaint.getOrDefault("id", taskId).toString();

        try {
            slaTrackingService.recordTaskClaim(taskId, targetUser.getUsername(), procInstId, ticketId,
                    task.getTaskDefinitionKey(), task.getName());
        } catch (Exception e) {
            log.error("Failed to record task assignment in SLA tracking: {}", e.getMessage());
        }

        try {
            auditService.log(ticketId, procInstId, taskId, "TASK_ASSIGNED", "manager", currentManager,
                    AUDIT_TASK_PREFIX + task.getName() + "' assigned/reassigned to " + targetUser.getFullName() + " ("
                            + targetUser.getUsername() + ") by " + currentManager + ".",
                    "", "", "");
        } catch (Exception e) {
            log.error("Failed to record audit log for task assignment: {}", e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put(KEY_MESSAGE, "Task successfully assigned to " + targetUser.getFullName());
        response.put(KEY_TASK_ID, taskId);
        response.put(KEY_ASSIGNEE, targetUser.getUsername());
        response.put("assigneeFullName", targetUser.getFullName());
        return ResponseEntity.ok(response);
    }

    @GetMapping({ "/complaints/{complaintId}/timeline", "/complaints/timeline" })
    public ResponseEntity<List<com.dashenbank.cms.model.TaskTimeTracking>> getComplaintSlaTimeline(
            @PathVariable(required = false) String complaintId,
            @RequestParam(name = KEY_COMPLAINT_ID, required = false) String queryComplaintId) {
        String targetId = complaintId != null && !complaintId.isBlank() ? complaintId : queryComplaintId;
        List<com.dashenbank.cms.model.TaskTimeTracking> timeline = slaTrackingService
                .getStageTimeline(targetId);
        return ResponseEntity.ok(timeline);
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(@PathVariable String taskId,
            @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> variables = body.containsKey(KEY_VARIABLES) && body.get(KEY_VARIABLES) instanceof Map
                    ? new HashMap<>(castToMap(body.get(KEY_VARIABLES)))
                    : new HashMap<>(body);

            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                return ResponseEntity.status(404).body(Map.of(KEY_ERROR, "Task not found", KEY_TASK_ID, taskId));
            }

            Map<String, Object> vars = taskService.getVariables(taskId);
            Map<String, Object> complaint = castToMap(vars.get(KEY_COMPLAINT));
            Map<String, Object> customer = castToMap(vars.get(KEY_CUSTOMER));

            String ticketId = complaint.getOrDefault("id", KEY_UNKNOWN).toString();
            String cat = (String) complaint.get(KEY_CATEGORY);
            String desc = (String) complaint.get(KEY_DESCRIPTION);
            String cName = (String) customer.get(KEY_NAME);
            String cEmail = (String) customer.get(KEY_EMAIL);

            executeTaskCompletion(task, ticketId, cName, cEmail, cat, desc, variables, vars, customer);

            return ResponseEntity.ok(Map.of(KEY_TASK_ID, taskId, "completed", true));

        } catch (Exception e) {
            log.error("Failed to complete task {}: ", taskId, e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(500)
                    .body(Map.of(KEY_ERROR, "Failed to complete task: " + errorMsg, KEY_TASK_ID, taskId));
        }
    }

    @SuppressWarnings({ "java:S107", "java:S3776" })
    private void executeTaskCompletion(Task task, String ticketId, String cName, String cEmail, String cat, String desc,
            Map<String, Object> variables, Map<String, Object> vars, Map<String, Object> customer) {
        logCompleteTaskAudit(task, ticketId, cName, cEmail, cat, desc, variables, vars, customer);
        tryRecordTaskCompletion(task.getId());
        Map<String, Object> processVars = cleanLargeProcessVars(variables);

        if (!processVars.containsKey(KEY_STATUS) || processVars.get(KEY_STATUS) == null) {
            processVars.put(KEY_STATUS, VAL_RECORDED);
        }
        if (!processVars.containsKey(KEY_DECISION) || processVars.get(KEY_DECISION) == null) {
            processVars.put(KEY_DECISION, "ONTRACK");
        }
        if (!processVars.containsKey(KEY_REQUIRES_INVESTIGATION) || processVars.get(KEY_REQUIRES_INVESTIGATION) == null) {
            processVars.put(KEY_REQUIRES_INVESTIGATION, false);
        }

        // Handle Customer Care Officer Classification (Complaint vs Other vs Decline
        // Complaint)
        String classification = (String) processVars.getOrDefault(KEY_CLASSIFICATION, VAL_COMPLAINT);

        if (VAL_DECLINED.equalsIgnoreCase(classification)
                || VAL_DECLINED.equalsIgnoreCase((String) processVars.get(KEY_DECISION))) {
            String priorityLevel = (String) processVars.getOrDefault(KEY_COMPLAINT_CLASSIFICATION,
                    processVars.getOrDefault(KEY_PRIORITY, CAT_GENERAL));
            if (priorityLevel == null || priorityLevel.isBlank() || VAL_DECLINED.equalsIgnoreCase(priorityLevel)) {
                priorityLevel = CAT_GENERAL;
            }

            processVars.put(KEY_CLASSIFICATION, VAL_COMPLAINT);
            processVars.put(KEY_DECISION, VAL_DECLINED);
            processVars.put(KEY_STATUS, VAL_DECLINED);
            processVars.put(KEY_IS_COMPLAINT, true);
            processVars.put("targetTab", "Declined");
            processVars.put(KEY_COMPLAINT_CLASSIFICATION, priorityLevel);

            String currentComplaintId = (String) processVars.get(KEY_COMPLAINT_ID);
            String initialIntakeId = (currentComplaintId != null && currentComplaintId.startsWith("CM-"))
                    ? currentComplaintId
                    : ticketId;
            String formalDbcId = getOrCreateDbcTicketId(processVars, initialIntakeId, task.getProcessInstanceId());

            processVars.put(KEY_GENERAL_TICKET_ID, initialIntakeId);
            processVars.put(KEY_DBC_TICKET_ID, formalDbcId);
            processVars.put(KEY_COMPLAINT_ID, formalDbcId);

            log.info("Task {} DECLINED. Assigned formal DBC complaint ID: {} (Intake ID: {})",
                    task.getId(), formalDbcId, initialIntakeId);

            String breachReason = processVars.get(KEY_DECLINE_REASON) != null
                    ? processVars.get(KEY_DECLINE_REASON).toString()
                    : "Declined";

            try {
                jdbcTemplate.update(
                        "UPDATE complaint_sla_metrics SET complaint_id = ?, dbc_ticket_id = ?, general_ticket_id = ?, status = 'DECLINED', classification = 'DECLINED', complaint_classification = ?, breach_reason = ?, resolved_at = NOW() WHERE process_instance_id = ? OR complaint_id = ? OR general_ticket_id = ?",
                        formalDbcId, formalDbcId, initialIntakeId, priorityLevel, breachReason,
                        task.getProcessInstanceId(), initialIntakeId, initialIntakeId);
            } catch (Exception e) {
                log.error("Could not update complaint_sla_metrics DECLINED complaint_id: {}", e.getMessage());
            }

            syncDeclinedMetricsEntity(task.getProcessInstanceId(), formalDbcId, initialIntakeId, priorityLevel,
                    breachReason);

            try {
                jdbcTemplate.update(
                        "UPDATE complaints SET ticket_number = ?, general_ticket_id = ?, classification = 'DECLINED', complaint_classification = ?, status = 'DECLINED' WHERE ticket_number = ? OR general_ticket_id = ?",
                        formalDbcId, initialIntakeId, priorityLevel, initialIntakeId, initialIntakeId);
            } catch (Exception e) {
                log.error("Could not update complaints table DECLINED: {}", e.getMessage());
            }

            try {
                jdbcTemplate.update(
                        "UPDATE complainant_related_information SET unique_id_no = ?, case_status = 'DECLINED' WHERE unique_id_no = ? OR unique_id_no = ?",
                        formalDbcId, initialIntakeId, formalDbcId);
            } catch (Exception e) {
                log.error("Could not update complainant_related_information DECLINED: {}", e.getMessage());
            }

            try {
                if (complainantRelatedInformationService != null) {
                    Optional<ComplaintSlaMetrics> mOpt = slaMetricsRepository
                            .findByProcessInstanceId(task.getProcessInstanceId());
                    if (mOpt.isPresent()) {
                        complainantRelatedInformationService.syncFromSlaMetrics(mOpt.get());
                    }
                }
            } catch (Exception e) {
                log.error("Could not sync CRI for declined complaint: {}", e.getMessage());
            }

            try {
                jdbcTemplate.update(
                        "UPDATE audit_log SET complaint_id = ? WHERE process_instance_id = ? OR complaint_id = ? OR general_ticket_id = ?",
                        formalDbcId, task.getProcessInstanceId(), initialIntakeId, initialIntakeId);
            } catch (Exception e) {
                log.error("Could not update audit_log DECLINED: {}", e.getMessage());
            }

            String declineReason = (String) processVars.getOrDefault(KEY_DECLINE_REASON, "No decline reason provided.");
            String username = getCurrentUsername();

            log.info("Task {} DECLINED by officer {}. Reason: {}", task.getId(), username, declineReason);
            auditService.log(formalDbcId, task.getProcessInstanceId(), task.getId(), "COMPLAINT_DECLINED", "officer",
                    getCurrentUsername(), "Complaint declined: " + declineReason, "", "", "");

            // Persist the declined complaint record into the `complaints` database table
            // with formal DBC ID
            upsertComplaintsTableRecord(task, processVars, formalDbcId);
        } else if (VAL_OTHER.equalsIgnoreCase(classification)) {
            finalizeOtherClassification(task, ticketId, processVars);
            return;
        } else {
            processVars.put(KEY_CLASSIFICATION, VAL_COMPLAINT);
            processVars.put(KEY_IS_COMPLAINT, true);
            String currentComplaintId = (String) processVars.get(KEY_COMPLAINT_ID);
            String initialIntakeId = (currentComplaintId != null && currentComplaintId.startsWith("CM-"))
                    ? currentComplaintId
                    : ticketId;
            String formalDbc = getOrCreateDbcTicketId(processVars, initialIntakeId, task.getProcessInstanceId());

            processVars.put(KEY_GENERAL_TICKET_ID, initialIntakeId);
            processVars.put(KEY_DBC_TICKET_ID, formalDbc);
            processVars.put(KEY_COMPLAINT_ID, formalDbc);
            log.info("Task {} classified as COMPLAINT. Authoritative DBC complaint ID: {} (General Intake ID: {})",
                    task.getId(), formalDbc, initialIntakeId);

            String selectedPriority = (String) processVars.getOrDefault(KEY_COMPLAINT_CLASSIFICATION,
                    processVars.getOrDefault(KEY_PRIORITY_LEVEL, processVars.getOrDefault(KEY_PRIORITY, CAT_GENERAL)));
            if (selectedPriority == null || selectedPriority.isBlank()
                    || VAL_DECLINED.equalsIgnoreCase(selectedPriority)) {
                selectedPriority = CAT_GENERAL;
            }
            if ("High Sensitive".equalsIgnoreCase(selectedPriority)) {
                selectedPriority = "Highly Sensitive";
            }

            try {
                jdbcTemplate.update(
                        "UPDATE complaint_sla_metrics SET complaint_id = ?, dbc_ticket_id = ?, general_ticket_id = ?, priority = ?, complaint_classification = ?, classification = 'COMPLAINT' WHERE complaint_id = ? OR general_ticket_id = ? OR process_instance_id = ?",
                        formalDbc, formalDbc, initialIntakeId, selectedPriority, selectedPriority, initialIntakeId,
                        initialIntakeId, task.getProcessInstanceId());
                // JDBC bypasses the persistence context. recordTaskCompletion already loaded
                // the INTAKE/CM- snapshot; overlay DBC + COMPLAINT before any later save.
                syncClassifiedComplaintMetricsEntity(task.getProcessInstanceId(), formalDbc, initialIntakeId,
                        selectedPriority);
                slaTrackingService.updateComplaintPriority(task.getProcessInstanceId(), selectedPriority);
            } catch (Exception e) {
                log.info("Note: Could not update complaint_sla_metrics complaint_id: {}", e.getMessage());
            }

            try {
                if (complainantRelatedInformationService != null) {
                    slaMetricsRepository.findByProcessInstanceId(task.getProcessInstanceId())
                            .ifPresent(complainantRelatedInformationService::syncFromSlaMetrics);
                }
            } catch (Exception e) {
                log.error("Could not sync CRI for classified complaint: {}", e.getMessage());
            }

            // Upsert into `complaints` database table
            upsertComplaintsTableRecord(task, processVars, ticketId);
        }

        if (processVars.containsKey("fcrAction")) {
            String fcrAction = (String) processVars.get("fcrAction");
            if (!processVars.containsKey(KEY_REQUIRES_INVESTIGATION)) {
                processVars.put(KEY_REQUIRES_INVESTIGATION, false);
            }
            if ("approve".equalsIgnoreCase(fcrAction)) {
                processVars.put(KEY_FCR_STATUS, VAL_VERIFIED);
                processVars.put(KEY_CURRENT_STAGE, VAL_RESOLVED);
                processVars.put(KEY_STATUS, VAL_RESOLVED);
                processVars.put(KEY_CLASSIFICATION, VAL_COMPLAINT);
                processVars.put(KEY_IS_COMPLAINT, true);

                String currentComplaintId = (String) processVars.get(KEY_COMPLAINT_ID);
                String dbcId = (String) processVars.get(KEY_DBC_TICKET_ID);
                String formalDbcId = (dbcId != null && dbcId.startsWith("DBC-")) ? dbcId : currentComplaintId;

                if (formalDbcId == null || !formalDbcId.startsWith("DBC-")) {
                    formalDbcId = generateDbcTicketId();
                    String initialIntakeId = (currentComplaintId != null && !currentComplaintId.isBlank())
                            ? currentComplaintId
                            : ticketId;
                    processVars.put(KEY_GENERAL_TICKET_ID, initialIntakeId);
                    processVars.put(KEY_DBC_TICKET_ID, formalDbcId);
                    processVars.put(KEY_COMPLAINT_ID, formalDbcId);
                    log.info("FCR Task {} approved by CCO. Generated formal DBC complaint ID: {} (Intake ID: {})",
                            task.getId(), formalDbcId, initialIntakeId);
                    try {
                        jdbcTemplate.update(
                                "UPDATE complaint_sla_metrics SET complaint_id = ?, general_ticket_id = ?, classification = 'COMPLAINT' WHERE complaint_id = ? OR process_instance_id = ?",
                                formalDbcId, initialIntakeId, initialIntakeId, task.getProcessInstanceId());
                        jdbcTemplate.update(
                                "UPDATE first_contact_resolutions SET complaint_id = ? WHERE process_instance_id = ? OR complaint_id = ?",
                                formalDbcId, task.getProcessInstanceId(), initialIntakeId);
                    } catch (Exception e) {
                        log.info("Note: Could not update complaint_sla_metrics FCR complaint_id: {}", e.getMessage());
                    }
                }

                // Upsert into `complaints` database table
                upsertComplaintsTableRecord(task, processVars, ticketId);

                try {
                    jdbcTemplate.update(
                            "UPDATE complaint_sla_metrics SET status = 'RESOLVED', fcr_status = true, current_stage = 'RESOLVED', resolved_at = NOW() WHERE process_instance_id = ? OR complaint_id = ?",
                            task.getProcessInstanceId(), formalDbcId);
                    jdbcTemplate.update(
                            "UPDATE complaints SET status = 'RESOLVED', resolved_at = NOW() WHERE process_instance_id = ? OR ticket_number = ?",
                            task.getProcessInstanceId(), formalDbcId);
                    jdbcTemplate.update(
                            "UPDATE complainant_related_information SET case_status = 'RESOLVED' WHERE process_instance_id = ? OR unique_id_no = ?",
                            task.getProcessInstanceId(), formalDbcId);
                    overlayFcrResolvedMetricsEntity(task.getProcessInstanceId(), formalDbcId);
                    slaTrackingService.markFcrResolved(task.getProcessInstanceId(), formalDbcId);
                    auditService.log(formalDbcId, task.getProcessInstanceId(), task.getId(), "FCR_VERIFIED",
                            ACTOR_CMD_OFFICER, getCurrentUsername(),
                            "Customer care officer confirmed case as FCR resolved.",
                            "", "", "");
                } catch (Exception e) {
                    log.info("Could not update FCR status in SLA metrics: {}", e.getMessage());
                }
            } else if ("reject".equalsIgnoreCase(fcrAction)) {
                processVars.put(KEY_FCR_STATUS, VAL_REJECTED);
                processVars.put(KEY_IS_FCR, false);
                processVars.put("isFCR", false);
                processVars.put(KEY_CURRENT_STAGE, STAGE_CMD_SCREENING);
                processVars.put(KEY_INITIATOR, getCurrentUsername());
                try {
                    jdbcTemplate.update(
                            "UPDATE complaint_sla_metrics SET fcr_status = false, current_stage = 'CMD_SCREENING' WHERE process_instance_id = ? OR complaint_id = ?",
                            task.getProcessInstanceId(), ticketId);
                    auditService.log(ticketId, task.getProcessInstanceId(), task.getId(), "FCR_REJECTED", ACTOR_CMD_OFFICER,
                            getCurrentUsername(),
                            "Customer care officer rejected FCR resolution and routed case to work unit.",
                            "", "", "");
                } catch (Exception e) {
                    log.info("Could not update FCR status in SLA metrics: {}", e.getMessage());
                }
            }
        }

        boolean isAuditEscalationPayload = "CHIEF_OPERATION_AUDIT"
                .equalsIgnoreCase(String.valueOf(processVars.get(KEY_CURRENT_STAGE)))
                || "AUDIT_INVESTIGATION".equalsIgnoreCase(String.valueOf(processVars.get(KEY_STAGE)))
                || processVars.containsKey("escalatedBy")
                || processVars.containsKey("cxRemarks");

        if (Boolean.TRUE.equals(processVars.get(KEY_REQUIRES_INVESTIGATION))) {
            boolean isCexOrAuditAction = isAuditEscalationPayload
                    || KEY_FORM_TASK_CHIEF_COMMITTEE.equals(task.getTaskDefinitionKey());
            if (!isCexOrAuditAction) {
                processVars.put(KEY_CURRENT_STAGE, STAGE_CHIEF_EXPERIENCE_REVIEW);
                processVars.put(KEY_STAGE, STAGE_CHIEF_EXPERIENCE_REVIEW);
                try {
                    jdbcTemplate.update(
                            "UPDATE complaint_sla_metrics SET current_stage = 'CHIEF_EXPERIENCE_REVIEW', requires_investigation = true WHERE process_instance_id = ? OR complaint_id = ?",
                            task.getProcessInstanceId(), ticketId);
                    overlaySlaStage(task.getProcessInstanceId(), STAGE_CHIEF_EXPERIENCE_REVIEW, true, VAL_ESCALATED);
                } catch (Exception e) {
                    log.info("Could not update SLA metrics for CEX review stage: {}", e.getMessage());
                }
            }
        }

        if (KEY_FORM_TASK_CHIEF_COMMITTEE.equals(task.getTaskDefinitionKey()) || isAuditEscalationPayload) {
            String decision = (String) processVars.get(KEY_COMMITTEE_DECISION);
            if (decision == null || decision.isBlank()) {
                decision = isAuditEscalationPayload ? DECISION_FURTHER_REVIEW : DECISION_APPROVED;
            } else {
                decision = decision.trim().toLowerCase();
            }
            if (isAuditEscalationPayload || DECISION_FURTHER_REVIEW.equals(decision) || "refer_audit".equals(decision)
                    || "escalate_audit".equals(decision) || "audit".equals(decision) || "investigation".equals(decision)
                    || decision.contains("audit") || decision.contains("review") || decision.contains("investigat")) {
                processVars.put(KEY_COMMITTEE_DECISION, DECISION_FURTHER_REVIEW);
                processVars.put(KEY_CURRENT_STAGE, "CHIEF_OPERATION_AUDIT");
                processVars.put(KEY_STAGE, "AUDIT_INVESTIGATION");
                processVars.put(KEY_COMMITTEE_STATUS, "FURTHER_REVIEW");
                processVars.put(KEY_REQUIRES_INVESTIGATION, true);
                try {
                    jdbcTemplate.update(
                            "UPDATE complaint_sla_metrics SET current_stage = 'CHIEF_OPERATION_AUDIT', requires_investigation = true WHERE process_instance_id = ? OR complaint_id = ?",
                            task.getProcessInstanceId(), ticketId);
                    slaTrackingService.advanceToStage(task.getProcessInstanceId(), STAGE_INVESTIGATION, true, null);
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
            } else if (DECISION_APPROVED.equals(decision) || "accepted".equals(decision) || "accept".equals(decision)) {
                processVars.put(KEY_COMMITTEE_DECISION, DECISION_APPROVED);
                processVars.put(KEY_CURRENT_STAGE, "COMMITTEE_ACCEPTED");
                processVars.put(KEY_COMMITTEE_STATUS, "ACCEPTED");
                processVars.put("isCommitteeAccepted", true);
                processVars.put(KEY_REQUIRES_INVESTIGATION, false);
                processVars.put(KEY_STATUS, VAL_RESOLVED);
                processVars.put(KEY_DECISION, VAL_RESOLVED);
                try {
                    jdbcTemplate.update(
                            "UPDATE complaint_sla_metrics SET current_stage = 'COMMITTEE_ACCEPTED', status = 'ESCALATED', resolved_at = NOW() WHERE process_instance_id = ? OR complaint_id = ?",
                            task.getProcessInstanceId(), ticketId);
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
                try {
                    jdbcTemplate.update(
                            "UPDATE complaints SET status = 'ESCALATED' WHERE process_instance_id = ? OR ticket_number = ?",
                            task.getProcessInstanceId(), ticketId);
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
                try {
                    jdbcTemplate.update(
                            "UPDATE complainant_related_information SET case_status = 'ESCALATED' WHERE process_instance_id = ? OR unique_id_no = ?",
                            task.getProcessInstanceId(), ticketId);
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
            } else if (DECISION_REJECTED.equals(decision)) {
                processVars.put(KEY_COMMITTEE_DECISION, DECISION_REJECTED);
                processVars.put(KEY_CURRENT_STAGE, "COMMITTEE_REJECTED");
                processVars.put(KEY_COMMITTEE_STATUS, VAL_REJECTED);
                processVars.put("isCommitteeRejected", true);
                processVars.put(KEY_REQUIRES_INVESTIGATION, false);
                processVars.put(KEY_STATUS, VAL_RESOLVED);
                processVars.put(KEY_DECISION, VAL_RESOLVED);
                try {
                    jdbcTemplate.update(
                            "UPDATE complaint_sla_metrics SET current_stage = 'COMMITTEE_REJECTED', status = 'ESCALATED', resolved_at = NOW() WHERE process_instance_id = ? OR complaint_id = ?",
                            task.getProcessInstanceId(), ticketId);
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
                try {
                    jdbcTemplate.update(
                            "UPDATE complaints SET status = 'ESCALATED' WHERE process_instance_id = ? OR ticket_number = ?",
                            task.getProcessInstanceId(), ticketId);
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
                try {
                    jdbcTemplate.update(
                            "UPDATE complainant_related_information SET case_status = 'ESCALATED' WHERE process_instance_id = ? OR unique_id_no = ?",
                            task.getProcessInstanceId(), ticketId);
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
            }
        }

        if ("Secondary Resolution Review".equals(task.getName())
                || "SecondaryResolutionReview".equals(task.getCategory())) {
            trySyncSecondaryResolutionTask(task.getProcessInstanceId(), variables);
        }

        if (!processVars.containsKey(KEY_REQUIRES_INVESTIGATION)) {
            processVars.put(KEY_REQUIRES_INVESTIGATION, false);
        }
        if (!processVars.containsKey(KEY_RESOLUTION_ACCEPTED) || processVars.get(KEY_RESOLUTION_ACCEPTED) == null) {
            processVars.put(KEY_RESOLUTION_ACCEPTED, false);
        }
        if (!processVars.containsKey(KEY_STAGE) || processVars.get(KEY_STAGE) == null
                || processVars.get(KEY_STAGE).toString().isBlank()) {
            processVars.put(KEY_STAGE, STAGE_CMD_SCREENING);
        }

        applyWorkUnitResolutionStage(task, processVars);
        applyCompletedTaskStageSla(task, processVars);

        try {
            taskService.complete(task.getId(), processVars);
        } catch (Exception flowException) {
            if (KEY_FORM_TASK_CHIEF_COMMITTEE.equals(task.getTaskDefinitionKey()) && flowException.getMessage() != null
                    && flowException.getMessage().contains("No outgoing sequence flow")) {
                log.warn("Gateway fallback for ChiefCommittee task {}: {}", task.getId(), flowException.getMessage());
                processVars.put(KEY_COMMITTEE_DECISION, DECISION_APPROVED);
                taskService.complete(task.getId(), processVars);
            } else {
                throw flowException;
            }
        }
        String processInstanceId = task.getProcessInstanceId();

        recordStartForNewTasks(processInstanceId, ticketId);

        boolean isEnded = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).finished().count() > 0;

        boolean isClosed = isEnded || VAL_CLOSED.equalsIgnoreCase((String) processVars.get(KEY_STATUS))
                || VAL_CLOSED.equalsIgnoreCase((String) processVars.get(KEY_CURRENT_STAGE))
                || VAL_RESOLVED.equalsIgnoreCase((String) processVars.get(KEY_DECISION))
                || Boolean.TRUE.equals(processVars.get(KEY_RESOLUTION_ACCEPTED))
                || "FCR_APPROVED".equalsIgnoreCase(String.valueOf(processVars.get(KEY_DECISION)))
                || VAL_VERIFIED.equalsIgnoreCase(String.valueOf(processVars.get(KEY_FCR_STATUS)));

        if (isClosed) {
            String dbcOrIntake = String.valueOf(processVars.getOrDefault(KEY_DBC_TICKET_ID,
                    processVars.getOrDefault(KEY_COMPLAINT_ID, ticketId)));
            tryLogCaseClosedAudit(dbcOrIntake, processInstanceId);
            tryMarkSlaResolved(processInstanceId);
        }

        if (isCustomerCareOfficerFinalClosure(processVars)) {
            trySendClosureNotification(processVars, customer, ticketId, processInstanceId);
        }

        tryPersistOverallStatus(processVars, processInstanceId, ticketId);
    }

    /**
     * The declined writes above go through jdbcTemplate, which bypasses the JPA
     * session. Since one session spans the whole request, a ComplaintSlaMetrics
     * instance loaded earlier still holds the pre-decline CM ticket, and the
     * markResolved() save at the end of the request would flush that stale copy
     * back over the DBC ticket. Applying the same values to the managed entity
     * keeps both views in agreement so the DBC ticket is what every screen reads.
     */
    private void syncDeclinedMetricsEntity(String processInstanceId, String formalDbcId, String initialIntakeId,
            String priorityLevel, String breachReason) {
        try {
            slaMetricsRepository.findByProcessInstanceId(processInstanceId).ifPresent(metrics -> {
                metrics.setComplaintId(formalDbcId);
                metrics.setDbcTicketId(formalDbcId);
                metrics.setGeneralTicketId(initialIntakeId);
                metrics.setStatus(VAL_DECLINED);
                metrics.setClassification(VAL_DECLINED);
                metrics.setPriority(priorityLevel);
                metrics.setBreachReason(breachReason);
                slaMetricsRepository.save(metrics);
            });
        } catch (Exception e) {
            log.error("Could not sync declined SLA metrics entity for {}: {}", processInstanceId, e.getMessage());
        }
    }

    private void overlayFcrResolvedMetricsEntity(String processInstanceId, String formalDbcId) {
        try {
            slaMetricsRepository.findByProcessInstanceId(processInstanceId).ifPresent(metrics -> {
                if (formalDbcId != null && !formalDbcId.isBlank()) {
                    metrics.setComplaintId(formalDbcId);
                    metrics.setDbcTicketId(formalDbcId);
                }
                metrics.setStatus(VAL_RESOLVED);
                metrics.setOverallStatus(VAL_RESOLVED);
                metrics.setCurrentStage(VAL_RESOLVED);
                metrics.setFcrStatus(true);
                metrics.setClassification(VAL_COMPLAINT);
                if (metrics.getResolvedAt() == null) {
                    metrics.setResolvedAt(LocalDateTime.now(SYSTEM_ZONE));
                }
                slaMetricsRepository.save(metrics);
            });
        } catch (Exception e) {
            log.error("Could not sync FCR resolved SLA metrics entity for {}: {}", processInstanceId, e.getMessage());
        }
    }

    /**
     * JDBC classification updates bypass JPA. Overlay the managed SLA row so a later
     * save cannot restore INTAKE / CM- and drop the complaint from reporting.
     */
    private void syncClassifiedComplaintMetricsEntity(String processInstanceId, String formalDbcId,
            String initialIntakeId, String priorityLevel) {
        try {
            slaMetricsRepository.findByProcessInstanceId(processInstanceId).ifPresent(metrics -> {
                metrics.setComplaintId(formalDbcId);
                metrics.setDbcTicketId(formalDbcId);
                if (initialIntakeId != null && !initialIntakeId.isBlank()) {
                    metrics.setGeneralTicketId(initialIntakeId);
                }
                metrics.setClassification(VAL_COMPLAINT);
                if (priorityLevel != null && !priorityLevel.isBlank()) {
                    metrics.setPriority(priorityLevel);
                }
                slaMetricsRepository.save(metrics);
            });
        } catch (Exception e) {
            log.error("Could not sync classified SLA metrics entity for {}: {}", processInstanceId, e.getMessage());
        }
    }

    private void overlaySlaStage(String processInstanceId, String stage, Boolean requiresInvestigation,
            String overallStatus) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return;
        }
        slaTrackingService.advanceToStage(processInstanceId, stage, requiresInvestigation, null);
        if (overallStatus != null && !overallStatus.isBlank()) {
            slaMetricsRepository.findByProcessInstanceId(processInstanceId).ifPresent(metrics -> {
                if (!VAL_RESOLVED.equalsIgnoreCase(metrics.getStatus())
                        && !VAL_CLOSED.equalsIgnoreCase(metrics.getStatus())
                        && !VAL_DECLINED.equalsIgnoreCase(metrics.getStatus())) {
                    metrics.setStatus(overallStatus);
                    slaMetricsRepository.save(metrics);
                }
            });
        }
    }

    private void applyCompletedTaskStageSla(Task task, Map<String, Object> processVars) {
        if (task == null) {
            return;
        }
        String defKey = task.getTaskDefinitionKey();
        String processInstanceId = task.getProcessInstanceId();
        String investigationType = processVars != null
                ? String.valueOf(processVars.getOrDefault("investigationType",
                        processVars.getOrDefault("investigationCategory", "")))
                : "";
        if (investigationType.isBlank() || "null".equalsIgnoreCase(investigationType)) {
            investigationType = null;
        }
        try {
            if ("FormTask_CEX".equals(defKey)) {
                slaTrackingService.advanceToStage(processInstanceId, STAGE_INVESTIGATION, true, investigationType);
            } else if (KEY_FORM_TASK_48.equals(defKey)) {
                slaTrackingService.advanceToStage(processInstanceId, "COMMITTEE_REVIEW", true, investigationType);
            } else if (KEY_FORM_TASK_57.equals(defKey)) {
                slaTrackingService.advanceToStage(processInstanceId, "SERVICE_QUALITY_REVIEW", false, null);
            } else if (KEY_SERVICE_TASK_65.equals(defKey) || "ServiceTask_62".equals(defKey)) {
                slaTrackingService.advanceToStage(processInstanceId, "NOTIFICATION", false, null);
            }
        } catch (Exception e) {
            log.info("Could not advance SLA stage after completing {}: {}", defKey, e.getMessage());
        }
    }

    /**
     * OTHER is a non-complaint item: it is recorded and listed under the Contact
     * Center "Other" tab but must never enter the complaint lifecycle. The task is
     * therefore not completed into the screening gateway (which would route it to
     * department assignment and work unit resolution); the runtime instance is
     * ended instead, while Flowable history is preserved.
     */
    private void finalizeOtherClassification(Task task, String ticketId, Map<String, Object> processVars) {
        String processInstanceId = task.getProcessInstanceId();

        processVars.put(KEY_CLASSIFICATION, VAL_OTHER);
        processVars.put(KEY_STATUS, VAL_OTHER);
        processVars.put(KEY_DECISION, VAL_OTHER);
        processVars.put(KEY_IS_COMPLAINT, false);
        processVars.put("targetTab", "Other");
        processVars.put(KEY_REQUIRES_INVESTIGATION, false);
        processVars.put(KEY_CURRENT_STAGE, STAGE_COMPLETED);
        processVars.put(KEY_STAGE, STAGE_COMPLETED);

        log.info("Task {} classified as OTHER (non-complaint). Retaining intake ticket {}; no DBC ticket issued.",
                task.getId(), ticketId);

        try {
            runtimeService.setVariables(processInstanceId, processVars);
        } catch (Exception e) {
            log.info("Note: Could not persist OTHER process variables: {}", e.getMessage());
        }

        try {
            jdbcTemplate.update(
                    "UPDATE complaint_sla_metrics SET status = 'OTHER', classification = 'OTHER', current_stage = 'COMPLETED' WHERE complaint_id = ? OR general_ticket_id = ? OR process_instance_id = ?",
                    ticketId, ticketId, processInstanceId);
        } catch (Exception e) {
            log.info("Note: Could not update complaint_sla_metrics OTHER status: {}", e.getMessage());
        }

        try {
            jdbcTemplate.update(
                    "UPDATE complaints SET status = 'OTHER', classification = 'OTHER' WHERE general_ticket_id = ? OR ticket_number = ?",
                    ticketId, ticketId);
        } catch (Exception e) {
            log.info("Note: Could not update complaints table OTHER: {}", e.getMessage());
        }

        try {
            jdbcTemplate.update(
                    "UPDATE complainant_related_information SET case_status = 'OTHER' WHERE unique_id_no = ?",
                    ticketId);
        } catch (Exception e) {
            log.info("Note: Could not update complainant_related_information OTHER: {}", e.getMessage());
        }

        syncOtherMetricsEntity(processInstanceId, ticketId);

        auditService.log(ticketId, processInstanceId, task.getId(), "CLASSIFIED_AS_OTHER", ACTOR_CMD_OFFICER,
                getCurrentUsername(),
                "Screened as OTHER (non-complaint). No DBC ticket issued; item routed to the Contact Center Other tab and the workflow ended at screening. Remarks: "
                        + resolveOtherRemarks(processVars),
                "", "", "");

        try {
            runtimeService.deleteProcessInstance(processInstanceId,
                    "Classified as OTHER - non-complaint routed to Contact Center Other");
        } catch (Exception e) {
            log.error("Could not end process instance {} after OTHER classification: {}", processInstanceId,
                    e.getMessage());
        }
    }

    private void syncOtherMetricsEntity(String processInstanceId, String ticketId) {
        try {
            slaMetricsRepository.findByProcessInstanceId(processInstanceId).ifPresent(metrics -> {
                metrics.setClassification(VAL_OTHER);
                metrics.setStatus(VAL_OTHER);
                metrics.setCurrentStage(STAGE_COMPLETED);
                if (ticketId != null && !ticketId.isBlank()) {
                    metrics.setComplaintId(ticketId);
                    if (ticketId.startsWith("CM-")) {
                        metrics.setGeneralTicketId(ticketId);
                    }
                }
                slaMetricsRepository.save(metrics);
            });
        } catch (Exception e) {
            log.error("Could not sync OTHER SLA metrics entity for {}: {}", processInstanceId, e.getMessage());
        }
    }

    private String resolveOtherRemarks(Map<String, Object> processVars) {
        Object notesVar = processVars.get(KEY_NOTES);
        if (notesVar != null && !notesVar.toString().isBlank()) {
            return notesVar.toString();
        }
        Object remarksVar = processVars.get("additionalRemarks");
        if (remarksVar != null && !remarksVar.toString().isBlank()) {
            return remarksVar.toString();
        }
        return "N/A";
    }

    private void applyWorkUnitResolutionStage(Task task, Map<String, Object> processVars) {
        if (task == null || processVars == null || !KEY_FORM_TASK_43.equals(task.getTaskDefinitionKey())) {
            return;
        }
        if (Boolean.TRUE.equals(processVars.get(KEY_REQUIRES_INVESTIGATION))) {
            return;
        }
        if (Boolean.TRUE.equals(processVars.get(KEY_RESOLUTION_ACCEPTED))) {
            return;
        }
        String classification = String.valueOf(processVars.getOrDefault(KEY_CLASSIFICATION, ""));
        String status = String.valueOf(processVars.getOrDefault(KEY_STATUS, ""));
        String decision = String.valueOf(processVars.getOrDefault(KEY_DECISION, ""));
        if (!VAL_COMPLAINT.equalsIgnoreCase(classification)) {
            return;
        }
        if (VAL_DECLINED.equalsIgnoreCase(status) || VAL_DECLINED.equalsIgnoreCase(decision)
                || VAL_OTHER.equalsIgnoreCase(status) || VAL_CLOSED.equalsIgnoreCase(status)
                || VAL_CLOSED.equalsIgnoreCase(decision) || VAL_RESOLVED.equalsIgnoreCase(decision)) {
            return;
        }
        processVars.put(KEY_STAGE, STAGE_WORK_UNIT_RESOLUTION);
        processVars.put(KEY_CURRENT_STAGE, STAGE_WORK_UNIT_RESOLUTION);
        try {
            jdbcTemplate.update(
                    "UPDATE complaint_sla_metrics SET current_stage = 'WORK_UNIT_RESOLUTION' WHERE process_instance_id = ?",
                    task.getProcessInstanceId());
            overlaySlaStage(task.getProcessInstanceId(), STAGE_WORK_UNIT_RESOLUTION, false, VAL_ON_TRACK);
        } catch (Exception e) {
            log.info("Could not update SLA metrics for work-unit stage: {}", e.getMessage());
        }
    }

    /**
     * Blank task or user org values mean "no restriction" for that dimension.
     * CMD often sends only branch or only department, never both.
     */
    private boolean orgValueUnrestrictedOrMatches(String taskValue, String userValue) {
        if (taskValue == null || taskValue.isBlank() || userValue == null || userValue.isBlank()) {
            return true;
        }
        return userValue.equalsIgnoreCase(taskValue);
    }

    private boolean isCustomerCareOfficerFinalClosure(Map<String, Object> processVars) {
        if (processVars == null) {
            return false;
        }
        return Boolean.TRUE.equals(processVars.get(KEY_RESOLUTION_ACCEPTED))
                || VAL_CLOSED.equalsIgnoreCase((String) processVars.get(KEY_STATUS))
                || VAL_CLOSED.equalsIgnoreCase((String) processVars.get(KEY_CURRENT_STAGE))
                || VAL_CLOSED.equalsIgnoreCase((String) processVars.get(KEY_STAGE));
    }

    private void trySendClosureNotification(Map<String, Object> processVars, Map<String, Object> customer,
            String ticketId, String processInstanceId) {
        try {
            notificationDelegate.sendNotificationFromMap(processVars, customer, ticketId, processInstanceId);
        } catch (Exception e) {
            log.error("Closure notification failed (non-fatal): {}", e.getMessage());
        }
    }

    private void tryPersistOverallStatus(Map<String, Object> processVars, String processInstanceId, String ticketId) {
        if (slaTrackingService == null) {
            return;
        }
        try {
            String complaintId = (String) processVars.getOrDefault(KEY_DBC_TICKET_ID,
                    processVars.getOrDefault(KEY_COMPLAINT_ID, ticketId));
            slaTrackingService.persistOverallStatus(processInstanceId, complaintId, processVars);
            if (ticketId != null && !ticketId.equals(complaintId)) {
                slaTrackingService.persistOverallStatus(processInstanceId, ticketId, processVars);
            }
        } catch (Exception e) {
            log.warn("Overall complaint status persist skipped: {}", e.getMessage());
        }
    }

    private void tryRecordTaskCompletion(String taskId) {
        try {
            slaTrackingService.recordTaskCompletion(taskId, getCurrentUsername());
        } catch (Exception e) {
            log.error("SLA task completion tracking failed: {}", e.getMessage());
        }
    }

    private void trySyncSecondaryResolutionTask(String pInstId, Map<String, Object> variables) {
        try {
            syncSecondaryResolutionTask(pInstId, variables);
        } catch (Exception e) {
            log.error("Failed to sync secondary resolution task: {}", e.getMessage());
        }
    }

    private void tryLogCaseClosedAudit(String ticketId, String processInstanceId) {
        try {
            auditService.log(ticketId, processInstanceId, null, "CASE_CLOSED", KEY_SYSTEM, KEY_SYSTEM,
                    "Complaint resolved and notification delivered to customer successfully.", "", "", "");
        } catch (Exception e) {
            log.error("Case-closed audit logging failed: {}", e.getMessage());
        }
    }

    private void tryMarkSlaResolved(String processInstanceId) {
        try {
            slaTrackingService.markResolved(processInstanceId);
        } catch (Exception e) {
            log.error("SLA resolution marking failed: {}", e.getMessage());
        }
    }

    private void recordStartForNewTasks(String processInstanceId, String ticketId) {
        try {
            List<Task> newTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId).list();
            for (Task newTask : newTasks) {
                slaTrackingService.recordTaskStart(processInstanceId, ticketId,
                        newTask.getId(), newTask.getTaskDefinitionKey(), newTask.getName(), newTask.getAssignee());
            }
        } catch (Exception e) {
            log.error("SLA new task tracking failed: {}", e.getMessage());
        }
    }

    private Map<String, Object> cleanLargeProcessVars(Map<String, Object> variables) {
        Map<String, Object> processVars = new HashMap<>(variables);
        processVars.entrySet().removeIf(entry -> {
            if (entry.getValue() instanceof String val) {
                return val.length() > 50000 && val.startsWith("data:");
            }
            return false;
        });
        return processVars;
    }

    @SuppressWarnings("java:S107")
    private void logCompleteTaskAudit(Task task, String ticketId, String cName, String cEmail, String cat, String desc,
            Map<String, Object> variables, Map<String, Object> vars, Map<String, Object> customer) {
        String[] actionActor = resolveTaskActionAndActor(task.getTaskDefinitionKey(), variables, vars, customer, cName,
                ticketId);
        String action = actionActor[0];
        String actor = actionActor[1];

        try {
            auditService.log(ticketId, task.getProcessInstanceId(), task.getId(), action, actor, getCurrentUsername(),
                    "Task completed: " + task.getName(), "", "", "");
        } catch (Exception e) {
            log.error("Audit logging failed (non-fatal): {}", e.getMessage());
        }
    }

    private String[] resolveTaskActionAndActor(String defKey, Map<String, Object> variables, Map<String, Object> vars,
            Map<String, Object> customer, String cName, String ticketId) {
        if ("FormTask_15".equals(defKey))
            return new String[] { "FCR_DECISION", "branch-staff" };
        if (KEY_FORM_TASK_43.equals(defKey))
            return new String[] { "CMD_CLASSIFICATION", "cmd" };
        if (KEY_FORM_TASK_48.equals(defKey))
            return new String[] { "INVESTIGATION_COMPLETED", "audit-team" };
        if (KEY_FORM_TASK_57.equals(defKey))
            return new String[] { "RESOLUTION_COMPLETED", "work-unit" };
        if (KEY_FORM_TASK_CHIEF_COMMITTEE.equals(defKey)) {
            checkChiefCommitteeRejection(variables, vars, customer, cName, ticketId);
            return new String[] { "COMMITTEE_DECISION", "chief-committee" };
        }
        if (KEY_SERVICE_TASK_65.equals(defKey) || "ServiceTask_62".equals(defKey))
            return new String[] { "NOTIFICATION_SENT", "sq-cmd" };
        return new String[] { "TASK_COMPLETED", "staff" };
    }

    private void checkChiefCommitteeRejection(Map<String, Object> variables, Map<String, Object> vars,
            Map<String, Object> customer, String cName, String ticketId) {
        String decision = (String) variables.get(KEY_COMMITTEE_DECISION);
        if (DECISION_REJECTED.equalsIgnoreCase(decision)) {
            String explanation = (String) variables.getOrDefault("committeeExplanation", "No explanation provided.");
            String preferredLanguage = (String) vars.get(KEY_PREFERRED_LANGUAGE);
            if (preferredLanguage == null && customer != null) {
                preferredLanguage = (String) customer.getOrDefault(KEY_PREFERRED_LANGUAGE, LANG_ENGLISH);
            }
            String customMsg;
            if (LANG_AMHARIC.equalsIgnoreCase(preferredLanguage)) {
                customMsg = """
                        ውድ %s፣

                        ቅሬታዎ (የቲኬት ቁጥር: %s) በዋናው ኮሚቴ ውድቅ የተደረገ መሆኑን እናሳውቃለን።

                        የኮሚቴው ማብራሪያ / የውድቅት ምክንያት:
                        %s

                        በመልካም አክብሮት፣
                        ዋና ኮሚቴ
                        ዳሽን ባንክ""".formatted(cName != null ? cName : "ውድ ደንበኛ", ticketId, explanation);
            } else {
                customMsg = """
                        Dear %s,

                        We regret to inform you that your complaint (Ticket Number: %s) has been rejected by the Chief Committee.

                        Committee Explanation / Rejection Reason:
                        %s

                        Best regards,
                        Chief Committee
                        Complaint Management System"""
                        .formatted(cName != null ? cName : "Valued Customer", ticketId, explanation);
            }
            variables.put("customNotificationMessage", customMsg);
        }
    }

    private void syncSecondaryResolutionTask(String pInstId, Map<String, Object> variables) {
        if (pInstId == null)
            return;
        List<Task> activeProcessTasks = taskService.createTaskQuery().processInstanceId(pInstId).list();
        for (Task t : activeProcessTasks) {
            if (KEY_FORM_TASK_57.equals(t.getTaskDefinitionKey()) || "FormTask_24".equals(t.getTaskDefinitionKey())) {
                Map<String, Object> syncVars = new HashMap<>();
                String notes = (String) variables.getOrDefault("resolutionDetails", "");
                if (notes.isEmpty()) {
                    notes = (String) variables.getOrDefault(KEY_FCR_COMMENTS, "");
                }
                syncVars.put("resolutionDetails", notes);
                syncVars.put("actionTaken", "Manager secondary resolution review");
                syncVars.put("isSensitive", false);
                taskService.complete(t.getId(), syncVars);
            }
        }
    }

    @DeleteMapping("/process/{instanceId}")
    public ResponseEntity<Void> deleteProcess(@PathVariable String instanceId) {
        runtimeService.deleteProcessInstance(instanceId, "Deleted by user from dashboard");
        return ResponseEntity.ok().build();
    }

    @PostMapping({ "/process/clear-all", "/tasks/clear-all" })
    @SuppressWarnings("java:S1141")
    public ResponseEntity<Map<String, Object>> clearAllTasksAndProcesses() {
        try {
            // Delete active process instances safely
            try {
                var activeInstances = runtimeService.createProcessInstanceQuery().list();
                for (var inst : activeInstances) {
                    tryDeleteActiveInstance(inst.getId());
                }
            } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }

            // Delete historic process instances safely
            try {
                var historicInstances = historyService.createHistoricProcessInstanceQuery().list();
                for (var hist : historicInstances) {
                    tryDeleteHistoricInstance(hist.getId());
                }
            } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }

            // Clear SLA tracking data safely
            try {
                slaTrackingService.clearAllSlaData();
            } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }

            // Direct SQL purge on Flowable tables and SLA tables to guarantee 0 residual
            // tasks
            try {
                try {
                    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
                try {
                    jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }

                String[] tablesToPurge = {
                        "task_time_tracking",
                        "sla_breach_records",
                        "sla_breach_record",
                        "sla_escalation_records",
                        "sla_escalation_record",
                        "audit_log",
                        "audit_logs",
                        "first_contact_resolutions",
                        "complaint_sla_metrics",
                        "complaints",
                        "customer_feedback",
                        "complainant_related_information",
                        "ACT_RU_TASK",
                        "ACT_RU_VARIABLE",
                        "ACT_RU_IDENTITYLINK",
                        "ACT_RU_EVENT_SUBSCR",
                        "ACT_RU_EXECUTION",
                        "ACT_HI_TASKINST",
                        "ACT_HI_VARINST",
                        "ACT_HI_PROCINST",
                        "ACT_HI_ACTINST",
                        "ACT_HI_DETAIL",
                        "ACT_HI_COMMENT",
                        "ACT_HI_ATTACHMENT"
                };

                for (String table : tablesToPurge) {
                    try {
                        jdbcTemplate.execute("DELETE FROM " + table);
                    } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
                }

                try {
                    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
                try {
                    jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
            } catch (Exception ex) {
                log.warn("SQL purge warning: {}", ex.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    KEY_MESSAGE, "All tasks, process instances, and SLA metrics successfully cleared for fresh start.",
                    "cleared", true));
        } catch (Exception e) {
            log.error("Failed to clear all tasks: ", e);
            return ResponseEntity.status(500).body(Map.of(KEY_ERROR, "Failed to clear all tasks: " + e.getMessage()));
        }
    }

    private void tryDeleteActiveInstance(String instanceId) {
        try {
            runtimeService.deleteProcessInstance(instanceId, "Fresh System Reset");
        } catch (Exception ignored) {
            // Ignored during reset
        }
    }

    private void tryDeleteHistoricInstance(String instanceId) {
        try {
            historyService.deleteHistoricProcessInstance(instanceId);
        } catch (Exception ignored) {
            // Ignored during reset
        }
    }

    @GetMapping("/process/{instanceId}")
    public ResponseEntity<Map<String, Object>> getProcess(@PathVariable String instanceId) {
        var processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
        var history = historyService.createHistoricTaskInstanceQuery().processInstanceId(instanceId).list();

        Map<String, Object> payload = Map.of(
                "id", instanceId,
                "isActive", processInstance != null,
                "historyTaskCount", history.size());

        return ResponseEntity.ok(payload);
    }

    @GetMapping({ "/complaints/status", "/complaints/status/{ticketId:.+}", "/complaints/status/**" })
    @SuppressWarnings("java:S3776")
    public ResponseEntity<Map<String, Object>> getComplaintStatus(
            @PathVariable(required = false) String ticketId,
            @RequestParam(name = "ticketId", required = false) String paramTicketId,
            HttpServletRequest request) {
        try {
            String rawTicket = paramTicketId;
            if (rawTicket == null || rawTicket.isBlank()) {
                rawTicket = ticketId;
            }
            if (rawTicket == null || rawTicket.isBlank()) {
                String fullPath = request.getRequestURI();
                String prefix = "/api/complaints/status/";
                if (fullPath.contains(prefix)) {
                    rawTicket = fullPath.substring(fullPath.indexOf(prefix) + prefix.length());
                }
            }

            if (rawTicket == null || rawTicket.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(KEY_ERROR, "Ticket number is required."));
            }

            // 1. URL-decode and Trim whitespace
            String queryTicket = URLDecoder.decode(rawTicket.trim(), StandardCharsets.UTF_8).trim();

            // 2. Validate expected ticket format (e.g. CM-001/2026-27, DBC-123,
            // FCR-001/2026-27)
            if (!queryTicket.matches("^[A-Za-z0-9/\\-_]{3,50}$")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(KEY_ERROR, "Invalid ticket number format."));
            }

            // 3. Query audit log
            List<AuditLog> logs = auditService.getLogs(null, queryTicket, null, null, null);
            if (logs == null || logs.isEmpty()) {
                List<AuditLog> allLogs = auditService.getLogs(null, null, null, null, null);
                final String targetTicket = queryTicket;
                if (allLogs != null) {
                    logs = allLogs.stream()
                            .filter(l -> l.getComplaintId() != null
                                    && l.getComplaintId().equalsIgnoreCase(targetTicket))
                            .collect(Collectors.toList());
                }
            }

            // 4. Query SLA metrics table as fallback
            if (logs == null || logs.isEmpty()) {
                final String targetTicket = queryTicket;
                var metricsOpt = slaMetricsRepository.findByComplaintId(targetTicket);
                if (metricsOpt.isEmpty()) {
                    metricsOpt = slaMetricsRepository.findByGeneralTicketId(targetTicket);
                }

                if (metricsOpt.isPresent()) {
                    ComplaintSlaMetrics metrics = metricsOpt.get();
                    Map<String, Object> publicResult = new HashMap<>();
                    publicResult.put(KEY_TICKET_NUMBER, queryTicket);
                    publicResult.put(KEY_CUSTOMER_NAME,
                            metrics.getCustomerName() != null ? metrics.getCustomerName() : "Customer");
                    publicResult.put("currentStatus",
                            resolvePublicOverallStatus(queryTicket, metrics.getStatus(), metrics.getCurrentStage()));
                    publicResult.put("submissionDate",
                            metrics.getCreatedAt() != null ? metrics.getCreatedAt().toString() : "");
                    publicResult.put(KEY_DESCRIPTION,
                            metrics.getComplaintCategory() != null ? metrics.getComplaintCategory()
                                    : "Complaint " + queryTicket);
                    publicResult.put(KEY_CATEGORY,
                            metrics.getComplaintCategory() != null ? metrics.getComplaintCategory() : CAT_GENERAL);
                    return ResponseEntity.ok(publicResult);
                }

                // 5. Ticket is valid format but does not exist -> 404 Not Found
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(KEY_ERROR, "No complaint found with the provided ticket number."));
            }

            AuditLog latestLog = logs.get(0);
            AuditLog oldestLog = logs.get(logs.size() - 1);

            // Expose ONLY safe public fields
            Map<String, Object> publicResult = new HashMap<>();
            publicResult.put(KEY_TICKET_NUMBER, queryTicket);
            publicResult.put(KEY_CUSTOMER_NAME,
                    oldestLog.getCustomerName() != null ? oldestLog.getCustomerName() : latestLog.getCustomerName());
            publicResult.put("currentStatus", resolvePublicOverallStatus(queryTicket, latestLog.getAction(), null));
            publicResult.put("submissionDate",
                    oldestLog.getCreatedAt() != null ? oldestLog.getCreatedAt().toString() : "");
            publicResult.put(KEY_DESCRIPTION,
                    oldestLog.getDetailsOfComplaint() != null ? oldestLog.getDetailsOfComplaint()
                            : latestLog.getDetailsOfComplaint());
            publicResult.put(KEY_CATEGORY, oldestLog.getComplaintCategory() != null ? oldestLog.getComplaintCategory()
                    : latestLog.getComplaintCategory());

            return ResponseEntity.ok(publicResult);
        } catch (Exception e) {
            log.error("Error looking up public complaint status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR,
                            "An unexpected server error occurred while retrieving complaint status. Please try again later."));
        }
    }

    @GetMapping("/complaints/status/{ticketId}/tasks")
    public ResponseEntity<Map<String, Object>> getComplaintTasks(@PathVariable String ticketId) {
        Map<String, Object> debugInfo = new HashMap<>();

        List<AuditLog> logs = auditService.getLogs(null, "all".equalsIgnoreCase(ticketId) ? null : ticketId, null, null,
                null);
        debugInfo.put("auditLogs", logs);
        debugInfo.put("activeTasks", getActiveTasksDebugInfo(ticketId));
        debugInfo.put("historicTasks", getHistoricTasksDebugInfo(ticketId));

        return ResponseEntity.ok(debugInfo);
    }

    private List<Map<String, Object>> getActiveTasksDebugInfo(String ticketId) {
        List<Task> activeTasks = taskService.createTaskQuery().list();
        List<Map<String, Object>> activeResult = new ArrayList<>();
        for (Task t : activeTasks) {
            Map<String, Object> vars = taskService.getVariables(t.getId());
            Map<String, Object> complaint = castToMap(vars.get(KEY_COMPLAINT));
            String cId = complaint.getOrDefault("id", KEY_UNKNOWN).toString();
            if (ticketId.equalsIgnoreCase(cId) || "all".equalsIgnoreCase(ticketId)) {
                activeResult.add(Map.of(
                        "id", t.getId(),
                        KEY_NAME, t.getName(),
                        KEY_ASSIGNEE, t.getAssignee() != null ? t.getAssignee() : "null",
                        KEY_DEFINITION_KEY, t.getTaskDefinitionKey(),
                        KEY_PROCESS_INSTANCE_ID, t.getProcessInstanceId(),
                        "vars", vars));
            }
        }
        return activeResult;
    }

    private List<Map<String, Object>> getHistoricTasksDebugInfo(String ticketId) {
        var historicTasks = historyService.createHistoricTaskInstanceQuery().list();
        List<Map<String, Object>> historicResult = new ArrayList<>();
        for (var ht : historicTasks) {
            buildHistoricTaskDebugEntry(ht, ticketId).ifPresent(historicResult::add);
        }
        return historicResult;
    }

    private Optional<Map<String, Object>> buildHistoricTaskDebugEntry(
            org.flowable.task.api.history.HistoricTaskInstance ht, String ticketId) {
        var procVarsResult = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(ht.getProcessInstanceId()).includeProcessVariables().singleResult();
        Map<String, Object> vars = procVarsResult != null ? procVarsResult.getProcessVariables() : Map.of();
        Map<String, Object> complaint = castToMap(vars.get(KEY_COMPLAINT));
        String cId = complaint.getOrDefault("id", KEY_UNKNOWN).toString();
        if (ticketId.equalsIgnoreCase(cId) || "all".equalsIgnoreCase(ticketId)) {
            return Optional.of(Map.of(
                    "id", ht.getId(),
                    KEY_NAME, ht.getName() != null ? ht.getName() : "null",
                    KEY_ASSIGNEE, ht.getAssignee() != null ? ht.getAssignee() : "null",
                    KEY_DEFINITION_KEY, ht.getTaskDefinitionKey() != null ? ht.getTaskDefinitionKey() : "null",
                    KEY_PROCESS_INSTANCE_ID, ht.getProcessInstanceId(),
                    "deleteReason", ht.getDeleteReason() != null ? ht.getDeleteReason() : "null"));
        }
        return Optional.empty();
    }

    @PostMapping("/complaints/upload-audio")
    @SuppressWarnings("java:S1141")
    public ResponseEntity<Map<String, Object>> uploadAudio(@RequestParam("file") MultipartFile file,
            @RequestParam(value = KEY_COMPLAINT_ID, required = false) String complaintId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "File is empty"));
            }

            File uploadsDir = new File(DIR_UPLOADS);
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            Path targetPath = Paths.get(DIR_UPLOADS).resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            if (complaintId != null && !complaintId.isBlank()) {
                try {
                    attachmentService.saveAttachmentRecord(complaintId,
                            originalFilename != null ? originalFilename : fileName, fileName, file.getContentType(),
                            getCurrentUsername());
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
            }

            String fileUrl = "/api/complaints/attachments/" + fileName;
            return ResponseEntity
                    .ok(Map.of("url", fileUrl, "fileName", originalFilename != null ? originalFilename : fileName));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Failed to upload audio: " + e.getMessage()));
        }
    }

    @GetMapping("/complaints/attachments/{fileName:.+}")
    public ResponseEntity<Resource> getAttachment(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(DIR_UPLOADS).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                String lowerName = fileName.toLowerCase();
                Path fallbackPath;
                if (lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".m4a")) {
                    fallbackPath = Paths.get(DIR_UPLOADS).resolve("voice.mp3").normalize();
                } else {
                    fallbackPath = Paths.get(DIR_UPLOADS).resolve("evidence.pdf").normalize();
                }
                Resource fallbackResource = new UrlResource(fallbackPath.toUri());
                if (fallbackResource.exists()) {
                    String contentType = Files.probeContentType(fallbackPath);
                    return ResponseEntity.ok()
                            .contentType(MediaType
                                    .parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                            .body(fallbackResource);
                }
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/complaints/upload-evidence")
    @SuppressWarnings("java:S1141")
    public ResponseEntity<Map<String, Object>> uploadEvidence(@RequestParam("file") MultipartFile file,
            @RequestParam(value = KEY_COMPLAINT_ID, required = false) String complaintId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "File is empty"));
            }

            File uploadsDir = new File(DIR_UPLOADS);
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            Path targetPath = Paths.get(DIR_UPLOADS).resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            if (complaintId != null && !complaintId.isBlank()) {
                try {
                    attachmentService.saveAttachmentRecord(complaintId,
                            originalFilename != null ? originalFilename : fileName, fileName, file.getContentType(),
                            getCurrentUsername());
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
            }

            String fileUrl = "/api/complaints/attachments/" + fileName;
            return ResponseEntity
                    .ok(Map.of("url", fileUrl, "fileName", originalFilename != null ? originalFilename : fileName));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Failed to upload evidence: " + e.getMessage()));
        }
    }

    @GetMapping("/complaints/nbe-reports")
    @SuppressWarnings("java:S3776")
    public ResponseEntity<List<Map<String, Object>>> getNbeReports() {
        List<Map<String, Object>> reportList = new ArrayList<>();
        try {
            var historicInstances = historyService.createHistoricProcessInstanceQuery()
                    .includeProcessVariables()
                    .orderByProcessInstanceStartTime().desc()
                    .list();

            List<ComplaintSlaMetrics> allSla = slaTrackingService.getAllMetrics();
            Map<String, ComplaintSlaMetrics> slaMap = new HashMap<>();
            if (allSla != null) {
                for (var m : allSla) {
                    if (m.getProcessInstanceId() != null) {
                        slaMap.put(m.getProcessInstanceId(), m);
                    }
                }
            }

            for (var pi : historicInstances) {
                Map<String, Object> vars = pi.getProcessVariables();
                ComplaintSlaMetrics sla = slaMap.get(pi.getId());
                if (!shouldIncludeInNbeReport(vars) && !SlaTrackingService.isClassifiedComplaint(sla)) {
                    continue;
                }

                Map<String, Object> complaint = castToMap(vars != null ? vars.get(KEY_COMPLAINT) : null);
                Map<String, Object> customer = castToMap(vars != null ? vars.get(KEY_CUSTOMER) : null);
                reportList.add(buildNbeReportRow(pi, vars != null ? vars : Map.of(), complaint, customer, sla));
            }

            return ResponseEntity.ok(nbeComplianceReportService.mergeWithOperationalSources(reportList));
        } catch (Exception e) {
            log.error("Failed to fetch NBE report data: {}", e.getMessage());
        }
        return ResponseEntity.ok(reportList);
    }

    private boolean shouldIncludeInNbeReport(Map<String, Object> vars) {
        if (vars == null || !vars.containsKey(KEY_COMPLAINT)) {
            return false;
        }
        String classification = String.valueOf(vars.getOrDefault(KEY_CLASSIFICATION, ""));
        String status = String.valueOf(vars.getOrDefault(KEY_STATUS, ""));
        if (VAL_OTHER.equalsIgnoreCase(classification) || "INTAKE".equalsIgnoreCase(classification)) {
            return false;
        }
        if (VAL_OTHER.equalsIgnoreCase(status)
                && !VAL_COMPLAINT.equalsIgnoreCase(classification)
                && !VAL_DECLINED.equalsIgnoreCase(classification)) {
            return false;
        }
        boolean isFcr = Boolean.TRUE.equals(vars.get(KEY_FCR_STATUS))
                || VAL_VERIFIED.equalsIgnoreCase(String.valueOf(vars.get(KEY_FCR_STATUS)));
        return isFcr
                || VAL_COMPLAINT.equalsIgnoreCase(classification)
                || VAL_DECLINED.equalsIgnoreCase(classification);
    }

    @SuppressWarnings("java:S107")
    private Map<String, Object> buildNbeReportRow(org.flowable.engine.history.HistoricProcessInstance pi,
            Map<String, Object> vars,
            Map<String, Object> complaint,
            Map<String, Object> customer,
            ComplaintSlaMetrics sla) {
        Map<String, Object> row = new HashMap<>();
        String dbcId = "";
        if (sla != null && sla.getComplaintId() != null && sla.getComplaintId().startsWith("DBC-")) {
            dbcId = sla.getComplaintId();
        } else if (sla != null && sla.getDbcTicketId() != null && sla.getDbcTicketId().startsWith("DBC-")) {
            dbcId = sla.getDbcTicketId();
        } else {
            dbcId = (String) vars.getOrDefault(KEY_DBC_TICKET_ID, "");
        }
        if (dbcId.isBlank()) {
            dbcId = String.valueOf(complaint.getOrDefault(KEY_DBC_TICKET_ID, complaint.getOrDefault("id", "N/A")));
        }
        row.put(KEY_COMPLAINT_ID, dbcId);
        row.put(KEY_DBC_TICKET_ID, dbcId);
        row.put(KEY_PROCESS_INSTANCE_ID, pi != null ? pi.getId() : null);

        String lodgedDate = pi.getStartTime() != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .format(pi.getStartTime().toInstant().atZone(SYSTEM_ZONE).toLocalDateTime()) : null;
        row.put("lodgedDate", lodgedDate);

        String resolvedDate = pi.getEndTime() != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .format(pi.getEndTime().toInstant().atZone(SYSTEM_ZONE).toLocalDateTime()) : null;
        row.put("resolvedDate", resolvedDate);

        row.put("complainantName", customer.getOrDefault(KEY_NAME, "N/A"));
        row.put("mobile", customer.getOrDefault(KEY_PHONE, "N/A"));
        row.put(KEY_EMAIL, customer.getOrDefault(KEY_EMAIL, "N/A"));

        row.put("issuesRaised", resolveCategoryValue(vars, complaint));
        row.put("daysOpen", calculateDaysOpen(pi));
        row.put(KEY_SLA_STATUS, sla != null ? sla.getSlaStatus() : STATUS_ON_TIME);

        boolean referredToNbe = Boolean.TRUE.equals(vars.get("referredToNbe"));
        row.put("referredToNbe", referredToNbe);
        String processId = pi != null ? pi.getId() : null;
        String overallStatus;
        if (slaTrackingService != null) {
            overallStatus = slaTrackingService.resolveOverallStatus(vars, processId,
                    String.valueOf(vars.getOrDefault(KEY_DBC_TICKET_ID, vars.getOrDefault(KEY_COMPLAINT_ID, ""))));
        } else {
            overallStatus = OverallComplaintStatus.resolve(vars, false);
        }
        row.put("overallStatus", overallStatus);
        row.put(KEY_STATUS, overallStatus);
        row.put("reportStatus", "");

        row.put("staffHandling", resolveStaffHandling(vars, complaint, customer));
        row.put("reasonForNonResolution", "");
        row.put("additionalComments", "");
        return row;
    }

    private long calculateDaysOpen(org.flowable.engine.history.HistoricProcessInstance pi) {
        if (pi.getStartTime() == null)
            return 0;
        Instant start = pi.getStartTime().toInstant();
        Instant end = pi.getEndTime() != null ? pi.getEndTime().toInstant() : Instant.now();
        return java.time.temporal.ChronoUnit.DAYS.between(
                start.atZone(SYSTEM_ZONE).toLocalDate(),
                end.atZone(SYSTEM_ZONE).toLocalDate());
    }

    /**
     * Resolves NBE Report overall complaint status from workflow state.
     * Ticket IDs (CM-*, DBC-*) are NEVER referenced and DO NOT influence business
     * status.
     */
    String resolveNbeStatus(org.flowable.engine.history.HistoricProcessInstance pi, Map<String, Object> vars,
            boolean referredToNbe) {
        if (referredToNbe) {
            return VAL_REFERRED_TO_NBE;
        }

        boolean processEnded = pi != null && pi.getEndTime() != null;
        boolean feedbackSubmitted = Boolean.TRUE.equals(vars.get("customerFeedbackSubmitted"))
                || "CASE_CLOSED_BY_CUSTOMER".equalsIgnoreCase(String.valueOf(vars.getOrDefault("lastAuditAction", "")));

        Map<String, Object> mapped = vars == null ? new HashMap<>() : new HashMap<>(vars);
        if (processEnded && !VAL_DECLINED.equalsIgnoreCase(String.valueOf(mapped.getOrDefault(KEY_STATUS, "")))
                && !VAL_DECLINED.equalsIgnoreCase(String.valueOf(mapped.getOrDefault(KEY_DECISION, "")))
                && !VAL_DECLINED.equalsIgnoreCase(String.valueOf(mapped.getOrDefault(KEY_CLASSIFICATION, "")))
                && !Boolean.TRUE.equals(mapped.get(KEY_REQUIRES_INVESTIGATION))
                && mapped.get(KEY_CURRENT_STAGE) == null) {
            mapped.putIfAbsent(KEY_STATUS, VAL_CLOSED);
        }

        return referredToNbe ? VAL_REFERRED_TO_NBE : OverallComplaintStatus.resolve(mapped, feedbackSubmitted);
    }

    @SuppressWarnings("java:S3776")
    private String resolvePublicOverallStatus(String ticketId, String rawStatus, String currentStage) {
        boolean feedback = slaTrackingService != null && slaTrackingService.hasCustomerFeedback(null, ticketId);
        if (slaMetricsRepository != null) {
            var metricsOpt = slaMetricsRepository.findByComplaintId(ticketId);
            if (metricsOpt.isEmpty()) {
                metricsOpt = slaMetricsRepository.findByGeneralTicketId(ticketId);
            }
            if (metricsOpt.isPresent()) {
                ComplaintSlaMetrics metrics = metricsOpt.get();
                feedback = slaTrackingService != null && (feedback || slaTrackingService.hasCustomerFeedback(
                        metrics.getProcessInstanceId(), metrics.getComplaintId()));
                if (feedback) {
                    return OverallComplaintStatus.CLOSED;
                }
                if (metrics.getStatus() != null && !metrics.getStatus().isBlank()) {
                    return OverallComplaintStatus.canonicalize(metrics.getStatus());
                }
            }
        }
        if (feedback) {
            return OverallComplaintStatus.CLOSED;
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put(KEY_STATUS, rawStatus);
        vars.put(KEY_CURRENT_STAGE, currentStage);
        return OverallComplaintStatus.resolve(vars, false);
    }

    private String resolveCategoryValue(Map<String, Object> vars, Map<String, Object> complaint) {
        String categoryVal = CAT_CUSTOMER_SERVICE_ISSUES;
        if (vars.containsKey(KEY_COMPLAINT_CATEGORY) && vars.get(KEY_COMPLAINT_CATEGORY) != null) {
            categoryVal = vars.get(KEY_COMPLAINT_CATEGORY).toString();
        } else if (vars.containsKey(KEY_CATEGORY) && vars.get(KEY_CATEGORY) != null) {
            categoryVal = vars.get(KEY_CATEGORY).toString();
        } else if (complaint.containsKey(KEY_CATEGORY) && complaint.get(KEY_CATEGORY) != null) {
            categoryVal = complaint.get(KEY_CATEGORY).toString();
        }

        if (!categoryVal.isEmpty()) {
            String catLower = categoryVal.toLowerCase().trim();
            return switch (catLower) {
                case "customer service issues", "general", KEY_BRANCH -> CAT_CUSTOMER_SERVICE_ISSUES;
                case "transaction error", "transfer" -> "Transaction Error";
                case "account management", "account" -> "Account Management";
                case "banking app issues", "mobile", "mobile_banking", "internet_banking", "super_app" ->
                    "Banking App Issues";
                case "credit/financing concerns", "loan" -> "Credit/Financing Concerns";
                case "fraud & security risk", "fraud" -> "Fraud & Security Risk";
                case "information disclosure" -> "Information Disclosure";
                case "atm & card banking issues", "atm", "card" -> "ATM & Card Banking Issues";
                case "policy & compliance disputes" -> "Policy & Compliance Disputes";
                case "system failure", "technical" -> "System Failure";
                case "branch operation", "employee_behaviour" -> "Branch Operation";
                default -> categoryVal;
            };
        }
        return CAT_CUSTOMER_SERVICE_ISSUES;
    }

    public String generateDbcTicketId() {
        return slaTrackingService.generateDbcTicketId();
    }

    @SuppressWarnings("java:S1141")
    public synchronized String generateCmTicketId() {
        LocalDate today = LocalDate.now(SYSTEM_ZONE);
        int year = today.getYear();

        // Gregorian Calendar Year reset: January 1st
        int fyEnd = year + 1;
        String fiscalYear = String.format("%d-%02d", year, fyEnd % 100);

        int maxSeq = 0;
        try {
            List<String> existing = jdbcTemplate.queryForList(
                    "SELECT general_ticket_id FROM complaint_sla_metrics WHERE general_ticket_id LIKE ?",
                    String.class, "CM-%/" + fiscalYear);
            for (String cm : existing) {
                if (cm != null && cm.startsWith("CM-") && cm.contains("/")) {
                    String seqPart = cm.substring(3, cm.indexOf('/'));
                    try {
                        int num = Integer.parseInt(seqPart);
                        if (num > maxSeq)
                            maxSeq = num;
                    } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
                }
            }
        } catch (Exception e) {
            log.info("CM sequence query note: {}", e.getMessage());
        }

        int nextSeq = maxSeq + 1;
        return String.format("CM-%03d/%s", nextSeq, fiscalYear);
    }

    private String resolveStaffHandling(Map<String, Object> vars, Map<String, Object> complaint,
            Map<String, Object> customer) {
        if (vars.containsKey(KEY_BRANCH) && vars.get(KEY_BRANCH) != null
                && !vars.get(KEY_BRANCH).toString().isEmpty()) {
            String b = vars.get(KEY_BRANCH).toString();
            String d = (vars.containsKey(KEY_DEPARTMENT) && vars.get(KEY_DEPARTMENT) != null)
                    ? vars.get(KEY_DEPARTMENT).toString()
                    : "";
            return !d.isEmpty() ? b + " / " + d : b;
        }
        if (complaint.get(KEY_BRANCH) != null) {
            return complaint.get(KEY_BRANCH).toString();
        }
        if (customer.get(KEY_BRANCH) != null) {
            return customer.get(KEY_BRANCH).toString();
        }
        return "CMD Screening";
    }

    @PostMapping("/complaints/{id}/close")
    public ResponseEntity<Map<String, Object>> closeComplaint(@PathVariable String id) {
        Optional<ComplaintSlaMetrics> opt = slaMetricsRepository.findByComplaintId(id);
        if (opt.isEmpty()) {
            opt = slaMetricsRepository.findByProcessInstanceId(id);
        }
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ComplaintSlaMetrics metrics = opt.get();
        slaTrackingService.markResolved(metrics.getProcessInstanceId());

        try {
            if (metrics.getProcessInstanceId() != null) {
                runtimeService.deleteProcessInstance(metrics.getProcessInstanceId(), "Closed by staff");
            }
        } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }

        return ResponseEntity
                .ok(Map.of(KEY_MESSAGE, "Complaint " + metrics.getComplaintId() + " successfully closed."));
    }

    @GetMapping("/fcr/all")
    public ResponseEntity<List<Map<String, Object>>> getAllFirstContactResolutions() {
        try {
            List<Map<String, Object>> records = jdbcTemplate.queryForList(
                    "SELECT id, complaint_id AS complaintId, process_instance_id AS processInstanceId, " +
                            "customer_name AS customerName, account_number AS accountNumber, " +
                            "complaint_category AS complaintCategory, fcr_notes AS fcrNotes " +
                            "FROM first_contact_resolutions ORDER BY id DESC");
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error("Failed to fetch FCR records: {}", e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/process/fcr/reject/{taskId}")
    @SuppressWarnings("java:S1141")
    public ResponseEntity<Map<String, Object>> rejectFcrTask(@PathVariable String taskId) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "Task not found: " + taskId));
            }

            String procInstId = task.getProcessInstanceId();
            Map<String, Object> vars = runtimeService.getVariables(procInstId);
            if (vars != null && vars.containsKey(KEY_COMPLAINT) && vars.get(KEY_COMPLAINT) instanceof Map) {
                Map<String, Object> complaintMap = new HashMap<>((Map<String, Object>) vars.get(KEY_COMPLAINT));
                complaintMap.put(KEY_IS_FCR, false);
                complaintMap.put(KEY_FCR_STATUS, VAL_REJECTED);
                runtimeService.setVariable(procInstId, KEY_COMPLAINT, complaintMap);
            }

            runtimeService.setVariable(procInstId, KEY_IS_FCR, false);
            runtimeService.setVariable(procInstId, "isFCR", false);
            runtimeService.setVariable(procInstId, KEY_FCR_STATUS, VAL_REJECTED);
            runtimeService.setVariable(procInstId, KEY_CURRENT_STAGE, STAGE_CMD_SCREENING);
            taskService.setVariable(task.getId(), KEY_IS_FCR, false);
            taskService.setVariable(task.getId(), KEY_FCR_STATUS, VAL_REJECTED);

            String ticketId = (String) runtimeService.getVariable(task.getProcessInstanceId(), KEY_COMPLAINT_ID);
            if (ticketId == null)
                ticketId = (String) runtimeService.getVariable(task.getProcessInstanceId(), KEY_GENERAL_TICKET_ID);

            try {
                jdbcTemplate.update(
                        "UPDATE complaint_sla_metrics SET fcr_status = false, current_stage = 'CMD_SCREENING' WHERE process_instance_id = ? OR complaint_id = ?",
                        task.getProcessInstanceId(), ticketId);
                auditService.log(ticketId, task.getProcessInstanceId(), task.getId(), "FCR_REJECTED", ACTOR_CMD_OFFICER,
                        getCurrentUsername(),
                        "FCR rejected by CCO; complaint routed to standard screening.", "", "", "");
            } catch (Exception e) {
                log.info("Could not update FCR rejection in SLA metrics: {}", e.getMessage());
            }

            return ResponseEntity
                    .ok(Map.of(KEY_MESSAGE, "FCR status rejected successfully. Task remains in screening queue."));
        } catch (Exception e) {
            log.error("Failed to reject FCR status: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @PostMapping("/complaints/{taskId}/update-details")
    @SuppressWarnings({ "java:S3776", "java:S1141" })
    public ResponseEntity<Map<String, Object>> updateComplaintRecordDetails(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> payload) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            String procInstId = task != null ? task.getProcessInstanceId() : null;

            if (task == null) {
                List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(taskId).list();
                if (!activeTasks.isEmpty()) {
                    task = activeTasks.get(0);
                    procInstId = task.getProcessInstanceId();
                } else {
                    procInstId = taskId;
                }
            }

            String branch = (String) payload.getOrDefault(KEY_BRANCH, payload.getOrDefault("complaintBranch", ""));
            String district = (String) payload.getOrDefault(KEY_DISTRICT, "");
            String category = (String) payload.getOrDefault(KEY_COMPLAINT_CATEGORY, payload.getOrDefault(KEY_CATEGORY, ""));
            String serviceType = (String) payload.getOrDefault(KEY_SERVICE_TYPE, "");
            String channel = (String) payload.getOrDefault(KEY_COMPLAINT_MADE_ON, payload.getOrDefault(KEY_CHANNEL, ""));
            String receivedBy = (String) payload.getOrDefault(KEY_RECEIVED_BY, "");
            String accNum = (String) payload.getOrDefault(KEY_ACCOUNT_NUMBER, "");
            String desc = (String) payload.getOrDefault(KEY_COMPLAINT_DESCRIPTION,
                    payload.getOrDefault(KEY_DESCRIPTION, ""));

            String classification = (String) payload.getOrDefault(KEY_CLASSIFICATION, "");

            Map<String, Object> varsToSet = new HashMap<>();
            if (!classification.isBlank())
                varsToSet.put(KEY_CLASSIFICATION, classification);
            if (!branch.isBlank())
                varsToSet.put(KEY_BRANCH, branch);
            if (!district.isBlank())
                varsToSet.put(KEY_DISTRICT, district);
            if (!category.isBlank())
                varsToSet.put(KEY_COMPLAINT_CATEGORY, category);
            if (!serviceType.isBlank())
                varsToSet.put(KEY_SERVICE_TYPE, serviceType);
            if (!channel.isBlank()) {
                varsToSet.put(KEY_CHANNEL, channel);
                varsToSet.put(KEY_COMPLAINT_MADE_ON, channel);
            }
            if (!receivedBy.isBlank())
                varsToSet.put(KEY_RECEIVED_BY, receivedBy);
            if (!accNum.isBlank())
                varsToSet.put(KEY_ACCOUNT_NUMBER, accNum);
            if (!desc.isBlank()) {
                varsToSet.put(KEY_DESCRIPTION, desc);
                varsToSet.put(KEY_COMPLAINT_DESCRIPTION, desc);
            }

            if (procInstId != null) {
                try {
                    runtimeService.setVariables(procInstId, varsToSet);

                    Object rawComplaint = runtimeService.getVariable(procInstId, KEY_COMPLAINT);
                    if (rawComplaint instanceof Map<?, ?> rawMap) {
                        Map<String, Object> complaintMap = new HashMap<>((Map<String, Object>) rawMap);
                        if (!branch.isBlank())
                            complaintMap.put(KEY_BRANCH, branch);
                        if (!district.isBlank())
                            complaintMap.put(KEY_DISTRICT, district);
                        if (!category.isBlank())
                            complaintMap.put(KEY_CATEGORY, category);
                        if (!serviceType.isBlank())
                            complaintMap.put(KEY_SERVICE_TYPE, serviceType);
                        if (!channel.isBlank())
                            complaintMap.put(KEY_CHANNEL, channel);
                        if (!receivedBy.isBlank())
                            complaintMap.put(KEY_RECEIVED_BY, receivedBy);
                        if (!accNum.isBlank())
                            complaintMap.put(KEY_ACCOUNT_NUMBER, accNum);
                        if (!desc.isBlank())
                            complaintMap.put(KEY_DESCRIPTION, desc);
                        runtimeService.setVariable(procInstId, KEY_COMPLAINT, complaintMap);
                    }

                    Object rawCustomer = runtimeService.getVariable(procInstId, KEY_CUSTOMER);
                    if (rawCustomer instanceof Map<?, ?> rawCustMap) {
                        Map<String, Object> custMap = new HashMap<>((Map<String, Object>) rawCustMap);
                        if (!accNum.isBlank())
                            custMap.put(KEY_ACCOUNT_NUMBER, accNum);
                        runtimeService.setVariable(procInstId, KEY_CUSTOMER, custMap);
                    }
                } catch (Exception e) {
                    log.info("Process instance variables update note: {}", e.getMessage());
                }
            }

            if (task != null) {
                try {
                    taskService.setVariables(task.getId(), varsToSet);
                } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
            }

            try {
                String sql = "UPDATE complaint_sla_metrics SET " +
                        "branch = COALESCE(NULLIF(?, ''), branch), " +
                        "district = COALESCE(NULLIF(?, ''), district), " +
                        "complaint_category = COALESCE(NULLIF(?, ''), complaint_category), " +
                        "channel = COALESCE(NULLIF(?, ''), channel) " +
                        "WHERE process_instance_id = ? OR complaint_id = ? OR general_ticket_id = ?";
                jdbcTemplate.update(sql, branch, district, category, channel, procInstId, procInstId, procInstId);
            } catch (Exception e) {
                log.info("Note: Could not update complaint_sla_metrics: {}", e.getMessage());
            }

            try {
                String sqlComp = "UPDATE complaints SET " +
                        "home_branch = COALESCE(NULLIF(?, ''), home_branch), " +
                        "district = COALESCE(NULLIF(?, ''), district), " +
                        "complaint_category = COALESCE(NULLIF(?, ''), complaint_category), " +
                        "service_type = COALESCE(NULLIF(?, ''), service_type), " +
                        "received_by = COALESCE(NULLIF(?, ''), received_by), " +
                        "description = COALESCE(NULLIF(?, ''), description), " +
                        "channel = COALESCE(NULLIF(?, ''), channel), " +
                        "account_number = COALESCE(NULLIF(?, ''), account_number) " +
                        "WHERE process_instance_id = ? OR ticket_number = ? OR general_ticket_id = ?";
                jdbcTemplate.update(sqlComp, branch, district, category, serviceType, receivedBy, desc, channel, accNum,
                        procInstId, procInstId, procInstId);
            } catch (Exception e) {
                log.error("Could not update complaints table: ", e);
            }

            try {
                String sqlAudit = "UPDATE audit_logs SET " +
                        "complaint_category = COALESCE(NULLIF(?, ''), complaint_category), " +
                        "complaint_description = COALESCE(NULLIF(?, ''), complaint_description) " +
                        "WHERE process_instance_id = ? OR ticket_id = ?";
                jdbcTemplate.update(sqlAudit, category, desc, procInstId, procInstId);
            } catch (Exception e) {
                log.error("Could not update audit_logs table: ", e);
            }

            return ResponseEntity.ok(Map.of(
                    KEY_MESSAGE, "Complaint details updated and saved as official record across the system.",
                    "updated", true));
        } catch (Exception e) {
            log.error("Failed to update complaint details: ", e);
            return ResponseEntity.status(500)
                    .body(Map.of(KEY_ERROR, "Failed to update complaint details: " + e.getMessage()));
        }
    }

    @SuppressWarnings({ "java:S3776", "java:S1141" })
    private void upsertComplaintsTableRecord(Task task, Map<String, Object> processVars, String fallbackTicketId) {
        try {
            Map<String, Object> vars = new HashMap<>();
            String procInstId = task != null ? task.getProcessInstanceId() : null;
            if (procInstId != null) {
                try {
                    Map<String, Object> pVars = runtimeService.getVariables(procInstId);
                    if (pVars != null)
                        vars.putAll(pVars);
                } catch (Exception e) {
                    log.warn("Could not retrieve process instance variables for {}: {}", procInstId, e.getMessage());
                }
            }
            if (processVars != null)
                vars.putAll(processVars);

            Map<String, Object> customer = castToMap(vars.get(KEY_CUSTOMER));
            Map<String, Object> complaint = castToMap(vars.get(KEY_COMPLAINT));

            String generalId = resolveVarString(vars, complaint, KEY_GENERAL_TICKET_ID, KEY_TICKET_NUMBER, fallbackTicketId);
            String formalDbcId = getOrCreateDbcTicketId(vars, generalId, procInstId);
            if (formalDbcId == null || !formalDbcId.startsWith("DBC-")) {
                formalDbcId = generateDbcTicketId();
            }
            String customerName = resolveVarString(vars, customer, KEY_CUSTOMER_NAME, KEY_NAME, null);
            String accountNumber = resolveVarString(vars, customer, KEY_ACCOUNT_NUMBER, "account_number", null);
            String contactNumber = resolveVarString(vars, customer, "preferredContactNumber", KEY_PHONE, null);
            String contactMethod = resolveVarString(vars, customer, KEY_PREFERRED_CONTACT_METHOD, "contactMethod", null);
            String district = resolveVarString(vars, complaint, KEY_DISTRICT, "assignedDistrict", null);
            String branch = resolveVarString(vars, complaint, KEY_BRANCH, "homeBranch", null);
            String description = resolveVarString(vars, complaint, KEY_DESCRIPTION, KEY_COMPLAINT_DESCRIPTION, null);
            String category = resolveVarString(vars, complaint, KEY_COMPLAINT_CATEGORY, KEY_CATEGORY, null);
            String serviceType = resolveVarString(vars, complaint, KEY_SERVICE_TYPE, "service_type", null);
            String channel = resolveVarString(vars, complaint, KEY_CHANNEL, KEY_COMPLAINT_MADE_ON, null);
            String receivedBy = resolveVarString(vars, complaint, KEY_RECEIVED_BY, "received_by", null);

            // Validation: Fail gracefully with log error if critical ticket identifier is
            // missing
            if (formalDbcId == null || formalDbcId.isBlank()) {
                log.error(
                        "Complaint persistence aborted: Missing valid formal ticket identifier for process instance {}",
                        procInstId);
                return;
            }

            // Dynamic Entity Lookup for Customer
            if (accountNumber != null && !accountNumber.isBlank()) {
                try {
                    Optional<Customer> custOpt = customerRepository.findByAccountNumber(accountNumber.trim());
                    if (custOpt.isPresent()) {
                        if (customerName == null)
                            customerName = custOpt.get().getName();
                        if (contactNumber == null)
                            contactNumber = custOpt.get().getPhoneNumber();
                    }
                } catch (Exception e) {
                    log.warn("Customer lookup info for account {}: {}", accountNumber, e.getMessage());
                }
            }

            String sql = "INSERT INTO complaints (" +
                    "general_ticket_id, ticket_number, customer_name, preferred_contact_number, preferred_contact_method, "
                    +
                    "home_branch, district, complaint_detail, complaint_category, service_type, " +
                    "complaint_made_on, received_by, account_number, classification, complaint_classification, status) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "ticket_number = COALESCE(VALUES(ticket_number), ticket_number), " +
                    "general_ticket_id = VALUES(general_ticket_id), " +
                    "customer_name = COALESCE(VALUES(customer_name), customer_name), " +
                    "preferred_contact_number = COALESCE(VALUES(preferred_contact_number), preferred_contact_number), "
                    +
                    "preferred_contact_method = COALESCE(VALUES(preferred_contact_method), preferred_contact_method), "
                    +
                    "home_branch = COALESCE(VALUES(home_branch), home_branch), " +
                    "district = COALESCE(VALUES(district), district), " +
                    "complaint_detail = COALESCE(VALUES(complaint_detail), complaint_detail), " +
                    "complaint_category = COALESCE(VALUES(complaint_category), complaint_category), " +
                    "service_type = COALESCE(VALUES(service_type), service_type), " +
                    "complaint_made_on = COALESCE(VALUES(complaint_made_on), complaint_made_on), " +
                    "received_by = COALESCE(VALUES(received_by), received_by), " +
                    "account_number = COALESCE(VALUES(account_number), account_number), " +
                    "classification = COALESCE(VALUES(classification), classification), " +
                    "complaint_classification = COALESCE(VALUES(complaint_classification), complaint_classification), "
                    +
                    "status = COALESCE(VALUES(status), status)";

            String complaintClass = resolveVarString(processVars, null, KEY_COMPLAINT_CLASSIFICATION, KEY_PRIORITY_LEVEL,
                    CAT_GENERAL);
            jdbcTemplate.update(sql,
                    generalId, formalDbcId, customerName, contactNumber, contactMethod,
                    branch, district, description, category, serviceType,
                    channel, receivedBy, accountNumber, VAL_COMPLAINT, complaintClass,
                    OverallComplaintStatus.resolve(vars, false));

            log.info(
                    "Classified complaint record successfully persisted to `complaints` DB table: DBC={} | CM={} | Customer={} | Category={}",
                    formalDbcId, generalId, customerName, category);
        } catch (Exception e) {
            log.error("Failed to upsert complaint record into `complaints` table: ", e);
        }
    }

    private String resolveVarString(Map<String, Object> vars, Map<String, Object> nestedMap, String key1, String key2,
            String defaultVal) {
        if (vars.get(key1) != null && !vars.get(key1).toString().isBlank())
            return vars.get(key1).toString();
        if (vars.get(key2) != null && !vars.get(key2).toString().isBlank())
            return vars.get(key2).toString();
        if (nestedMap != null) {
            if (nestedMap.get(key1) != null && !nestedMap.get(key1).toString().isBlank())
                return nestedMap.get(key1).toString();
            if (nestedMap.get(key2) != null && !nestedMap.get(key2).toString().isBlank())
                return nestedMap.get(key2).toString();
        }
        return defaultVal;
    }

    @SuppressWarnings("java:S3776")
    private synchronized String getOrCreateDbcTicketId(Map<String, Object> processVars, String intakeTicketId,
            String processInstanceId) {
        if (processVars != null) {
            String existingDbc = (String) processVars.get(KEY_DBC_TICKET_ID);
            if (existingDbc != null && existingDbc.startsWith("DBC-")) {
                return existingDbc;
            }

            String complaintIdVar = (String) processVars.get(KEY_COMPLAINT_ID);
            if (complaintIdVar != null && complaintIdVar.startsWith("DBC-")) {
                return complaintIdVar;
            }
        }

        if (processInstanceId != null && !processInstanceId.isBlank()) {
            try {
                Object rDbc = runtimeService.getVariable(processInstanceId, KEY_DBC_TICKET_ID);
                if (rDbc != null && rDbc.toString().startsWith("DBC-")) {
                    return rDbc.toString();
                }
                Object rCId = runtimeService.getVariable(processInstanceId, KEY_COMPLAINT_ID);
                if (rCId != null && rCId.toString().startsWith("DBC-")) {
                    return rCId.toString();
                }
            } catch (Exception e) {
                log.debug("Note: Could not fetch runtime variable for {}: {}", processInstanceId, e.getMessage());
            }
        }

        String generalId = (processVars != null && processVars.get(KEY_GENERAL_TICKET_ID) != null)
                ? (String) processVars.get(KEY_GENERAL_TICKET_ID)
                : intakeTicketId;

        boolean hasPId = processInstanceId != null && !processInstanceId.isBlank();
        boolean hasGId = generalId != null && !generalId.isBlank();

        if (hasPId || hasGId) {
            try {
                List<String> existingDbList = jdbcTemplate.queryForList(
                        "SELECT complaint_id FROM complaint_sla_metrics WHERE ((? = TRUE AND process_instance_id = ?) OR (? = TRUE AND (general_ticket_id = ? OR complaint_id = ?))) AND complaint_id LIKE 'DBC-%' LIMIT 1",
                        String.class, hasPId, processInstanceId, hasGId, generalId, generalId);
                if (!existingDbList.isEmpty() && existingDbList.get(0) != null
                        && existingDbList.get(0).startsWith("DBC-")) {
                    return existingDbList.get(0);
                }
            } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }

            try {
                List<String> existingComplaintsList = jdbcTemplate.queryForList(
                        "SELECT ticket_number FROM complaints WHERE ((? = TRUE AND process_instance_id = ?) OR (? = TRUE AND general_ticket_id = ?)) AND ticket_number LIKE 'DBC-%' LIMIT 1",
                        String.class, hasPId, processInstanceId, hasGId, generalId);
                if (!existingComplaintsList.isEmpty() && existingComplaintsList.get(0) != null
                        && existingComplaintsList.get(0).startsWith("DBC-")) {
                    return existingComplaintsList.get(0);
                }
            } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }

            try {
                List<String> existingCriList = jdbcTemplate.queryForList(
                        "SELECT unique_id_no FROM complainant_related_information WHERE ((? = TRUE AND process_instance_id = ?) OR (? = TRUE AND unique_id_no = ?)) AND unique_id_no LIKE 'DBC-%' LIMIT 1",
                        String.class, hasPId, processInstanceId, hasGId, generalId);
                if (!existingCriList.isEmpty() && existingCriList.get(0) != null
                        && existingCriList.get(0).startsWith("DBC-")) {
                    return existingCriList.get(0);
                }
            } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }

            try {
                List<String> existingAuditList = jdbcTemplate.queryForList(
                        "SELECT complaint_id FROM audit_log WHERE ((? = TRUE AND process_instance_id = ?) OR (? = TRUE AND (general_ticket_id = ? OR complaint_id = ?))) AND complaint_id LIKE 'DBC-%' LIMIT 1",
                        String.class, hasPId, processInstanceId, hasGId, generalId, generalId);
                if (!existingAuditList.isEmpty() && existingAuditList.get(0) != null
                        && existingAuditList.get(0).startsWith("DBC-")) {
                    return existingAuditList.get(0);
                }
            } catch (Exception ignored) {
                    log.debug(LOG_OPTIONAL_SKIPPED, ignored.getMessage());
                }
        }

        String newDbcTicketId = generateDbcTicketId();

        if (processInstanceId != null && !processInstanceId.isBlank()) {
            try {
                runtimeService.setVariable(processInstanceId, KEY_DBC_TICKET_ID, newDbcTicketId);
                runtimeService.setVariable(processInstanceId, KEY_COMPLAINT_ID, newDbcTicketId);
            } catch (Exception e) {
                log.debug("Note: Could not set runtime variable for {}: {}", processInstanceId, e.getMessage());
            }
        }

        return newDbcTicketId;
    }
}
