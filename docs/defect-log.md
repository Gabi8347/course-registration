# Defect Log

Only defects that actually occurred are recorded.

## Product defects

| ID | Found by | Input / test | Expected | Actual | Root cause | Fix | Re-test |
|---|---|---|---|---|---|---|---|
| DEF-01 | First execution of the console demo (`Main`) | `new Student("2021001", 2.45, true, true, Set.of("SWE101","MAT101"))` | Student created | `NullPointerException` at `Student.<init>` | `completedCourses.contains(null)` was used to reject null elements; immutable sets from `Set.of` throw NPE on `contains(null)` instead of returning false | Loop over elements and check `code == null` | Demo runs; covered by unit tests `validStudentWithImmutableSet` and `nullElementInCompletedCourses` |

## Test defects (weaknesses in the test suite itself)

| ID | Found by | Problem | Consequence | Fix | Re-test |
|---|---|---|---|---|---|
| TD-01 | PIT mutation testing, run 1 (surviving mutant `Course.java:55`) | Invalid-input tests only asserted *that* `IllegalArgumentException` was thrown. `startHour = 20` was rejected by the end-of-day rule, so the start-hour rule could be deleted without any test failing. | A real regression in the start-hour check would go unnoticed, despite 100 % line/branch coverage. | `assertValidity` now also checks that the exception message names the violated rule (ECTS, Capacity, Start hour, Duration, end by 20:00). | Full suite 147/147 pass; PIT 166/166 killed |
| TD-02 | Review of the test code while fixing TD-01 | `endOfDayBoundary` was described as testing end = 19, 20, 21 but had no end = 19 case. | Below-boundary value of BVA missing. | Added `15, 4, true` (ends 19:00). | Pass |
