# Regression Testing Log — v1.0 → v1.1

## Change
**FR-2 (v1.1):** honour students with **gpa ≥ 3.50** may register up to **50 ECTS**
(v1.0: every gpa ≥ 3.00 had 45). Code change: one extra branch in
`CreditPolicy.maxEcts` and the constant `HONOUR_LIMIT = 50`.
Work done on branch `feature/v1.1-honours-limit`; merged to `main` only after the full
suite was green.

## Step 1 — re-run the unchanged v1.0 suite against v1.1 code
Command: `./mvnw clean test` (test sources identical to `main`).

Result: **147 run, 143 passed, 4 failed** — `BUILD FAILURE`.

```
[ERROR]   CreditPolicyTest.boundaryValues:36 expected: <45> but was: <50>
[ERROR]   CreditPolicyTest.boundaryValues:36 expected: <45> but was: <50>
[ERROR]   CreditPolicyTest.equivalenceClasses:24 expected: <45> but was: <50>
[ERROR]   RegistrationServiceTest$EctsLimit.ectsBoundaries:197 Multiple Failures (2 failures)
[ERROR] Tests run: 147, Failures: 4, Errors: 0, Skipped: 0
```

| # | Failing test (parameters) | v1.0 expectation | v1.1 actual | Classification |
|---|---|---|---|---|
| 1 | `CreditPolicyTest.equivalenceClasses` (3.50) | 45 | 50 | Obsolete expectation — FR-2 changed |
| 2 | `CreditPolicyTest.boundaryValues` (3.99) | 45 | 50 | Obsolete expectation |
| 3 | `CreditPolicyTest.boundaryValues` (4.00) | 45 | 50 | Obsolete expectation |
| 4 | `EctsLimit.ectsBoundaries` (gpa 3.50, 31 + 15 = 46) | REJECTED | REGISTERED | Obsolete expectation — new limit 50 |

**Real regressions: 0.** The 143 passing tests show that everything not covered by the
change still behaves as before: all 16 decision-table combinations, FR-10 priority,
capacity, time conflict, drop, duplicate/negative cases and input validation.

Observation: every failure was in a test that encodes FR-2 directly or indirectly. This is
the expected "footprint" of the change, which gives confidence that the change did not leak
into unrelated rules. Had any decision-table or validation test failed, it would have been a
real regression.

## Step 2 — update obsolete tests and add tests for the new boundary
* EP: GPA class [3.00, 4.00] split into [3.00, 3.50) → 45 (rep. 3.25) and [3.50, 4.00] → 50 (rep. 3.75).
* BVA (FR-2): added 3.49 → 45, 3.50 → 50, 3.51 → 50; 3.99 and 4.00 now expect 50.
* BVA (FR-3): band 45 moved to gpa 3.25 (44 / 45 / 46); new band 50 at gpa 3.75 (49 / 50 / 51);
  GPA boundary in registration: 3.49 with 50 ECTS → rejected, 3.50 → registered.
* Ordering check extended: low < standard < high < honour.

## Step 3 — full regression run on v1.1

| Run | Code | Tests | Passed | Failed | JaCoCo line · branch | PIT killed |
|---|---|---|---|---|---|---|
| v1.0 baseline (`main`) | v1.0 | 147 | 147 | 0 | 148/148 · 96/96 | 166/166 |
| Regression run 1 | v1.1 | 147 (v1.0 tests) | 143 | 4 | — | — |
| Regression run 2 | v1.1 | 156 | 156 | 0 | 150/150 · 98/98 | 165/165 |

(PIT generates 165 instead of 166 mutants because the extra `else if` changes the bytecode.)

## v1.1 → v1.2 (defect fix DEF-02, DEF-03)
Three new tests were written first and run against the unfixed v1.1 code:

```
[ERROR]   CourseTest$Validation.selfPrerequisite:144 Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
[ERROR]   RegistrationServiceTest$Negative.sameCodeOtherSection:343 Multiple Failures (2 failures)
[ERROR]   RegistrationServiceTest$Drop.dropBySameCodeOtherObject:365 » IllegalState Student 2021001 is not registered for SWE201
[ERROR] Tests run: 159, Failures: 2, Errors: 1, Skipped: 0
```

Only the 3 new tests failed. After the fix the full suite was re-run: **159 / 159 passed**, so none of the
156 earlier tests was broken by the change (branch `fix/course-identity`).

| Run | Code | Tests | Passed | Failed | JaCoCo line · branch | PIT killed |
|---|---|---|---|---|---|---|
| Red run | v1.1 | 159 | 156 | 3 (new tests) | — | — |
| After fix | v1.2 | 159 | 159 | 0 | 158/158 · 106/106 | 171/171 |

## Test refactoring (v1.2): simpler test code, same test cases
The test classes were rewritten in a plainer style (no `@Nested`, no `assertAll`, no helper that takes code
as a parameter, no `if/else` inside tests; decision-table and ECTS-limit tests split into an *accepted* and a
*rejected* method). The production code was not touched. Every test case was kept, so the refactoring was
checked like any other change: the full suite and PIT were re-run.

| Run | Tests (per class) | Passed | JaCoCo line · branch | PIT killed |
|---|---|---|---|---|
| Before refactoring | 159 (Course 66, CreditPolicy 25, RegistrationService 52, Student 16) | 159 | 158/158 · 106/106 | 171/171 |
| After refactoring | 159 (Course 66, CreditPolicy 25, RegistrationService 52, Student 16) | 159 | 158/158 · 106/106 | 171/171 |

Course-code tests gained a message check as well (they previously only checked that an exception was thrown).
