package com.company.txn.service;

import com.company.txn.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/** Single-process, in-memory assessment implementation. Global sequence ordering follows the supplied sample. */
@Service
public class TransactionService {
 private static final Logger LOG=Logger.getLogger(TransactionService.class.getName());
 private static final Set<String> CURRENCIES=Set.of("USD","EUR","GBP","INR");
 private final int maxAttempts;
 private final Map<String,MutableRecord> records=new LinkedHashMap<>();
 private final Map<String,String> fingerprints=new HashMap<>(), requestOwners=new HashMap<>();
 private final Map<Long,Slot> slots=new HashMap<>();
 private final Map<String,BigDecimal> balances=new HashMap<>();
 private final Map<String,String> accountCurrencies=new HashMap<>(), reversedOriginals=new HashMap<>();
 private final Map<String,LedgerEntry> ledger=new LinkedHashMap<>();
 private final Map<String,Integer> injectedFailures=new ConcurrentHashMap<>();
 private final List<TransactionResult> duplicateEvents=new ArrayList<>();
 private long nextSequence=1;
 public TransactionService(@Value("${transaction.retry.max-attempts:3}") int maxAttempts){this.maxAttempts=Math.max(1,maxAttempts);}

 public synchronized TransactionResult submit(TransactionRequest r){
  List<String> errors=validate(r);
  if(!errors.isEmpty()){
   TransactionResult failed=result(r,TransactionStatus.FAILED,"VALIDATION",0,"Invalid fields: "+String.join(",",errors));
   // A malformed sequenced record consumes its slot when it reaches the head, preventing poison blocking.
   if(r!=null&&r.getSequenceNumber()!=null&&r.getSequenceNumber()>0&&r.getSequenceNumber()>=nextSequence&&!slots.containsKey(r.getSequenceNumber())){
    if(!blank(r.getTransactionId())){MutableRecord invalidRecord=new MutableRecord(r);invalidRecord.status=TransactionStatus.FAILED;invalidRecord.code="VALIDATION";records.putIfAbsent(r.getTransactionId().trim(),invalidRecord);}
    slots.put(r.getSequenceNumber(),new Slot(null,failed)); drainReadySlots();
   }
   return failed;
  }
  String id=r.getTransactionId().trim(), req=r.getRequestId().trim(), fp=fingerprint(r);
  String old=fingerprints.get(id);
  if(old!=null){
   if(!old.equals(fp))return result(r,TransactionStatus.FAILED,"CONFLICT",0,"Transaction ID reused with different business payload");
   TransactionResult duplicate=result(r,TransactionStatus.DUPLICATE,null,0,"Duplicate business transaction; no effect applied"); duplicateEvents.add(duplicate);
   LOG.info("event=duplicate transactionId="+safe(id)); return duplicate;
  }
  String owner=requestOwners.get(req);
  if(owner!=null&&!owner.equals(id))return result(r,TransactionStatus.FAILED,"REQUEST_ID_REUSE",0,"Request ID already belongs to another transaction");
  long seq=r.getSequenceNumber();
  if(seq<nextSequence||slots.containsKey(seq))return result(r,TransactionStatus.FAILED,"SEQUENCE_CONFLICT",0,"Sequence slot already consumed or owned");
  fingerprints.put(id,fp); requestOwners.put(req,id); MutableRecord record=new MutableRecord(r); records.put(id,record); slots.put(seq,new Slot(r,null));
  if(seq>nextSequence){record.status=TransactionStatus.PENDING_SEQUENCE;return record.toResult("Waiting for sequence "+nextSequence);}
  drainReadySlots(); return record.toResult(record.code==null?"Transaction accepted":record.code);
 }

 /** Deterministic fault injection for assessment and automated retry tests. */
 public void injectTransientFailures(String transactionId,int count){if(count<0)throw new IllegalArgumentException("count must not be negative");injectedFailures.put(transactionId,count);}
 private void drainReadySlots(){
  while(slots.containsKey(nextSequence)){Slot slot=slots.remove(nextSequence);
   if(slot.poison!=null){nextSequence++;continue;}
   MutableRecord r=records.get(slot.request.getTransactionId().trim());if(r!=null)processWithRetry(r);nextSequence++;
  }
 }
 private void processWithRetry(MutableRecord r){
  r.status=TransactionStatus.PROCESSING;
  for(int attempt=1;attempt<=maxAttempts;attempt++){
   r.attempts=attempt;int remaining=injectedFailures.getOrDefault(r.request.getTransactionId(),0);
   if(remaining>0){injectedFailures.put(r.request.getTransactionId(),remaining-1);r.status=attempt==maxAttempts?TransactionStatus.FAILED:TransactionStatus.RETRY_PENDING;
    if(attempt==maxAttempts)r.code="RETRY_EXHAUSTED";if(attempt==maxAttempts)return;continue;}
   try{applyEffect(r);r.status=TransactionStatus.PROCESSED;r.code=null;LOG.info("event=processed transactionId="+safe(r.request.getTransactionId())+" attempts="+attempt);return;}
   catch(BusinessException ex){r.status=TransactionStatus.FAILED;r.code=ex.code;return;}
   catch(RuntimeException ex){r.status=TransactionStatus.FAILED;r.code="INTERNAL_ERROR";LOG.warning("event=internal_error transactionId="+safe(r.request.getTransactionId())+" exception="+ex.getClass().getSimpleName());return;}
  }
 }
 private void applyEffect(MutableRecord record){
  TransactionRequest r=record.request;String account=r.getAccountId().trim(),currency=r.getCurrency().trim().toUpperCase(Locale.ROOT);
  String existingCurrency=accountCurrencies.get(account);if(existingCurrency!=null&&!existingCurrency.equals(currency))throw new BusinessException("CURRENCY_MISMATCH");
  BigDecimal balance=balances.getOrDefault(account,new BigDecimal("0.00")),amount=r.getAmount().setScale(2,RoundingMode.UNNECESSARY);
  switch(r.getType()){
   case CREDIT -> balance=balance.add(amount);
   case DEBIT -> {if(balance.compareTo(amount)<0)throw new BusinessException("INSUFFICIENT_FUNDS");balance=balance.subtract(amount);}
   case REVERSAL -> {LedgerEntry original=ledger.get(r.getOriginalTransactionId());if(original==null)throw new BusinessException("ORIGINAL_NOT_FOUND");
    if(reversedOriginals.containsKey(r.getOriginalTransactionId()))throw new BusinessException("ALREADY_REVERSED");
    if(!original.account.equals(account)||!original.currency.equals(currency))throw new BusinessException("CURRENCY_MISMATCH");
    balance=original.type==TransactionType.CREDIT?balance.subtract(original.amount):balance.add(original.amount);
    if(balance.signum()<0)throw new BusinessException("INSUFFICIENT_FUNDS");reversedOriginals.put(r.getOriginalTransactionId(),r.getTransactionId());}
  }
  // This synchronized operation protects balance + ledger updates as one in-process critical section.
  balances.put(account,balance.setScale(2));accountCurrencies.putIfAbsent(account,currency);
  ledger.put(r.getTransactionId(),new LedgerEntry(account,r.getType(),amount,currency));
 }
 public synchronized ProcessingReport report(){
  Map<String,Long> counts=new LinkedHashMap<>();for(TransactionStatus s:TransactionStatus.values())counts.put(s.name(),0L);
  records.values().forEach(r->counts.compute(r.status.name(),(k,v)->v+1));counts.compute("DUPLICATE",(k,v)->v+duplicateEvents.size());
  List<Long> missing=new ArrayList<>();if(!slots.isEmpty()){long max=slots.keySet().stream().mapToLong(Long::longValue).max().orElse(nextSequence);for(long i=nextSequence;i<max&&missing.size()<1000;i++)if(!slots.containsKey(i))missing.add(i);}
  List<TransactionResult> all=new ArrayList<>();records.values().forEach(r->all.add(r.toResult(r.code)));all.addAll(duplicateEvents);
  return new ProcessingReport(counts,new TreeMap<>(balances),duplicateEvents.size(),ledger.size(),nextSequence,missing,all);
 }
 public synchronized List<TransactionResult> transactions(){List<TransactionResult> all=new ArrayList<>();records.values().forEach(r->all.add(r.toResult(r.code)));all.addAll(duplicateEvents);return all;}
 private List<String> validate(TransactionRequest r){List<String> e=new ArrayList<>();if(r==null)return List.of("body");
  if(blank(r.getTransactionId()))e.add("transactionId");if(blank(r.getRequestId()))e.add("requestId");if(blank(r.getAccountId()))e.add("accountId");
  if(r.getType()==null)e.add("type");if(r.getAmount()==null||r.getAmount().signum()<=0||r.getAmount().scale()>2)e.add("amount");
  if(blank(r.getCurrency())||!CURRENCIES.contains(r.getCurrency().trim().toUpperCase(Locale.ROOT)))e.add("currency");
  if(r.getSequenceNumber()==null||r.getSequenceNumber()<1)e.add("sequenceNumber");if(r.getType()==TransactionType.REVERSAL&&blank(r.getOriginalTransactionId()))e.add("originalTransactionId");return e;}
 private String fingerprint(TransactionRequest r){return String.join("|",r.getAccountId().trim(),r.getType().name(),r.getAmount().setScale(2).toPlainString(),r.getCurrency().trim().toUpperCase(Locale.ROOT),String.valueOf(r.getSequenceNumber()),Objects.toString(r.getOriginalTransactionId(),""));}
 private boolean blank(String s){return s==null||s.trim().isEmpty();}private String safe(String s){return s==null?"unknown":s.replaceAll("[^A-Za-z0-9_-]","_");}
 private TransactionResult result(TransactionRequest r,TransactionStatus s,String code,int attempts,String msg){return new TransactionResult(r==null?null:r.getTransactionId(),r==null?null:r.getRequestId(),s,code,attempts,msg);}
 private static class Slot{final TransactionRequest request;final TransactionResult poison;Slot(TransactionRequest r,TransactionResult p){request=r;poison=p;}}
 private static class MutableRecord{final TransactionRequest request;TransactionStatus status=TransactionStatus.RECEIVED;int attempts;String code;MutableRecord(TransactionRequest r){request=r;}TransactionResult toResult(String msg){return new TransactionResult(request.getTransactionId(),request.getRequestId(),status,code,attempts,msg);}}
 private record LedgerEntry(String account,TransactionType type,BigDecimal amount,String currency){}
 private static class BusinessException extends RuntimeException{final String code;BusinessException(String c){code=c;}}
}
