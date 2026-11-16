package com.company.txn.service;

import com.company.txn.domain.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {
 private TransactionRequest tx(String id,String req,long seq,TransactionType type,String amount){
  TransactionRequest r=new TransactionRequest();r.setTransactionId(id);r.setRequestId(req);r.setAccountId("ACC-201");r.setSequenceNumber(seq);r.setType(type);r.setAmount(new BigDecimal(amount));r.setCurrency("USD");return r;
 }
 @Test void processesCreditAndDebitInSequence(){
  TransactionService s=new TransactionService(3);
  assertEquals(TransactionStatus.PENDING_SEQUENCE,s.submit(tx("T2","R2",2,TransactionType.DEBIT,"40")).status());
  assertEquals(TransactionStatus.PROCESSED,s.submit(tx("T1","R1",1,TransactionType.CREDIT,"100")).status());
  assertEquals(new BigDecimal("60.00"),s.report().balances().get("ACC-201"));
  assertEquals(TransactionStatus.PROCESSED,s.transactions().stream().filter(x->"T2".equals(x.transactionId())).findFirst().orElseThrow().status());
 }
 @Test void duplicateDoesNotApplySecondEffect(){
  TransactionService s=new TransactionService(3);TransactionRequest r=tx("T1","R1",1,TransactionType.CREDIT,"100");
  assertEquals(TransactionStatus.PROCESSED,s.submit(r).status());
  TransactionRequest duplicate=tx("T1","R2",1,TransactionType.CREDIT,"100");
  assertEquals(TransactionStatus.DUPLICATE,s.submit(duplicate).status());
  assertEquals(new BigDecimal("100.00"),s.report().balances().get("ACC-201"));
  assertEquals(1,s.report().processedLedgerEntries());
 }
 @Test void sameTransactionIdDifferentPayloadIsConflict(){
  TransactionService s=new TransactionService(3);s.submit(tx("T1","R1",1,TransactionType.CREDIT,"100"));
  assertEquals("CONFLICT",s.submit(tx("T1","R2",1,TransactionType.CREDIT,"200")).code());
  assertEquals(new BigDecimal("100.00"),s.report().balances().get("ACC-201"));
 }
 @Test void invalidRecordDoesNotStopFollowingSequence(){
  TransactionService s=new TransactionService(3);s.submit(tx("T1","R1",1,TransactionType.CREDIT,"100"));
  TransactionRequest invalid=tx("T2","R2",2,TransactionType.DEBIT,"-10");
  assertEquals("VALIDATION",s.submit(invalid).code());
  assertEquals(TransactionStatus.PROCESSED,s.submit(tx("T3","R3",3,TransactionType.CREDIT,"5")).status());
  assertEquals(new BigDecimal("105.00"),s.report().balances().get("ACC-201"));
 }
 @Test void transientFailureRetriesWithoutDuplicateEffect(){
  TransactionService s=new TransactionService(3);s.injectTransientFailures("T1",1);
  TransactionResult result=s.submit(tx("T1","R1",1,TransactionType.CREDIT,"100"));
  assertEquals(TransactionStatus.PROCESSED,result.status());assertEquals(2,result.attempts());
  assertEquals(new BigDecimal("100.00"),s.report().balances().get("ACC-201"));assertEquals(1,s.report().processedLedgerEntries());
 }
 @Test void retryExhaustionDoesNotApplyEffect(){
  TransactionService s=new TransactionService(2);s.injectTransientFailures("T1",5);
  TransactionResult result=s.submit(tx("T1","R1",1,TransactionType.CREDIT,"100"));
  assertEquals(TransactionStatus.FAILED,result.status());assertEquals("RETRY_EXHAUSTED",result.code());
  assertFalse(s.report().balances().containsKey("ACC-201"));
 }
 @Test void insufficientFundsIsTerminalBusinessFailure(){
  TransactionService s=new TransactionService(3);TransactionResult r=s.submit(tx("T1","R1",1,TransactionType.DEBIT,"10"));
  assertEquals(TransactionStatus.FAILED,r.status());assertEquals("INSUFFICIENT_FUNDS",r.code());
 }
}
