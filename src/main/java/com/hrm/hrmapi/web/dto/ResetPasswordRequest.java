package com.hrm.hrmapi.web.dto;

public record ResetPasswordRequest(String token, String newPassword) {}
