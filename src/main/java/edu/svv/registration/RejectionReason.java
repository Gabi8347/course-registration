package edu.svv.registration;

/** Reason codes, declared in the priority order of FR-10. */
public enum RejectionReason {
    ALREADY_REGISTERED,
    ALREADY_COMPLETED,
    FEES_UNPAID,
    PREREQUISITE_MISSING,
    ADVISOR_APPROVAL_MISSING,
    TIME_CONFLICT,
    ECTS_LIMIT_EXCEEDED,
    COURSE_FULL
}
