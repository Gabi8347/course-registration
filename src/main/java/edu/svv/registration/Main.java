package edu.svv.registration;

import java.time.DayOfWeek;
import java.util.Set;

/** Small console demo of the registration rules. */
public class Main {

    public static void main(String[] args) {
        RegistrationService service = new RegistrationService();

        Student ali = new Student("2021001", 2.45, true, true, Set.of("SWE101", "MAT101"));

        Course algorithms = new Course("SWE201", 6, 40, Set.of("SWE101"), DayOfWeek.MONDAY, 9, 3);
        Course databases = new Course("SWE301", 6, 40, Set.of("SWE201"), DayOfWeek.TUESDAY, 10, 2);
        Course physics = new Course("PHY101", 5, 60, Set.of(), DayOfWeek.MONDAY, 11, 2);
        Course statistics = new Course("MAT201", 5, 50, Set.of("MAT101"), DayOfWeek.MONDAY, 12, 2);

        System.out.println("Student " + ali.getId() + ", GPA " + ali.getGpa()
                + ", ECTS limit " + CreditPolicy.maxEcts(ali.getGpa()));
        for (Course c : new Course[] {algorithms, databases, physics, statistics}) {
            System.out.println("  " + c + " -> " + service.register(ali, c));
        }
        System.out.println("Registered ECTS: " + ali.getRegisteredEcts());
    }
}
