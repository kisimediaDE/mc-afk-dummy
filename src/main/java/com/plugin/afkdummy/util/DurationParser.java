package com.plugin.afkdummy.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** Strict, bounded duration input. No implicit units, signs, fractions or overflow. */
public final class DurationParser {
    private static final Pattern PART = Pattern.compile("(\\d+)([dhms])");
    private DurationParser() {}

    public static long parse(String input, long maximumMillis) {
        if (input == null || input.length() > 64 || maximumMillis <= 0) throw invalid();
        var matcher = PART.matcher(input.trim().toLowerCase(Locale.ROOT));
        String text = input.trim().toLowerCase(Locale.ROOT);
        long total = 0;
        int end = 0;
        int previousOrder = 5;
        try {
            while (matcher.find()) {
                if (matcher.start() != end) throw invalid();
                char unit = matcher.group(2).charAt(0);
                int order = switch (unit) { case 'd' -> 4; case 'h' -> 3; case 'm' -> 2; default -> 1; };
                if (order >= previousOrder) throw invalid();
                previousOrder = order;
                long multiplier = switch (unit) { case 'd' -> 86_400_000L; case 'h' -> 3_600_000L; case 'm' -> 60_000L; default -> 1_000L; };
                total = Math.addExact(total, Math.multiplyExact(Long.parseLong(matcher.group(1)), multiplier));
                if (total > maximumMillis) throw invalid();
                end = matcher.end();
            }
        } catch (ArithmeticException | NumberFormatException e) {
            throw invalid();
        }
        if (end != text.length() || total <= 0) throw invalid();
        return total;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Ungültige oder zu lange Dauer. Beispiele: 30m, 2h, 1h30m.");
    }
}
