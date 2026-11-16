# Real-Time Business Flow – Assessment Sample

## 1. Business context

An upstream payment system sends six transaction events for a background processor. The events are not delivered in sequence and one event is delivered twice. One record is invalid, and one valid record experiences a simulated transient failure.

The service must preserve the business outcome while isolating the invalid record and preventing duplicate financial effects.

## 2. Starting state

Restart the Spring Boot application before this demonstration.

```text
Expected global sequence = 1
Transactions stored      = 0
Ledger entries           = 0
Balances                 = empty
```

Start the app:

```bash
mvn spring-boot:run
```

## 3. Inject the transient failure

Before submitting the batch, ask the service to fail the first attempt of TXN-2005:

```bash
curl -i -X POST 'http://localhost:8080/api/test/faults/TXN-2005?count=1'
```

This is a deterministic assessment hook, not a production retry scheduler.

## 4. Submit the sample batch

```bash
curl -i -X POST http://localhost:8080/api/transactions/batch \
  -H 'Content-Type: application/json' \
  --data-binary @data/sample_input.json
```

## 5. Record-by-record processing flow

| Input order | Transaction | Sequence | Input condition | Processing behaviour | Final status |
|---:|---|---:|---|---|---|
| 1 | TXN-2002 | 2 | DEBIT 40.00, ACC-201 | Sequence 1 is missing, so buffer it | PROCESSED after sequence 1 arrives |
| 2 | TXN-2001 | 1 | CREDIT 100.00, ACC-201 | Process sequence 1, then drain ready sequence 2 | PROCESSED |
| 3 | TXN-2002 | 2 | Same business transaction, new request ID | Fingerprint identifies an already-seen transaction | DUPLICATE |
| 4 | TXN-2003 | 3 | CREDIT 25.00, ACC-201 | Next contiguous sequence | PROCESSED |
| 5 | TXN-2004 | 4 | DEBIT -10.00, ACC-202 | Validation rejects non-positive amount; poison slot is consumed | FAILED / VALIDATION |
| 6 | TXN-2005 | 5 | CREDIT 500.00, ACC-203 | First attempt hits injected transient fault; second attempt succeeds | PROCESSED |

## 6. Business balance calculation

ACC-201:

```text
Opening balance                 0.00
TXN-2001 CREDIT                +100.00
TXN-2002 DEBIT                  -40.00
TXN-2003 CREDIT                 +25.00
TXN-2002 duplicate effect         0.00
--------------------------------------
Expected closing balance        85.00 USD
```

ACC-203:

```text
Opening balance                 0.00
TXN-2005 CREDIT                +500.00
--------------------------------------
Expected closing balance       500.00 USD
```

ACC-202 remains without a balance update because its debit amount is invalid.

## 7. Expected final report

Call:

```bash
curl -s http://localhost:8080/api/report
```

Expected key values:

| Report field | Expected value |
|---|---:|
| `statusCounts.PROCESSED` | 4 |
| `statusCounts.DUPLICATE` | 1 |
| `statusCounts.FAILED` | 1 |
| `balances.ACC-201` | 85.00 |
| `balances.ACC-203` | 500.00 |
| `duplicateDeliveries` | 1 |
| `processedLedgerEntries` | 4 |
| `nextExpectedSequence` | 6 |

The exact JSON includes transaction result objects and other status counts. Compare the key values above; do not compare JSON object property order.

## 8. What to explain to the client / interviewer

**Requirement understanding:** The main risk is duplicate financial effects when upstream delivery is at-least-once and events arrive out of order.

**Idempotency:** The service uses transaction ID plus a normalized business fingerprint. The same transaction with the same business payload is a duplicate; reusing the transaction ID with a changed payload is a conflict.

**Ordering:** The implementation uses a global sequence buffer because the sample sequence runs across multiple accounts. A future sequence is held until the next expected sequence arrives. This is an explicit assumption that must be confirmed with the business owner.

**Resilience:** A transient failure is simulated before the business effect. The service retries within the configured maximum. The successful retry applies the effect once.

**Failure isolation:** A malformed/invalid record is reported as failed. Its sequence slot is consumed when it reaches the head so a poison record does not block later work.

**Consistency limitation:** The current in-memory synchronized implementation provides single-process protection only. Production requires database transactions, unique constraints, durable retry, queue acknowledgement after commit, and multi-instance coordination.

## 9. QA evidence to capture

- Batch API request and response (synthetic data only).
- `GET /api/report` response after the batch.
- `GET /api/transactions` response showing statuses.
- Application log excerpt showing retry/processed/duplicate events without sensitive values.
- Git commit SHA and Java/Maven versions.
- `mvn clean verify` output and JaCoCo report.

Do not claim a test passed until it has been run and the evidence is saved.
