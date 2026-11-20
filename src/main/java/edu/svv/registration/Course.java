package edu.svv.registration;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** A course offered in the semester (FR-1 validation, FR-7 quota, FR-8 time slot). */
public class Course {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z]{2,4}[0-9]{3}");

    static final int MIN_ECTS = 1;
    static final int MAX_ECTS = 15;
    static final int MIN_CAPACITY = 1;
    static final int MAX_CAPACITY = 500;
    static final int FIRST_START_HOUR = 8;
    static final int LAST_START_HOUR = 19;
    static final int MIN_DURATION = 1;
    static final int MAX_DURATION = 4;
    static final int DAY_END_HOUR = 20;

    private final String code;
    private final int ects;
    private final int capacity;
    private final Set<String> prerequisites;
    private final DayOfWeek day;
    private final int startHour;
    private final int durationHours;
    private int enrolled;

    public Course(String code, int ects, int capacity, Set<String> prerequisites,
                  DayOfWeek day, int startHour, int durationHours) {
        if (!isValidCode(code)) {
            throw new IllegalArgumentException("Invalid course code: " + code);
        }
        if (ects < MIN_ECTS || ects > MAX_ECTS) {
            throw new IllegalArgumentException("ECTS must be between 1 and 15: " + ects);
        }
        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Capacity must be between 1 and 500: " + capacity);
        }
        if (prerequisites == null) {
            throw new IllegalArgumentException("Prerequisites must not be null");
        }
        for (String p : prerequisites) {
            if (!isValidCode(p)) {
                throw new IllegalArgumentException("Invalid prerequisite code: " + p);
            }
        }
        if (day == null || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Day must be Monday to Friday: " + day);
        }
        if (startHour < FIRST_START_HOUR || startHour > LAST_START_HOUR) {
            throw new IllegalArgumentException("Start hour must be between 8 and 19: " + startHour);
        }
        if (durationHours < MIN_DURATION || durationHours > MAX_DURATION) {
            throw new IllegalArgumentException("Duration must be between 1 and 4 hours: " + durationHours);
        }
        if (startHour + durationHours > DAY_END_HOUR) {
            throw new IllegalArgumentException("Course must end by 20:00");
        }
        this.code = code;
        this.ects = ects;
        this.capacity = capacity;
        this.prerequisites = Collections.unmodifiableSet(new HashSet<>(prerequisites));
        this.day = day;
        this.startHour = startHour;
        this.durationHours = durationHours;
        this.enrolled = 0;
    }

    static boolean isValidCode(String code) {
        return code != null && CODE_PATTERN.matcher(code).matches();
    }

    /** FR-8: same day and the time intervals [start, end) intersect. */
    public boolean overlaps(Course other) {
        if (this.day != other.day) {
            return false;
        }
        return this.startHour < other.getEndHour() && other.startHour < this.getEndHour();
    }

    public boolean hasFreeSeat() {
        return enrolled < capacity;
    }

    void incrementEnrolled() {
        enrolled++;
    }

    void decrementEnrolled() {
        enrolled--;
    }

    public String getCode() { return code; }
    public int getEcts() { return ects; }
    public int getCapacity() { return capacity; }
    public int getEnrolled() { return enrolled; }
    public Set<String> getPrerequisites() { return prerequisites; }
    public DayOfWeek getDay() { return day; }
    public int getStartHour() { return startHour; }
    public int getDurationHours() { return durationHours; }
    public int getEndHour() { return startHour + durationHours; }

    @Override
    public String toString() {
        return code + " (" + ects + " ECTS, " + day + " " + startHour + ":00-" + getEndHour() + ":00)";
    }
}
