package com.reliaquest.api.controller;

import com.reliaquest.api.model.EmployeeDTO;
import com.reliaquest.api.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeController implements IEmployeeController {

    private final WebClient webServer = WebClient.create("http://localhost:8112/server/v1");

    @Autowired
    EmployeeService employeeService;


    @GetMapping("/test-server")
    public String testServerConnection() {

        return String.valueOf(webServer.get()
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
        List<EmployeeDTO> employees = employeeService.getAllEmployees();

        return ResponseEntity.ok(employees);
    }

    @Override
    @GetMapping("/names/{name}")
    public ResponseEntity<List<EmployeeDTO>> getEmployeesByNameSearch(@PathVariable String name) {
        List<EmployeeDTO> employees = employeeService.getEmployeesByNameSearch(name);

        return ResponseEntity.ok(employees);
    }

    @Override
    @GetMapping("/id/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable String id) {
        EmployeeDTO employee = employeeService.getEmployeeById(id);

        return ResponseEntity.ok(employee);
    }

    @Override
    @GetMapping("/highest-salary")
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        Integer highestSalary = employeeService.getHighestSalaryOfEmployees();

        return ResponseEntity.ok(highestSalary);
    }

    @Override
    @GetMapping("/top-ten")
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        List<String> topTenners = employeeService.getTopTenHighestEarningEmployeeNames();

        return ResponseEntity.ok(topTenners);
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
