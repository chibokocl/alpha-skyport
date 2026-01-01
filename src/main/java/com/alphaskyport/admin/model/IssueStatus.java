package com.alphaskyport.admin.model;

public enum IssueStatus {
    OPEN("open"),
    INVESTIGATING("investigating"),
    RESOLVED("resolved"),
    CLOSED("closed");

    private final String value;

    IssueStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
