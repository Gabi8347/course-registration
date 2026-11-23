# Defect Log

Only defects that actually occurred are recorded.

## Product defects

| ID | Found by | Input / test | Expected | Actual | Root cause | Fix | Re-test |
|---|---|---|---|---|---|---|---|
| DEF-01 | First execution of the console demo (`Main`) | `new Student("2021001", 2.45, true, true, Set.of("SWE101","MAT101"))` | Student created | `NullPointerException` at `Student.<init>` | `completedCourses.contains(null)` was used to reject null elements; immutable sets from `Set.of` throw NPE on `contains(null)` instead of returning false | Loop over elements and check `code == null` | Demo runs; covered by unit tests `validStudentWithImmutableSet` and `nullElementInCompletedCourses` |
| DEF-02 | Code review of the whole project before writing the final report | Register `SWE201` (Monday section), then `SWE201` (Tuesday section, another `Course` object) | `REJECTED (ALREADY_REGISTERED)` (FR-9) | `REGISTERED` twice, 10 ECTS counted | `isRegisteredFor` used `List.contains`, i.e. object identity, not the course code. All earlier duplicate tests reused the same object, so they could not reveal it. `drop` had the same identity problem. | Look up registrations by course code (`Student.findRegistered`); `drop` removes and decrements the registered section | New tests `sameCodeOtherSection`, `dropBySameCodeOtherObject` failed on the old code (red run), pass after the fix; all 156 earlier tests still pass |
| DEF-03 | Same review | `new Course("SWE301", …, Set.of("SWE301"), …)` | `IllegalArgumentException` | course created; nobody can ever register (always `PREREQUISITE_MISSING`) | Missing validation rule in FR-1 | Reject a prerequisite equal to the course code; FR-1 updated | `selfPrerequisite` red before, green after |


## Test defects (weaknesses in the test suite itself)

| ID | Found by | Problem | Consequence | Fix | Re-test |
|---|---|---|---|---|---|
| TD-01 | PIT mutation testing, run 1 (surviving mutant `Course.java:55`) | Invalid-input tests only asserted *that* `IllegalArgumentException` was thrown. `startHour = 20` was rejected by the end-of-day rule, so the start-hour rule could be deleted without any test failing. | A real regression in the start-hour check would go unnoticed, despite 100 % line/branch coverage. | `assertValidity` now also checks that the exception message names the violated rule (ECTS, Capacity, Start hour, Duration, end by 20:00). | Full suite 147/147 pass; PIT 166/166 killed |
| TD-02 | Review of the test code while fixing TD-01 | `endOfDayBoundary` was described as testing end = 19, 20, 21 but had no end = 19 case. | Below-boundary value of BVA missing. | Added `15, 4, true` (ends 19:00). | Pass |
