package com.reliaquest.api;

import com.reliaquest.api.model.EmployeeDTO;
import com.reliaquest.api.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest
class ApiApplicationTest {

    @Test
    void someTest() {
        System.out.println("Testing one two three!");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @MockBean
    EmployeeService employeeService;

    @Spy
    private EmployeeService employeeServicespy;

    List<EmployeeDTO> mockList = List.of(
            new EmployeeDTO(UUID.randomUUID().toString(), "Hans Herzog", "324832", "58", "Consultant", "hans@example.com"),
            new EmployeeDTO(UUID.randomUUID().toString(), "Hans Low", "32", "58", "Consultant", "hans@example.com"),
            new EmployeeDTO(UUID.randomUUID().toString(), "Hans Mid", "3248", "58", "Consultant", "hans@example.com"),
            new EmployeeDTO(UUID.randomUUID().toString(), "Joe Biden", "1000", "58", "Consultant", "hans@example.com"),
            new EmployeeDTO(UUID.randomUUID().toString(), "Donald Trump", "100100000", "58", "Consultant", "hans@example.com")
    );

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
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/names/{name}", name))
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
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/all-employees"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(mockList.size()))
                .andExpect(jsonPath("$[0].employee_name").value("Hans Herzog"))
                .andExpect(jsonPath("$[1].employee_name").value("Hans Low"))
                .andExpect(jsonPath("$[4].employee_name").value("Donald Trump"));
    }

    @Test
    void testGetHighestSalaryOfEmployees()  {
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

        List<String> expected = List.of(
                "Donald Trump", "Hans Herzog", "Hans Mid", "Joe Biden", "Hans Low"
        );

        assertEquals(expected, topTen);
    }
}
