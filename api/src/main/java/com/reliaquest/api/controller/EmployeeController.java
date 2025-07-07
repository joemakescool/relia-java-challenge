package com.reliaquest.api.controller;

import com.reliaquest.api.model.EmployeeDTO;
import com.reliaquest.api.model.EmployeeWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeController implements IEmployeeController {

    private final WebClient webClient = WebClient.create("http://localhost:8112/server/v1");


    @GetMapping("/test-server")
    public String testServerConnection() {

        return String.valueOf(webClient.get()
                .uri("/employee/test")
                .retrieve()
                .bodyToFlux(String.class)  // get Flux<EmployeeDTO>
                .collectList()  // convert to Mono<List<EmployeeDTO>>
                .block());
    }

    @GetMapping("/test")
    public String testMeOut() {
        return "Here is a test :!";
    }

    @Override
    @GetMapping("/all-employees")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        EmployeeWrapper wrapper = webClient.get()
                .uri("/employee")
                .retrieve()
                .bodyToMono(EmployeeWrapper.class)
                .log()
                .block();

        List<EmployeeDTO> employees = wrapper != null && wrapper.getEmployees() != null
                ? wrapper.getEmployees()
                : Collections.emptyList();

        return ResponseEntity.ok(employees);
    }

    @Override
    public ResponseEntity<List> getEmployeesByNameSearch(String searchString) {
        return null;
    }

    @Override
    public ResponseEntity getEmployeeById(String id) {
        return null;
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        return null;
    }

    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        return null;
    }

    @Override
    public ResponseEntity createEmployee(Object employeeInput) {
        return null;
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        return null;
    }
}
