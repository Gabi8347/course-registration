package edu.svv.registration;

/** FR-2: maximum ECTS per semester depending on GPA (v1.1 adds the honour band). */
public final class CreditPolicy {

    static final int LOW_LIMIT = 30;
    static final int STANDARD_LIMIT = 37;
    static final int HIGH_LIMIT = 45;
    static final int HONOUR_LIMIT = 50;

    private CreditPolicy() {
    }

    public static int maxEcts(double gpa) {
        if (!Student.isValidGpa(gpa)) {
            throw new IllegalArgumentException("GPA must be between 0.00 and 4.00: " + gpa);
        }
        if (gpa < 2.0) {
            return LOW_LIMIT;
        } else if (gpa < 3.0) {
            return STANDARD_LIMIT;
        } else if (gpa < 3.5) {
            return HIGH_LIMIT;
        } else {
            return HONOUR_LIMIT;
        }
    }
}
