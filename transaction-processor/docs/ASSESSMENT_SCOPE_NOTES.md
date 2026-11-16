# Assessment Scope Notes

The uploaded reference implementation is Python + SQLite. This repository is a Java 17/Spring Boot starter translation of the behaviours and QA expectations, not a line-by-line language conversion. The Java version intentionally starts with in-memory storage to avoid external setup and expose the processing rules clearly.

The reference QA guide's measured figures (108 tests, 97% coverage, 12/12 mutation checks, Python/SQLite throughput) belong only to the supplied Python repository. They are not claims about this Java project. Re-run Java tests, coverage and performance measurements before putting numbers in the client submission.

The reference guide interprets ordering as one global sequence because the sample sequence continues across accounts. This project follows that assumption and calls it out in the architecture decision document. Confirm with the assessment owner if sequence is intended to be per-account instead.
