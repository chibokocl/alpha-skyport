package com.alphaskyport.admin.model;

public enum IssueSeverity {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical");

    private final String value;

    IssueSeverity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
