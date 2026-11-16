package com.company.txn.qa.utility;

import com.company.txn.qa.config.FrameworkConfig;
import io.restassured.response.Response;
import static io.restassured.RestAssured.given;

/** Common REST client exposing HTTP verbs and writing sanitized request/response metadata to the QA log. */
public class ApiClient {
    private final String baseUrl;
    public ApiClient() { this(FrameworkConfig.apiBaseUrl()); }
    public ApiClient(String baseUrl) { this.baseUrl = baseUrl; }
    private Response record(String method, String path, Response response) {
        LogUtils.log("API", method + " " + path + " -> HTTP " + response.statusCode());
        return response;
    }
    public Response get(String path) { return record("GET", path, given().baseUri(baseUrl).when().get(path)); }
    public Response post(String path, Object body) { return record("POST", path, given().baseUri(baseUrl).contentType("application/json").body(body).when().post(path)); }
    public Response put(String path, Object body) { return record("PUT", path, given().baseUri(baseUrl).contentType("application/json").body(body).when().put(path)); }
    public Response patch(String path, Object body) { return record("PATCH", path, given().baseUri(baseUrl).contentType("application/json").body(body).when().patch(path)); }
    public Response delete(String path) { return record("DELETE", path, given().baseUri(baseUrl).when().delete(path)); }
    public Response head(String path) { return record("HEAD", path, given().baseUri(baseUrl).when().head(path)); }
    public Response options(String path) { return record("OPTIONS", path, given().baseUri(baseUrl).when().options(path)); }
}
