package com.reliaquest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.NoEmployeesFoundException;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.EmployeeDTO;
import com.reliaquest.api.service.EmployeeService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@AutoConfigureMockMvc
@SpringBootTest
class ApiApplicationTest {

    @Test
    void someTest() {
        System.out.println("Testing one two three!");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    EmployeeService employeeService;

    @Spy
    private EmployeeService employeeServicespy = new EmployeeService(Mockito.mock(WebClient.class));

    List<EmployeeDTO> mockList = List.of(
            new EmployeeDTO(UUID.randomUUID().toString(), "Hans Herzog", 324832, 58, "Consultant", "hans@example.com"),
            new EmployeeDTO(UUID.randomUUID().toString(), "Hans Low", 32, 58, "Consultant", "hans@example.com"),
            new EmployeeDTO(UUID.randomUUID().toString(), "Hans Mid", 3248, 58, "Consultant", "hans@example.com"),
            new EmployeeDTO(UUID.randomUUID().toString(), "Joe Biden", 1000, 58, "Consultant", "hans@example.com"),
            new EmployeeDTO(
                    UUID.randomUUID().toString(), "Donald Trump", 100100000, 58, "Consultant", "hans@example.com"));

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetEmployeesByNameSearch() throws Exception {
        // Given
        String name = "Hans";

        // When
        Mockito.when(employeeService.getEmployeesByNameSearch(name)).thenReturn(mockList);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/search/{searchString}", name))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].employee_name").value("Hans Herzog"))
                .andExpect(jsonPath("$[1].employee_name").value("Hans Low"));
    }

    @Test
    void testGetAllEmployees() throws Exception {
        // When
        Mockito.when(employeeService.getAllEmployees()).thenReturn(mockList);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(mockList.size()))
                .andExpect(jsonPath("$[0].employee_name").value("Hans Herzog"))
                .andExpect(jsonPath("$[1].employee_name").value("Hans Low"))
                .andExpect(jsonPath("$[4].employee_name").value("Donald Trump"));
    }

    @Test
    void testGetHighestSalaryOfEmployees() {
        // Mock getAllEmployees to return mock list
        Mockito.doReturn(mockList).when(employeeServicespy).getAllEmployees();

        // When
        Integer highestSalary = employeeServicespy.getHighestSalaryOfEmployees();

        // Then
        assertEquals(100100000, highestSalary);
    }

    @Test
    void testGetTopTenHighestEarningEmployeeNames() {
        // When
        Mockito.doReturn(mockList).when(employeeServicespy).getAllEmployees();
        List<String> topTen = employeeServicespy.getTopTenHighestEarningEmployeeNames();

        List<String> expected = List.of("Donald Trump", "Hans Herzog", "Hans Mid", "Joe Biden", "Hans Low");

        assertEquals(expected, topTen);
    }

    @Test
    void testGetEmployeeByIdNotFound() throws Exception {
        // Given
        String id = UUID.randomUUID().toString();

        // When
        Mockito.when(employeeService.getEmployeeById(id)).thenThrow(new EmployeeNotFoundException(id));

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void testGetHighestSalaryOfEmployeesWhenNoneExist() throws Exception {
        // When
        Mockito.when(employeeService.getHighestSalaryOfEmployees()).thenThrow(new NoEmployeesFoundException());

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/highestSalary"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateEmployee() throws Exception {
        // Given
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setName("Jill Jenkins");
        request.setSalary(139082);
        request.setAge(48);
        request.setTitle("Financial Advisor");

        EmployeeDTO created = new EmployeeDTO(
                UUID.randomUUID().toString(), "Jill Jenkins", 139082, 48, "Financial Advisor", "jillj@company.com");

        // When
        Mockito.when(employeeService.createEmployee(Mockito.any(CreateEmployeeRequest.class)))
                .thenReturn(created);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/employee/" + created.getId()))
                .andExpect(jsonPath("$.employee_name").value("Jill Jenkins"))
                .andExpect(jsonPath("$.employee_salary").value(139082));
    }

    @Test
    void testCreateEmployeeValidationFailure() throws Exception {
        // Given: blank name, non-positive salary, and under-age all violate CreateEmployeeRequest's constraints
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setName("");
        request.setSalary(-5);
        request.setAge(10);
        request.setTitle("Engineer");

        // Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(employeeService);
    }

    @Test
    void testDeleteEmployeeById() throws Exception {
        // Given
        String id = UUID.randomUUID().toString();

        // When
        Mockito.when(employeeService.deleteEmployeeById(id)).thenReturn("Jill Jenkins");

        // Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/employee/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().string("Jill Jenkins"));
    }

    @Test
    void testDeleteEmployeeByIdNotFound() throws Exception {
        // Given
        String id = UUID.randomUUID().toString();

        // When
        Mockito.when(employeeService.deleteEmployeeById(id)).thenThrow(new EmployeeNotFoundException(id));

        // Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/employee/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteEmployeeByIdWhenDeletionFails() throws Exception {
        // Given: the id exists, but the downstream delete-by-name call reports it wasn't actually removed
        // (a genuinely unexpected condition, not a "not found" case) - EmployeeService surfaces this as
        // IllegalStateException, which has no dedicated handler, so it should fall through to the generic 500
        String id = UUID.randomUUID().toString();
        Mockito.when(employeeService.deleteEmployeeById(id))
                .thenThrow(new IllegalStateException("Employee with id " + id + " was not deleted"));

        // Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/employee/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Unexpected error occurred"));
    }

    @Test
    void testDownstreamErrorIsForwarded() throws Exception {
        // Given: the mock server rejected the request (e.g. its random rate-limiting) with a 429
        WebClientResponseException exception = WebClientResponseException.create(
                429, "Too Many Requests", HttpHeaders.EMPTY, "Rate limited".getBytes(StandardCharsets.UTF_8), null);

        // When
        Mockito.when(employeeService.getAllEmployees()).thenThrow(exception);

        // Then: the real downstream status and body are forwarded, not masked as a generic 500
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee"))
                .andExpect(status().is(429))
                .andExpect(content().string("Rate limited"));
    }

    @Test
    void testUnexpectedErrorReturns500() throws Exception {
        // When
        Mockito.when(employeeService.getAllEmployees()).thenThrow(new RuntimeException("boom"));

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Unexpected error occurred"));
    }
}
