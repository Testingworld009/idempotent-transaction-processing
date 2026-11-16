# Idempotent Background Transaction Processing Service

**Assessment:** Practical FDE L2 – Idempotent Background Transaction Processing Service  
**Stack:** Java 17 · Spring Boot 3 · Maven · JUnit 5 · Mockito/Spring Test · Rest Assured · JaCoCo · GitHub Actions  
**Service type:** REST-enabled background transaction processor

## 1. Business problem

Upstream systems can deliver transaction events more than once, out of order, or after a transient failure. This service demonstrates how to process `CREDIT`, `DEBIT`, and `REVERSAL` requests while preventing duplicate business effects, preserving a deterministic sequence, isolating invalid records, and reporting processing outcomes.

The supplied assessment's six-record sample is included in `data/sample_input.json`.

## 2. Scope and important assumptions

### Implemented in this assessment version

- REST endpoint for one transaction and a batch of transactions.
- Input validation for required identifiers, transaction type, positive amount, supported currency, sequence number, and reversal reference.
- Duplicate detection by `transactionId` and a business-payload fingerprint.
- `CONFLICT` when the same transaction ID is reused with a different business payload.
- Request-ID reuse detection.
- Global sequence-based ordering (sequence 1, 2, 3, …), selected to match the supplied sample data across accounts.
- Pending future sequence events and automatic drain when the missing sequence arrives.
- CREDIT, DEBIT, and simplified REVERSAL handling.
- Insufficient-funds and account-currency checks.
- Deterministic transient-failure injection for retry demonstrations.
- Processing report with status counts, balances, duplicate count, ledger count, next expected sequence, missing sequences, and transaction outcomes.
- Unit tests, REST API tests, JaCoCo report configuration, and GitHub Actions workflow.

### Explicit limitations

- State is held in memory. Restarting the application clears balances, sequence state, duplicate records, and ledger entries.
- The sequence is **global**, not per account. The source specification does not explicitly resolve this; the global interpretation follows its sample sequence numbers. Confirm this assumption with the assessment owner.
- The synchronized service provides single-JVM serialization only. It is not a distributed lock and does not establish multi-instance safety.
- Retry is synchronous and deterministic for the assessment. Production retry should use durable scheduling, backoff/jitter, queue acknowledgement, and a dead-letter queue.
- The reversal implementation is intentionally simplified and should be aligned with the business owner's reversal policy before production use.
- The Selenium classes are only an optional foundation for a future browser dashboard. No dashboard is part of the assessment. Use Rest Assured for the current API tests.
- Do not reuse performance figures from the supplied Python reference project. Measure this Java implementation separately.

## 3. Architecture

```text
REST Client / Batch Input
          |
          v
TransactionController ----> ApiExceptionHandler
          |
          v
TransactionService
   |       |        |        |
   |       |        |        +--> Retry / fault simulation
   |       |        +-----------> Sequence slots / ordering
   |       +--------------------> Idempotency / request ownership
   +----------------------------> Validation / business rules
          |
          v
In-memory transaction records, balances, sequence slots and ledger
          |
          v
ProcessingReport / transaction outcomes
```

Detailed decisions are in [`docs/ARCHITECTURE_AND_DECISIONS.md`](docs/ARCHITECTURE_AND_DECISIONS.md).

## 4. Prerequisites

- JDK 17
- Maven 3.8 or later
- Git
- IntelliJ IDEA or another Java IDE (optional)
- Postman or curl for manual API testing

Verify:

```bash
java -version
mvn -version
git --version
```

## 5. Build, test, and run

From the project root:

```bash
mvn clean verify
```

Run the service:

```bash
mvn spring-boot:run
```

Base URL: `http://localhost:8080`

The application uses in-memory state. Restart it between independent manual test scenarios unless a test explicitly needs multiple requests in the same scenario.

## 6. REST API reference

### 6.1 Submit one transaction

`POST /api/transactions`

Example:

```bash
curl -i -X POST http://localhost:8080/api/transactions \
  -H 'Content-Type: application/json' \
  -d '{"transactionId":"TXN-1","requestId":"REQ-1","accountId":"ACC-1","type":"CREDIT","amount":100.00,"currency":"USD","sequenceNumber":1}'
```

Expected successful response fields include `transactionId`, `requestId`, `status`, `code`, `attempts`, and `message`. A processed transaction returns HTTP 200. A business/validation result with status `FAILED` returns HTTP 422.

### 6.2 Submit a batch

`POST /api/transactions/batch`

```bash
curl -i -X POST http://localhost:8080/api/transactions/batch \
  -H 'Content-Type: application/json' \
  --data-binary @data/sample_input.json
```

The batch endpoint returns one result per submitted input record. It continues through records rather than failing the complete batch for a validation/business failure.

### 6.3 Read transaction outcomes

`GET /api/transactions`

```bash
curl -s http://localhost:8080/api/transactions
```

### 6.4 Read processing report

`GET /api/report`

```bash
curl -s http://localhost:8080/api/report
```

The report contains status counts, balances, duplicate delivery count, processed ledger entry count, next expected sequence, missing sequence numbers, and transaction results.

### 6.5 Inject a transient failure (assessment only)

`POST /api/test/faults/{transactionId}?count={n}`

```bash
curl -i -X POST 'http://localhost:8080/api/test/faults/TXN-2005?count=1'
```

Call this before submitting the transaction/batch. With the default maximum of three attempts, one injected failure should be followed by a successful attempt. This endpoint must be disabled or protected in production.

## 7. Supplied sample: expected real-time flow

Before running the sample, restart the application so the global expected sequence is 1 and the in-memory store is empty.

1. Inject one transient failure for `TXN-2005`.
2. Submit `data/sample_input.json` to `/api/transactions/batch`.
3. Inspect the batch response and `GET /api/report`.

Expected final summary:

| Measure | Expected |
|---|---:|
| PROCESSED | 4 |
| DUPLICATE | 1 |
| FAILED | 1 (VALIDATION) |
| ACC-201 | 85.00 USD |
| ACC-203 | 500.00 USD |
| Processed ledger entries | 4 |

Flow explanation:

- `TXN-2002` sequence 2 arrives first and is held as `PENDING_SEQUENCE`.
- `TXN-2001` sequence 1 arrives; it is processed, then sequence 2 is released and processed.
- The later delivery of `TXN-2002` is detected as a duplicate and has no second balance effect.
- `TXN-2003` sequence 3 is processed.
- `TXN-2004` sequence 4 has a negative amount; it fails validation and its sequence slot is consumed so the stream can continue.
- `TXN-2005` sequence 5 encounters the injected transient failure, retries, and is processed once.

## 8. Testing strategy

| Test layer | Tool | Purpose |
|---|---|---|
| Developer unit tests | JUnit 5 | Service rules, ordering, duplicates, retry, validation |
| API integration tests | Spring Boot Test + Rest Assured | HTTP endpoints, response codes and JSON contract |
| Manual functional tests | Postman / curl | Business workflow and acceptance evidence |
| Regression | Maven test suite | Re-run automated tests after code changes |
| UI automation | Selenium (optional only) | Future dashboard browser flows; no current UI exists |
| Performance | JMeter (planned) | Throughput, latency percentiles and concurrency measurements |
| Coverage | JaCoCo | Generate Java coverage evidence |

Manual test cases and their RTM mapping are in [`docs/MANUAL_TEST_CASES.md`](docs/MANUAL_TEST_CASES.md). The requirement-to-test mapping is in [`docs/REQUIREMENT_TRACEABILITY_MATRIX.md`](docs/REQUIREMENT_TRACEABILITY_MATRIX.md). The end-to-end operational walkthrough is in [`docs/REAL_TIME_FLOW.md`](docs/REAL_TIME_FLOW.md).

Run tests and generate the coverage report:

```bash
mvn clean verify
```

JaCoCo HTML report (after a successful build): `target/site/jacoco/index.html`.

## 9. GitHub setup and submission

```bash
git init -b main
git add .
git commit -m "chore: add Java transaction processing assessment"
git remote add origin https://github.com/<YOUR_USERNAME>/<YOUR_REPOSITORY>.git
git push -u origin main
```

Before making the repository public, inspect the staged files and ensure no credentials, tokens, private customer data, or local database files are committed. The included GitHub Actions workflow runs `mvn --batch-mode clean verify` on pushes and pull requests to `main`.

## 10. Production-readiness roadmap

1. Replace in-memory maps with PostgreSQL and enforce unique constraints for transaction ID, request ID, sequence ownership, and ledger transaction ID.
2. Persist account balance, ledger entry, and transaction status atomically in one database transaction.
3. Revisit global ordering; partition by the agreed business ordering key (for example, account) if the domain requires it.
4. Use durable queue consumption, acknowledgement only after commit, retry scheduling, and a dead-letter queue.
5. Add multi-instance concurrency controls and idempotent downstream delivery (for example, transactional outbox).
6. Secure endpoints, remove assessment-only fault injection, add authentication/authorization, rate limits, audit controls, metrics, and alerts.
7. Run Java-specific JMeter load, stress, and soak tests and publish measured results with environment details.

## 11. Assessment integrity

Only claim tests, coverage, performance, or CI results that you have personally executed against this Java repository. Complete `docs/AI_USAGE_TEMPLATE.md` in your own words and be prepared to explain every design decision and code path.
