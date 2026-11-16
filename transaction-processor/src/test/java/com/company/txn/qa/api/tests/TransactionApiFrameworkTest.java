package com.company.txn.qa.api.tests;

import com.company.txn.qa.utility.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(TestEvidenceExtension.class)
class TransactionApiFrameworkTest {
    @LocalServerPort int port;
    private ApiClient api;
    @BeforeEach void setup() { api = new ApiClient("http://localhost:" + port); }
    private Map<String,Object> txn(String id, String req, long seq, String type, String amount) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("transactionId", id); m.put("requestId", req); m.put("accountId", "QA-ACC");
        m.put("type", type); m.put("amount", amount); m.put("currency", "INR"); m.put("sequenceNumber", seq);
        return m;
    }
    @Test void api01GetTransactionsReturns200() { assertEquals(200, api.get("/api/transactions").statusCode()); }
    @Test void api02GetReportReturns200() { assertEquals(200, api.get("/api/report").statusCode()); }
    @Test void api03PostValidCreditReturns200() {
        Response r = api.post("/api/transactions", txn("QAT-01","QAR-01",1,"CREDIT","100.00"));
        assertEquals(200, r.statusCode()); assertEquals("PROCESSED", r.jsonPath().getString("status"));
    }
    @Test void api04MalformedJsonReturns400() {
        Response r = io.restassured.RestAssured.given().baseUri("http://localhost:" + port)
                .contentType("application/json").body("{bad").post("/api/transactions");
        assertEquals(400, r.statusCode()); assertEquals("INVALID_JSON", r.jsonPath().getString("code"));
    }
    @Test void api05BatchEndpointReturns200() {
        Response r = api.post("/api/transactions/batch", List.of(txn("QAT-05","QAR-05",1,"CREDIT","10.00")));
        assertEquals(200, r.statusCode()); assertTrue(r.jsonPath().getList("$").size() == 1);
    }
    @Test void api06FaultInjectionEndpointReturns200() {
        assertEquals(200, api.post("/api/test/faults/QAT-06?count=1", "").statusCode());
    }
    @Test void api07MissingRequiredFieldIsBusinessFailure() {
        Map<String,Object> m = txn("QAT-07","QAR-07",1,"CREDIT","10.00"); m.remove("accountId");
        Response r = api.post("/api/transactions", m);
        assertTrue(r.statusCode() == 422 || r.statusCode() == 200);
        assertEquals("FAILED", r.jsonPath().getString("status"));
    }
    @Test void api08DuplicateDeliveryIsReported() {
        Map<String,Object> m = txn("QAT-08","QAR-08",1,"CREDIT","10.00");
        api.post("/api/transactions", m);
        m.put("requestId", "QAR-08-DUP");
        Response r = api.post("/api/transactions", m);
        assertEquals("DUPLICATE", r.jsonPath().getString("status"));
    }
    @Test void api09UnsupportedHttpPutIsNotFoundOrMethodNotAllowed() {
        int code = api.put("/api/transactions", txn("QAT-09","QAR-09",1,"CREDIT","1.00")).statusCode();
        assertTrue(code == 404 || code == 405);
    }
    @Test void api10UnsupportedHttpDeleteIsNotFoundOrMethodNotAllowed() {
        int code = api.delete("/api/transactions").statusCode();
        assertTrue(code == 404 || code == 405);
    }
}
