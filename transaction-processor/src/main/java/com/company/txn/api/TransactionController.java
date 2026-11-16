package com.company.txn.api;

import com.company.txn.domain.*;
import com.company.txn.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TransactionController {
 private final TransactionService service;
 public TransactionController(TransactionService service){this.service=service;}
 @PostMapping("/transactions")
 public ResponseEntity<TransactionResult> submit(@RequestBody TransactionRequest request){
  TransactionResult result=service.submit(request);
  HttpStatus status=result.status()==TransactionStatus.FAILED?HttpStatus.UNPROCESSABLE_ENTITY:HttpStatus.OK;
  return ResponseEntity.status(status).body(result);
 }
 @PostMapping("/transactions/batch")
 public List<TransactionResult> submitBatch(@RequestBody List<TransactionRequest> requests){return requests.stream().map(service::submit).toList();}
 @GetMapping("/transactions") public List<TransactionResult> transactions(){return service.transactions();}
 @GetMapping("/report") public ProcessingReport report(){return service.report();}
 /** Assessment-only fault injection endpoint; disable in production deployments. */
 @PostMapping("/test/faults/{transactionId}")
 public Map<String,Object> inject(@PathVariable String transactionId,@RequestParam(defaultValue="1") int count){
  service.injectTransientFailures(transactionId,count);return Map.of("transactionId",transactionId,"injectedFailures",count);
 }
}
