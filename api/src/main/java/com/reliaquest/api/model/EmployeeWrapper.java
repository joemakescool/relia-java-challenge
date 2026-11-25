package com.reliaquest.api.model;

/**
 * Mirrors the mock server's {@code {"data": ..., "status": "..."}} envelope.
 * Parameterized so the same type covers both the list and single-employee response shapes.
 */
public record EmployeeWrapper<T>(T data, String status) {}
