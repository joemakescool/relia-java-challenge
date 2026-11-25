package com.reliaquest.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.NoEmployeesFoundException;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.EmployeeDTO;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Exercises EmployeeService against a real local HTTP server instead of hand-mocking WebClient's
 * fluent chain, so the actual outgoing request and response parsing both get real coverage.
 *
 * <p>Each test gets its own MockWebServer instance (started/stopped per test, not shared across the
 * class) so a test that doesn't drain every recorded request it triggers can't leak stale requests
 * into another test's queue.
 */
class EmployeeServiceTest {

    private MockWebServer mockWebServer;
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient =
                WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build();
        employeeService = new EmployeeService(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse().setBody(body).addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    }

    private static String employeeJson(String id, String name, int salary, int age, String title, String email) {
        return """
                {"id": "%s", "employee_name": "%s", "employee_salary": %d, "employee_age": %d, "employee_title": "%s", "employee_email": "%s"}"""
                .formatted(id, name, salary, age, title, email);
    }

    private static String wrapAsListResponse(String... employeesJson) {
        return """
                {"data": [%s], "status": "Successfully processed request."}"""
                .formatted(String.join(",", employeesJson));
    }

    private static String wrapAsSingleResponse(String employeeJson) {
        return """
                {"data": %s, "status": "Successfully processed request."}""".formatted(employeeJson);
    }

    @Test
    void getAllEmployees_fetchesAndParsesEmployeeList() throws InterruptedException {
        mockWebServer.enqueue(jsonResponse(wrapAsListResponse(
                employeeJson("1", "Hans Herzog", 324832, 58, "Consultant", "hans@example.com"),
                employeeJson("2", "Hans Low", 32, 58, "Consultant", "hans@example.com"))));

        List<EmployeeDTO> employees = employeeService.getAllEmployees();

        assertEquals(2, employees.size());
        assertEquals("Hans Herzog", employees.get(0).getEmployeeName());

        RecordedRequest recorded = mockWebServer.takeRequest();
        assertEquals("GET", recorded.getMethod());
        assertEquals("/employee", recorded.getPath());
    }

    @Test
    void getAllEmployees_returnsEmptyListWhenDownstreamHasNoEmployees() {
        mockWebServer.enqueue(jsonResponse(wrapAsListResponse()));

        List<EmployeeDTO> employees = employeeService.getAllEmployees();

        assertTrue(employees.isEmpty());
    }

    @Test
    void getEmployeesByNameSearch_filtersFetchedEmployees() {
        mockWebServer.enqueue(jsonResponse(wrapAsListResponse(
                employeeJson("1", "Hans Herzog", 324832, 58, "Consultant", "hans@example.com"),
                employeeJson("2", "Joe Biden", 1000, 58, "Consultant", "joe@example.com"))));

        List<EmployeeDTO> matches = employeeService.getEmployeesByNameSearch("hans");

        assertEquals(1, matches.size());
        assertEquals("Hans Herzog", matches.get(0).getEmployeeName());
    }

    @Test
    void getEmployeesByNameSearch_returnsEmptyListWhenNoMatches() {
        mockWebServer.enqueue(jsonResponse(
                wrapAsListResponse(employeeJson("1", "Hans Herzog", 324832, 58, "Consultant", "hans@example.com"))));

        List<EmployeeDTO> matches = employeeService.getEmployeesByNameSearch("zzz-no-such-name");

        assertTrue(matches.isEmpty());
    }

    @Test
    void getEmployeeById_returnsEmployeeForRealId() throws InterruptedException {
        String id = "d005f39a-beb8-4390-afec-fd54e91d94ee";
        mockWebServer.enqueue(jsonResponse(wrapAsSingleResponse(
                employeeJson(id, "Jill Jenkins", 139082, 48, "Financial Advisor", "jillj@company.com"))));

        EmployeeDTO employee = employeeService.getEmployeeById(id);

        assertEquals("Jill Jenkins", employee.getEmployeeName());

        RecordedRequest recorded = mockWebServer.takeRequest();
        assertEquals("GET", recorded.getMethod());
        assertEquals("/employee/" + id, recorded.getPath());
    }

    @Test
    void getEmployeeById_throwsEmployeeNotFoundOn404() {
        String id = "00000000-0000-0000-0000-000000000000";
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        EmployeeNotFoundException exception =
                assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(id));
        assertTrue(exception.getMessage().contains(id));
    }

    @Test
    void getEmployeeById_propagatesNonNotFoundErrorsUnmodified() {
        // Given: a downstream failure that is NOT a 404 - e.g. a transient server error
        String id = "d005f39a-beb8-4390-afec-fd54e91d94ee";
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        // Then: this must NOT be mistaken for "not found" - only 404 gets translated to EmployeeNotFoundException
        WebClientResponseException exception =
                assertThrows(WebClientResponseException.class, () -> employeeService.getEmployeeById(id));
        assertEquals(500, exception.getStatusCode().value());
    }

    @Test
    void getHighestSalaryOfEmployees_returnsMaxSalary() {
        mockWebServer.enqueue(jsonResponse(wrapAsListResponse(
                employeeJson("1", "Hans Herzog", 324832, 58, "Consultant", "hans@example.com"),
                employeeJson("2", "Donald Trump", 100100000, 58, "Consultant", "donald@example.com"),
                employeeJson("3", "Joe Biden", 1000, 58, "Consultant", "joe@example.com"))));

        Integer highestSalary = employeeService.getHighestSalaryOfEmployees();

        assertEquals(100100000, highestSalary);
    }

    @Test
    void getHighestSalaryOfEmployees_throwsWhenNoEmployeesExist() {
        mockWebServer.enqueue(jsonResponse(wrapAsListResponse()));

        assertThrows(NoEmployeesFoundException.class, () -> employeeService.getHighestSalaryOfEmployees());
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_returnsTopTenSortedDescending() {
        String[] employeesJson = IntStream.rangeClosed(1, 12)
                .mapToObj(i -> employeeJson(
                        String.valueOf(i), "Employee" + i, i * 1000, 30, "Title", "employee" + i + "@example.com"))
                .toArray(String[]::new);
        mockWebServer.enqueue(jsonResponse(wrapAsListResponse(employeesJson)));

        List<String> topTen = employeeService.getTopTenHighestEarningEmployeeNames();

        assertEquals(10, topTen.size());
        assertEquals("Employee12", topTen.get(0));
        assertEquals("Employee3", topTen.get(9));
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_returnsFewerThanTenWhenNotEnoughEmployees() {
        mockWebServer.enqueue(jsonResponse(wrapAsListResponse(
                employeeJson("1", "Hans Herzog", 324832, 58, "Consultant", "hans@example.com"),
                employeeJson("2", "Hans Low", 32, 58, "Consultant", "hans@example.com"),
                employeeJson("3", "Hans Mid", 3248, 58, "Consultant", "hans@example.com"))));

        List<String> topTen = employeeService.getTopTenHighestEarningEmployeeNames();

        // Only 3 employees exist, so no padding or error - just all 3, correctly ordered
        assertEquals(List.of("Hans Herzog", "Hans Mid", "Hans Low"), topTen);
    }

    @Test
    void createEmployee_sendsRequestAndParsesCreatedEmployee() throws InterruptedException {
        // Given
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setName("Jill Jenkins");
        request.setSalary(139082);
        request.setAge(48);
        request.setTitle("Financial Advisor");

        mockWebServer.enqueue(jsonResponse(wrapAsSingleResponse(employeeJson(
                "d005f39a-beb8-4390-afec-fd54e91d94ee",
                "Jill Jenkins",
                139082,
                48,
                "Financial Advisor",
                "jillj@company.com"))));

        // When
        EmployeeDTO created = employeeService.createEmployee(request);

        // Then: the response body was parsed into the right EmployeeDTO
        assertEquals("Jill Jenkins", created.getEmployeeName());
        assertEquals(139082, created.getEmployeeSalary());

        // And: the actual outgoing HTTP request was shaped the way the mock server expects
        RecordedRequest recorded = mockWebServer.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertEquals("/employee", recorded.getPath());
        assertTrue(recorded.getBody().readUtf8().contains("\"name\":\"Jill Jenkins\""));
    }

    @Test
    void createEmployee_propagatesDownstreamValidationFailureUnmodified() {
        // Given: the downstream mock server rejects the request (its own @Valid failed for some reason
        // ours didn't catch). createEmployee must not swallow or retry this - just let it propagate.
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setName("Test Person");
        request.setSalary(50000);
        request.setAge(30);
        request.setTitle("Engineer");

        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("downstream rejected the request"));

        WebClientResponseException exception =
                assertThrows(WebClientResponseException.class, () -> employeeService.createEmployee(request));
        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void createEmployee_throwsWhenDownstreamReturnsNoData() {
        // Given: a 200 response with no usable payload - shouldn't happen in practice, but must fail loudly
        // rather than silently returning a null/garbage EmployeeDTO
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setName("Test Person");
        request.setSalary(50000);
        request.setAge(30);
        request.setTitle("Engineer");

        mockWebServer.enqueue(
                jsonResponse("""
                {"data": null, "status": "Successfully processed request."}"""));

        assertThrows(IllegalStateException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    void deleteEmployeeById_looksUpThenDeletesByName() throws InterruptedException {
        // Given: deleteEmployeeById first looks the employee up by id to learn their name
        String id = "d005f39a-beb8-4390-afec-fd54e91d94ee";
        mockWebServer.enqueue(jsonResponse(wrapAsSingleResponse(
                employeeJson(id, "Jill Jenkins", 139082, 48, "Financial Advisor", "jillj@company.com"))));
        mockWebServer.enqueue(
                jsonResponse("""
                {"data": true, "status": "Successfully processed request."}"""));

        // When
        String deletedName = employeeService.deleteEmployeeById(id);

        // Then: returns the employee's name, not the id it was given
        assertEquals("Jill Jenkins", deletedName);

        // And: looked up by id first...
        RecordedRequest getRequest = mockWebServer.takeRequest();
        assertEquals("GET", getRequest.getMethod());
        assertEquals("/employee/" + id, getRequest.getPath());

        // ...then deleted downstream by name, not id
        RecordedRequest deleteRequest = mockWebServer.takeRequest();
        assertEquals("DELETE", deleteRequest.getMethod());
        assertEquals("/employee", deleteRequest.getPath());
        assertTrue(deleteRequest.getBody().readUtf8().contains("\"name\":\"Jill Jenkins\""));
    }

    @Test
    void deleteEmployeeById_throwsWhenDownstreamReportsNoDeletion() {
        // Given: the employee is found by id, but the downstream delete-by-name reports nothing was removed
        String id = "d005f39a-beb8-4390-afec-fd54e91d94ee";
        mockWebServer.enqueue(jsonResponse(wrapAsSingleResponse(
                employeeJson(id, "Jill Jenkins", 139082, 48, "Financial Advisor", "jillj@company.com"))));
        mockWebServer.enqueue(
                jsonResponse("""
                {"data": false, "status": "Successfully processed request."}"""));

        // When / Then
        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> employeeService.deleteEmployeeById(id));
        assertTrue(exception.getMessage().contains(id));
        assertTrue(exception.getMessage().contains("Jill Jenkins"));
    }

    @Test
    void deleteEmployeeById_neverCallsDeleteWhenLookupFails() {
        // Given: the id doesn't exist, so the initial lookup 404s
        String id = "00000000-0000-0000-0000-000000000000";
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        // Then: the delete-by-name call must never be attempted - fail fast on the lookup, don't waste
        // a second downstream call (or risk deleting the wrong thing) on a request we already know is bad
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployeeById(id));
        assertEquals(1, mockWebServer.getRequestCount());
    }
}
