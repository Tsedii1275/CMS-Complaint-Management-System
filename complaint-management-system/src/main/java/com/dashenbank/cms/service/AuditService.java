package com.dashenbank.cms.service;

import com.dashenbank.cms.model.AuditLog;
import com.dashenbank.cms.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String complaintId, String processInstanceId, String taskId, String action, String actor,
            String actorId, String description,
            String customerName, String complaintCategory, String detailsOfComplaint) {
        try {
            AuditLog log = AuditLog.builder()
                    .complaintId(complaintId)
                    .generalTicketId(complaintId)
                    .processInstanceId(processInstanceId)
                    .taskId(taskId)
                    .action(action)
                    .actor(actor)
                    .actorId(actorId)
                    .description(description)
                    .customerName(customerName)
                    .complaintCategory(complaintCategory)
                    .detailsOfComplaint(detailsOfComplaint)
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuditService.class).error("Failed to save audit log: {}", e.getMessage());
        }
    }

    @Transactional
    public void log(String complaintId, String processInstanceId, String taskId, String action, String actor,
            String actorId, String description) {
        log(complaintId, processInstanceId, taskId, action, actor, actorId, description, null, null, null);
    }

    public List<AuditLog> getLogs(String action, String complaintId, String actor, LocalDateTime startDate,
            LocalDateTime endDate) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (action != null && !action.isEmpty()) {
                predicates = cb.and(predicates, cb.equal(root.get("action"), action));
            }
            if (complaintId != null && !complaintId.isEmpty()) {
                predicates = cb.and(predicates, cb.or(
                        cb.equal(root.get("complaintId"), complaintId),
                        cb.equal(root.get("generalTicketId"), complaintId)));
            }
            if (actor != null && !actor.isEmpty()) {
                predicates = cb.and(predicates, cb.equal(root.get("actor"), actor));
            }
            if (startDate != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            return predicates;
        };

        return auditLogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
