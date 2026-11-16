# Architecture and Design Decisions

## Architecture

REST Controller -> Transaction Service -> in-memory transaction state, sequence slots, account balances and ledger.

The controller owns HTTP concerns. The service owns validation, idempotency, ordering and business processing. Domain records describe requests, statuses and reports.

## Decisions

1. Java 17 and Spring Boot for an enterprise-friendly service runtime.
2. `BigDecimal` for monetary values; floating-point arithmetic is not used.
3. Global sequence ordering is selected because the supplied sample has sequence numbers 1–5 across ACC-201, ACC-202 and ACC-203. The original specification does not explicitly settle global versus per-account ordering; this is an explicit assumption to confirm with the assessment owner.
4. `transactionId` protects the business effect. `requestId` tracks request delivery and cannot be reused for a different transaction.
5. Same transaction ID and same business fingerprint is a duplicate; same ID with a changed payload is a conflict.
6. Future sequence numbers are held until the next expected sequence arrives. A malformed sequenced record consumes its sequence slot when it reaches the head, so a poison record does not block later work.
7. Transient failures are retried up to a configurable maximum. Business failures are terminal.
8. A single synchronized critical section provides deterministic single-process behaviour. It is not a distributed lock and does not provide multi-instance safety.
9. In-memory state is used to keep the assessment runnable without external services. It is lost on restart.

## Production path

Use PostgreSQL with unique constraints for transaction ID, request ID and ledger transaction ID; wrap balance, ledger and transaction-state changes in one database transaction; coordinate sequence partitions with database locking; add a durable queue adapter with acknowledgement after commit; add an outbox for downstream effects and a DLQ for poison records.
