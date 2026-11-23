package edu.svv.registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A student who wants to register for courses (FR-1 validation). */
public class Student {

    static final double MIN_GPA = 0.0;
    static final double MAX_GPA = 4.0;

    private final String id;
    private final double gpa;
    private final boolean feesPaid;
    private final boolean advisorApproved;
    private final Set<String> completedCourses;
    private final List<Course> registeredCourses = new ArrayList<>();

    public Student(String id, double gpa, boolean feesPaid, boolean advisorApproved,
                   Set<String> completedCourses) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Student id must not be blank");
        }
        if (!isValidGpa(gpa)) {
            throw new IllegalArgumentException("GPA must be between 0.00 and 4.00: " + gpa);
        }
        if (completedCourses == null) {
            throw new IllegalArgumentException("Completed courses must not be null");
        }
        // DEF-01: Set.contains(null) throws NullPointerException for immutable sets (Set.of),
        // so each element is checked instead.
        for (String code : completedCourses) {
            if (code == null) {
                throw new IllegalArgumentException("Completed courses must not contain null");
            }
        }
        this.id = id;
        this.gpa = gpa;
        this.feesPaid = feesPaid;
        this.advisorApproved = advisorApproved;
        this.completedCourses = Collections.unmodifiableSet(new HashSet<>(completedCourses));
    }

    /** Also rejects NaN, because every comparison with NaN is false. */
    static boolean isValidGpa(double gpa) {
        return gpa >= MIN_GPA && gpa <= MAX_GPA;
    }

    public int getRegisteredEcts() {
        int total = 0;
        for (Course c : registeredCourses) {
            total += c.getEcts();
        }
        return total;
    }

    /** DEF-02: a course is identified by its code, so another section of the same course counts too. */
    public boolean isRegisteredFor(Course course) {
        return findRegistered(course.getCode()) != null;
    }

    /** The registered course with this code, or null if there is none. */
    Course findRegistered(String code) {
        for (Course c : registeredCourses) {
            if (c.getCode().equals(code)) {
                return c;
            }
        }
        return null;
    }

    void addCourse(Course course) {
        registeredCourses.add(course);
    }

    void removeCourse(Course course) {
        registeredCourses.remove(course);
    }

    public String getId() { return id; }
    public double getGpa() { return gpa; }
    public boolean isFeesPaid() { return feesPaid; }
    public boolean isAdvisorApproved() { return advisorApproved; }
    public Set<String> getCompletedCourses() { return completedCourses; }
    public List<Course> getRegisteredCourses() { return Collections.unmodifiableList(registeredCourses); }
}
