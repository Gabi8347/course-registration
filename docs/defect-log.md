# Defect Log

Only defects that actually occurred are recorded.

| ID | Found by | Input / test | Expected | Actual | Root cause | Fix | Re-test |
|---|---|---|---|---|---|---|---|
| DEF-01 | First execution of the console demo (`Main`) | `new Student("2021001", 2.45, true, true, Set.of("SWE101","MAT101"))` | Student created | `NullPointerException` at `Student.<init>` | `completedCourses.contains(null)` was used to reject null elements; immutable sets from `Set.of` throw NPE on `contains(null)` instead of returning false | Loop over elements and check `code == null` | Demo runs; covered by unit tests `validStudentWithImmutableSet` and `nullElementInCompletedCourses` |
