package com.reliaquest.api.service;

import com.reliaquest.api.model.EmployeeDTO;
import com.reliaquest.api.model.EmployeeWrapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final WebClient webServer = WebClient.create("http://localhost:8112/server/v1");

    public List<EmployeeDTO> getAllEmployees() {
        EmployeeWrapper wrapper = webServer.get()
                .uri("/employee")
                .retrieve()
                .bodyToMono(EmployeeWrapper.class)
                .log()
                .block();

        return wrapper != null && wrapper.getEmployees() != null
                ? wrapper.getEmployees()
                : Collections.emptyList();
    }

    public List<EmployeeDTO> getEmployeesByNameSearch(String name) {
        List<EmployeeDTO> employeeList = this.getAllEmployees();

        return employeeList.stream()
                .filter(emp -> emp.getEmployee_name().toLowerCase().contains(name.toLowerCase())).toList();
    }

    public EmployeeDTO getEmployeeById(String id) {
        EmployeeWrapper wrapper = webServer.get()
                .uri("/employee/{id}", id)
                .retrieve()
                .bodyToMono(EmployeeWrapper.class)
                .log()
                .block();

        return wrapper != null && wrapper.getEmployees() != null
                ? wrapper.getEmployees().get(0)
                : null;
    }

    public Integer getHighestSalaryOfEmployees() {
        List<EmployeeDTO> employeeList = this.getAllEmployees();

        return employeeList.stream()
                .mapToInt(emp -> Integer.parseInt(emp.getEmployee_salary()))
                .max()
                .orElseThrow(() -> new RuntimeException("Employee list is empty :("));
    }

    public List<String> getTopTenHighestEarningEmployeeNames() {
        List<EmployeeDTO> employeeList = this.getAllEmployees();

        return employeeList.stream()
                .sorted((e1, e2) -> Integer.compare(
                        Integer.parseInt(e2.getEmployee_salary()),
                        Integer.parseInt(e1.getEmployee_salary()))
                )
                .limit(10)
                .map(EmployeeDTO::getEmployee_name)
                .collect(Collectors.toList());
    }
}
