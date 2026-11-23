# Mutation Testing (PIT) — Test Effectiveness

**Why:** coverage only shows that code was *executed*, not that a wrong result would be
*detected*. PIT injects small faults (mutants) into a copy of the bytecode — e.g. `<` → `<=`,
a removed condition, a changed return value — and re-runs the JUnit suite. A mutant is
**killed** if at least one test fails; a **surviving** mutant points to a weak or missing
test. This is a direct check of the RIPR model: the test must reach the fault, infect the
state, propagate it, and the assertion must reveal it.

**Setup:** `pitest-maven` 1.17.4 + `pitest-junit5-plugin` 1.2.1, mutator group `STRONGER`,
`Main` excluded.
Command: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`
Report: `target/pit-reports/index.html`

## Results

| Run | Tests | Mutants | Killed | Survived | No coverage | Mutation score | JaCoCo line / branch |
|---|---|---|---|---|---|---|---|
| 1 (v1 suite) | 146 | 166 | 164 | 1 | 1 | 98.8 % | 147/148 · 96/96 |
| 2 (after TD-01, TD-02) | 147 | 166 | 166 | 0 | 0 | 100 % | 148/148 · 96/96 |
| 3 (v1.1) | 156 | 165 | 165 | 0 | 0 | 100 % | 150/150 · 98/98 |
| 4 (v1.2, after DEF-02/03 fix) | 159 | 171 | 171 | 0 | 0 | 100 % | 158/158 · 106/106 |

JaCoCo figures are from `./mvnw clean test` (a build without `clean` after a PIT run gave
stale totals, so `clean` is always used for reported numbers).

## Analysis of run 1

**Surviving mutant — `Course.java:55`, `RemoveConditionalMutator_ORDER_ELSE`.**
PIT replaced `startHour > LAST_START_HOUR` with `false`, i.e. deleted the upper start-hour
check, and all 146 tests still passed. The BVA test for `startHour = 20` expected an
`IllegalArgumentException` and got one — but from the *end-of-day* rule
(20 + 1 > 20), not from the start-hour rule. The test passed for the wrong reason.
This line had 100 % line and branch coverage, so JaCoCo could not reveal the problem.
→ recorded as **TD-01** in the defect log.

**No-coverage mutant — `Course.toString()`.** Only the console demo called it.
→ assertion added to `validCourse`.

## Lessons
* 100 % branch coverage ≠ effective tests: the weak test sat on fully covered code.
* Negative tests must check *which* rule rejected the input (message / reason code), not
  only *that* an exception was thrown. `RegistrationService` already did this through
  `RejectionReason`; the constructor validation tests did not.
* Equivalent mutants were not observed; every generated mutant was killable.
* 100 % mutation score did **not** mean the program was correct: DEF-02 (duplicate check by object identity)
  was found later by code review. Mutants are small changes to code that exists; they cannot reveal a
  wrong design decision or a scenario no test sets up (here: two `Course` objects with the same code).
