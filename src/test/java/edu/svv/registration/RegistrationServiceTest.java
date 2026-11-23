package edu.svv.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** FR-3 to FR-12: the registration rules. */
@DisplayName("RegistrationService")
class RegistrationServiceTest {

    private RegistrationService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService();   // fresh fixture for every test
    }

    // ================= helpers =================

    /** A student who has paid fees, has advisor approval and has completed SWE101. */
    private Student eligibleStudent(double gpa) {
        return new Student("2021001", gpa, true, true, Set.of("SWE101"));
    }

    /** A 2-hour course with no prerequisites. */
    private Course course(String code, int ects, int capacity, DayOfWeek day, int start) {
        return new Course(code, ects, capacity, Set.of(), day, start, 2);
    }

    /** Gives the student the wanted number of ECTS by registering filler courses (max 15 ECTS each, Mon-Thu 08:00). */
    private void giveStudentEcts(Student student, int ects) {
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY};
        int remaining = ects;
        for (int i = 0; remaining > 0; i++) {
            int chunk = Math.min(15, remaining);
            Course filler = new Course("FIL10" + i, chunk, 100, Set.of(), days[i], 8, 1);
            service.register(student, filler);
            remaining = remaining - chunk;
        }
        assertEquals(ects, student.getRegisteredEcts(), "test setup: filler courses must be registered");
    }

    /** Checks that the result is REJECTED with the given reason. */
    private void assertRejected(RegistrationResult result, RejectionReason reason) {
        assertEquals(RegistrationResult.Status.REJECTED, result.getStatus());
        assertEquals(reason, result.getReason());
    }

    // ================= Decision table: fees x prerequisites x advisor x seat (FR-4..FR-7, FR-10) =================

    /** Course with capacity 1 that needs SWE101; if no seat should be left, another student takes the only seat. */
    private Course decisionTableCourse(boolean seatAvailable) {
        Course target = new Course("SWE201", 6, 1, Set.of("SWE101"), DayOfWeek.FRIDAY, 10, 2);
        if (!seatAvailable) {
            service.register(new Student("2021999", 2.5, true, true, Set.of("SWE101")), target);
        }
        return target;
    }

    @Test
    @DisplayName("Decision table R5: all four conditions true -> REGISTERED (FR-11 state changes)")
    void decisionTableRegistered() {
        Student student = new Student("2021001", 2.5, true, true, Set.of("SWE101"));
        Course target = decisionTableCourse(true);

        RegistrationResult result = service.register(student, target);

        assertTrue(result.isRegistered());
        assertNull(result.getReason());
        assertEquals(1, target.getEnrolled());
        assertEquals(6, student.getRegisteredEcts());
        assertTrue(student.isRegisteredFor(target));
    }

    @ParameterizedTest(name = "{0}: fees={1} prereq={2} advisor={3} seat={4} -> {5}")
    @CsvSource({
            // rule, fees, prereq, advisor, seat, expected reason
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
    @DisplayName("Decision table R1-R4: the other 15 combinations are rejected with the right reason")
    void decisionTableRejected(String rule, boolean fees, boolean prereq, boolean advisor, boolean seat,
                               RejectionReason expected) {
        Set<String> completed = Set.of();
        if (prereq) {
            completed = Set.of("SWE101");
        }
        Student student = new Student("2021001", 2.5, fees, advisor, completed);
        Course target = decisionTableCourse(seat);
        int enrolledBefore = target.getEnrolled();

        RegistrationResult result = service.register(student, target);

        assertRejected(result, expected);
        assertEquals(enrolledBefore, target.getEnrolled());   // nothing changed
        assertEquals(0, student.getRegisteredEcts());
    }

    @Test
    @DisplayName("Prerequisites: all must be completed, one missing is enough to reject")
    void allPrerequisitesRequired() {
        Course target = new Course("SWE401", 6, 10, Set.of("SWE201", "SWE301"), DayOfWeek.FRIDAY, 10, 2);
        Student oneMissing = new Student("1", 3.0, true, true, Set.of("SWE201"));
        Student allDone = new Student("2", 3.0, true, true, Set.of("SWE201", "SWE301", "MAT101"));

        assertRejected(service.register(oneMissing, target), RejectionReason.PREREQUISITE_MISSING);
        assertTrue(service.register(allDone, target).isRegistered());
    }

    // ================= FR-10 priority when several rules fail =================

    @Test
    @DisplayName("Priority: ALREADY_COMPLETED is reported before FEES_UNPAID")
    void completedBeforeFees() {
        Student s = new Student("1", 2.5, false, false, Set.of("SWE201"));
        Course c = course("SWE201", 6, 10, DayOfWeek.FRIDAY, 10);

        assertRejected(service.register(s, c), RejectionReason.ALREADY_COMPLETED);
    }

    @Test
    @DisplayName("Priority: TIME_CONFLICT is reported before ECTS_LIMIT_EXCEEDED")
    void conflictBeforeEcts() {
        Student s = eligibleStudent(1.0);   // limit 30
        giveStudentEcts(s, 25);             // a filler is on Monday 08-09
        Course clash = new Course("SWE201", 10, 10, Set.of(), DayOfWeek.MONDAY, 8, 1);

        assertRejected(service.register(s, clash), RejectionReason.TIME_CONFLICT);
    }

    @Test
    @DisplayName("Priority: ECTS_LIMIT_EXCEEDED is reported before COURSE_FULL")
    void ectsBeforeFull() {
        Course full = course("SWE201", 10, 1, DayOfWeek.FRIDAY, 10);
        service.register(eligibleStudent(3.0), full);   // takes the only seat
        Student s = eligibleStudent(1.0);
        giveStudentEcts(s, 25);

        assertRejected(service.register(s, full), RejectionReason.ECTS_LIMIT_EXCEEDED);
    }

    // ================= FR-3 ECTS limit (Boundary Value Analysis) =================

    @ParameterizedTest(name = "GPA {0}: {1} + {2} = {3} ECTS -> registered")
    @CsvSource({
            // gpa, already, new, total
            "1.50, 25,  4, 29",   // limit 30: limit-1
            "1.50, 25,  5, 30",   //           limit
            "2.50, 30,  6, 36",   // limit 37
            "2.50, 30,  7, 37",
            "3.25, 30, 14, 44",   // limit 45
            "3.25, 30, 15, 45",
            "3.75, 35, 14, 49",   // limit 50 (v1.1 honour band)
            "3.75, 35, 15, 50",
            "2.00, 30,  7, 37",   // GPA boundary: 2.00 already has limit 37
            "3.00, 30, 15, 45",   // GPA boundary: 3.00 already has limit 45
            "3.50, 35, 15, 50"    // GPA boundary: 3.50 already has limit 50
    })
    @DisplayName("BVA: total ECTS at limit-1 and exactly at the limit is accepted")
    void ectsWithinLimit(double gpa, int already, int newEcts, int total) {
        Student s = eligibleStudent(gpa);
        giveStudentEcts(s, already);
        Course target = course("SWE201", newEcts, 10, DayOfWeek.FRIDAY, 10);

        RegistrationResult result = service.register(s, target);

        assertTrue(result.isRegistered());
        assertEquals(total, s.getRegisteredEcts());
    }

    @ParameterizedTest(name = "GPA {0}: {1} + {2} ECTS -> rejected")
    @CsvSource({
            // gpa, already, new
            "1.50, 25,  6",   // limit 30: limit+1
            "2.50, 30,  8",   // limit 37: limit+1
            "3.25, 31, 15",   // limit 45: limit+1
            "3.75, 36, 15",   // limit 50: limit+1
            "1.99, 30,  7",   // GPA boundary: 1.99 still has limit 30
            "2.99, 30, 15",   // GPA boundary: 2.99 still has limit 37
            "3.49, 35, 15"    // GPA boundary: 3.49 still has limit 45
    })
    @DisplayName("BVA: total ECTS at limit+1 is rejected and nothing changes")
    void ectsOverLimit(double gpa, int already, int newEcts) {
        Student s = eligibleStudent(gpa);
        giveStudentEcts(s, already);
        Course target = course("SWE201", newEcts, 10, DayOfWeek.FRIDAY, 10);

        RegistrationResult result = service.register(s, target);

        assertRejected(result, RejectionReason.ECTS_LIMIT_EXCEEDED);
        assertEquals(already, s.getRegisteredEcts());
        assertEquals(0, target.getEnrolled());
    }

    // ================= FR-7 capacity (Boundary Value Analysis) =================

    @Test
    @DisplayName("Capacity BVA: enrolled = capacity-1 accepts, enrolled = capacity rejects")
    void capacityBoundary() {
        Course c = course("SWE201", 5, 2, DayOfWeek.FRIDAY, 10);
        Student first = new Student("1", 2.5, true, true, Set.of());
        Student second = new Student("2", 2.5, true, true, Set.of());
        Student third = new Student("3", 2.5, true, true, Set.of());

        assertTrue(service.register(first, c).isRegistered());    // 0 -> 1
        assertTrue(service.register(second, c).isRegistered());   // capacity-1 -> capacity
        assertFalse(c.hasFreeSeat());
        assertRejected(service.register(third, c), RejectionReason.COURSE_FULL);
        assertEquals(2, c.getEnrolled());
    }

    @Test
    @DisplayName("Capacity BVA: minimum capacity 1, first student accepted, second rejected")
    void minimumCapacity() {
        Course c = course("SWE201", 5, 1, DayOfWeek.FRIDAY, 10);
        Student first = new Student("1", 2.5, true, true, Set.of());
        Student second = new Student("2", 2.5, true, true, Set.of());

        assertTrue(service.register(first, c).isRegistered());
        assertRejected(service.register(second, c), RejectionReason.COURSE_FULL);
    }

    // ================= FR-8 time conflict =================

    @Test
    @DisplayName("Time BVA: course starting exactly when another ends is accepted")
    void adjacentCourseAccepted() {
        Student s = eligibleStudent(2.5);
        Course first = course("SWE201", 5, 10, DayOfWeek.MONDAY, 10);    // 10-12
        Course second = course("SWE202", 5, 10, DayOfWeek.MONDAY, 12);   // 12-14

        assertTrue(service.register(s, first).isRegistered());
        assertTrue(service.register(s, second).isRegistered());
    }

    @Test
    @DisplayName("Time BVA: one hour overlap is rejected and state is unchanged")
    void overlappingCourseRejected() {
        Student s = eligibleStudent(2.5);
        service.register(s, course("SWE201", 5, 10, DayOfWeek.MONDAY, 10));   // 10-12
        Course clash = course("SWE202", 5, 10, DayOfWeek.MONDAY, 11);        // 11-13

        assertRejected(service.register(s, clash), RejectionReason.TIME_CONFLICT);
        assertEquals(5, s.getRegisteredEcts());
        assertEquals(0, clash.getEnrolled());
    }

    @Test
    @DisplayName("Time: conflict is checked against every registered course, not only the first")
    void conflictWithLaterCourse() {
        Student s = eligibleStudent(2.5);
        service.register(s, course("SWE201", 5, 10, DayOfWeek.MONDAY, 8));
        service.register(s, course("SWE202", 5, 10, DayOfWeek.TUESDAY, 8));
        Course clash = course("SWE203", 5, 10, DayOfWeek.TUESDAY, 9);

        assertRejected(service.register(s, clash), RejectionReason.TIME_CONFLICT);
    }

    // ================= Negative testing: invalid inputs and incorrect operations (FR-9, FR-12) =================

    @Test
    @DisplayName("Negative: registering twice for the same course is rejected; enrolled counted once")
    void duplicateRegistration() {
        Student s = eligibleStudent(2.5);
        Course c = course("SWE201", 5, 10, DayOfWeek.FRIDAY, 10);
        service.register(s, c);

        assertRejected(service.register(s, c), RejectionReason.ALREADY_REGISTERED);
        assertEquals(1, c.getEnrolled());
        assertEquals(5, s.getRegisteredEcts());
        assertEquals(1, s.getRegisteredCourses().size());
    }

    @Test
    @DisplayName("Negative DEF-02: another section with the same course code is ALREADY_REGISTERED")
    void sameCodeOtherSection() {
        Student s = eligibleStudent(2.5);
        Course monday = course("SWE201", 5, 10, DayOfWeek.MONDAY, 10);
        Course tuesday = course("SWE201", 5, 10, DayOfWeek.TUESDAY, 10);
        assertTrue(service.register(s, monday).isRegistered());

        assertRejected(service.register(s, tuesday), RejectionReason.ALREADY_REGISTERED);
        assertEquals(0, tuesday.getEnrolled());
        assertEquals(5, s.getRegisteredEcts());
        assertEquals(1, s.getRegisteredCourses().size());
    }

    @Test
    @DisplayName("Negative: registering for an already completed course is rejected")
    void completedCourse() {
        Student s = new Student("1", 2.5, true, true, Set.of("SWE201"));
        Course c = course("SWE201", 5, 10, DayOfWeek.FRIDAY, 10);

        assertRejected(service.register(s, c), RejectionReason.ALREADY_COMPLETED);
    }

    @Test
    @DisplayName("Negative: null student or course throws IllegalArgumentException")
    void nullArguments() {
        Student s = eligibleStudent(2.5);
        Course c = course("SWE201", 5, 10, DayOfWeek.FRIDAY, 10);

        assertThrows(IllegalArgumentException.class, () -> service.register(null, c));
        assertThrows(IllegalArgumentException.class, () -> service.register(s, null));
        assertThrows(IllegalArgumentException.class, () -> service.drop(null, c));
        assertThrows(IllegalArgumentException.class, () -> service.drop(s, null));
    }

    @Test
    @DisplayName("Negative: dropping a course that was never registered throws IllegalStateException")
    void dropUnregistered() {
        Student s = eligibleStudent(2.5);
        Course c = course("SWE201", 5, 10, DayOfWeek.FRIDAY, 10);

        Exception e = assertThrows(IllegalStateException.class, () -> service.drop(s, c));
        assertTrue(e.getMessage().contains("SWE201"));
        assertEquals(0, c.getEnrolled());
    }

    @Test
    @DisplayName("Negative: dropping the same course twice, second drop throws, enrolled never negative")
    void dropTwice() {
        Student s = eligibleStudent(2.5);
        Course c = course("SWE201", 5, 10, DayOfWeek.FRIDAY, 10);
        service.register(s, c);
        service.drop(s, c);

        assertThrows(IllegalStateException.class, () -> service.drop(s, c));
        assertEquals(0, c.getEnrolled());
    }

    // ================= FR-12 drop =================

    @Test
    @DisplayName("Drop frees ECTS and seat, so the course can be taken again")
    void dropFreesEctsAndSeat() {
        Student s = eligibleStudent(1.0);   // limit 30
        giveStudentEcts(s, 25);
        Course c = course("SWE201", 5, 1, DayOfWeek.FRIDAY, 10);
        assertTrue(service.register(s, c).isRegistered());   // now 30 ECTS and the course is full

        service.drop(s, c);

        assertEquals(25, s.getRegisteredEcts());
        assertEquals(0, c.getEnrolled());
        assertFalse(s.isRegisteredFor(c));
        assertTrue(c.hasFreeSeat());
        assertTrue(service.register(eligibleStudent(2.5), c).isRegistered());   // freed seat is usable
    }

    @Test
    @DisplayName("Drop DEF-02: drop is by course code; the registered section's count is decreased")
    void dropBySameCodeOtherObject() {
        Student s = eligibleStudent(2.5);
        Course registered = course("SWE201", 5, 10, DayOfWeek.MONDAY, 10);
        Course sameCode = course("SWE201", 5, 10, DayOfWeek.TUESDAY, 10);
        service.register(s, registered);

        service.drop(s, sameCode);

        assertEquals(0, registered.getEnrolled());
        assertEquals(0, sameCode.getEnrolled());   // never negative
        assertEquals(0, s.getRegisteredEcts());
        assertFalse(s.isRegisteredFor(registered));
    }

    // ================= RegistrationResult =================

    @Test
    @DisplayName("RegistrationResult text form")
    void resultToString() {
        assertEquals("REGISTERED", RegistrationResult.registered().toString());
        assertEquals("REJECTED (COURSE_FULL)", RegistrationResult.rejected(RejectionReason.COURSE_FULL).toString());
    }
}
