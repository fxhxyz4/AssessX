package com.assessx.assessx.utils;

import com.assessx.assessx.dto.SignUpRequest;
import java.util.HashMap;
import java.util.Map;

public class SignUpValidator {

    public Map<String, String> validate(SignUpRequest request) {
        Map<String, String> errors = new HashMap<>();

        if (request.firstName().isBlank()) {
            errors.put("firstName", "Required");
        }

        if (request.lastName().isBlank()) {
            errors.put("lastName", "Required");
        }

        if (!request.groupId().matches("\\d{3}")) {
            errors.put("groupId", "Must be 3 digits");
        }

        if (!request.email().matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-z]{2,}$")) {
            errors.put("email", "Invalid email");
        }

        return errors;
    }
}
