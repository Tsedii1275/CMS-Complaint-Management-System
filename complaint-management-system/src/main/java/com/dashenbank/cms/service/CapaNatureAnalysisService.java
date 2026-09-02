package com.dashenbank.cms.service;

import com.dashenbank.cms.model.CapaAnalysis;
import com.dashenbank.cms.model.CapaFiveWhys;
import com.dashenbank.cms.model.CapaNatureAction;
import com.dashenbank.cms.repository.CapaAnalysisRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CapaNatureAnalysisService {

    private static final String KEY_NATURE = "nature";
    private static final String KEY_ACTION = "action";
    private static final String KEY_DEPARTMENT = "responsibleDepartment";
    private static final String KEY_OFFICER = "responsibleOfficer";
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private final RcaAnalysisService rcaAnalysisService;
    private final CapaAnalysisRepository capaAnalysisRepository;

    public CapaNatureAnalysisService(
            RcaAnalysisService rcaAnalysisService,
            CapaAnalysisRepository capaAnalysisRepository) {
        this.rcaAnalysisService = rcaAnalysisService;
        this.capaAnalysisRepository = capaAnalysisRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getByNature(String nature) {
        Map<String, Object> natureBlock = requireNatureBlock(nature);
        String canonical = String.valueOf(natureBlock.get(KEY_NATURE));
        CapaAnalysis stored = capaAnalysisRepository.findByComplaintNatureIgnoreCase(canonical).orElse(null);
        return assemble(natureBlock, stored);
    }

    @Transactional
    public Map<String, Object> save(Map<String, Object> body, String username) {
        NatureCapaPayload payload = fromBody(body);
        if (payload.nature == null || payload.nature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nature is required");
        }
        Map<String, Object> natureBlock = requireNatureBlock(payload.nature);
        String canonical = String.valueOf(natureBlock.get(KEY_NATURE));

        CapaAnalysis entity = capaAnalysisRepository.findByComplaintNatureIgnoreCase(canonical)
                .orElseGet(() -> CapaAnalysis.builder().complaintNature(canonical).build());
        entity.setComplaintNature(canonical);
        entity.setProblemStatement(blankToNull(payload.problemStatement) != null
                ? payload.problemStatement.trim()
                : canonical);
        entity.setRootCause(blankToNull(payload.rootCause));
        entity.setUpdatedBy(username);
        LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);

        CapaFiveWhys whys = entity.getFiveWhys();
        if (whys == null) {
            whys = new CapaFiveWhys();
            whys.setCapaAnalysis(entity);
            entity.setFiveWhys(whys);
        }
        whys.setWhy1(blankToNull(payload.why1));
        whys.setWhy2(blankToNull(payload.why2));
        whys.setWhy3(blankToNull(payload.why3));
        whys.setWhy4(blankToNull(payload.why4));
        whys.setWhy5(blankToNull(payload.why5));

        upsertAction(entity, CapaNatureAction.TYPE_CORRECTIVE, payload.corrective);
        upsertAction(entity, CapaNatureAction.TYPE_PREVENTIVE, payload.preventive);

        CapaAnalysis saved = capaAnalysisRepository.save(entity);
        return assemble(natureBlock, saved);
    }

    private Map<String, Object> requireNatureBlock(String nature) {
        Map<String, Object> block = rcaAnalysisService.findNatureBlock(nature);
        if (block == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No complaints found for this Nature of Complaint");
        }
        return block;
    }

    private Map<String, Object> assemble(Map<String, Object> natureBlock, CapaAnalysis stored) {
        Map<String, Object> response = new LinkedHashMap<>();
        String nature = String.valueOf(natureBlock.get(KEY_NATURE));
        response.put(KEY_NATURE, nature);
        response.put("complaintCount", natureBlock.get("complaintCount"));
        response.put("percentage", natureBlock.get("percentage"));
        response.put("complaintCategories", distinctCategories(natureBlock));
        response.put("complaintDescriptions", natureBlock.get("supportingComplaints"));

        CapaFiveWhys whys = stored == null ? null : stored.getFiveWhys();
        response.put("problemStatement", stored != null && stored.getProblemStatement() != null
                ? stored.getProblemStatement()
                : nature);
        response.put("why1", whys != null ? whys.getWhy1() : null);
        response.put("why2", whys != null ? whys.getWhy2() : null);
        response.put("why3", whys != null ? whys.getWhy3() : null);
        response.put("why4", whys != null ? whys.getWhy4() : null);
        response.put("why5", whys != null ? whys.getWhy5() : null);
        response.put("rootCause", stored != null ? stored.getRootCause() : null);
        response.put("corrective", actionMap(stored, CapaNatureAction.TYPE_CORRECTIVE));
        response.put("preventive", actionMap(stored, CapaNatureAction.TYPE_PREVENTIVE));
        response.put("updatedBy", stored != null ? stored.getUpdatedBy() : null);
        response.put("updatedAt", stored != null && stored.getUpdatedAt() != null
                ? stored.getUpdatedAt().toString()
                : null);
        return response;
    }

    private List<String> distinctCategories(Map<String, Object> natureBlock) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) natureBlock.get("supportingComplaints");
        if (rows == null) {
            return List.of();
        }
        Set<String> categories = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            Object category = row.get("category");
            if (category != null) {
                String text = String.valueOf(category).trim();
                if (!text.isEmpty() && !"-".equals(text) && !"unspecified".equalsIgnoreCase(text)) {
                    categories.add(text);
                }
            }
        }
        return new ArrayList<>(categories);
    }

    private Map<String, Object> actionMap(CapaAnalysis stored, String type) {
        Map<String, Object> map = new LinkedHashMap<>();
        CapaNatureAction action = findAction(stored, type);
        map.put(KEY_ACTION, action != null ? action.getActionText() : null);
        map.put(KEY_DEPARTMENT, action != null ? action.getResponsibleDepartment() : null);
        map.put(KEY_OFFICER, action != null ? action.getResponsibleOfficer() : null);
        return map;
    }

    private CapaNatureAction findAction(CapaAnalysis stored, String type) {
        if (stored == null || stored.getActions() == null) {
            return null;
        }
        return stored.getActions().stream()
                .filter(item -> type.equalsIgnoreCase(item.getActionType()))
                .findFirst()
                .orElse(null);
    }

    private void upsertAction(CapaAnalysis entity, String type, ActionPayload payload) {
        CapaNatureAction action = findAction(entity, type);
        if (action == null) {
            action = CapaNatureAction.builder()
                    .capaAnalysis(entity)
                    .actionType(type)
                    .build();
            if (entity.getActions() == null) {
                entity.setActions(new ArrayList<>());
            }
            entity.getActions().add(action);
        }
        ActionPayload safe = payload != null ? payload : new ActionPayload();
        action.setActionText(blankToNull(safe.action));
        action.setResponsibleDepartment(blankToNull(safe.responsibleDepartment));
        action.setResponsibleOfficer(blankToNull(safe.responsibleOfficer));
    }

    private static NatureCapaPayload fromBody(Map<String, Object> body) {
        NatureCapaPayload payload = new NatureCapaPayload();
        if (body == null) {
            return payload;
        }
        payload.nature = stringVal(body.get(KEY_NATURE));
        payload.problemStatement = stringVal(body.get("problemStatement"));
        payload.why1 = stringVal(body.get("why1"));
        payload.why2 = stringVal(body.get("why2"));
        payload.why3 = stringVal(body.get("why3"));
        payload.why4 = stringVal(body.get("why4"));
        payload.why5 = stringVal(body.get("why5"));
        payload.rootCause = stringVal(body.get("rootCause"));
        payload.corrective = actionFrom(body.get("corrective"));
        payload.preventive = actionFrom(body.get("preventive"));
        return payload;
    }

    @SuppressWarnings("unchecked")
    private static ActionPayload actionFrom(Object raw) {
        ActionPayload action = new ActionPayload();
        if (!(raw instanceof Map<?, ?> map)) {
            return action;
        }
        Map<String, Object> values = (Map<String, Object>) map;
        action.action = stringVal(values.get(KEY_ACTION));
        action.responsibleDepartment = stringVal(values.get(KEY_DEPARTMENT));
        action.responsibleOfficer = stringVal(values.get(KEY_OFFICER));
        return action;
    }

    private static String stringVal(Object value) {
        if (value == null || value instanceof Map || value instanceof List) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private static class NatureCapaPayload {
        private String nature;
        private String problemStatement;
        private String why1;
        private String why2;
        private String why3;
        private String why4;
        private String why5;
        private String rootCause;
        private ActionPayload corrective;
        private ActionPayload preventive;
    }

    private static class ActionPayload {
        private String action;
        private String responsibleDepartment;
        private String responsibleOfficer;
    }
}
