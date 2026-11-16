package com.company.txn.api;

import com.company.txn.domain.TransactionRequest;
import com.company.txn.domain.TransactionType;
import com.company.txn.service.TransactionService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.math.BigDecimal;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransactionApiTest {
 @LocalServerPort int port;
 @Autowired TransactionService service;
 @BeforeEach void setup(){RestAssured.port=port;}
 @Test void postTransactionReturnsProcessed(){
  TransactionRequest r=new TransactionRequest();r.setTransactionId("API-T1");r.setRequestId("API-R1");r.setAccountId("API-ACC");r.setType(TransactionType.CREDIT);r.setAmount(new BigDecimal("25.00"));r.setCurrency("INR");r.setSequenceNumber(1L);
  given().contentType("application/json").body(r).when().post("/api/transactions").then().statusCode(200).body("status",equalTo("PROCESSED"));
 }
 @Test void malformedJsonReturnsBadRequest(){given().contentType("application/json").body("{broken").when().post("/api/transactions").then().statusCode(400).body("code",equalTo("INVALID_JSON"));}
 @Test void reportEndpointIsAvailable(){given().when().get("/api/report").then().statusCode(200).body("nextExpectedSequence",equalTo(1));}
}
