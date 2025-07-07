package com.reliaquest.api;

import com.reliaquest.api.model.EmployeeDTO;
import com.reliaquest.api.model.EmployeeWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    @Test
    void testGetAllEmployees() throws Exception {
        // Arrange
        EmployeeDTO emp1 = new EmployeeDTO();
        emp1.setId("1");
        emp1.setEmployee_name("Alice");
        emp1.setEmployee_salary("100");
        emp1.setEmployee_age("3");
        emp1.setEmployee_title("testing data");
        emp1.setEmployee_email("test_email@gmail.com");

        EmployeeDTO emp2 = new EmployeeDTO();
        emp2.setId("2");
        emp2.setEmployee_name("Bob");
        emp2.setEmployee_salary("100");
        emp2.setEmployee_age("3");
        emp2.setEmployee_title("testing data");
        emp2.setEmployee_email("test_email@gmail.com");

        EmployeeWrapper wrapper = new EmployeeWrapper();
        wrapper.setEmployees(Arrays.asList(emp1, emp2));

        // Act
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(EmployeeWrapper.class)).thenReturn(Flux.just(wrapper));

        // Assert
        mockMvc.perform(get("/api/v1/employee/all-employees"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("4e144fd5-4b90-4715-86d0-37b098cdc6ef"))
                .andExpect(jsonPath("$[0].employee_name").value("Farrah Wehner"))
                .andExpect(jsonPath("$[1].id").value("30935567-2075-40cc-afd8-6b43da3a93aa"))
                .andExpect(jsonPath("$[1].employee_name").value("Boyd Stark"));
    }
}
