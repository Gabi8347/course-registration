package edu.svv.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * FR-1 validation and FR-8 time overlap of Course.
 * For invalid input, each test also checks that the exception message names the violated rule,
 * so a test cannot pass because a different rule rejected the input (TD-01, found by PIT).
 */
@DisplayName("Course")
class CourseTest {

    /** Helper: a course with no prerequisites. */
    private static Course course(String code, int ects, int capacity, DayOfWeek day, int start, int duration) {
        return new Course(code, ects, capacity, Set.of(), day, start, duration);
    }

    // ---------- FR-1: valid course ----------

    @Test
    @DisplayName("EP valid: course with valid data is created")
    void validCourse() {
        Course c = new Course("SWE301", 6, 40, Set.of("SWE201"), DayOfWeek.MONDAY, 9, 3);

        assertEquals("SWE301", c.getCode());
        assertEquals(6, c.getEcts());
        assertEquals(40, c.getCapacity());
        assertEquals(0, c.getEnrolled());
        assertEquals(Set.of("SWE201"), c.getPrerequisites());
        assertEquals(DayOfWeek.MONDAY, c.getDay());
        assertEquals(9, c.getStartHour());
        assertEquals(3, c.getDurationHours());
        assertEquals(12, c.getEndHour());
        assertTrue(c.hasFreeSeat());
        assertEquals("SWE301 (6 ECTS, MONDAY 9:00-12:00)", c.toString());
    }

    // ---------- FR-1: course code (EP + BVA) ----------

    @ParameterizedTest(name = "valid code {0}")
    @ValueSource(strings = {"AB123", "SWE301", "ABCD999"})
    @DisplayName("EP/BVA: 2, 3 and 4 letters + 3 digits are valid codes")
    void validCodes(String code) {
        Course c = course(code, 5, 10, DayOfWeek.MONDAY, 9, 2);
        assertEquals(code, c.getCode());
    }

    @ParameterizedTest(name = "invalid code [{0}]")
    @ValueSource(strings = {"", "A123", "ABCDE123", "SWE30", "SWE3011", "swe301", "Swe301",
            "SWE 301", " SWE301", "301SWE", "SWE30A", "   "})
    @DisplayName("EP/BVA/Negative: malformed codes throw")
    void invalidCodes(String code) {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> course(code, 5, 10, DayOfWeek.MONDAY, 9, 2));
        assertTrue(e.getMessage().contains("course code"));
    }

    @Test
    @DisplayName("Negative: null code throws")
    void nullCode() {
        assertThrows(IllegalArgumentException.class, () -> course(null, 5, 10, DayOfWeek.MONDAY, 9, 2));
    }

    // ---------- FR-1: ECTS [1, 15] (BVA) ----------

    @ParameterizedTest(name = "valid ECTS {0}")
    @ValueSource(ints = {1, 2, 14, 15})
    @DisplayName("BVA: ECTS on and inside [1, 15] is accepted")
    void validEcts(int ects) {
        Course c = course("SWE301", ects, 10, DayOfWeek.MONDAY, 9, 2);
        assertEquals(ects, c.getEcts());
    }

    @ParameterizedTest(name = "invalid ECTS {0}")
    @ValueSource(ints = {0, 16, -5})
    @DisplayName("BVA/Negative: ECTS outside [1, 15] throws")
    void invalidEcts(int ects) {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> course("SWE301", ects, 10, DayOfWeek.MONDAY, 9, 2));
        assertTrue(e.getMessage().contains("ECTS"));
    }

    // ---------- FR-1: capacity [1, 500] (BVA) ----------

    @ParameterizedTest(name = "valid capacity {0}")
    @ValueSource(ints = {1, 2, 499, 500})
    @DisplayName("BVA: capacity on and inside [1, 500] is accepted")
    void validCapacity(int capacity) {
        Course c = course("SWE301", 5, capacity, DayOfWeek.MONDAY, 9, 1);
        assertEquals(capacity, c.getCapacity());
    }

    @ParameterizedTest(name = "invalid capacity {0}")
    @ValueSource(ints = {0, 501})
    @DisplayName("BVA/Negative: capacity outside [1, 500] throws")
    void invalidCapacity(int capacity) {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> course("SWE301", 5, capacity, DayOfWeek.MONDAY, 9, 1));
        assertTrue(e.getMessage().contains("Capacity"));
    }

    // ---------- FR-1: start hour [8, 19] (BVA, 1-hour course) ----------

    @ParameterizedTest(name = "valid start {0}")
    @ValueSource(ints = {8, 9, 18, 19})
    @DisplayName("BVA: start hour on and inside [8, 19] is accepted")
    void validStartHour(int start) {
        Course c = course("SWE301", 5, 10, DayOfWeek.MONDAY, start, 1);
        assertEquals(start, c.getStartHour());
    }

    @ParameterizedTest(name = "invalid start {0}")
    @ValueSource(ints = {7, 20})
    @DisplayName("BVA/Negative: start hour outside [8, 19] throws")
    void invalidStartHour(int start) {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> course("SWE301", 5, 10, DayOfWeek.MONDAY, start, 1));
        assertTrue(e.getMessage().contains("Start hour"));   // TD-01: not the end-of-day rule
    }

    // ---------- FR-1: duration [1, 4] (BVA) ----------

    @ParameterizedTest(name = "valid duration {0}")
    @ValueSource(ints = {1, 4})
    @DisplayName("BVA: duration 1 and 4 hours is accepted")
    void validDuration(int duration) {
        Course c = course("SWE301", 5, 10, DayOfWeek.MONDAY, 9, duration);
        assertEquals(duration, c.getDurationHours());
    }

    @ParameterizedTest(name = "invalid duration {0}")
    @ValueSource(ints = {0, 5})
    @DisplayName("BVA/Negative: duration outside [1, 4] throws")
    void invalidDuration(int duration) {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> course("SWE301", 5, 10, DayOfWeek.MONDAY, 9, duration));
        assertTrue(e.getMessage().contains("Duration"));
    }

    // ---------- FR-1: course must end by 20:00 (BVA: end = 19, 20, 21) ----------

    @ParameterizedTest(name = "start {0} + {1}h ends {2}:00")
    @CsvSource({"15, 4, 19", "16, 4, 20", "17, 3, 20", "19, 1, 20"})
    @DisplayName("BVA: course ending at 19:00 or 20:00 is accepted")
    void endOfDayValid(int start, int duration, int end) {
        Course c = course("SWE301", 5, 10, DayOfWeek.MONDAY, start, duration);
        assertEquals(end, c.getEndHour());
    }

    @ParameterizedTest(name = "start {0} + {1}h ends 21:00")
    @CsvSource({"17, 4", "19, 2"})
    @DisplayName("BVA/Negative: course ending at 21:00 throws")
    void endOfDayInvalid(int start, int duration) {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> course("SWE301", 5, 10, DayOfWeek.MONDAY, start, duration));
        assertTrue(e.getMessage().contains("end by 20:00"));
    }

    // ---------- FR-1: day (EP) ----------

    @ParameterizedTest(name = "weekday {0}")
    @ValueSource(strings = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"})
    @DisplayName("EP valid: Monday to Friday")
    void weekdaysValid(DayOfWeek day) {
        Course c = course("SWE301", 5, 10, day, 9, 2);
        assertEquals(day, c.getDay());
    }

    @ParameterizedTest(name = "weekend {0}")
    @ValueSource(strings = {"SATURDAY", "SUNDAY"})
    @DisplayName("EP invalid: weekend days throw")
    void weekendInvalid(DayOfWeek day) {
        assertThrows(IllegalArgumentException.class, () -> course("SWE301", 5, 10, day, 9, 2));
    }

    @Test
    @DisplayName("Negative: null day throws")
    void nullDay() {
        assertThrows(IllegalArgumentException.class, () -> course("SWE301", 5, 10, null, 9, 2));
    }

    // ---------- FR-1: prerequisites (negative) ----------

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

        assertThrows(IllegalArgumentException.class,
                () -> new Course("SWE301", 5, 10, Set.of("swe201"), DayOfWeek.MONDAY, 9, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new Course("SWE301", 5, 10, withNull, DayOfWeek.MONDAY, 9, 2));
    }

    @Test
    @DisplayName("DEF-03 Negative: a course cannot be its own prerequisite")
    void selfPrerequisite() {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> new Course("SWE301", 5, 10, Set.of("SWE201", "SWE301"), DayOfWeek.MONDAY, 9, 2));
        assertTrue(e.getMessage().contains("own prerequisite"));
    }

    // ---------- FR-8: time overlap (BVA / EP) ----------

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

        assertEquals(expected, base.overlaps(other));
        assertEquals(expected, other.overlaps(base));   // same answer in both directions
    }
}
