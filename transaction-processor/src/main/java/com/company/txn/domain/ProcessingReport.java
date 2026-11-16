package com.company.txn.domain;
import java.math.BigDecimal; import java.util.List; import java.util.Map;
public record ProcessingReport(Map<String,Long> statusCounts, Map<String,BigDecimal> balances, long duplicateDeliveries,
 long processedLedgerEntries, long nextExpectedSequence, List<Long> missingSequences, List<TransactionResult> transactions) { }
