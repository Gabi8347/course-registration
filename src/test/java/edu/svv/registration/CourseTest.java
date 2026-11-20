package edu.svv.registration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Course")
class CourseTest {

    private static Course course(String code, int ects, int capacity, DayOfWeek day, int start, int duration) {
        return new Course(code, ects, capacity, Set.of(), day, start, duration);
    }

    @Nested
    @DisplayName("FR-1 validation")
    class Validation {

        @Test
        @DisplayName("EP valid: course with valid data is created")
        void validCourse() {
            Course c = new Course("SWE301", 6, 40, Set.of("SWE201"), DayOfWeek.MONDAY, 9, 3);
            assertAll(
                    () -> assertEquals("SWE301", c.getCode()),
                    () -> assertEquals(6, c.getEcts()),
                    () -> assertEquals(40, c.getCapacity()),
                    () -> assertEquals(0, c.getEnrolled()),
                    () -> assertEquals(Set.of("SWE201"), c.getPrerequisites()),
                    () -> assertEquals(DayOfWeek.MONDAY, c.getDay()),
                    () -> assertEquals(9, c.getStartHour()),
                    () -> assertEquals(3, c.getDurationHours()),
                    () -> assertEquals(12, c.getEndHour()),
                    () -> assertTrue(c.hasFreeSeat()));
        }

        @ParameterizedTest(name = "valid code {0}")
        @ValueSource(strings = {"AB123", "SWE301", "ABCD999"})
        @DisplayName("EP/BVA: 2, 3 and 4 letters + 3 digits are valid codes")
        void validCodes(String code) {
            assertDoesNotThrow(() -> course(code, 5, 10, DayOfWeek.MONDAY, 9, 2));
        }

        @ParameterizedTest(name = "invalid code [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"A123", "ABCDE123", "SWE30", "SWE3011", "swe301", "Swe301",
                "SWE 301", " SWE301", "301SWE", "SWE30A", "   "})
        @DisplayName("EP/BVA/Negative: malformed codes throw")
        void invalidCodes(String code) {
            assertThrows(IllegalArgumentException.class,
                    () -> course(code, 5, 10, DayOfWeek.MONDAY, 9, 2));
        }

        @ParameterizedTest(name = "ECTS {0} valid={1}")
        @CsvSource({"0, false", "1, true", "2, true", "14, true", "15, true", "16, false", "-5, false"})
        @DisplayName("BVA: ECTS range [1, 15]")
        void ectsBoundaries(int ects, boolean valid) {
            assertValidity(valid, () -> course("SWE301", ects, 10, DayOfWeek.MONDAY, 9, 2));
        }

        @ParameterizedTest(name = "capacity {0} valid={1}")
        @CsvSource({"0, false", "1, true", "2, true", "499, true", "500, true", "501, false"})
        @DisplayName("BVA: capacity range [1, 500]")
        void capacityBoundaries(int capacity, boolean valid) {
            assertValidity(valid, () -> course("SWE301", 5, capacity, DayOfWeek.MONDAY, 9, 1));
        }

        @ParameterizedTest(name = "start {0} valid={1}")
        @CsvSource({"7, false", "8, true", "9, true", "18, true", "19, true", "20, false"})
        @DisplayName("BVA: start hour range [8, 19] (1-hour course)")
        void startHourBoundaries(int start, boolean valid) {
            assertValidity(valid, () -> course("SWE301", 5, 10, DayOfWeek.MONDAY, start, 1));
        }

        @ParameterizedTest(name = "duration {0} valid={1}")
        @CsvSource({"0, false", "1, true", "4, true", "5, false"})
        @DisplayName("BVA: duration range [1, 4]")
        void durationBoundaries(int duration, boolean valid) {
            assertValidity(valid, () -> course("SWE301", 5, 10, DayOfWeek.MONDAY, 9, duration));
        }

        @ParameterizedTest(name = "start {0} + {1}h valid={2}")
        @CsvSource({"16, 4, true", "17, 3, true", "17, 4, false", "19, 1, true", "19, 2, false"})
        @DisplayName("BVA: course must end by 20:00 (end = 19, 20, 21)")
        void endOfDayBoundary(int start, int duration, boolean valid) {
            assertValidity(valid, () -> course("SWE301", 5, 10, DayOfWeek.MONDAY, start, duration));
        }

        @ParameterizedTest(name = "weekday {0}")
        @EnumSource(value = DayOfWeek.class, names = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"})
        @DisplayName("EP valid: Monday to Friday")
        void weekdaysValid(DayOfWeek day) {
            assertDoesNotThrow(() -> course("SWE301", 5, 10, day, 9, 2));
        }

        @ParameterizedTest(name = "weekend {0}")
        @EnumSource(value = DayOfWeek.class, names = {"SATURDAY", "SUNDAY"})
        @DisplayName("EP invalid: weekend days throw")
        void weekendInvalid(DayOfWeek day) {
            assertThrows(IllegalArgumentException.class, () -> course("SWE301", 5, 10, day, 9, 2));
        }

        @Test
        @DisplayName("Negative: null day throws")
        void nullDay() {
            assertThrows(IllegalArgumentException.class, () -> course("SWE301", 5, 10, null, 9, 2));
        }

        @Test
        @DisplayName("Negative: null prerequisite set throws")
        void nullPrerequisites() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Course("SWE301", 5, 10, null, DayOfWeek.MONDAY, 9, 2));
        }

        @Test
        @DisplayName("Negative: malformed or null prerequisite code throws")
        void invalidPrerequisiteCode() {
            Set<String> withNull = new HashSet<>();
            withNull.add(null);
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Course("SWE301", 5, 10, Set.of("swe201"), DayOfWeek.MONDAY, 9, 2)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Course("SWE301", 5, 10, withNull, DayOfWeek.MONDAY, 9, 2)));
        }

        private void assertValidity(boolean valid, org.junit.jupiter.api.function.Executable creation) {
            if (valid) {
                assertDoesNotThrow(creation);
            } else {
                assertThrows(IllegalArgumentException.class, creation);
            }
        }
    }

    @Nested
    @DisplayName("FR-8 time overlap")
    class Overlap {

        @ParameterizedTest(name = "{0}: MON 10-12 vs {1} {2}+{3}h -> overlap={4}")
        @CsvSource({
                "adjacent after (end == start),   MONDAY,    12, 1, false",
                "adjacent before (end == start),  MONDAY,     9, 1, false",
                "overlap by 1 hour at the end,    MONDAY,    11, 2, true",
                "overlap by 1 hour at the start,  MONDAY,     9, 2, true",
                "identical slot,                  MONDAY,    10, 2, true",
                "fully inside,                    MONDAY,    11, 1, true",
                "fully covering,                  MONDAY,     9, 4, true",
                "same hours on another day,       TUESDAY,   10, 2, false",
                "far apart on the same day,       MONDAY,    15, 2, false"
        })
        @DisplayName("BVA/EP: overlap is symmetric and adjacent slots do not conflict")
        void overlaps(String label, DayOfWeek day, int start, int duration, boolean expected) {
            Course base = course("SWE301", 5, 10, DayOfWeek.MONDAY, 10, 2);
            Course other = course("SWE302", 5, 10, day, start, duration);
            assertAll(
                    () -> assertEquals(expected, base.overlaps(other), "base.overlaps(other)"),
                    () -> assertEquals(expected, other.overlaps(base), "other.overlaps(base)"));
        }
    }
}
