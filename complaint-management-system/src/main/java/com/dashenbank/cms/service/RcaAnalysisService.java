package com.dashenbank.cms.service;

import com.dashenbank.cms.model.ComplainantRelatedInformation;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregated Root Cause Analysis by CRI Nature of Complaint.
 * Does not create per-ticket RCA records.
 */
@Service
public class RcaAnalysisService {

    private static final String KEY_COMPLAINT_COUNT = "complaintCount";
    private static final String KEY_NATURE = "nature";
    private static final String KEY_NATURES = "natures";
    private static final String KEY_STATUSES = "statuses";
    private static final String KEY_PERCENTAGE = "percentage";
    private static final String KEY_ANALYSIS = "analysis";
    private static final String KEY_THEME = "theme";
    private static final String STATUS_ESCALATED = "ESCALATED";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_ON_TRACK = "ON_TRACK";
    private static final String STATUS_RECORDED = "RECORDED";

    private static final List<ThemeRule> THEME_RULES = List.of(
            new ThemeRule("ATM / card transaction problems", "atm", "card", "pos", "pin", "withdrawal", "debit"),
            new ThemeRule("Unsuccessful or pending transactions", "transaction", "transfer", "failed", "unsuccessful",
                    "pending", "reversal"),
            new ThemeRule("Service delay", "delay", "late", "waiting", "slow", "queue"),
            new ThemeRule("Information or disclosure issues", "information", "disclosure", "privacy", "statement",
                    "data"),
            new ThemeRule("Credit / financing issues", "loan", "credit", "collateral", "interest", "facility"),
            new ThemeRule("Digital channel or system issues", "app", "mobile", "login", "otp", "system", "super app"),
            new ThemeRule("Process or policy issues", "process", "procedure", "policy", "documentation"),
            new ThemeRule("Staff or branch service issues", "staff", "teller", "rude", "service", "branch"));

    private final ComplainantRelatedInformationService criService;

    public RcaAnalysisService(ComplainantRelatedInformationService criService) {
        this.criService = criService;
    }

    public Map<String, Object> analyze(RcaFilter filter) {
        List<ComplainantRelatedInformation> allClassified = criService.listClassifiedComplaints();
        RcaFilter safe = filter != null ? filter : new RcaFilter();
        List<ComplainantRelatedInformation> records = allClassified.stream()
                .filter(row -> matches(row, safe))
                .collect(Collectors.toMap(ComplainantRelatedInformation::getUniqueIdNo, r -> r, (a, b) -> a,
                        LinkedHashMap::new))
                .values().stream().toList();
        int total = records.size();
        List<ComplainantRelatedInformation> recordsWithNature = records.stream()
                .filter(this::hasNature)
                .toList();

        Map<String, List<ComplainantRelatedInformation>> grouped = recordsWithNature.stream()
                .collect(Collectors.groupingBy(this::natureKey, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> natures = new ArrayList<>();
        for (Map.Entry<String, List<ComplainantRelatedInformation>> entry : grouped.entrySet()) {
            natures.add(buildNatureBlock(entry.getKey(), entry.getValue(), total));
        }
        natures.sort((a, b) -> Integer.compare(
                (Integer) b.get(KEY_COMPLAINT_COUNT), (Integer) a.get(KEY_COMPLAINT_COUNT)));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalComplaints", total);
        response.put("totalNatures", natures.size());
        response.put("mostCommonNature", natures.isEmpty() ? null : natures.get(0).get(KEY_NATURE));
        response.put("highestVolumeNature", natures.isEmpty() ? null : natures.get(0).get(KEY_NATURE));
        response.put("highestEscalationResolutionNature", highestRateNature(natures));
        response.put(KEY_NATURES, natures);
        response.put("filterOptions", filterOptions(records, allClassified));
        return response;
    }

    public String exportCsv(RcaFilter filter) {
        Map<String, Object> analysis = analyze(filter);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> natures = (List<Map<String, Object>>) analysis.get(KEY_NATURES);
        StringBuilder csv = new StringBuilder();
        csv.append("Sheet 1 - Summary\n");
        csv.append("Nature of Complaint,Complaint Count,Percentage,Escalated,Resolved,Closed,Declined,On Track,Recorded\n");
        for (Map<String, Object> nature : natures) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> statuses = (Map<String, Integer>) nature.get(KEY_STATUSES);
            csv.append(csvCell(nature.get(KEY_NATURE))).append(',')
                    .append(nature.get(KEY_COMPLAINT_COUNT)).append(',')
                    .append(nature.get(KEY_PERCENTAGE)).append(',')
                    .append(statuses.getOrDefault(STATUS_ESCALATED, 0)).append(',')
                    .append(statuses.getOrDefault(STATUS_RESOLVED, 0)).append(',')
                    .append(statuses.getOrDefault(STATUS_CLOSED, 0)).append(',')
                    .append(statuses.getOrDefault("DECLINED", 0)).append(',')
                    .append(statuses.getOrDefault(STATUS_ON_TRACK, 0)).append(',')
                    .append(statuses.getOrDefault(STATUS_RECORDED, 0)).append('\n');
        }

        csv.append("\nSheet 2 - Root Cause Analysis\n");
        csv.append("Nature of Complaint,Observed Cause/Theme,Complaint Count,Percentage,Analysis\n");
        for (Map<String, Object> nature : natures) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> themes = (List<Map<String, Object>>) nature.get("themes");
            if (themes == null || themes.isEmpty()) {
                csv.append(csvCell(nature.get(KEY_NATURE))).append(",,,," )
                        .append(csvCell(nature.get(KEY_ANALYSIS))).append('\n');
                continue;
            }
            for (Map<String, Object> theme : themes) {
                csv.append(csvCell(nature.get(KEY_NATURE))).append(',')
                        .append(csvCell(theme.get(KEY_THEME))).append(',')
                        .append(theme.get("count")).append(',')
                        .append(theme.get(KEY_PERCENTAGE)).append(',')
                        .append(csvCell(nature.get(KEY_ANALYSIS))).append('\n');
            }
        }

        csv.append("\nSheet 3 - Supporting Complaints\n");
        csv.append("Unique ID,Nature,Category,Description,Status,Channel,Branch,District,Department\n");
        for (ComplainantRelatedInformation row : filteredRecords(filter)) {
            csv.append(csvCell(row.getUniqueIdNo())).append(',')
                    .append(csvCell(natureKey(row))).append(',')
                    .append(csvCell(row.getComplaintsCategory())).append(',')
                    .append(csvCell(row.getDetailsOfComplaint())).append(',')
                    .append(csvCell(row.getCaseStatus())).append(',')
                    .append(csvCell(row.getComplaintMadeOnChannel())).append(',')
                    .append(csvCell(row.getCaseForwardedTo())).append(',')
                    .append(csvCell(row.getDistrictDepartment())).append(',')
                    .append(csvCell(row.getCaseAssignedTo())).append('\n');
        }
        return csv.toString();
    }

    private List<ComplainantRelatedInformation> filteredRecords(RcaFilter filter) {
        RcaFilter safe = filter != null ? filter : new RcaFilter();
        return criService.listClassifiedComplaints().stream()
                .filter(row -> matches(row, safe))
                .collect(Collectors.toMap(ComplainantRelatedInformation::getUniqueIdNo, r -> r, (a, b) -> a,
                        LinkedHashMap::new))
                .values().stream().toList();
    }

    private boolean matches(ComplainantRelatedInformation row, RcaFilter filter) {
        if (!equalsOrAll(filter.nature, natureKey(row))) {
            return false;
        }
        if (!equalsOrAll(filter.category, row.getComplaintsCategory())) {
            return false;
        }
        if (!equalsOrAll(filter.status, row.getCaseStatus())) {
            return false;
        }
        if (!equalsOrAll(filter.channel, row.getComplaintMadeOnChannel())) {
            return false;
        }
        if (!equalsOrAll(filter.branch, row.getCaseForwardedTo())) {
            return false;
        }
        if (!equalsOrAll(filter.district, row.getDistrictDepartment())) {
            return false;
        }
        if (!equalsOrAll(filter.department, row.getCaseAssignedTo())) {
            return false;
        }
        LocalDate complaintDate = toDate(row.getDateOfComplaint());
        if (filter.fromDate != null && (complaintDate == null || complaintDate.isBefore(filter.fromDate))) {
            return false;
        }
        return filter.toDate == null || (complaintDate != null && !complaintDate.isAfter(filter.toDate));
    }

    private Map<String, Object> buildNatureBlock(String nature, List<ComplainantRelatedInformation> rows, int total) {
        int count = rows.size();
        Map<String, Integer> statuses = emptyStatusMap();
        for (ComplainantRelatedInformation row : rows) {
            String status = normalizeStatus(row.getCaseStatus());
            statuses.put(status, statuses.getOrDefault(status, 0) + 1);
        }

        Map<String, Integer> themeCounts = new LinkedHashMap<>();
        int described = 0;
        for (ComplainantRelatedInformation row : rows) {
            String theme = classifyTheme(row);
            if (theme != null) {
                described++;
                themeCounts.merge(theme, 1, Integer::sum);
            }
        }

        List<Map<String, Object>> themes = new ArrayList<>();
        int themeBase = Math.max(described, 1);
        themeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    Map<String, Object> theme = new LinkedHashMap<>();
                    theme.put(KEY_THEME, entry.getKey());
                    theme.put("count", entry.getValue());
                    theme.put(KEY_PERCENTAGE, percent(entry.getValue(), themeBase));
                    themes.add(theme);
                });

        Map<String, Object> block = new LinkedHashMap<>();
        block.put(KEY_NATURE, nature);
        block.put(KEY_COMPLAINT_COUNT, count);
        block.put(KEY_PERCENTAGE, percent(count, total));
        block.put(KEY_STATUSES, statuses);
        block.put("themes", themes);
        block.put(KEY_ANALYSIS, buildAnalysis(nature, count, total, statuses, themes, described));
        block.put("commonPattern", buildPattern(themes, described, count));
        block.put("operationalObservation", buildObservation(nature, count, statuses));
        block.put("supportingComplaints", rows.stream().map(this::toSupportRow).toList());
        return block;
    }

    private Map<String, Object> toSupportRow(ComplainantRelatedInformation row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uniqueIdNo", row.getUniqueIdNo());
        map.put(KEY_NATURE, blankToDash(row.getNatureOfComplaints()));
        map.put("category", blankToDash(row.getComplaintsCategory()));
        map.put("description", blankToDash(row.getDetailsOfComplaint()));
        map.put("status", blankToDash(row.getCaseStatus()));
        map.put("channel", blankToDash(row.getComplaintMadeOnChannel()));
        map.put("branch", blankToDash(row.getCaseForwardedTo()));
        map.put("district", blankToDash(row.getDistrictDepartment()));
        map.put("department", blankToDash(row.getCaseAssignedTo()));
        map.put("priority", blankToDash(row.getComplaintClassification()));
        map.put("remarks", blankToDash(row.getRemarkAndSpecialNote()));
        return map;
    }

    private String classifyTheme(ComplainantRelatedInformation row) {
        String blob = (safe(row.getDetailsOfComplaint()) + " " + safe(row.getComplaintsCategory()) + " "
                + safe(row.getNatureOfComplaints()) + " " + safe(row.getRemarkAndSpecialNote()))
                .toLowerCase(Locale.ROOT);
        if (blob.isBlank() || "-".equals(blob.trim())) {
            return null;
        }
        for (ThemeRule rule : THEME_RULES) {
            for (String keyword : rule.keywords) {
                if (blob.contains(keyword)) {
                    return rule.theme;
                }
            }
        }
        return "Other described issue";
    }

    private String buildAnalysis(String nature, int count, int total, Map<String, Integer> statuses,
            List<Map<String, Object>> themes, int described) {
        if (count == 1) {
            return "Only one complaint is currently available for this Nature of Complaint. "
                    + "A recurring root cause cannot be reliably identified.";
        }
        if (described == 0) {
            return "Insufficient complaint information to identify a recurring root cause for this Nature of Complaint.";
        }
        String topTheme = String.valueOf(themes.get(0).get(KEY_THEME));
        int escalated = statuses.getOrDefault(STATUS_ESCALATED, 0);
        StringBuilder text = new StringBuilder();
        text.append(nature).append(" represents ").append(percent(count, total))
                .append("% of classified complaints. The most frequent observed theme is ")
                .append(topTheme.toLowerCase(Locale.ROOT)).append('.');
        if (escalated > count / 2) {
            text.append(" Most complaints in this group were escalated, indicating that the issue frequently requires additional investigation or intervention.");
        } else {
            text.append(" The available complaint descriptions indicate a concentrated pattern around this theme.");
        }
        return text.toString();
    }

    private String buildPattern(List<Map<String, Object>> themes, int described, int count) {
        if (count == 1) {
            return "A recurring pattern cannot be identified from a single complaint.";
        }
        if (described == 0 || themes.isEmpty()) {
            return "Insufficient complaint information to identify a recurring root cause for this Nature of Complaint.";
        }
        return "Complaints are concentrated around " + String.valueOf(themes.get(0).get(KEY_THEME)).toLowerCase(Locale.ROOT)
                + ".";
    }

    private String buildObservation(String nature, int count, Map<String, Integer> statuses) {
        int escalated = statuses.getOrDefault(STATUS_ESCALATED, 0);
        int resolved = statuses.getOrDefault(STATUS_RESOLVED, 0) + statuses.getOrDefault(STATUS_CLOSED, 0);
        return nature + " currently has " + count + " classified complaint(s), of which " + escalated
                + " are escalated and " + resolved + " are resolved or closed.";
    }

    private String highestRateNature(List<Map<String, Object>> natures) {
        String best = null;
        double bestRate = -1;
        for (Map<String, Object> nature : natures) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> statuses = (Map<String, Integer>) nature.get(KEY_STATUSES);
            int count = (Integer) nature.get(KEY_COMPLAINT_COUNT);
            if (count <= 0) {
                continue;
            }
            int rateNumerator = statuses.getOrDefault(STATUS_ESCALATED, 0) + statuses.getOrDefault(STATUS_RESOLVED, 0);
            double rate = (double) rateNumerator / count;
            if (rate > bestRate) {
                bestRate = rate;
                best = String.valueOf(nature.get(KEY_NATURE));
            }
        }
        return best;
    }

    private Map<String, Object> filterOptions(List<ComplainantRelatedInformation> filtered,
            List<ComplainantRelatedInformation> allClassified) {
        List<ComplainantRelatedInformation> source = allClassified != null ? allClassified : filtered;
        Map<String, Object> options = new LinkedHashMap<>();
        options.put(KEY_NATURES, distinct(source, this::natureKey));
        options.put("categories", distinct(source, ComplainantRelatedInformation::getComplaintsCategory));
        options.put(KEY_STATUSES, distinct(source, ComplainantRelatedInformation::getCaseStatus));
        options.put("channels", distinct(source, ComplainantRelatedInformation::getComplaintMadeOnChannel));
        options.put("branches", distinct(source, ComplainantRelatedInformation::getCaseForwardedTo));
        options.put("districts", distinct(source, ComplainantRelatedInformation::getDistrictDepartment));
        options.put("departments", distinct(source, ComplainantRelatedInformation::getCaseAssignedTo));
        return options;
    }

    private List<String> distinct(List<ComplainantRelatedInformation> rows,
            java.util.function.Function<ComplainantRelatedInformation, String> getter) {
        return rows.stream()
                .map(getter)
                .filter(value -> value != null && !value.isBlank())
                .map(RcaAnalysisService::blankToDash)
                .filter(value -> value != null && !"-".equals(value) && !"Unspecified".equalsIgnoreCase(value))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private boolean hasNature(ComplainantRelatedInformation row) {
        return natureKey(row) != null;
    }

    private String natureKey(ComplainantRelatedInformation row) {
        String nature = safe(row.getNatureOfComplaints());
        if (nature.isBlank() || "-".equals(nature) || "null".equalsIgnoreCase(nature)
                || "unspecified".equalsIgnoreCase(nature)) {
            return null;
        }
        return nature;
    }

    private static Map<String, Integer> emptyStatusMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(STATUS_RECORDED, 0);
        map.put(STATUS_ON_TRACK, 0);
        map.put(STATUS_ESCALATED, 0);
        map.put(STATUS_RESOLVED, 0);
        map.put(STATUS_CLOSED, 0);
        map.put("DECLINED", 0);
        return map;
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_RECORDED;
        }
        String upper = status.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (upper) {
            case "ONTRACK" -> STATUS_ON_TRACK;
            default -> upper;
        };
    }

    private static boolean equalsOrAll(String expected, String actual) {
        if (expected == null || expected.isBlank() || "ALL".equalsIgnoreCase(expected)) {
            return true;
        }
        return expected.equalsIgnoreCase(safe(actual));
    }

    private static LocalDate toDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    private static double percent(int part, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(part * 1000.0 / total) / 10.0;
    }

    private static String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToDash(String value) {
        String text = safe(value);
        return text.isEmpty() ? "-" : text;
    }

    public static class RcaFilter {
        public String nature;
        public String category;
        public String status;
        public String channel;
        public String branch;
        public String district;
        public String department;
        public LocalDate fromDate;
        public LocalDate toDate;
    }

    private record ThemeRule(String theme, String... keywords) {
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ThemeRule that)) {
                return false;
            }
            return Objects.equals(theme, that.theme) && Arrays.equals(keywords, that.keywords);
        }

        @Override
        public int hashCode() {
            return Objects.hash(theme, Arrays.hashCode(keywords));
        }

        @Override
        public String toString() {
            return "ThemeRule[theme=" + theme + ", keywords=" + Arrays.toString(keywords) + "]";
        }
    }
}
