package com.reliaquest.api.exception;

public class NoEmployeesFoundException extends RuntimeException {
    public NoEmployeesFoundException() {
        super("No employees found; cannot compute highest salary");
    }
}
