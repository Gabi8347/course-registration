# Course Registration Eligibility Checker — Requirements & Test Design (v1.2)

## 1. Problem
At the start of each semester students register for courses through a set of rules
(GPA-based credit limits, prerequisites, unpaid fees, advisor approval, quotas, timetable
clashes). When these rules are checked by hand or by poorly tested software, students are
wrongly blocked or wrongly accepted, and errors are found only after the add/drop period.
The project is a small Java library + console demo that decides, for one student and one
course, whether registration is accepted and, if not, **why**.

**Scope:** plain Java 17, no GUI, no database (in-memory objects). Single student, single
semester. Grades, payments and advisor workflows are *inputs* (boolean / numeric), not
modelled processes.

## 2. Domain objects and input validation

| Object | Field | Valid values | Invalid → |
|---|---|---|---|
| Student | `id` | non-null, non-blank | `IllegalArgumentException` |
| | `gpa` | 0.00 ≤ gpa ≤ 4.00 (not NaN) | `IllegalArgumentException` |
| | `feesPaid`, `advisorApproved` | true / false | — |
| | `completedCourses` | set of course codes (may be empty, not null) | `IllegalArgumentException` |
| Course | `code` | 2–4 uppercase letters + 3 digits, e.g. `SWE301` | `IllegalArgumentException` |
| | `ects` | 1 ≤ ects ≤ 15 | `IllegalArgumentException` |
| | `capacity` | 1 ≤ capacity ≤ 500 | `IllegalArgumentException` |
| | `prerequisites` | set of course codes (may be empty), not the course's own code (*v1.2*) | `IllegalArgumentException` |
| | `day` | MONDAY … FRIDAY | `IllegalArgumentException` (SAT/SUN/null) |
| | `startHour` | 8 ≤ startHour ≤ 19 | `IllegalArgumentException` |
| | `durationHours` | 1 ≤ duration ≤ 4 **and** startHour + duration ≤ 20 | `IllegalArgumentException` |

Convention: **invalid data → exception**, **valid data that breaks a business rule →
`RegistrationResult` with status `REJECTED` and one reason code**.

## 3. Functional requirements

| ID | Requirement |
|---|---|
| FR-1 | The system shall validate Student and Course data as in Section 2 and throw `IllegalArgumentException` with a descriptive message for invalid data. |
| FR-2 | The maximum ECTS per semester shall depend on GPA: **gpa < 2.00 → 30**, **2.00 ≤ gpa < 3.00 → 37**, **3.00 ≤ gpa < 3.50 → 45**, **gpa ≥ 3.50 → 50** (honour students, *changed in v1.1*; v1.0 had gpa ≥ 3.00 → 45). |
| FR-3 | A registration shall be rejected with `ECTS_LIMIT_EXCEEDED` if (ECTS already registered + course ECTS) > limit. Reaching the limit exactly is allowed. |
| FR-4 | Rejected with `FEES_UNPAID` if the student has not paid fees. |
| FR-5 | Rejected with `PREREQUISITE_MISSING` if any prerequisite is not in the student's completed courses. |
| FR-6 | Rejected with `ADVISOR_APPROVAL_MISSING` if the advisor has not approved. |
| FR-7 | Rejected with `COURSE_FULL` if enrolled count == capacity. |
| FR-8 | Rejected with `TIME_CONFLICT` if the course overlaps a registered course on the same day. Overlap: `s1 < e2 && s2 < e1` (a course ending at 10:00 and one starting at 10:00 do **not** conflict). |
| FR-9 | Rejected with `ALREADY_REGISTERED` if the student is already registered for a course with the **same code** (any section; *clarified in v1.2*, DEF-02), and `ALREADY_COMPLETED` if it is in completed courses. |
| FR-10 | When several rules fail, the **first** in this order is reported: ALREADY_REGISTERED → ALREADY_COMPLETED → FEES_UNPAID → PREREQUISITE_MISSING → ADVISOR_APPROVAL_MISSING → TIME_CONFLICT → ECTS_LIMIT_EXCEEDED → COURSE_FULL. |
| FR-11 | On success the result is `REGISTERED`, the course's enrolled count increases by 1 and its ECTS count toward the student's total. |
| FR-12 | Dropping a course (identified by its code) shall remove the registered section, decrease **that section's** enrolled count by 1 and free its ECTS. Dropping a course the student is not registered for shall throw `IllegalStateException` (incorrect operation). |

Rationale for FR-10 order: administrative blocks (duplicates, fees) first, then academic
eligibility, then timetable/credit load, then quota — the student fixes problems in a
sensible order and the outcome is deterministic (testable).

## 4. Decision table (FR-4…FR-7, FR-10)
Assumes FR-1, FR-8, FR-9 and FR-3 are satisfied. 4 binary conditions → 16 combinations,
collapsed to 5 rules using FR-10 priority ("–" = don't care).

| Condition / Action | R1 | R2 | R3 | R4 | R5 |
|---|---|---|---|---|---|
| C1 Fees paid | N | Y | Y | Y | Y |
| C2 Prerequisites met | – | N | Y | Y | Y |
| C3 Advisor approved | – | – | N | Y | Y |
| C4 Seat available | – | – | – | N | Y |
| **Outcome** | FEES_UNPAID | PREREQUISITE_MISSING | ADVISOR_APPROVAL_MISSING | COURSE_FULL | REGISTERED |

Test selection: one test per rule (5) **plus** don't-care checks (e.g. N,N,N,N → FEES_UNPAID;
Y,N,N,N → PREREQUISITE_MISSING) to verify the priority really holds when several
conditions fail together — the place where an implementation is most likely to be wrong.

## 5. Design (planned classes)

| Class | Responsibility |
|---|---|
| `Student` | validated data: id, gpa, feesPaid, advisorApproved, completedCourses; list of registered courses |
| `Course` | validated data: code, ects, capacity, enrolled, prerequisites, day, startHour, durationHours; `overlaps(Course)` |
| `CreditPolicy` | `maxEcts(double gpa)` — FR-2 |
| `RegistrationService` | `register(Student, Course)`, `drop(Student, Course)` — FR-3…FR-12 |
| `RegistrationResult` | status (`REGISTERED` / `REJECTED`) + `RejectionReason` |
| `RejectionReason` | enum of the 8 reason codes |
| `Main` | small console demo (not part of the tested logic) |

## 6. Test plan (skeleton)

| Technique | Applied to | Selected values / cases |
|---|---|---|
| Equivalence Partitioning | GPA (FR-1, FR-2) | invalid low (<0), [0,2), [2,3), [3,3.5), [3.5,4] (v1.1), invalid high (>4), NaN |
| | Course code (FR-1) | valid `SWE301`; invalid: lowercase, 1 letter, 5 letters, 2 digits, 4 digits, blank, null |
| | Day (FR-1) | weekday / weekend / null |
| Boundary Value Analysis | GPA limits (FR-2) | -0.01, 0.00, 0.01, 1.99, 2.00, 2.01, 2.99, 3.00, 3.01, 3.49, 3.50, 3.51 (v1.1), 3.99, 4.00, 4.01 |
| | ECTS limit (FR-3) | total = limit-1, limit, limit+1 (for each GPA band) |
| | ECTS, capacity, startHour, duration (FR-1) | min-1, min, max, max+1 |
| | Capacity (FR-7) | enrolled = capacity-1 (accept), = capacity (reject) |
| | Time overlap (FR-8) | end == start (no conflict), overlap by 1 hour, identical slot, different day |
| Decision Table | FR-4…FR-7, FR-10, FR-11 | 5 rules + don't-care priority checks (Section 4); R5 also asserts enrolled+1 and ECTS total (FR-11) |
| Negative Testing | FR-1, FR-9, FR-12 | null objects, invalid fields, self-prerequisite, duplicate registration (same object and other section), completed course, drop unregistered course, drop twice |
| Regression Testing | whole suite | full suite re-run after every defect fix and after the planned change (see below); results logged per run |

**Change used for regression (implemented in v1.1, see `regression-log.md`):** after v1 is green, a
requirement change *v1.1* is introduced — honour students (**gpa ≥ 3.50**) get a 50 ECTS
limit. The whole v1 suite is re-run unchanged to show existing behaviour is preserved,
then new BVA cases (3.49 / 3.50 / 3.51) are added.

**Coverage:** JaCoCo (line + branch), target ≥ 90 % branch on business classes; `Main`
excluded. Coverage is reported but not treated as proof of correctness.

**Defect log:** every failing test is recorded with: ID, test, expected, actual, root
cause, fix, re-run result. Only defects that really occur are reported.
