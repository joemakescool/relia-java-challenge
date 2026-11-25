package com.reliaquest.api.service;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.NoEmployeesFoundException;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.EmployeeDTO;
import com.reliaquest.api.model.EmployeeWrapper;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final WebClient employeeWebClient;

    public List<EmployeeDTO> getAllEmployees() {
        EmployeeWrapper<List<EmployeeDTO>> response = employeeWebClient
                .get()
                .uri("/employee")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<EmployeeWrapper<List<EmployeeDTO>>>() {})
                .block();

        List<EmployeeDTO> employees =
                response != null && response.data() != null ? response.data() : Collections.emptyList();

        log.debug("Fetched {} employees from downstream", employees.size());
        return employees;
    }

    public List<EmployeeDTO> getEmployeesByNameSearch(String name) {
        List<EmployeeDTO> matches = getAllEmployees().stream()
                .filter(employee -> employee.getEmployeeName().toLowerCase().contains(name.toLowerCase()))
                .toList();

        log.debug("Found {} employees matching name search '{}'", matches.size(), name);
        return matches;
    }

    public EmployeeDTO getEmployeeById(String id) {
        EmployeeWrapper<EmployeeDTO> response;
        try {
            response = employeeWebClient
                    .get()
                    .uri("/employee/{id}", id)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<EmployeeWrapper<EmployeeDTO>>() {})
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            throw new EmployeeNotFoundException(id);
        }

        EmployeeDTO employee = Optional.ofNullable(response)
                .map(EmployeeWrapper::data)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        log.debug("Fetched employee {}", id);
        return employee;
    }

    public Integer getHighestSalaryOfEmployees() {
        return getAllEmployees().stream()
                .mapToInt(EmployeeDTO::getEmployeeSalary)
                .max()
                .orElseThrow(NoEmployeesFoundException::new);
    }

    public List<String> getTopTenHighestEarningEmployeeNames() {
        return getAllEmployees().stream()
                .sorted(Comparator.comparing(EmployeeDTO::getEmployeeSalary).reversed())
                .limit(10)
                .map(EmployeeDTO::getEmployeeName)
                .toList();
    }

    public EmployeeDTO createEmployee(CreateEmployeeRequest request) {
        // TODO: CreateEmployeeRequest is sent downstream as-is because its fields (name/salary/age/title)
        // happen to match the mock server's CreateMockEmployeeInput exactly. If either shape needs to
        // diverge, add an explicit mapping step here instead of relying on that coincidence.
        // Deliberately not retried, including for the mock server's random rate-limiting: this call either
        // succeeds or the failure propagates to the caller, who can retry the whole request themselves.
        // Retrying automatically here risks creating the employee twice if an earlier attempt actually
        // succeeded server-side but its response was lost.
        EmployeeWrapper<EmployeeDTO> response = employeeWebClient
                .post()
                .uri("/employee")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<EmployeeWrapper<EmployeeDTO>>() {})
                .block();

        EmployeeDTO employee = Optional.ofNullable(response)
                .map(EmployeeWrapper::data)
                .orElseThrow(() -> new IllegalStateException("Employee creation did not return a result"));

        log.debug("Created employee {}", employee.getId());
        return employee;
    }

    public String deleteEmployeeById(String id) {
        EmployeeDTO employee = getEmployeeById(id);

        // Deliberately not retried, same reasoning as createEmployee: let the failure propagate and have
        // the caller retry the whole request. The risk here would've been different (deleting an
        // already-deleted name returns `false` rather than erroring, so a retry after a successful-but-lost
        // first response would incorrectly report failure) but the fix is the same - don't retry.
        EmployeeWrapper<Boolean> response = employeeWebClient
                .method(HttpMethod.DELETE)
                .uri("/employee")
                .bodyValue(new DeleteEmployeeRequest(employee.getEmployeeName()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<EmployeeWrapper<Boolean>>() {})
                .block();

        Optional.ofNullable(response)
                .map(EmployeeWrapper::data)
                .filter(Boolean::booleanValue)
                .orElseThrow(
                        () -> new IllegalStateException("Employee with id " + id + " (" + employee.getEmployeeName()
                                + ") was not deleted; they may have already been removed or renamed"));

        log.debug("Deleted employee {} ({})", id, employee.getEmployeeName());
        return employee.getEmployeeName();
    }

    private record DeleteEmployeeRequest(String name) {}
}
