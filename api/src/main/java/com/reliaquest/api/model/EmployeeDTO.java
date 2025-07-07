package com.reliaquest.api.model;

import lombok.Data;

@Data
public class EmployeeDTO {
    private String id;
    private String employee_name;
    private String employee_salary;
    private String employee_age;
    private String employee_title;
    private String employee_email;
}
