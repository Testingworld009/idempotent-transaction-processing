# Manual QA Test Cases – Idempotent Transaction Processing Service

**Application URL:** `http://localhost:8080`  
**Tools:** Postman or curl; browser is not required for this API-only assessment.  
**Default retry limit:** 3 attempts (`transaction.retry.max-attempts=3`).

## 1. Execution rules

1. Start the service with `mvn spring-boot:run`.
2. The application uses in-memory state and a global sequence starting at 1.
3. Restart the application before each independent test case unless the steps explicitly describe a multi-request flow. This resets balances, transaction IDs, request IDs, sequence slots, and fault counters.
4. Within one test case, execute requests in the stated order.
5. Record the actual response, report output, build SHA, execution date, and PASS/FAIL.
6. Do not expose real customer data in screenshots or logs. Use synthetic IDs only.

## 2. Common request format

```json
{
  "transactionId": "TXN-001",
  "requestId": "REQ-001",
  "accountId": "ACC-001",
  "type": "CREDIT",
  "amount": 100.00,
  "currency": "USD",
  "sequenceNumber": 1
}
```

Optional for REVERSAL:

```json
"originalTransactionId": "TXN-001"
```

Submit one transaction:

```bash
curl -i -X POST http://localhost:8080/api/transactions \
  -H 'Content-Type: application/json' \
  -d '{"transactionId":"TXN-001","requestId":"REQ-001","accountId":"ACC-001","type":"CREDIT","amount":100.00,"currency":"USD","sequenceNumber":1}'
```

Check report:

```bash
curl -s http://localhost:8080/api/report
```

Check transaction outcomes:

```bash
curl -s http://localhost:8080/api/transactions
```

## 3. Detailed manual test cases

### TC-01 – Valid CREDIT transaction

- **RTM:** L2-FR-01, FR-03, FR-04
- **Priority:** Critical
- **Precondition:** Fresh application; no transactions submitted.
- **Steps:** Submit a CREDIT of 100.00 USD to ACC-001 with sequence 1.
- **Expected:** HTTP 200; status `PROCESSED`; attempts 1; report balance ACC-001 = 100.00; processed ledger entries = 1.

### TC-02 – Out-of-order sequence handling

- **RTM:** L2-FR-03, FR-04, FR-07
- **Priority:** Critical
- **Precondition:** Fresh application.
- **Steps:** (1) Submit sequence 2 DEBIT 40.00 to ACC-201. (2) Check report. (3) Submit sequence 1 CREDIT 100.00 to ACC-201. (4) Check report and transaction list.
- **Expected:** First request is `PENDING_SEQUENCE`; report shows missing sequence 1. After sequence 1 arrives, sequence 1 and sequence 2 are processed in order; final balance ACC-201 = 60.00; no overdraft failure.

### TC-03 – Exact duplicate delivery

- **RTM:** L2-FR-05, FR-06
- **Priority:** Critical
- **Precondition:** Fresh application.
- **Steps:** Submit TXN-003 / REQ-003 CREDIT 75.00 at sequence 1. Submit the identical request again using the same transactionId and requestId.
- **Expected:** First response `PROCESSED`; second response `DUPLICATE`; balance remains 75.00; ledger entries = 1.

### TC-04 – Duplicate business transaction with a new request ID

- **RTM:** L2-FR-05, FR-06
- **Priority:** Critical
- **Precondition:** Fresh application.
- **Steps:** Submit TXN-004 / REQ-004 CREDIT 50.00 at sequence 1. Submit same business transaction with requestId REQ-004-REPLAY and same account/type/amount/currency/sequence.
- **Expected:** Second request is `DUPLICATE`; balance remains 50.00; ledger entries = 1; duplicate delivery count increments.

### TC-05 – Processing report and reconciliation

- **RTM:** L2-FR-03, FR-11
- **Priority:** High
- **Precondition:** Fresh application.
- **Steps:** Submit CREDIT 100.00 at sequence 1 and DEBIT 20.00 at sequence 2 for the same account/currency. Call GET `/api/report`.
- **Expected:** Balance = 80.00; processed ledger entries = 2; status counts show two processed transactions; transaction outcomes are present.

### TC-06 – Missing mandatory field

- **RTM:** L2-FR-02
- **Priority:** High
- **Precondition:** Fresh application.
- **Steps:** Submit a request with `accountId` omitted, keeping other fields valid and sequenceNumber = 1.
- **Expected:** Result status `FAILED`, code `VALIDATION`; no balance update; following valid sequence 2 can still be processed. For this service, a valid positive sequence on a poison record consumes its sequence slot when it reaches the head.

### TC-07 – Invalid amount boundaries

- **RTM:** L2-FR-02
- **Priority:** High
- **Precondition:** Restart between each independent amount variant, or use distinct sequence numbers and inspect each result.
- **Steps:** Submit separate requests with amount values `-1.00`, `0.00`, and `10.123`.
- **Expected:** Each is `FAILED/VALIDATION`; no balance or ledger effect. Amount must be positive and have no more than two decimal places.

### TC-08 – Unsupported transaction type / malformed JSON

- **RTM:** L2-FR-02
- **Priority:** High
- **Precondition:** Service running.
- **Steps:** (1) POST malformed JSON such as `{broken`. (2) In a fresh app, submit a valid JSON request with type `TRANSFER`.
- **Expected:** Malformed/unsupported JSON binding returns HTTP 400 with code `INVALID_JSON`; no stack trace or business effect.

### TC-09 – Same transaction ID with changed payload

- **RTM:** L2-FR-05, FR-06
- **Priority:** Critical
- **Precondition:** Fresh application.
- **Steps:** Submit TXN-009 CREDIT 100.00 at sequence 1. Submit TXN-009 again with a different request ID and amount 200.00.
- **Expected:** Second result `FAILED/CONFLICT`; original balance remains 100.00; original ledger entry is unchanged.

### TC-10 – Request ID reused for another transaction

- **RTM:** L2-FR-02, FR-05
- **Priority:** High
- **Precondition:** Fresh application.
- **Steps:** Submit TXN-010 / REQ-SHARED as CREDIT 100.00 at sequence 1. Submit TXN-011 using the same REQ-SHARED but sequence 2 and a different transactionId.
- **Expected:** Second result `FAILED/REQUEST_ID_REUSE`; first transaction remains processed; no effect from second transaction.

### TC-11 – Transient failure followed by successful retry

- **RTM:** L2-FR-08, FR-09
- **Priority:** Critical
- **Precondition:** Fresh application.
- **Steps:** (1) Inject one failure: `curl -X POST 'http://localhost:8080/api/test/faults/TXN-011?count=1'`. (2) Submit TXN-011 CREDIT 100.00 at sequence 1. (3) Check report.
- **Expected:** Final status `PROCESSED`; attempts = 2; balance = 100.00; ledger entries = 1. The transient failure must not apply a partial financial effect.

### TC-12 – Retry exhaustion

- **RTM:** L2-FR-08, FR-09
- **Priority:** High
- **Precondition:** Fresh application; default maximum attempts = 3.
- **Steps:** Inject three failures: `curl -X POST 'http://localhost:8080/api/test/faults/TXN-012?count=3'`. Submit TXN-012 CREDIT 100.00 at sequence 1. Then submit a valid CREDIT at sequence 2.
- **Expected:** TXN-012 becomes `FAILED` with code `RETRY_EXHAUSTED`; no balance/ledger effect for TXN-012; sequence 2 is not blocked and can process. The retry endpoint is assessment-only.

### TC-13 – Sequence gap visibility

- **RTM:** L2-FR-07, FR-11
- **Priority:** High
- **Precondition:** Fresh application.
- **Steps:** Submit valid CREDIT at sequence 2, then another valid CREDIT at sequence 3. Call GET `/api/report`.
- **Expected:** Both transactions are `PENDING_SEQUENCE`; next expected sequence = 1; missingSequences includes 1; balances unchanged.

### TC-14 – Invalid poison record does not block later sequence

- **RTM:** L2-FR-02, FR-07
- **Priority:** High
- **Precondition:** Fresh application.
- **Steps:** Submit invalid negative-amount transaction at sequence 1. Submit valid CREDIT 20.00 at sequence 2. Check report.
- **Expected:** First record `FAILED/VALIDATION`; its sequence slot is consumed when it reaches the head; second transaction is processed; balance = 20.00.

### TC-15 – Batch processing and record isolation

- **RTM:** L2-FR-01, FR-10, FR-11
- **Priority:** High
- **Precondition:** Fresh application; sample file available.
- **Steps:** Inject one transient failure for TXN-2005. POST `data/sample_input.json` to `/api/transactions/batch`.
- **Expected:** Six result items; final counts PROCESSED=4, DUPLICATE=1, FAILED=1 (VALIDATION); ACC-201=85.00 USD; ACC-203=500.00 USD; ledger entries=4. See `docs/REAL_TIME_FLOW.md`.

### TC-16 – Insufficient funds and currency mismatch

- **RTM:** Business-rule extension; NFR-03
- **Priority:** High
- **Precondition:** Fresh application.
- **Steps A:** Submit DEBIT 10.00 USD at sequence 1 against a new account with no funds.
- **Expected A:** `FAILED/INSUFFICIENT_FUNDS`; no negative balance; no ledger entry.
- **Steps B:** In a fresh app, submit CREDIT 100.00 USD to ACC-CUR at sequence 1, then CREDIT 10.00 EUR to ACC-CUR at sequence 2.
- **Expected B:** First transaction processed; second fails with `CURRENCY_MISMATCH`; balance remains 100.00 USD.

### TC-17 – Automated regression and fresh-clone verification

- **RTM:** L2-FR-12, NFR-08
- **Priority:** Critical
- **Precondition:** Java 17 and Maven installed; repository cloned into a clean directory.
- **Steps:** Run `mvn clean verify`; inspect Surefire reports and JaCoCo HTML report.
- **Expected:** Build exits successfully; all automated tests pass; report is generated at `target/site/jacoco/index.html`. Record actual test count and coverage from this run; do not copy numbers from another implementation.

### TC-18 – Performance / hot-path baseline

- **RTM:** NFR-01, NFR-07
- **Priority:** Medium
- **Precondition:** Stable test machine; service running with a clean state for each comparable run; JMeter installed.
- **Steps:** Create a JMeter test plan for transaction POST requests with unique transaction IDs and increasing sequence numbers. Run baseline at low concurrency, then increase concurrency gradually. Capture throughput, error rate, average, p95 and p99 latency, CPU and memory. Repeat with duplicate-heavy traffic and report reads.
- **Expected:** No unexplained errors or duplicate financial effects. Record environment and measured results. No numerical performance target is specified in the assessment, so establish a baseline before setting a threshold. The current synchronized in-memory service is a single-process assessment implementation, not a multi-node throughput claim.

### TC-19 – Reversal and duplicate reversal

- **RTM:** Business-rule extension
- **Priority:** Medium
- **Precondition:** Fresh application.
- **Steps:** (1) CREDIT 100.00 at sequence 1. (2) DEBIT 30.00 at sequence 2. (3) REVERSAL at sequence 3 with amount 30.00 and `originalTransactionId` equal to the debit transaction ID. (4) Submit another reversal of the same original at sequence 4.
- **Expected:** First two transactions processed; reversal restores balance to 100.00; second reversal fails with `ALREADY_REVERSED`. Confirm reversal semantics with the business owner before production use.

### TC-20 – Concurrent duplicate submission (single JVM)

- **RTM:** L2-FR-05; NFR-03, NFR-07
- **Priority:** High
- **Precondition:** Fresh application; use a controlled concurrency tool such as JMeter or a small parallel client.
- **Steps:** Send multiple simultaneous POST requests with identical transactionId, business payload, and sequenceNumber. Use the same request ID for a strict replay or distinct request IDs for duplicate delivery semantics.
- **Expected:** Exactly one business effect and one ledger entry in this single JVM; other accepted replays are reported as duplicates. This does not prove multi-process or multi-node safety.

## 4. Sample test data

The canonical six-record assessment data is `data/sample_input.json`. Do not edit it to hide failures. Create separate files under `data/` for additional exploratory cases and commit them with meaningful names.

## 5. Defect reporting template

```text
Defect ID:
Title:
Severity: Blocker / Critical / Major / Minor
Build / Git commit SHA:
Environment:
Requirement ID / Test Case ID:
Preconditions:
Steps to reproduce:
Expected result:
Actual result:
Sanitized request / response:
Evidence (screenshot or log reference):
Impact:
Suggested owner:
```

## 6. Execution record template

| TC ID | Build SHA | Execution date | Actual result | Status (PASS/FAIL/BLOCKED) | Defect ID | Evidence link |
|---|---|---|---|---|---|---|
| TC-01 |  |  |  |  |  |  |
| TC-02 |  |  |  |  |  |  |
| TC-03 |  |  |  |  |  |  |
