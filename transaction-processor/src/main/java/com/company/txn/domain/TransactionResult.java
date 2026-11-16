package com.company.txn.domain;
public record TransactionResult(String transactionId, String requestId, TransactionStatus status, String code, int attempts, String message) { }
