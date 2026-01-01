package com.alphaskyport.admin.model;

public enum PaymentStatus {
    PENDING("pending"),
    COMPLETED("completed"),
    FAILED("failed"),
    REFUNDED("refunded");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
