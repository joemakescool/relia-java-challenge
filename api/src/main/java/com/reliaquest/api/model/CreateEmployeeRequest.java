package com.reliaquest.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** Request body for {@code POST /api/v1/employee}. */
@Data
public class CreateEmployeeRequest {

    @NotBlank
    private String name;

    @NotNull @Positive private Integer salary;

    @NotNull @Min(16)
    @Max(75)
    private Integer age;

    @NotBlank
    private String title;
}
