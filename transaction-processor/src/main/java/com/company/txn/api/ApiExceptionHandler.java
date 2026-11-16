package com.company.txn.api;
import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(HttpMessageNotReadableException.class)
 public ResponseEntity<Map<String,String>> unreadable(HttpMessageNotReadableException ex){return ResponseEntity.badRequest().body(Map.of("code","INVALID_JSON","message","Request body is malformed or contains an unsupported value"));}
 @ExceptionHandler(Exception.class)
 public ResponseEntity<Map<String,String>> unexpected(Exception ex){return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("code","INTERNAL_ERROR","message","Request could not be processed"));}
}
