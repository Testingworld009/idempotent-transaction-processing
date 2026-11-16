# Requirement Traceability Matrix (RTM)

**Project:** Idempotent Background Transaction Processing Service  
**Implementation:** Java 17 + Spring Boot + Maven  
**Evidence rule:** A manual case is a proposed execution until a tester records Actual Result, Status, date, and build/commit SHA. Automated test names below are the current Java repository evidence; do not treat planned tests as executed.

## 1. Functional requirement mapping

| Requirement ID | Assessment requirement | Java implementation / component | Manual test case(s) | Current automated evidence | Acceptance expectation |
|---|---|---|---|---|---|
| L2-FR-01 | Accept requests from replaceable/configurable input | `TransactionController`: POST single and batch endpoints | TC-01, TC-15 | `TransactionApiTest` | Single and batch inputs accepted; result returned per record |
| L2-FR-02 | Validate mandatory fields and reject malformed records | `TransactionService.validate`; `ApiExceptionHandler` | TC-06, TC-07, TC-08, TC-09, TC-10 | `TransactionServiceTest.invalidRecordDoesNotStopFollowingSequence`; `TransactionApiTest.malformedJsonReturnsBadRequest` | Invalid record rejected; malformed JSON returns HTTP 400; batch can continue |
| L2-FR-03 | Maintain transaction processing state | `MutableRecord`, in-memory records, `GET /api/transactions` | TC-02, TC-03, TC-05, TC-11, TC-12 | Service tests and report API test | State and outcome visible through transaction listing/report |
| L2-FR-04 | Maintain status lifecycle | `TransactionStatus` enum | TC-02, TC-03, TC-11, TC-12 | Service ordering/retry tests | RECEIVED, PENDING_SEQUENCE, PROCESSING, PROCESSED, DUPLICATE, FAILED and RETRY_PENDING represented where applicable |
| L2-FR-05 | Process each unique business transaction once | `fingerprints` map and synchronized `submit` | TC-03, TC-04, TC-17 | `TransactionServiceTest.duplicateDoesNotApplySecondEffect` | One business effect and one ledger entry per transaction ID |
| L2-FR-06 | Detect late duplicates | Fingerprint lookup before sequence reservation | TC-04 | `TransactionServiceTest.duplicateDoesNotApplySecondEffect` | Same business payload with another request ID is DUPLICATE |
| L2-FR-07 | Define out-of-order strategy | Global `slots` map and `drainReadySlots` | TC-02, TC-13, TC-14 | `TransactionServiceTest.processesCreditAndDebitInSequence` | Future sequence waits; contiguous slots drain in order |
| L2-FR-08 | Simulate transient failure | `POST /api/test/faults/{transactionId}` and fault counter | TC-11, TC-12 | `TransactionServiceTest.transientFailureRetriesWithoutDuplicateEffect`; `retryExhaustionDoesNotApplyEffect` | Transient failure can be injected deterministically |
| L2-FR-09 | Retry transient failures without duplicate effects | `processWithRetry` | TC-11, TC-12 | Retry success and exhaustion service tests | Retry success creates one effect; exhausted retry creates no effect |
| L2-FR-10 | Separate business logic from input handling | Controller/domain/service package separation | TC-01, TC-15 | Project structure and Spring API tests | Controller handles HTTP; service owns business processing |
| L2-FR-11 | Produce processing summary by status | `ProcessingReport`, `GET /api/report` | TC-05, TC-15 | `TransactionApiTest.reportEndpointIsAvailable` | Status counts, balances, ledger count, missing sequences and outcomes available |
| L2-FR-12 | Test duplicate, ordering, invalid and retry scenarios | JUnit 5 and Rest Assured test packages | TC-01 through TC-17 | `mvn clean verify` (must be run locally) | Automated tests pass and manual evidence is recorded |

## 2. Non-functional requirement mapping

| NFR | Requirement | Implementation / evidence | Manual test / validation | Current limitation |
|---|---|---|---|---|
| NFR-01 | Performance | HashMap-based lookups; sequence slot lookup; synchronized critical section | TC-18 performance baseline | No Java benchmark results supplied; run JMeter and record measurements |
| NFR-02 | Reliability | Bounded synchronous retry; failure isolation | TC-11, TC-12, TC-14 | Retry is not durable across restart |
| NFR-03 | Data consistency | Balance and ledger update in one synchronized method | TC-03, TC-11, TC-12, TC-16 | In-memory atomicity only; no database transaction |
| NFR-04 | Maintainability | Layered controller/service/domain packages | Code review; Maven build | Persistence and adapters can be further separated |
| NFR-05 | Observability | Transaction list and processing report; Java logging | TC-05, TC-15 | No metrics backend, tracing, or alerting |
| NFR-06 | Security/data handling | Basic input validation and generic malformed/unexpected API errors | TC-08, TC-09, TC-10 | Authentication, authorization, rate limiting, and comprehensive log masking are not implemented |
| NFR-07 | Scalability | Replaceable API boundary and documented production roadmap | TC-18 plus architecture review | Single JVM/global lock; no multi-instance claim |
| NFR-08 | Testability | JUnit 5, Spring Boot Test, Rest Assured, JaCoCo configuration | TC-17 and Maven verification | Actual coverage percentage must be generated locally |

## 3. Manual test case index

| TC ID | Scenario | Requirement mapping | Priority |
|---|---|---|---|
| TC-01 | Valid credit transaction | FR-01, FR-03, FR-04 | Critical |
| TC-02 | Out-of-order sequence 2 before sequence 1 | FR-03, FR-04, FR-07 | Critical |
| TC-03 | Duplicate delivery, same transaction and same request | FR-05, FR-06 | Critical |
| TC-04 | Duplicate business transaction with a new request ID | FR-05, FR-06 | Critical |
| TC-05 | Processing report and balance reconciliation | FR-03, FR-11 | High |
| TC-06 | Missing mandatory field | FR-02 | High |
| TC-07 | Negative / zero / excess decimal amount | FR-02 | High |
| TC-08 | Unsupported transaction type / malformed JSON | FR-02 | High |
| TC-09 | Same transaction ID with changed payload | FR-05, FR-06 | Critical |
| TC-10 | Request ID reused for another transaction | FR-02, FR-05 | High |
| TC-11 | One transient failure then successful retry | FR-08, FR-09 | Critical |
| TC-12 | Retry exhaustion | FR-08, FR-09 | High |
| TC-13 | Invalid sequence gap and pending report | FR-07, FR-11 | High |
| TC-14 | Invalid poison record does not block later sequence | FR-02, FR-07 | High |
| TC-15 | Batch processing and per-record result isolation | FR-01, FR-10, FR-11 | High |
| TC-16 | Insufficient funds and currency mismatch | Business rules / NFR-03 | High |
| TC-17 | Regression automation and fresh build | FR-12, NFR-08 | Critical |
| TC-18 | Performance / hot-path baseline | NFR-01, NFR-07 | Medium |
| TC-19 | Reversal and duplicate reversal | Business rule extension | Medium |
| TC-20 | Concurrent duplicate submissions in one JVM | FR-05, NFR-03, NFR-07 | High |

## 4. Evidence and sign-off fields

For every executed manual test, record:

- Build/version and Git commit SHA
- Environment and Java version
- Test data used
- Actual result and response body
- PASS / FAIL / BLOCKED
- Defect ID (if failed)
- Tester and execution date
- Screenshot or sanitized log reference

**Exit rule:** Do not mark a requirement PASS solely because a test case is written. Mark it PASS only after execution evidence confirms the expected result.
