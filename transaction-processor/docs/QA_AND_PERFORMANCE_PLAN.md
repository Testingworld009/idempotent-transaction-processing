# QA, Robustness and Performance Plan

## Functional / regression scenarios

1. Credit updates balance once.
2. Debit after a prior credit updates balance correctly.
3. Sequence 2 arriving before sequence 1 is pending, then processed after sequence 1.
4. Same transaction ID and same fingerprint is duplicate; balance is unchanged.
5. Same transaction ID and changed amount is conflict; original ledger entry remains unchanged.
6. Negative, zero and over-precision amounts fail validation.
7. Insufficient funds is a terminal business failure.
8. Currency mismatch is rejected.
9. One transient failure followed by success produces one ledger entry.
10. Retry exhaustion produces FAILED/RETRY_EXHAUSTED and no balance effect.
11. Malformed JSON returns HTTP 400 without stack trace.
12. Batch report contains status counts and balances.
13. Concurrent submissions for the same transaction should be tested before production; the current synchronized service serializes calls within one JVM.

## Performance / hot path

Hot path: validate -> identify transaction -> reserve sequence -> process ready slot -> update balance/ledger -> report status.

Current optimizations: HashMap-based identifier lookups, Tree-free O(1) sequence slot lookup, single synchronized processing domain, BigDecimal correctness, no external I/O per record.

Measure Java throughput and p50/p95/p99 latency using JMeter or a repeatable JMH benchmark on the target machine. Record JVM version, heap, CPU, request mix, concurrency and payload size. The uploaded QA guide's Python/SQLite benchmark numbers do not apply to this Java implementation and must not be copied into the submission.

## Known limitations

- In-memory persistence; restart loses state.
- One JVM / one global ordering lock; no multi-instance correctness claim.
- Retry simulation is synchronous and deterministic; production retry should be durable and scheduled with backoff/jitter.
- The simplified reversal model must be validated against the business owner's precise reversal rules.
- Selenium UI testing is not applicable until a browser dashboard exists.
