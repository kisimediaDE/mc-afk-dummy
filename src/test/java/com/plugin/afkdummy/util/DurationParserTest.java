package com.plugin.afkdummy.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class DurationParserTest {
    private static final long DAY = 86_400_000L;
    @Test void acceptsMenuAndCompoundDurations() {
        assertEquals(1_800_000, DurationParser.parse("30m", DAY));
        assertEquals(5_400_000, DurationParser.parse("1h30m", DAY));
        assertEquals(DAY, DurationParser.parse("24H", DAY));
        assertEquals(1_000, DurationParser.parse("1s", DAY));
    }
    @ParameterizedTest
    @ValueSource(strings={"", "0m", "-1h", "1.5h", "90", "1h garbage", "25h", "1h1h", "30m1h", "1h 30m", "99999999999999999999d"})
    void rejectsInvalidOrOverLimit(String text) {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse(text, DAY));
    }
}
