# Course Registration Eligibility Checker

A small Java 17 library that decides whether a student can register for a course — and if
not, **why** — based on GPA-dependent ECTS limits, prerequisites, unpaid fees, advisor
approval, quotas and timetable clashes. Built for the *Software Verification and Validation*
course to practise systematic unit testing with **JUnit 5**.

## Results at a glance (v1.2)

| Metric | Value |
|---|---|
| JUnit 5 tests | 159 / 159 passing |
| JaCoCo line coverage | 158 / 158 (100 %) |
| JaCoCo branch coverage | 106 / 106 (100 %) |
| PIT mutation score (`STRONGER`) | 171 / 171 killed (100 %) |
| Defects found | 3 product defects, 2 test-suite defects ([defect log](docs/defect-log.md)) |

## Rules (FR-1 … FR-12)
* **ECTS limit by GPA:** < 2.00 → 30 · 2.00–2.99 → 37 · 3.00–3.49 → 45 · ≥ 3.50 → 50 (v1.1)
* Rejected with a reason code if: already registered, already completed, fees unpaid,
  prerequisite missing, advisor approval missing, time conflict, ECTS limit exceeded, course full
  — reported in this priority order.
* Invalid data (e.g. GPA 4.01, course code `swe301`) → `IllegalArgumentException`;
  dropping a course that was never registered → `IllegalStateException`.

Full specification: [docs/requirements.md](docs/requirements.md)

## Build and run
Only a JDK 17+ is needed; the Maven Wrapper downloads Maven on first use.

```bash
./mvnw clean test                                              # tests + JaCoCo report
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage    # mutation testing
./mvnw -q compile && java -cp target/classes edu.svv.registration.Main   # console demo
```
On Windows use `mvnw.cmd` instead of `./mvnw`.

Reports are written to `target/site/jacoco/index.html` and `target/pit-reports/index.html`.
Snapshots of the submitted runs are kept in [docs/reports](docs/reports).

## Project structure
```
src/main/java/edu/svv/registration/
  Course, Student            validated domain objects (FR-1), time overlap (FR-8)
  CreditPolicy               ECTS limit by GPA (FR-2)
  RegistrationService        register / drop rules (FR-3 … FR-12)
  RegistrationResult, RejectionReason
  Main                       console demo (excluded from coverage)
src/test/java/edu/svv/registration/
  StudentTest, CourseTest, CreditPolicyTest, RegistrationServiceTest
docs/
  requirements.md            requirements, decision table, test plan
  defect-log.md              defects found and fixed
  mutation-testing.md        PIT analysis
  regression-log.md          v1.0 → v1.1 regression run
  reports/                   JaCoCo, PIT and test-execution snapshots
```

## Test design techniques
| Technique | Where |
|---|---|
| Equivalence Partitioning | GPA bands, course-code format, weekday vs weekend |
| Boundary Value Analysis | GPA limits (1.99/2.00, 2.99/3.00, 3.49/3.50), ECTS limit ±1, capacity, start hour, duration, end of day, adjacent time slots |
| Decision Table | fees × prerequisites × advisor × seat — all 16 combinations |
| Negative Testing | null / malformed input, duplicate registration, dropping an unregistered course |
| Regression Testing | unchanged v1.0 suite re-run on v1.1 code ([log](docs/regression-log.md)) |

## Development workflow
Every change was made on its own branch and merged through a pull request
(`feature/v1-registration`, `feature/pit-mutation`, `feature/v1.1-honours-limit`).

## Tools
Java 17 · JUnit 5.11 · Maven 3.9 (wrapper) · JaCoCo 0.8.12 · PIT 1.17.4.
AI assistance (Claude) was used during development, with the instructor's permission.
