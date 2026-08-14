package com.moveo.aicryptoadvisor.dto.response;

import java.util.Map;

public record ErrorResponse(String error, String message, Map<String, String> fieldErrors) {
}
