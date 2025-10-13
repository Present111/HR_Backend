package com.hrm.hrmapi.web.dto;

public record ChangePasswordRequest(String oldPassword, String newPassword) {}
