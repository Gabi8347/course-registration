package edu.svv.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("FR-1 Student validation")
class StudentTest {

    @Test
    @DisplayName("EP valid: student with valid data is created")
    void validStudent() {
        Student s = new Student("2021001", 2.5, true, false, Set.of("SWE101"));

        assertEquals("2021001", s.getId());
        assertEquals(2.5, s.getGpa());
        assertTrue(s.isFeesPaid());
        assertFalse(s.isAdvisorApproved());
        assertEquals(Set.of("SWE101"), s.getCompletedCourses());
        assertEquals(0, s.getRegisteredEcts());
    }

    @Test
    @DisplayName("Regression DEF-01: immutable Set.of for completed courses is accepted")
    void validStudentWithImmutableSet() {
        Student s = new Student("2021001", 2.45, true, true, Set.of("SWE101", "MAT101"));
        assertEquals(2, s.getCompletedCourses().size());
    }

    @ParameterizedTest(name = "BVA valid GPA {0}")
    @ValueSource(doubles = {0.00, 0.01, 3.99, 4.00})
    @DisplayName("BVA: GPA on and just inside [0.00, 4.00] is accepted")
    void gpaBoundariesValid(double gpa) {
        Student s = new Student("1", gpa, true, true, Set.of());
        assertEquals(gpa, s.getGpa());
    }

    @ParameterizedTest(name = "Negative GPA {0}")
    @ValueSource(doubles = {-0.01, 4.01, Double.NaN})
    @DisplayName("BVA/Negative: GPA just outside the range or NaN throws")
    void gpaBoundariesInvalid(double gpa) {
        assertThrows(IllegalArgumentException.class, () -> new Student("1", gpa, true, true, Set.of()));
    }

    @ParameterizedTest(name = "Negative id [{0}]")
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("Negative: empty or blank id throws")
    void blankId(String id) {
        assertThrows(IllegalArgumentException.class, () -> new Student(id, 2.0, true, true, Set.of()));
    }

    @Test
    @DisplayName("Negative: null id throws")
    void nullId() {
        assertThrows(IllegalArgumentException.class, () -> new Student(null, 2.0, true, true, Set.of()));
    }

    @Test
    @DisplayName("Negative: null completed-courses set throws")
    void nullCompletedCourses() {
        assertThrows(IllegalArgumentException.class, () -> new Student("1", 2.0, true, true, null));
    }

    @Test
    @DisplayName("Negative: null element in completed courses throws IllegalArgumentException (not NPE)")
    void nullElementInCompletedCourses() {
        Set<String> withNull = new HashSet<>();
        withNull.add("SWE101");
        withNull.add(null);

        assertThrows(IllegalArgumentException.class, () -> new Student("1", 2.0, true, true, withNull));
    }

    @Test
    @DisplayName("Completed courses are copied: later changes to the caller's set have no effect")
    void completedCoursesAreDefensivelyCopied() {
        Set<String> input = new HashSet<>();
        input.add("SWE101");
        Student s = new Student("1", 2.0, true, true, input);

        input.add("SWE201");

        assertEquals(Set.of("SWE101"), s.getCompletedCourses());
    }
}
