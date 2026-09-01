package com.dashenbank.cms.util;

import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ascending order for DBC/FCR/CM ticket numbers (sequence, then fiscal year).
 */
public final class TicketNumberSort {

    private static final Pattern TICKET = Pattern.compile(
            "^(DBC|FCR|CM)-(\\d+)(?:/(\\d{4})-(\\d{2}))?$", Pattern.CASE_INSENSITIVE);

    private TicketNumberSort() {
    }

    public static final Comparator<String> ASC = TicketNumberSort::compare;

    public static int compare(String left, String right) {
        Parsed a = parse(left);
        Parsed b = parse(right);
        int byPrefix = Integer.compare(a.prefixRank, b.prefixRank);
        if (byPrefix != 0) {
            return byPrefix;
        }
        int byYear = Integer.compare(a.year, b.year);
        if (byYear != 0) {
            return byYear;
        }
        int bySeq = Integer.compare(a.sequence, b.sequence);
        if (bySeq != 0) {
            return bySeq;
        }
        return a.raw.compareToIgnoreCase(b.raw);
    }

    private static Parsed parse(String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty() || "-".equals(raw)) {
            return new Parsed(99, Integer.MAX_VALUE, Integer.MAX_VALUE, raw);
        }
        Matcher matcher = TICKET.matcher(raw);
        if (!matcher.matches()) {
            return new Parsed(50, Integer.MAX_VALUE, Integer.MAX_VALUE, raw);
        }
        String prefix = matcher.group(1).toUpperCase(Locale.ROOT);
        int prefixRank = switch (prefix) {
            case "DBC" -> 0;
            case "FCR" -> 1;
            case "CM" -> 2;
            default -> 50;
        };
        int sequence = Integer.parseInt(matcher.group(2));
        int year = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        return new Parsed(prefixRank, year, sequence, raw);
    }

    private record Parsed(int prefixRank, int year, int sequence, String raw) {
    }
}
