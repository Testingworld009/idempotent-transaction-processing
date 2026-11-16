package com.company.txn.domain;
import java.math.BigDecimal;
public class TransactionRequest {
 private String transactionId, requestId, accountId, currency, originalTransactionId;
 private TransactionType type; private BigDecimal amount; private Long sequenceNumber;
 public String getTransactionId(){return transactionId;} public void setTransactionId(String v){transactionId=v;}
 public String getRequestId(){return requestId;} public void setRequestId(String v){requestId=v;}
 public String getAccountId(){return accountId;} public void setAccountId(String v){accountId=v;}
 public TransactionType getType(){return type;} public void setType(TransactionType v){type=v;}
 public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
 public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
 public Long getSequenceNumber(){return sequenceNumber;} public void setSequenceNumber(Long v){sequenceNumber=v;}
 public String getOriginalTransactionId(){return originalTransactionId;} public void setOriginalTransactionId(String v){originalTransactionId=v;}
}
