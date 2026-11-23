package edu.svv.registration;

/** Applies the registration rules FR-3 to FR-12. */
public class RegistrationService {

    public RegistrationResult register(Student student, Course course) {
        requireNotNull(student, course);

        // FR-10: the first failing rule in this order is reported.
        if (student.isRegisteredFor(course)) {
            return RegistrationResult.rejected(RejectionReason.ALREADY_REGISTERED);
        }
        if (student.getCompletedCourses().contains(course.getCode())) {
            return RegistrationResult.rejected(RejectionReason.ALREADY_COMPLETED);
        }
        if (!student.isFeesPaid()) {
            return RegistrationResult.rejected(RejectionReason.FEES_UNPAID);
        }
        if (!student.getCompletedCourses().containsAll(course.getPrerequisites())) {
            return RegistrationResult.rejected(RejectionReason.PREREQUISITE_MISSING);
        }
        if (!student.isAdvisorApproved()) {
            return RegistrationResult.rejected(RejectionReason.ADVISOR_APPROVAL_MISSING);
        }
        for (Course registered : student.getRegisteredCourses()) {
            if (registered.overlaps(course)) {
                return RegistrationResult.rejected(RejectionReason.TIME_CONFLICT);
            }
        }
        int newTotal = student.getRegisteredEcts() + course.getEcts();
        if (newTotal > CreditPolicy.maxEcts(student.getGpa())) {
            return RegistrationResult.rejected(RejectionReason.ECTS_LIMIT_EXCEEDED);
        }
        if (!course.hasFreeSeat()) {
            return RegistrationResult.rejected(RejectionReason.COURSE_FULL);
        }

        // FR-11
        student.addCourse(course);
        course.incrementEnrolled();
        return RegistrationResult.registered();
    }

    /** FR-12: dropping a course the student is not registered for is an incorrect operation. */
    public void drop(Student student, Course course) {
        requireNotNull(student, course);
        // DEF-02: look up the section the student actually registered for (same code).
        Course registered = student.findRegistered(course.getCode());
        if (registered == null) {
            throw new IllegalStateException(
                    "Student " + student.getId() + " is not registered for " + course.getCode());
        }
        student.removeCourse(registered);
        registered.decrementEnrolled();
    }

    private static void requireNotNull(Student student, Course course) {
        if (student == null) {
            throw new IllegalArgumentException("Student must not be null");
        }
        if (course == null) {
            throw new IllegalArgumentException("Course must not be null");
        }
    }
}
