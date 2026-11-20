package edu.svv.registration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("RegistrationService")
class RegistrationServiceTest {

    private RegistrationService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(); // fresh fixture for every test
    }

    // ---------- test data helpers ----------

    private static Student eligibleStudent(double gpa) {
        return new Student("2021001", gpa, true, true, Set.of("SWE101"));
    }

    private static Course course(String code, int ects, int capacity, DayOfWeek day, int start) {
        return new Course(code, ects, capacity, Set.of(), day, start, 2);
    }

    /** Registers filler courses (max 15 ECTS each, Mon-Thu 08:00) until the student has {@code ects}. */
    private void fillEcts(Student student, int ects) {
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY};
        int remaining = ects;
        int i = 0;
        while (remaining > 0) {
            int chunk = Math.min(15, remaining);
            Course filler = new Course("FIL10" + i, chunk, 100, Set.of(), days[i], 8, 1);
            assertTrue(service.register(student, filler).isRegistered(), "filler must register");
            remaining -= chunk;
            i++;
        }
        assertEquals(ects, student.getRegisteredEcts());
    }

    private static void assertRejected(RegistrationResult result, RejectionReason reason) {
        assertAll(
                () -> assertEquals(RegistrationResult.Status.REJECTED, result.getStatus()),
                () -> assertEquals(reason, result.getReason()));
    }

    // ---------- Decision table ----------

    @Nested
    @DisplayName("Decision table: fees x prerequisites x advisor x seat (FR-4..FR-7, FR-10)")
    class DecisionTable {

        @ParameterizedTest(name = "{0}: fees={1} prereq={2} advisor={3} seat={4} -> {5}")
        @CsvSource({
                // rule, fees, prereq, advisor, seat, expected
                "R5, true,  true,  true,  true,  REGISTERED",
                "R4, true,  true,  true,  false, COURSE_FULL",
                "R3, true,  true,  false, true,  ADVISOR_APPROVAL_MISSING",
                "R3, true,  true,  false, false, ADVISOR_APPROVAL_MISSING",
                "R2, true,  false, true,  true,  PREREQUISITE_MISSING",
                "R2, true,  false, true,  false, PREREQUISITE_MISSING",
                "R2, true,  false, false, true,  PREREQUISITE_MISSING",
                "R2, true,  false, false, false, PREREQUISITE_MISSING",
                "R1, false, true,  true,  true,  FEES_UNPAID",
                "R1, false, true,  true,  false, FEES_UNPAID",
                "R1, false, true,  false, true,  FEES_UNPAID",
                "R1, false, true,  false, false, FEES_UNPAID",
                "R1, false, false, true,  true,  FEES_UNPAID",
                "R1, false, false, true,  false, FEES_UNPAID",
                "R1, false, false, false, true,  FEES_UNPAID",
                "R1, false, false, false, false, FEES_UNPAID"
        })
        @DisplayName("All 16 combinations map to the 5 collapsed rules")
        void decisionTable(String rule, boolean fees, boolean prereq, boolean advisor, boolean seat,
                           String expected) {
            Set<String> completed = prereq ? Set.of("SWE101") : Set.of();
            Student student = new Student("2021001", 2.5, fees, advisor, completed);
            Course target = new Course("SWE201", 6, 1, Set.of("SWE101"), DayOfWeek.FRIDAY, 10, 2);
            if (!seat) {
                Student other = new Student("2021999", 2.5, true, true, Set.of("SWE101"));
                assertTrue(service.register(other, target).isRegistered());
            }
            int enrolledBefore = target.getEnrolled();

            RegistrationResult result = service.register(student, target);

            if (expected.equals("REGISTERED")) {
                // FR-11: state changes only on success
                assertAll(
                        () -> assertTrue(result.isRegistered()),
                        () -> assertNull(result.getReason()),
                        () -> assertEquals(enrolledBefore + 1, target.getEnrolled()),
                        () -> assertEquals(6, student.getRegisteredEcts()),
                        () -> assertTrue(student.isRegisteredFor(target)));
            } else {
                assertRejected(result, RejectionReason.valueOf(expected));
                assertAll(
                        () -> assertEquals(enrolledBefore, target.getEnrolled()),
                        () -> assertEquals(0, student.getRegisteredEcts()));
            }
        }

        @Test
        @DisplayName("Prerequisites: all must be completed, one missing is enough to reject")
        void allPrerequisitesRequired() {
            Course target = new Course("SWE401", 6, 10, Set.of("SWE201", "SWE301"), DayOfWeek.FRIDAY, 10, 2);
            Student oneMissing = new Student("1", 3.0, true, true, Set.of("SWE201"));
            Student allDone = new Student("2", 3.0, true, true, Set.of("SWE201", "SWE301", "MAT101"));
            assertAll(
                    () -> assertRejected(service.register(oneMissing, target), RejectionReason.PREREQUISITE_MISSING),
                    () -> assertTrue(service.register(allDone, target).isRegistered()));
        }
    }

    // ---------- FR-10 priority outside the decision table ----------

    @Nested
    @DisplayName("FR-10 priority when several rules fail")
    class Priority {

        @Test
        @DisplayName("ALREADY_COMPLETED is reported before FEES_UNPAID")
        void completedBeforeFees() {
            Student s = new Student("1", 2.5, false, false, Set.of("SWE201"));
            Course c = course("SWE201", 6, 10, DayOfWeek.FRIDAY, 10);
            assertRejected(service.register(s, c), RejectionReason.ALREADY_COMPLETED);
        }

        @Test
        @DisplayName("TIME_CONFLICT is reported before ECTS_LIMIT_EXCEEDED")
        void conflictBeforeEcts() {
            Student s = eligibleStudent(1.0);           // limit 30
            fillEcts(s, 25);                            // filler on Monday 08-09
            Course clash = new Course("SWE201", 10, 10, Set.of(), DayOfWeek.MONDAY, 8, 1);
            assertRejected(service.register(s, clash), RejectionReason.TIME_CONFLICT);
        }

        @Test
        @DisplayName("ECTS_LIMIT_EXCEEDED is reported before COURSE_FULL")
        void ectsBeforeFull() {
            Course full = course("SWE201", 10, 1, DayOfWeek.FRIDAY, 10);
            assertTrue(service.register(eligibleStudent(3.0), full).isRegistered());
            Student s = eligibleStudent(1.0);
            fillEcts(s, 25);
            assertRejected(service.register(s, full), RejectionReason.ECTS_LIMIT_EXCEEDED);
        }
    }

    // ---------- BVA on the ECTS limit (FR-3) ----------

    @Nested
    @DisplayName("FR-3 ECTS limit (Boundary Value Analysis)")
    class EctsLimit {

        @ParameterizedTest(name = "GPA {0}: {1} + {2} = {3} ECTS -> registered={4}")
        @CsvSource({
                // gpa, already, new, total, expected
                "1.50, 25,  4, 29, true",   // limit 30: limit-1
                "1.50, 25,  5, 30, true",   // limit
                "1.50, 25,  6, 31, false",  // limit+1
                "2.50, 30,  6, 36, true",   // limit 37
                "2.50, 30,  7, 37, true",
                "2.50, 30,  8, 38, false",
                "3.50, 30, 14, 44, true",   // limit 45
                "3.50, 30, 15, 45, true",
                "3.50, 31, 15, 46, false",
                "1.99, 30,  7, 37, false",  // GPA boundary changes the decision
                "2.00, 30,  7, 37, true",
                "2.99, 30, 15, 45, false",
                "3.00, 30, 15, 45, true"
        })
        @DisplayName("Total ECTS at limit-1, limit and limit+1 for every GPA band")
        void ectsBoundaries(double gpa, int already, int newEcts, int total, boolean expected) {
            Student s = eligibleStudent(gpa);
            fillEcts(s, already);
            Course target = course("SWE201", newEcts, 10, DayOfWeek.FRIDAY, 10);

            RegistrationResult result = service.register(s, target);

            if (expected) {
                assertTrue(result.isRegistered());
                assertEquals(total, s.getRegisteredEcts());
            } else {
                assertRejected(result, RejectionReason.ECTS_LIMIT_EXCEEDED);
                assertEquals(already, s.getRegisteredEcts());
                assertEquals(0, target.getEnrolled());
            }
        }
    }

    // ---------- BVA on capacity (FR-7) ----------

    @Nested
    @DisplayName("FR-7 capacity (Boundary Value Analysis)")
    class Capacity {

        @Test
        @DisplayName("enrolled = capacity-1 accepts, enrolled = capacity rejects")
        void capacityBoundary() {
            Course c = course("SWE201", 5, 2, DayOfWeek.FRIDAY, 10);
            Student first = new Student("1", 2.5, true, true, Set.of());
            Student second = new Student("2", 2.5, true, true, Set.of());
            Student third = new Student("3", 2.5, true, true, Set.of());

            assertTrue(service.register(first, c).isRegistered());   // 0 -> 1
            assertTrue(service.register(second, c).isRegistered());  // capacity-1 -> capacity
            assertFalse(c.hasFreeSeat());
            assertRejected(service.register(third, c), RejectionReason.COURSE_FULL);
            assertEquals(2, c.getEnrolled());
        }

        @Test
        @DisplayName("Minimum capacity 1: first student accepted, second rejected")
        void minimumCapacity() {
            Course c = course("SWE201", 5, 1, DayOfWeek.FRIDAY, 10);
            assertTrue(service.register(new Student("1", 2.5, true, true, Set.of()), c).isRegistered());
            assertRejected(service.register(new Student("2", 2.5, true, true, Set.of()), c),
                    RejectionReason.COURSE_FULL);
        }
    }

    // ---------- Time conflict through the service (FR-8) ----------

    @Nested
    @DisplayName("FR-8 time conflict")
    class TimeConflict {

        @Test
        @DisplayName("BVA: course starting exactly when another ends is accepted")
        void adjacentCourseAccepted() {
            Student s = eligibleStudent(2.5);
            assertTrue(service.register(s, course("SWE201", 5, 10, DayOfWeek.MONDAY, 10)).isRegistered()); // 10-12
            assertTrue(service.register(s, course("SWE202", 5, 10, DayOfWeek.MONDAY, 12)).isRegistered()); // 12-14
        }

        @Test
        @DisplayName("BVA: one hour overlap is rejected and state is unchanged")
        void overlappingCourseRejected() {
            Student s = eligibleStudent(2.5);
            service.register(s, course("SWE201", 5, 10, DayOfWeek.MONDAY, 10)); // 10-12
            Course clash = course("SWE202", 5, 10, DayOfWeek.MONDAY, 11);      // 11-13
            assertRejected(service.register(s, clash), RejectionReason.TIME_CONFLICT);
            assertEquals(5, s.getRegisteredEcts());
            assertEquals(0, clash.getEnrolled());
        }

        @Test
        @DisplayName("Conflict is checked against every registered course, not only the first")
        void conflictWithLaterCourse() {
            Student s = eligibleStudent(2.5);
            service.register(s, course("SWE201", 5, 10, DayOfWeek.MONDAY, 8));
            service.register(s, course("SWE202", 5, 10, DayOfWeek.TUESDAY, 8));
            Course clash = course("SWE203", 5, 10, DayOfWeek.TUESDAY, 9);
            assertRejected(service.register(s, clash), RejectionReason.TIME_CONFLICT);
        }
    }

    // ---------- Negative testing: incorrect operations ----------

    @Nested
    @DisplayName("Negative testing: invalid inputs and incorrect operations (FR-9, FR-12)")
    class Negative {

        @Test
        @DisplayName("Registering twice for the same course is rejected; enrolled counted once")
        void duplicateRegistration() {
            Student s = eligibleStudent(2.5);
            Course c = course("SWE201", 5, 10, DayOfWeek.FRIDAY, 10);
            service.register(s, c);
            assertRejected(service.register(s, c), RejectionReason.ALREADY_REGISTERED);
            assertAll(
                    () -> assertEquals(1, c.getEnrolled()),
                    () -> assertEquals(5, s.getRegisteredEcts()),
                    () -> assertEquals(1, s.getRegisteredCourses().size()));
        }

        @Test
        @DisplayName("Registering for an already completed course is rejected")
        void completedCourse() {
            Student s = new Student("1", 2.5, true, true, Set.of("SWE201"));
            assertRejected(service.register(s, course("SWE201", 5, 10, DayOfWeek.FRIDAY, 10)),
                    RejectionReason.ALREADY_COMPLETED);
        }

        @Test
        @DisplayName("null student or course throws IllegalArgumentException")
        void nullArguments() {
            Student s = eligibleStudent(2.5);
            Course c = course("SWE201", 5, 10, DayOfWeek.FRIDAY, 10);
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> service.register(null, c)),
                    () -> assertThrows(IllegalArgumentException.class, () -> service.register(s, null)),
                    () -> assertThrows(IllegalArgumentException.class, () -> service.drop(null, c)),
                    () -> assertThrows(IllegalArgumentException.class, () -> service.drop(s, null)));
        }

        @Test
        @DisplayName("Dropping a course that was never registered throws IllegalStateException")
        void dropUnregistered() {
            Student s = eligibleStudent(2.5);
            Course c = course("SWE201", 5, 10, DayOfWeek.FRIDAY, 10);
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> service.drop(s, c));
            assertTrue(e.getMessage().contains("SWE201"));
            assertEquals(0, c.getEnrolled());
        }

        @Test
        @DisplayName("Dropping the same course twice: second drop throws, enrolled never negative")
        void dropTwice() {
            Student s = eligibleStudent(2.5);
            Course c = course("SWE201", 5, 10, DayOfWeek.FRIDAY, 10);
            service.register(s, c);
            service.drop(s, c);
            assertThrows(IllegalStateException.class, () -> service.drop(s, c));
            assertEquals(0, c.getEnrolled());
        }
    }

    // ---------- Drop (FR-12) ----------

    @Nested
    @DisplayName("FR-12 drop")
    class Drop {

        @Test
        @DisplayName("Drop frees ECTS and seat, so the course can be taken again")
        void dropFreesEctsAndSeat() {
            Student s = eligibleStudent(1.0);   // limit 30
            fillEcts(s, 25);
            Course c = course("SWE201", 5, 1, DayOfWeek.FRIDAY, 10);
            assertTrue(service.register(s, c).isRegistered());   // 30 ECTS, course full

            service.drop(s, c);

            assertAll(
                    () -> assertEquals(25, s.getRegisteredEcts()),
                    () -> assertEquals(0, c.getEnrolled()),
                    () -> assertFalse(s.isRegisteredFor(c)),
                    () -> assertTrue(c.hasFreeSeat()));
            Student other = eligibleStudent(2.5);
            assertTrue(service.register(other, c).isRegistered(), "freed seat is usable");
        }
    }

    @Test
    @DisplayName("RegistrationResult text form")
    void resultToString() {
        assertAll(
                () -> assertEquals("REGISTERED", RegistrationResult.registered().toString()),
                () -> assertEquals("REJECTED (COURSE_FULL)",
                        RegistrationResult.rejected(RejectionReason.COURSE_FULL).toString()));
    }
}
