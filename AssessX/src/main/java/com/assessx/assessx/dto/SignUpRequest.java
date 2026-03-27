package com.assessx.assessx.dto;

public record SignUpRequest(
    String firstName,
    String lastName,
    String groupId,
    String email
) {}
